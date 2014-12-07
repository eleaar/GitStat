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

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by Daniel on 2014-12-05.
 */
class GitHubServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  import logic.GitHubService._
  import logic.GitHubV3Format._

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(100, Millis))
  implicit val context = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  "The github service search" must {

    "parse the JSON answer" in {
      // given
      val name = "test"
      val url = searchUrl(name)
      val repositoriesInfo = Seq(RepositoryInfo("name1", "description1"), RepositoryInfo("name2", "description2"))
      val ws = new MockWS({
        case (GET, `url`) => Action {
          Ok(Json.obj("items" -> Json.toJson(repositoriesInfo)))
        }
      })
      val service = new GitHubService(ws)

      // when
      val futureResult = service.search(name)

      // then
      whenReady(futureResult) { result =>
        result must contain theSameElementsAs repositoriesInfo
      }
    }

    "transform null description into empty string" in {
      // given
      val name = "test"
      val url = searchUrl(name)
      val repositoryInfo = RepositoryInfo("name1", "")
      val nullDescriptionJson = JsObject(Seq(
        "full_name" -> JsString(repositoryInfo.full_name),
        "description" -> JsNull
      ))
      val ws = new MockWS({
        case (GET, `url`) => Action {
          Ok(Json.obj("items" -> JsArray(Seq(nullDescriptionJson))))
        }
      })
      val service = new GitHubService(ws)

      // when
      val futureResult = service.search(name)

      // then
      whenReady(futureResult) { result =>
        result must contain theSameElementsAs Seq(repositoryInfo)
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
      val url = commitsUrl(user, repo)
      val commitsInfo = Seq.tabulate(3)(i => CommitInfo(s"sha$i", DateTime.now, CommitAuthor(s"name$i", i.toLong)))
      val ws = new MockWS({
        case (GET, `url`) => Action {
          Ok(Json.toJson(commitsInfo))
        }
      })
      val service = new GitHubService(ws)

      // when
      val futureResult = service.stats(user, repo)

      // then
      whenReady(futureResult) { result =>
        result must contain theSameElementsAs commitsInfo
      }
    }

    "fail with RateExceeded when the rate limit is exceeded" in rateLimitTest {
      service => service.stats("user","repo")
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
