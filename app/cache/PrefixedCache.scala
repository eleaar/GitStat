package cache

import play.api.Application
import play.api.cache.Cache

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
 * Created by Daniel on 2014-12-17.
 */
sealed trait PrefixedCache {
  def get(key: String): Option[Any]

  def getAs[T](key: String)(implicit ct: ClassTag[T]): Option[T]

  def set(key: String, value: Any, expiration: Duration)

  def at(key: String): PrefixedCache
}

case class LocalCache(cache: PrefixedCache, prefix: String) extends PrefixedCache {
  def get(key: String) = cache.get(s"$prefix.$key")

  def getAs[T](key: String)(implicit ct: ClassTag[T]) = cache.getAs(s"$prefix.$key")

  def set(key: String, value: Any, expiration: Duration) = cache.set(s"$prefix.$key", value, expiration)

  def at(key: String) = new LocalCache(this, s"$prefix.$key")
}

case class RootWrapper(implicit val app: Application) extends PrefixedCache {
  def get(key: String) = Cache.get(key)

  def getAs[T](key: String)(implicit ct: ClassTag[T]) = Cache.getAs(key)

  def set(key: String, value: Any, expiration: Duration) = Cache.set(key, value, expiration)

  def at(key: String) = new LocalCache(this, key)
}
