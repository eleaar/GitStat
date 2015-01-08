import play.api.mvc.ActionBuilder
import play.mvc.Action

/**
 * Created by Daniel on 2015-01-08.
 */
package object atomic {

    implicit class ActionWithAtomic(action: ActionBuilder[Any]) extends AnyRef {
      /**
       * Merges the given future with possible previous futures under the same key.
       * If no future is currently registered under this key, the given future is registered and returned.
       * If there already is a future registered under this key and it is not yet completed, it is returned.
       * When the register future completes, it is unregistered and a subsequent call will register the next future.
       */
      def atomic(key: String) = AtomicAction.atomic(key)
    }
}
