package logic

import play.api.http.Status._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Configuration, Logger}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


/**
 * Created by Daniel on 2014-12-05.
 */
object GitHubServiceImpl {

  val github = "https://api.github.com"

  def searchUrl(name: String) = github + s"/search/repositories?per_page=100"

  def userRepositoriesUrl(user: String) = github + s"/users/$user/repos"

  def contributorsUrl(user: String, repo: String) = github + s"/repos/$user/$repo/contributors"

  def commitsUrl(user: String, repo: String) = github + s"/repos/$user/$repo/commits?per_page=100"

}

class GitHubServiceImpl(client: WSClient, config: Configuration) extends GitHubService {

  private val log = Logger(this.getClass())

  val clientId = config.getString("github.id").getOrElse("")
  val clientSecret = config.getString("github.secret").getOrElse("")

  import logic.GitHubServiceImpl._
  import logic.GitHubV3Format._

  def search(name: String)(implicit context: ExecutionContext) = queryGithub(searchUrl(name), etag = None, "q" -> name) {
    json => (json \ "items").validate[Seq[RepositoryInfo]]
  }

  def userRepositories(user: String, etag: Option[String] = None)(implicit context: ExecutionContext) = queryGithub(userRepositoriesUrl(user), etag) {
    json => json.validate[Seq[RepositoryInfo]]
  }

  def contributors(user: String, repo: String, etag: Option[String] = None)(implicit context: ExecutionContext) = queryGithub(contributorsUrl(user, repo), etag) {
    json => json.validate[Seq[Contributor]]
  }

  def commits(user: String, repo: String, etag: Option[String] = None)(implicit context: ExecutionContext) = queryGithub(commitsUrl(user, repo), etag) {
    json => json.validate[Seq[CommitInfo]]
  }

  private def queryGithub[T](queryUrl: String, etag: Option[String], queryStrings: (String, String)*)
                            (parseResponse: (JsValue) => JsResult[T])
                            (implicit context: ExecutionContext): Future[GitHubResponse[T]] = {
    val rawQuery = client.url(queryUrl)
    val authenticationParameters = mutable.Seq("client_id" -> clientId, "client_secret" -> clientSecret)
    val parameters = authenticationParameters ++ queryStrings
    val queryWithParameters = rawQuery.withQueryString(parameters: _*)
    val queryWithEtag = etag match {
      case None => queryWithParameters
      case Some(value) => queryWithParameters.withHeaders("If-None-Match" -> s"$value")
    }
    queryWithEtag.get()
      .map {
      case response if response.status == OK =>
        parseResponse(response.json) match {
          case JsSuccess(result, _) =>
            log.debug(s"Querying $queryUrl successfull: $result")
            Data[T](result, response.header("ETag"))
          case e: JsError =>
            log.error(s"Could not validate response from $queryUrl. \n Response: ${Json.stringify(response.json)}. \n Errors: ${Json.stringify(JsError.toFlatJson(e))}")
            throw new Exception("Response validation failed")
        }
      case response if response.status == NOT_MODIFIED =>
        log.debug(s"Querying $queryUrl: resource not modified")
        NotModified
      case response if isRepositoryEmpty(response) =>
        log.debug(s"Querying $queryUrl: repository empty")
        Empty
      case response if response.status == NOT_FOUND =>
        log.debug(s"Querying $queryUrl: resource not found")
        NotFound
      case response if isRateExceeded(response) =>
        log.warn(s"Exceeding github rates when querying $queryUrl")
        RateExceeded(getResetTime(response))
      case x =>
        log.error(s"Unexpected response when querying $queryUrl: ${x.allHeaders}, ${x.body}")
        throw new Exception("Unexpected response type")
    }
  }

  def isRepositoryEmpty(response: WSResponse) = response.status == NO_CONTENT || response.status == CONFLICT

  private def isRateExceeded(response: WSResponse) =
    response.status == FORBIDDEN && response.header("X-RateLimit-Remaining").map("0".equals).getOrElse(false)

  private def getResetTime(response: WSResponse) = response.header("X-RateLimit-Reset").get.toLong


}



