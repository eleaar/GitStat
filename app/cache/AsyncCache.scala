package cache

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import cache.AsyncCacheActor.Task
import cache.AsyncCacheResourceActor.Reply
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._

import scala.concurrent.Future

/**
 * Created by Daniel on 2014-12-16.
 */
object AsyncCache {

  lazy val asyncCache = new AsyncCache(Akka.system)

  def mergeBy[T](key: String)(body: => Future[T]) = asyncCache.mergeBy(key)(body)
}

class AsyncCache(system: ActorSystem) {

  val cache = system.actorOf(Props[AsyncCacheActor])

  import system.dispatcher
  implicit val cacheTimeout = Timeout(100 millis)

  def mergeBy[T](key: String)(body: => Future[T]) = {
    // TODO escape keys
    val task = () => body
    val answer = cache ? Task(key, task)

    answer flatMap {
      case Reply(f) => f.asInstanceOf[Future[T]]
    }
  }
}



