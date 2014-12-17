package controllers

import cache.{AsyncCached, PrefixedCache}
import logic.Analytics.UserActivity
import logic.GitHubV3Format._
import logic.{Analytics, GitHubService, GitHubServiceImpl, GitHubV3Format}
import org.joda.time.{DateTime, DateTimeZone, Seconds}
import play.api.Play
import play.api.Play.current
import play.api.cache.Cached
import play.api.libs.ws.WS
import play.api.mvc.{Results, _}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

object Application extends Controller {

  val cacheExpiration = 1 hour

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }

  def index = Cached("index") {
    Action {
      Ok(views.html.index())
    }
  }

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  val gitHubService: GitHubService = new GitHubServiceImpl(WS.client, Play.current.configuration)

  def search(name: String) = Action.async {
    AsyncCached("search").mergeRequestsBy(name) {
      if (name.trim.isEmpty) {
        Future.successful(Redirect(controllers.routes.Application.index()))
      } else {
        val futureData = gitHubService.search(name.trim)
        futureData map handleResult(name) {
          case Data(repositories, _) =>
            Ok(views.html.search(name, repositories))
        }
      }
    }
  }

  def userRepositories(user: String) = Action.async {
    AsyncCached("userRepos").mergeCacheRequestsBy(user) {
      cache =>
        val result = tryWithCache(cache)(gitHubService.userRepositories(user, _))
        result map handleResult(user) {
          case Data(repositories, _) =>
            Ok(views.html.user(user, repositories))
        }
    }
  }

  implicit val zone = DateTimeZone.getDefault()
  implicit val contributorsOrdering = Ordering.by[Contributor, String](_.login.toLowerCase)
  implicit val descendingCommitCount = Ordering.by[UserActivity, Int](_._2.size).reverse


  def stats(user: String, repo: String) = Action.async {
    AsyncCached("stats").mergeCacheRequestsBy(s"$user.$repo") {
      cache =>
        val contributorsResponseF = tryWithCache(cache.at("contributors"))(gitHubService.contributors(user, repo, _))
        val commitsResponseF = tryWithCache(cache.at("commits")) {
          gitHubService.commits(user, repo, _).map[GitHubResponse[Seq[UserActivity]]] {
            case Data(commits, etag) =>
              val userActivity = Analytics.commitsPerUser(commits)
              Data(userActivity, etag)
            case x => x.asInstanceOf[GitHubResponse[Seq[UserActivity]]]
          }
        }

        for (contributorsResponse <- contributorsResponseF;
             commitsResponse <- commitsResponseF) yield {
          (contributorsResponse, commitsResponse) match {
            case (Data(contributors, _), Data(commits, _)) =>
              Ok(views.html.stats(s"$user/$repo", contributors.sorted, commits))
            case (x, y) =>
              val handle = handleDefaultsFor(s"$user/$repo").lift
              handle(x).orElse(handle(y)).get
          }
        }
    }
  }

  def tryWithCache[T](cache: PrefixedCache)(body: (Option[String]) => Future[GitHubResponse[T]])(implicit ct: ClassTag[T]): Future[GitHubResponse[T]] = {
    val dataKey = "data"
    val etagKey = "etag"
    val cacheExpiration = 1 hour

    val cachedData = cache.getAs[T](dataKey)
    val etag = cachedData.flatMap(x => cache.getAs[String](etagKey))

    val result = body(etag)
    result.map {
      case d@Data(newData, newEtag) =>
        cache.set(dataKey, newData, cacheExpiration)
        newEtag.foreach(x => cache.set(etagKey, x, cacheExpiration))
        d
      case GitHubV3Format.NotModified =>
        // We can only receive this if we previously had some data and etag cached, so we're safe to get
        Data(cachedData.get, etag)
      case x => x
    }
  }

  def handleResult[T](resource: String)(handler: PartialFunction[GitHubResponse[T], Result]) = handler orElse notModified orElse handleDefaultsFor(resource)

  def notModified[T]: PartialFunction[GitHubResponse[T], Result] = {
    case GitHubV3Format.NotModified => Results.NotModified
  }

  def handleDefaultsFor[T](resource: String): PartialFunction[GitHubResponse[T], Result] = {
    case GitHubV3Format.RateExceeded(time) =>
      val seconds = Seconds.secondsBetween(DateTime.now, new DateTime(time * 1000)).getSeconds
      Results.Forbidden(views.html.error(s"GitHub access rate exceeded", s"We currently have too many requests. Please try again in $seconds "))
    case GitHubV3Format.NotFound => Results.NotFound(views.html.error(s"$resource not found", s"Sorry, couldn't find $resource. Did you spell it right?"))
    case GitHubV3Format.Empty => Results.NotFound(views.html.error(s"$resource is empty", s"Sorry, there is no data for $resource"))
  }
}
