package atomic

import java.util.concurrent.{ConcurrentMap, ConcurrentHashMap}

import atomic.AtomicMerger.AtomicContext
import play.api.mvc.{Result, Action}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Daniel on 2015-01-07.
 */
object AtomicAction {

  lazy val merger = AtomicMerger.newMapMerger

  def atomic(key: String) = new AtomicAction(key, merger)
}

class AtomicAction(key: String, merger: AtomicMerger) {
  def apply(body: => Future[Result])(implicit e: ExecutionContext) = Action.async(merger.merge(key, body))
  def apply(body: (AtomicContext) => Future[Result])(implicit e: ExecutionContext) = Action.async(merger.merge(key, body))
}




