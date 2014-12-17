package base

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

/**
 * Created by Daniel on 2014-12-16.
 */
abstract class ActorSpecs(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with MustMatchers with BeforeAndAfterAll with ScalaFutures {

}
