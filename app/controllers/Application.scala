package controllers

import cache.AsyncCache
import logic.GitHubV3Format.{RepositoryInfo, Contributor, Data, GitHubResponse}
import logic.{Analytics, GitHubService, GitHubServiceImpl, GitHubV3Format}
import org.joda.time.{DateTime, DateTimeZone, Seconds}
import play.api.Play
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.mvc.{Results, _}

import scala.concurrent.Future
import scala.concurrent.duration._

object Application extends Controller {

  val cacheExpiration = 1 hour

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }

  def index = Action {
    Ok(views.html.index())
  }

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  val gitHubService: GitHubService = new GitHubServiceImpl(WS.client, Play.current.configuration)

  def search(name: String) = Action.async {
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

  def userRepositories(user: String) = Action.async {
    AsyncCache.mergeCacheBy(user) {
      cache =>
        val cachedData = cache.getAs[Seq[RepositoryInfo]]("data")
        val etag = cachedData.flatMap(x => cache.getAs[String]("etag"))

        val futureData = gitHubService.userRepositories(user, etag)
        futureData map {
          case d @ Data(newData, newEtag) =>
            cache.set("data", newData, cacheExpiration)
            cache.set("etag", newEtag, cacheExpiration)
            d
          case NotModified =>
            // We can only receive this if we previously had some data and etag cached, so we're safe to get
            Data(cachedData.get, etag)
          case x => x
        } map handleResult(user) {
            case Data(repositories, _) =>
              Ok(views.html.user(user, repositories))
          }
    }
  }


  def stats(user: String, repo: String) = Action.async {
    implicit val zone = DateTimeZone.getDefault()
    implicit val contributorsOrdering = Ordering.by[Contributor, String](_.login.toLowerCase)

    val contributorsResponseF = gitHubService.contributors(user, repo)
    val commitsResponseF = gitHubService.commits(user, repo)

    for (contributorsResponse <- contributorsResponseF;
         commitsResponse <- commitsResponseF) yield {
      (contributorsResponse, commitsResponse) match {
        case (Data(contributors, _), Data(commits, _)) =>
          val userActivity = Analytics.commitsPerUser(commits)
          val userActivity2 = Analytics.commitsPerUser2(commits)
          val dateActivity = Analytics.commitsPerDate(commits)
          Ok(views.html.stats(s"$user/$repo", contributors.sorted, userActivity, userActivity2, dateActivity))
        case (NotModified, NotModified) =>
          Results.NotModified
        case (x, y) =>
          val handle = handleDefaultsFor(s"$user/$repo").lift
          handle(x).orElse(handle(y)).get
      }
    }
  }

  def handleResult[T](resource: String)(handler: PartialFunction[GitHubResponse[T], Result]) = handler orElse notModified orElse handleDefaultsFor(resource)

  def notModified[T]: PartialFunction[GitHubResponse[T], Result] = {
    case NotModified => Results.NotModified
  }

  def handleDefaultsFor[T](resource: String): PartialFunction[GitHubResponse[T], Result] = {
    case GitHubV3Format.RateExceeded(time) =>
      val seconds = Seconds.secondsBetween(DateTime.now, new DateTime(time * 1000)).getSeconds
      Results.Forbidden(views.html.error(s"GitHub access rate exceeded", s"We currently have too many requests. Please try again in $seconds "))
    case GitHubV3Format.NotFound => Results.NotFound(views.html.error(s"$resource not found", s"Sorry, couldn't find $resource. Did you spell it right?"))
    case GitHubV3Format.Empty => Results.NotFound(views.html.error(s"$resource is empty", s"Sorry, there is no data for $resource"))
  }
}
