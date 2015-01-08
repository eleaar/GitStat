package atomic

import java.util.concurrent.ConcurrentHashMap

import atomic.AtomicMerger.AtomicContext
import atomic.map.ConcurrentMapAtomicMerger
import atomic.map.ConcurrentMapAtomicMerger.Memoizer

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Daniel on 2015-01-07.
 */
object AtomicMerger {

  case class AtomicContext(path: String)

  def newMapMerger = new ConcurrentMapAtomicMerger(new ConcurrentHashMap[String, Memoizer]())
}

trait AtomicMerger {

  /**
   * Merges the given future with possible previous futures under the same key.
   * If no future is currently registered under this key, the given future is registered and returned.
   * If there already is a future registered under this key and it is not yet completed, it is returned.
   * When the register future completes, it is unregistered and a subsequent call will register the next future.
   */
  def merge[T](key: String, value: => Future[T])(implicit e: ExecutionContext): Future[T]

  /**
   * Merges the given future with possible previous futures under the same key.
   * If no future is currently registered under this key, the given future is registered and returned.
   * If there already is a future registered under this key and it is not yet completed, it is returned.
   * When the register future completes, it is unregistered and a subsequent call will register the next future.
   * <br /><br />
   * The atomic context object is thus guaranteed not to be shared by two or more futures for a given key.
   *
   */
  def merge[T](key: String, value: (AtomicContext) => Future[T])(implicit e: ExecutionContext): Future[T] = merge(key, value(AtomicContext(key)))
}
