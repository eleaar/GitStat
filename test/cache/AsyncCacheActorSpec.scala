package cache

import cache.AsyncCacheActor._
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestActorRef, TestFSMRef, TestKit}
import base.ActorSpecs
import cache.AsyncCacheResourceActor.Reply

import scala.concurrent.Promise

/**
 * Created by Daniel on 2014-12-16.
 */
class AsyncCacheActorSpec (_system: ActorSystem) extends ActorSpecs(_system) {
  def this() = this(ActorSystem("AsyncCacheActorSpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An AsyncCache actor" must {

    "create new actor at first request" in {
      // given
      val probe = TestProbe()
      val p = Promise[Unit]()
      val f = p.future
      val key = "key"
      val task = () => f
      val cache = TestActorRef[AsyncCacheActor]

      // when
      probe.send(cache, Task(key, task))

      // then
      probe.expectMsg(Reply(f))
    }

    "reuse existing actor" in {
      // given
      val probe = TestProbe()
      val p = Promise[Unit]()
      val f = p.future
      val key = "key"
      val task = () => f
      val cache = TestActorRef[AsyncCacheActor]
      cache ! Task(key, task)

      // when
      probe.send(cache, Task(key, task))

      // then
      probe.expectMsg(Reply(f))
    }

    "distinguish keys" in {
      // given
      val probe1 = TestProbe()
      val p1 = Promise[Unit]()
      val f1 = p1.future
      val key1 = "key1"
      val task1 = () => f1

      val probe2 = TestProbe()
      val p2 = Promise[Unit]()
      val f2 = p2.future
      val key2 = "key2"
      val task2 = () => f2

      val cache = TestActorRef[AsyncCacheActor]

      // when
      probe1.send(cache, Task(key1, task1))
      probe2.send(cache, Task(key2, task2))

      // then
      probe1.expectMsg(Reply(f1))
      probe2.expectMsg(Reply(f2))
    }
  }
}
