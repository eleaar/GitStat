package controllers

import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.Results._
import atomic._

/**
 * Created by Daniel on 2015-01-22.
 */
object LoadTest {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def default(time: Long) = Action.async {
    Future {
      Thread.sleep(time)
      Ok(s"Slept $time miliseconds")
    }
  }

  def atomic(time: Long) = Action.atomic("atomic") {
    Future {
      Thread.sleep(time)
      Ok(s"Slept $time miliseconds")
    }
  }

}
