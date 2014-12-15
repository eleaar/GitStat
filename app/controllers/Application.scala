package controllers

import java.net.ConnectException

import com.github.nscala_time.time.Imports._
import logic.GitHubV3Format.Contributor
import logic.{Analytics, GitHubService}
import logic.GitHubService._
import org.joda.time.{DateTimeZone, DateTime, Seconds}
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.Future

object Application extends Controller {

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }

  def index = Action {
    Ok(views.html.index())
  }

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  val gitHubService = new GitHubService(WS.client)

  def search(name: String) = Action.async {
    if (name.trim.isEmpty) {
      Future.successful(Redirect(controllers.routes.Application.index()))
    } else {
      gitHubService.search(name.trim).map {
        case Right(repositories) =>
          Ok(views.html.search(name, repositories))
      } recover (handleErrorsFor("name"))
    }
  }

  def userRepositories(user: String) = Action.async {
    gitHubService.userRepositories(user).map {
      case Right(repositories) =>
        Ok(views.html.user(user, repositories))
    } recover (handleErrorsFor("name"))
  }


  def stats(user: String, repo: String) = Action.async {
    implicit val zone = DateTimeZone.getDefault()
    implicit val contributorsOrdering = Ordering.by[Contributor, String](_.login.toLowerCase)

    val contributorsF = gitHubService.contributors(user, repo)
    val commitsF = gitHubService.commits(user, repo)
    val userActivityF = commitsF.map {case (Right(x)) => Analytics.commitsPerUser(x) }
    val userActivity2F = commitsF.map{case (Right(x)) => Analytics.commitsPerUser2(x) }
    val dateActivityF = commitsF.map{case (Right(x)) => Analytics.commitsPerDate(x) }

    val response = for (
      contributors <- contributorsF;
      userActivity <- userActivityF;
      userActivity2 <- userActivity2F;
      dateActivity <- dateActivityF
    ) yield {
      Ok(views.html.stats(s"$user/$repo", contributors.right.get.sorted, userActivity, userActivity2, dateActivity))
    }

    response recover (handleErrorsFor(s"$user/$repo"))
  }

  def handleErrorsFor(resource: String): PartialFunction[Throwable, Result] = {
    case error: ConnectException =>
      ServiceUnavailable("Could not connect to Github")
//    case RateExceeded(time) =>
//      val seconds = Seconds.secondsBetween(DateTime.now, new DateTime(time * 1000)).getSeconds
//      Forbidden(s"Rate exceeded. Please try again in $seconds s")
//    case NotFound =>
//      NotFound(s"Sorry, couldn't find $resource. Did you spell it right?")
  }
}
