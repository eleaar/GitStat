package atomic

import com.google.common.util.concurrent.MoreExecutors
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Promise}

/**
 * Created by Daniel on 2014-12-16.
 */
abstract class AtomicMergerSpec extends PlaySpec with ScalaFutures {

  implicit val context = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  def createMerger: AtomicMerger

  "An AtomicMerger" must {

    "return the original future" in {
      // given
      val value = "value"
      val p = Promise[String]()
      val key = "key"

      val merger = createMerger

      // when
      val result = merger.merge(key, p.future)
      p.success(value)

      // then
      whenReady(result) {
        x => x must equal(value)
      }
    }

    "return the first future when not completed" in {
      // given
      val value1 = "value1"
      val value2 = "value2"
      val p1 = Promise[String]()
      val p2 = Promise[String]()

      val key = "key"
      val merger = createMerger

      val r1 = merger.merge(key, p1.future)

      // when
      val result = merger.merge(key, p2.future)
      p1.success(value1)
      p2.success(value2)

      // then
      whenReady(result) {
        x => x must equal(value1)
      }
    }

    "return the next future when first completed" in {
      // given
      val value1 = "value1"
      val value2 = "value2"
      val p1 = Promise[String]()
      val p2 = Promise[String]()

      val key = "key"
      val merger = createMerger

      merger.merge(key, p1.future)
      p1.success(value1)

      // when
      val result = merger.merge(key, p2.future)
      p2.success(value2)

      // then
      whenReady(result) {
        x => x must equal(value2)
      }
    }

    "distinguish keys" in {
      // given

      val value1 = "value1"
      val value2 = "value2"
      val p1 = Promise[String]()
      val p2 = Promise[String]()

      val key1 = "key1"
      val key2 = "key2"
      val merger = createMerger

      // when
      val result1 = merger.merge(key1, p1.future)
      val result2 = merger.merge(key2, p2.future)
      p1.success(value1)
      p2.success(value2)

      // then
      whenReady(result1) {
        x => x must equal(value1)
      }
      whenReady(result2) {
        x => x must equal(value2)
      }
    }
  }
}
