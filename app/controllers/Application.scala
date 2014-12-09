package controllers

import java.net.ConnectException

import com.github.nscala_time.time.Imports._
import logic.{Analytics, GitHubService}
import logic.GitHubService._
import org.joda.time.{DateTimeZone, DateTime, Seconds}
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.Future

object Application extends Controller {

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
        case repositories =>
          Ok(views.html.search(name, repositories))
      } recover {
        case error: ConnectException =>
          ServiceUnavailable("Could not connect to Github")
        case RateExceeded(time) =>
          val seconds = Seconds.secondsBetween(DateTime.now, new DateTime(time * 1000)).getSeconds
          Forbidden(s"Rate exceeded. Please try again in $seconds s")
      }
    }
  }

  def stats(user: String, repo: String) = Action.async {
    implicit val zone = DateTimeZone.getDefault()
    val contributorsF = gitHubService.contributors(user, repo)
    val commitsF = gitHubService.commits(user, repo)
    val userActivityF = commitsF.map(Analytics.commitsPerUser)
    val userActivity2F = commitsF.map(Analytics.commitsPerUser2)
    val dateActivityF = commitsF.map(Analytics.commitsPerDate)

    val response = for (
      contributors <- contributorsF;
      userActivity <- userActivityF;
      userActivity2 <- userActivity2F;
      dateActivity <- dateActivityF
    ) yield {
      Ok(views.html.stats(s"$user/$repo", contributors, userActivity, userActivity2, dateActivity))
    }

    response recover {
      case error: ConnectException =>
        ServiceUnavailable("Could not connect to Github")
      case RateExceeded(time) =>
        val seconds = Seconds.secondsBetween(DateTime.now, new DateTime(time * 1000)).getSeconds
        Forbidden(s"Rate exceeded. Please try again in $seconds s")
      case NotFoundException =>
        NotFound(s"Sorry, couldn't find $user/$repo. Did you spell it right?")
    }
  }
}
