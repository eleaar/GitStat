package logic

import java.util.concurrent.Executors

import akka.dispatch.ExecutionContexts
import com.google.common.util.concurrent.MoreExecutors
import logic.GitHubService.RepositoryInfo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.http.HttpVerbs._
import mockws.MockWS

import play.api._
import play.api.mvc.Results._
import play.api.http.Status._
import scala.concurrent.{Await, ExecutionContext}

import scala.concurrent.duration._

/**
 * Created by Daniel on 2014-12-05.
 */
class GitHubServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  import GitHubService._

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
        result must equal (RateExceeded(resetTime))
      }
    }
  }
}
