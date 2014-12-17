import play.api.{GlobalSettings, _}
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.Future

/**
 * Created by Daniel on 2014-12-17.
 */
object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error(s"An error happenned when accessing ${request.path}: $ex")
    Future.successful(InternalServerError(
        views.html.error("Error", "We're sorry. An internal error has happened and has been send to our staff.")
    ))
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.error("Not found", s"The resource you requested does not exist: (${request.path}). Did you spell it right?")
    ))
  }
}
