package logic

import com.google.common.util.concurrent.MoreExecutors
import mockws.MockWS
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.HttpVerbs._
import play.api.libs.json.{Json, _}
import play.api.mvc.Action
import play.api.mvc.Results._
import scala.Some

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Daniel on 2014-12-05.
 */
class GitHubServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  import logic.GitHubService._
  import logic.GitHubV3Format._

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(200, Millis))
  implicit val context = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  "The github service search" must {

    def search(name: String) = (s: GitHubService) => s.search(name)

    "parse the JSON answer" in {
      // given
      val name = "test"
      val repositoriesInfo = Seq.tabulate(2)(i => RepositoryInfo(s"name$i", s"description$i"))

      jsonResponseTest(
        queryUrl = searchUrl(name),
        withMethod = search(name),
        replyWith = Json.obj("items" -> Json.toJson(repositoriesInfo)),
        expectedResult = repositoriesInfo)
    }

    "transform null description into empty string" in {
      // given
      val name = "test"
      val repositoriesInfo = Seq(RepositoryInfo("name1", ""))
      val nullDescriptionJson = JsObject(Seq(
        "full_name" -> JsString(repositoriesInfo.head.full_name),
        "description" -> JsNull
      ))

      jsonResponseTest(
        queryUrl = searchUrl(name),
        withMethod = search(name),
        replyWith = Json.obj("items" -> JsArray(Seq(nullDescriptionJson))),
        expectedResult = repositoriesInfo)
    }

    "fail with RateExceeded when the rate limit is exceeded" in rateLimitTest(withMethod = search("name"))
  }

  "The github service contributors" must {
    def contributors(user: String, repo: String) = (s: GitHubService) => s.contributors(user, repo)

    "parse the JSON answer" in {
      // given
      val user = "user"
      val repo = "repo"
      val contributorsData = Seq.tabulate(2)(i => Contributor(s"name$i", i))

      jsonResponseTest(
        queryUrl = contributorsUrl(user, repo),
        withMethod = contributors(user, repo),
        replyWith = Json.toJson(contributorsData),
        expectedResult = contributorsData)
    }

    "fail with RateExceeded when the rate limit is exceeded" in rateLimitTest(withMethod = contributors("user", "repo"))

    "fail with NotFound when not found" in notFoundTest(withMethod = contributors("user", "repo"))
  }

  "The github service commits" must {

    def commits(user: String, repo: String) = (s: GitHubService) => s.commits(user, repo)

    "parse the JSON answer" in {
      // given
      val user = "user"
      val repo = "repo"
      val commitsInfo = Seq.tabulate(2)(i => CommitInfo(s"sha$i", DateTime.now, Some(Contributor(s"name$i", i))))

      jsonResponseTest(
        queryUrl = commitsUrl(user, repo),
        withMethod = commits("user", "repo"),
        replyWith = Json.toJson(commitsInfo),
        expectedResult = commitsInfo)
    }

    "handle null commiter" in {
      // given
      val user = "user"
      val repo = "repo"
      val commitsInfo = Seq(CommitInfo("sha-null", DateTime.now, None))

      jsonResponseTest(
        queryUrl = commitsUrl(user, repo),
        withMethod = commits("user", "repo"),
        replyWith = Json.toJson(commitsInfo),
        expectedResult = commitsInfo)
    }

    "fail with RateExceeded when the rate limit is exceeded" in rateLimitTest(withMethod = commits("user", "repo"))

    "fail with NotFound when not found" in notFoundTest(withMethod = commits("user", "repo"))
  }

  def jsonResponseTest[T](queryUrl: String, withMethod: (GitHubService) => Future[T], replyWith: JsValue, expectedResult: T) = {
    val ws = new MockWS({
      case (GET, `queryUrl`) => Action {
        Ok(replyWith)
      }
    })
    val service = new GitHubService(ws)

    // when
    val futureResult = withMethod(service)

    // then
    whenReady(futureResult) { result =>
      result must equal(expectedResult)
    }
  }

  def rateLimitTest(withMethod: (GitHubService) => Future[Any]) = {
    // given
    val resetTime = 12345
    val ws = new MockWS({
      case (GET, _) => Action {
        Forbidden.withHeaders("X-RateLimit-Remaining" -> "0", "X-RateLimit-Reset" -> resetTime.toString)
      }
    })
    val service = new GitHubService(ws)

    // when
    val futureResult = withMethod(service)

    // then
    whenReady(futureResult.failed) { result =>
      result must equal(RateExceeded(resetTime))
    }
  }

  def notFoundTest(withMethod: (GitHubService) => Future[Any]) = {
    val ws = new MockWS({
      case (GET, _) => Action {
        NotFound
      }
    })
    val service = new GitHubService(ws)

    // when
    val futureResult = withMethod(service)

    // then
    whenReady(futureResult.failed) { result =>
      result must equal(NotFoundException)
    }
  }
}
