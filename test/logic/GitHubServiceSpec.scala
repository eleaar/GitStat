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

    "parse the JSON answer" in {
      // given
      val name = "test"
      val repositoriesInfo = Seq.tabulate(2)(i => RepositoryInfo(s"name$i", s"description$i"))

      jsonResponseTest(
        queryUrl = searchUrl(name),
        replied = Json.obj("items" -> Json.toJson(repositoriesInfo)),
        expected = repositoriesInfo) {
        service => service.search(name)
      }
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
        replied = Json.obj("items" -> JsArray(Seq(nullDescriptionJson))),
        expected = repositoriesInfo) {
        service => service.search(name)
      }
    }

    "fail with RateExceeded when the rate limit is exceeded" in rateLimitTest {
      service => service.search("name")
    }
  }

  "The github service stats" must {

    "parse the JSON answer" in {
      // given
      val user = "user"
      val repo = "repo"
      val commitsInfo = Seq.tabulate(2)(i => CommitInfo(s"sha$i", DateTime.now, CommitAuthor(s"name$i", i.toLong)))

      jsonResponseTest(
        queryUrl = commitsUrl(user, repo),
        replied = Json.toJson(commitsInfo),
        expected = commitsInfo) {
        service => service.stats(user, repo)
      }
    }

    "fail with RateExceeded when the rate limit is exceeded" in rateLimitTest {
      service => service.stats("user", "repo")
    }
  }

  def jsonResponseTest[T](queryUrl: String, replied: JsValue, expected: T)(body: (GitHubService) => Future[T]) = {
    // given
    val ws = new MockWS({
      case (GET, `queryUrl`) => Action {
        Ok(replied)
      }
    })
    val service = new GitHubService(ws)

    // when
    val futureResult = body(service)

    // then
    whenReady(futureResult) { result =>
      result must equal(expected)
    }
  }

  def rateLimitTest(body: (GitHubService) => Future[Any]) = {
    // given
    val resetTime = 12345
    val ws = new MockWS({
      case (GET, _) => Action {
        Forbidden.withHeaders("X-RateLimit-Remaining" -> "0", "X-RateLimit-Reset" -> resetTime.toString)
      }
    })
    val service = new GitHubService(ws)

    // when
    val futureResult = body(service)

    // then
    whenReady(futureResult.failed) { result =>
      result must equal(RateExceeded(resetTime))
    }
  }
}
