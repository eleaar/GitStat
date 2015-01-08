package atomic.map

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import atomic.AtomicMerger
import atomic.map.ConcurrentMapAtomicMerger.Memoizer

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Daniel on 2015-01-07.
 */
object ConcurrentMapAtomicMerger {

  class Memoizer(op: => Future[Any]) {
    lazy val value = op
  }

  def create = new ConcurrentMapAtomicMerger(new ConcurrentHashMap[String, Memoizer]())
}

class ConcurrentMapAtomicMerger(cache: ConcurrentMap[String, Memoizer]) extends AtomicMerger {

  def merge[T](key: String, value: => Future[T])(implicit e: ExecutionContext) = {
    val memo = new Memoizer(value)
    val v = cache.putIfAbsent(key, memo) match {
      case null =>
        val f = memo.value
        f.onComplete {
          case _ => cache.remove(key, memo)
        }
        f
      case m => m.value
    }
    v.asInstanceOf[Future[T]]
  }
}


