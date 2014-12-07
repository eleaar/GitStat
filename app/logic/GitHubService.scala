package logic

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext


/**
 * Created by Daniel on 2014-12-05.
 */
object GitHubService {

  val github = "https://api.github.com"

  def searchUrl(name: String) = github + s"/search/repositories?q=$name"

  def commitsUrl(user: String, repo: String) = github + s"/repos/"

  trait GitHubException extends Exception

  case class RateExceeded(resetTime: Long) extends GitHubException

}

class GitHubService(client: WSClient) {

  val log = Logger(this.getClass())

  import logic.GitHubService._
  import logic.GitHubV3Format._

  def search(name: String)(implicit context: ExecutionContext) = {
    client.url(searchUrl(name)).get().map {
      case response if response.status == OK =>
        (response.json \ "items").validate[Seq[RepositoryInfo]] match {
          case JsSuccess(result, _) =>
            result
          case e: JsError =>
            log.error(s"Could not validate response when searching for '$name'. \n Response: ${Json.stringify(response.json)}. \n Errors: ${Json.stringify(JsError.toFlatJson(e))}")
            throw new Exception("Response validation failed")
        }
      case response if isRateExceeded(response) =>
        log.warn("Exceeding github rates")
        throw new RateExceeded(getResetTime(response))
      case x =>
        log.error(s"Unexpected response: $x")
        throw new Exception("Unexpected response type")
    }
  }

  def stats(user: String, repo: String)(implicit context: ExecutionContext) = {
    client.url(commitsUrl(user, repo)).get().map {
      case response if response.status == OK =>
        response.json.validate[Seq[CommitInfo]] match {
          case JsSuccess(result, _) =>
            result
          case e: JsError =>
            log.error(s"Could not validate response when searching for '$user/$repo' commits. \n Response: ${Json.stringify(response.json)}. \n Errors: ${Json.stringify(JsError.toFlatJson(e))}")
            throw new Exception("Response validation failed")
        }
      case response if isRateExceeded(response) =>
        log.warn("Exceeding github rates")
        throw new RateExceeded(getResetTime(response))
      case x =>
        log.error(s"Unexpected response: $x")
        throw new Exception("Unexpected response type")
    }
  }

  def isRateExceeded(response: WSResponse) =
    response.status == FORBIDDEN && response.header("X-RateLimit-Remaining").map("0".equals).getOrElse(false)

  def getResetTime(response: WSResponse) = response.header("X-RateLimit-Reset").get.toLong


}



