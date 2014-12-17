package cache

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import cache.AsyncCacheActor.Task
import cache.AsyncCacheResourceActor.Reply

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by Daniel on 2014-12-16.
 */
class AsyncCache(system: ActorSystem) {

  val cache = system.actorOf(Props[AsyncCacheActor])

  import system.dispatcher
  implicit val cacheTimeout = Timeout(500 millis)

  def mergeBy[T](key: String)(body: => Future[T]) = {
    val task = () => body
    val answer = cache ? Task(key, task)

    answer flatMap {
      case Reply(f) => f.asInstanceOf[Future[T]]
    }
  }
}



