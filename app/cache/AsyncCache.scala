package cache

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import cache.AsyncCacheActor.Task
import cache.AsyncCacheResourceActor.Reply
import play.api.Application
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Created by Daniel on 2014-12-16.
 */

case class PrefixedCache(prefix: String) {
  def get(key: String) = Cache.get(s"$prefix.$key")

  def getAs[T](key: String)(implicit ct: ClassTag[T]) = Cache.getAs(s"$prefix.$key")

  def set(key: String, value: Any, expiration: Duration) = Cache.set(s"$prefix.$key", value, expiration)
}

object AsyncCache {

  lazy val asyncCache = new AsyncCache(Akka.system)

  def mergeBy[T](key: String)(body: => Future[T]) = asyncCache.mergeBy(key)(body)
  def mergeCacheBy[T](key: String)(body: (PrefixedCache) => Future[T]) = asyncCache.mergeBy(key)(body(new PrefixedCache(key)))
}

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



