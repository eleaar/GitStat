package cache

import base.ActorSpecs
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestFSMRef, ImplicitSender, TestKit}
import cache.AsyncCacheResourceActor._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.{Promise, Future}

/**
 * Created by Daniel on 2014-12-16.
 */
class AsyncCacheResourceActorSpec(_system: ActorSystem) extends ActorSpecs(_system) {
  def this() = this(ActorSystem("AsyncCacheResourceActorSpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An AsyncCacheResource actor" must {

    "be created idle" in {
      // given
      val actor = TestFSMRef(new AsyncCacheResourceActor)

      // then
      actor.stateName must equal(Idle)
      actor.stateData must equal(None)
    }

    "start work when triggered" in {
      // given
      val p = Promise[Unit]()
      val f = p.future
      val task = () => f
      val actor = TestFSMRef(new AsyncCacheResourceActor)

      // when
      actor ! Request(task)

      // then
      actor.stateName must equal(Active)
      actor.stateData must equal(Some(f))
    }

    "switch back to idle when future completes successfully" in {
      // given
      val p = Promise[Unit]()
      val f = p.future
      val task = () => f
      val actor = TestFSMRef(new AsyncCacheResourceActor)
      actor ! Request(task)

      // when
      p.success(())

      // then
      whenReady(f) {
        x =>
          actor.stateName must equal(Idle)
          actor.stateData must equal(None)
      }
    }

    "switch back to idle when future completes unsuccessfully" in {
      // given
      val p = Promise[Unit]()
      val f = p.future
      val task = () => f
      val actor = TestFSMRef(new AsyncCacheResourceActor)
      actor ! Request(task)

      // when
      p.failure(new Exception())

      // then
      whenReady(f.failed) {
        x =>
          actor.stateName must equal(Idle)
          actor.stateData must equal(None)
      }
    }

    "reply future when started" in {
      // given
      val probe = TestProbe()
      val p = Promise[Unit]()
      val f = p.future
      val task = () => f
      val actor = TestFSMRef(new AsyncCacheResourceActor)

      // when
      probe.send(actor, Request(task))

      // then
      probe.expectMsg(Reply(f))
    }

    "reply future when already active" in {
      // given
      val probe = TestProbe()
      val p = Promise[Unit]()
      val f = p.future
      val task = () => f
      val actor = TestFSMRef(new AsyncCacheResourceActor)
      actor ! Request

      // when
      probe.send(actor, Request(task))

      // then
      probe.expectMsg(Reply(f))
    }

    "reply new future after first completition" in {
      // given
      val probe = TestProbe()
      val p1 = Promise[Unit]()
      val f1 = p1.future
      val p2 = Promise[Unit]()
      val f2 = p2.future
      val task1 = () => f1
      val task2 = () => f2
      val actor = TestFSMRef(new AsyncCacheResourceActor)

      // when
      actor ! Request(task1)
      p1.success(())
      whenReady(f1) {
        x =>
          probe.send(actor, Request(task2))

          // then
          probe.expectMsg(Reply(f2))
      }
    }

  }

}
