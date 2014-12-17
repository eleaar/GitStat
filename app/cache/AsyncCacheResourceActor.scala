package cache

import akka.actor.FSM
import cache.AsyncCacheResourceActor._

import scala.concurrent.Future

/**
 * Created by Daniel on 2014-12-16.
 */
object AsyncCacheResourceActor {

  /* State */
  sealed trait State

  case object Idle extends State

  case object Active extends State

  /* Data*/

  type Data = Option[Future[Any]]

  /* Communication */
  case class Request(task: () => Future[Any])

  case class Reply(futureResult: Future[Any])

}

// TODO: timeout on inactivity
class AsyncCacheResourceActor extends FSM[State, Data] {

  case object Done

  import context.dispatcher

  startWith(Idle, None)

  when(Idle) {
    case Event(Request(task), None) =>
      val futureResult = task()
      futureResult.onComplete {
        case x => self ! Done
      }
      goto(Active) using Some(futureResult) replying (Reply(futureResult))
  }

  when(Active) {
    case Event(Request(_), Some(futureResult)) =>
      stay replying (Reply(futureResult))

    case Event(Done, _) =>
      goto(Idle) using None
  }

  initialize()
}
