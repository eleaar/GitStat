package logic

import com.google.common.util.concurrent.MoreExecutors
import mockws.MockWS
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.HttpVerbs._
import play.api.libs.json.{Json, _}
import play.api.mvc.Action
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext

/**
 * Created by Daniel on 2014-12-05.
 */
class GitHubServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  import logic.GitHubService._

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(100, Millis))
  implicit val context = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  "The github service" must {

    //    "use github v3 api" in {
    //
    //      //      val ws = new MockWS({
    //      //        case (GET, "http://dns/url") => Action {
    //      //          Ok("http response")
    //      //        }
    //      //      })
    //      //      val wsMock = mock[WSClient]
    //      //
    //      //      val service = new GitHubService(ws)
    //
    //    }

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


    "fail with RateExceeded when the rate limit is exceeded" in {
      // given
      val name = "test"
      val url = searchUrl(name)
      val resetTime = 12345
      val ws = new MockWS({
        case (GET, `url`) => Action {
          Forbidden.withHeaders("X-RateLimit-Remaining" -> "0", "X-RateLimit-Reset" -> resetTime.toString)
        }
      })
      val service = new GitHubService(ws)

      // when
      val futureResult = service.search(name)

      // then
      whenReady(futureResult.failed) { result =>
        result must equal(RateExceeded(resetTime))
      }
    }
  }
}
