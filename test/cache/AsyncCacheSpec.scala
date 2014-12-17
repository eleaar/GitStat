package cache

import akka.actor.ActorSystem
import akka.testkit.TestKit
import base.ActorSpecs

import scala.concurrent.Promise

/**
 * Created by Daniel on 2014-12-16.
 */
class AsyncCacheSpec(_system: ActorSystem) extends ActorSpecs(_system) {
  def this() = this(ActorSystem("AsyncCacheSpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "An AsyncCache" must {

    "return the original future" in {
      // given
      val value = "value"
      val p = Promise[String]()
      val key = "key"
      val cache = new AsyncCache(system)

      // when
      val result = cache.mergeBy(key) {
        p.future
      }
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
      val cache = new AsyncCache(system)

      cache.mergeBy(key) {
        p1.future
      }

      // when
      val result = cache.mergeBy(key) {
        p2.future
      }
      p1.success(value1)
      p2.success(value2)

      // then
      whenReady(result) {
        x => x must equal(value1)
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
      val cache = new AsyncCache(system)

      // when
      val result1 = cache.mergeBy(key1) {
        p1.future
      }
      val result2 = cache.mergeBy(key2) {
        p2.future
      }
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
