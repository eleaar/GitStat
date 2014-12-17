package cache

import akka.actor.{Props, ActorRef, Actor}
import akka.pattern._
import akka.util.Timeout
import cache.AsyncCacheActor.Task
import cache.AsyncCacheResourceActor._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by Daniel on 2014-12-16.
 */
object AsyncCacheActor {

  case class Task[T](key: String, task: () => Future[T])

}


class AsyncCacheActor extends Actor {

  val resources = mutable.Map.empty[String, ActorRef]

  def receive = {
    case Task(key, task) =>
      // TODO currently leaking. Remove after inactivity
      val actor = resources.getOrElseUpdate(key, context.actorOf(Props[AsyncCacheResourceActor]))
      actor.forward(Request(task))
  }
}
