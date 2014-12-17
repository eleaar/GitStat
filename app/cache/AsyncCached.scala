package cache

import play.api.Play.current
import play.api.libs.concurrent.Akka

import scala.concurrent.Future

/**
 * Created by Daniel on 2014-12-17.
 */

object AsyncCached {

  lazy val root = new AsyncCache(Akka.system)

  def apply(prefix: String) = new AsyncCached(prefix)
}

class AsyncCached(prefix: String) {

  val localCache = RootWrapper().at(prefix)

  def mergeRequestsBy[T](key: String)(body: => Future[T]) = AsyncCached.root.mergeBy(key)(body)
  def mergeCacheRequestsBy[T](key: String)(body: (PrefixedCache) => Future[T]) = AsyncCached.root.mergeBy(key)(body(localCache.at(key)))

}
