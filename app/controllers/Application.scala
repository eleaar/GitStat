package controllers

import java.net.ConnectException

import logic.GitHubService
import logic.GitHubService._
import org.joda.time.{DateTime, Seconds}
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  val gitHubService = new GitHubService(WS.client)

  def search(name: String) = Action.async {
    gitHubService.search(name).map {
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

  def stats(user: String, repo: String) = Action.async {
    gitHubService.stats(user, repo).map {
      case commits =>
        Ok(views.html.stats(s"$user/$repo", commits))
    } recover {
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
