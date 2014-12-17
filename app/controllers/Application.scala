package controllers

import java.net.ConnectException

import com.github.nscala_time.time.Imports._
import logic.GitHubV3Format.{RepositoryInfo, GitHubResponse, Data, Contributor}
import logic.{GitHubService, GitHubV3Format, Analytics, GitHubServiceImpl}
import logic.GitHubServiceImpl._
import org.joda.time.{DateTimeZone, DateTime, Seconds}
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.mvc.Results

import scala.concurrent.Future

object Application extends Controller {

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }

  def index = Action {
    Ok(views.html.index())
  }

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  val gitHubService: GitHubService = new GitHubServiceImpl(WS.client)

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
    val futureData = gitHubService.userRepositories(user)
    futureData map handleResult(user) {
      case Data(repositories, _) =>
        Ok(views.html.user(user, repositories))
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
      Forbidden(s"Rate exceeded. Please try again in $seconds s")
    case GitHubV3Format.NotFound => Results.NotFound(s"Sorry, couldn't find $resource. Did you spell it right?")
    case GitHubV3Format.Empty => Ok(views.html.empty(resource))
  }
}
