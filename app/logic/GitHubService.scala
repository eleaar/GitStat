package logic

import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext


/**
 * Created by Daniel on 2014-12-05.
 */
object GitHubService {

  val github = "https://api.github.com"

  def searchUrl(name: String) = github + s"/search/repositories?q=$name"

  case class RepositoryInfo(full_name: String, description: String)

  implicit val repositoryInfoReads = Json.reads[RepositoryInfo]
  implicit val repositoryInfoWrites = Json.writes[RepositoryInfo]

  trait GitHubException extends Exception

  case class RateExceeded(resetTime: Long) extends GitHubException

}

class GitHubService(client: WSClient) {

  import logic.GitHubService._

  def search(name: String)(implicit context: ExecutionContext) = {
    client.url(searchUrl(name)).get().map {
      case response if response.status == OK =>
        (response.json \ "items").as[Seq[RepositoryInfo]]
      case response if isRateExceeded(response) =>
        throw new RateExceeded(getResetTime(response))
      case x =>
        throw new Exception("Unexpected response type")
    }
  }

  def isRateExceeded(response: WSResponse) =
    response.status == FORBIDDEN && response.header("X-RateLimit-Remaining").map("0".equals).getOrElse(false)

  def getResetTime(response: WSResponse) = response.header("X-RateLimit-Reset").get.toLong


}



