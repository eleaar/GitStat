package logic

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext


/**
 * Created by Daniel on 2014-12-05.
 */
object GitHubService {

  val github = "https://api.github.com"

  def searchUrl(name: String) = github + s"/search/repositories?q=$name"

  case class RepositoryInfo(full_name: String, description: String)

  implicit val repositoryInfoReads: Reads[RepositoryInfo] = (
    (JsPath \ "full_name").read[String] and
      (JsPath \ "description").readNullable[String].map(_.getOrElse(""))
    )(RepositoryInfo.apply _)
  implicit val repositoryInfoWrites = Json.writes[RepositoryInfo]

  trait GitHubException extends Exception

  case class RateExceeded(resetTime: Long) extends GitHubException

}

class GitHubService(client: WSClient) {

  val log = Logger(this.getClass())

  import logic.GitHubService._

  def search(name: String)(implicit context: ExecutionContext) = {
    client.url(searchUrl(name)).get().map {
      case response if response.status == OK =>
        (response.json \ "items").validate[Seq[RepositoryInfo]] match {
          case JsSuccess(result, _) =>
            result
          case e: JsError =>

//            log.error(s"Could not validate response when searching for '$name'")
//            errors.foreach {
//              case (path, some) =>
//                log.error(Json.prettyPrint(path.asSingleJson(response.json \ "items")))
//            }
            log.error(s"Could not validate response when searching for '$name'. \n Response: ${Json.prettyPrint(response.json)}. \n Errors: ${Json.prettyPrint(JsError.toFlatJson(e))}")
            log.error(Json.prettyPrint((response.json \ "items")(11)))
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



