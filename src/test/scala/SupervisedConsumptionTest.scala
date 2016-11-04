import akka.actor.Status.Failure
import akka.actor.{PoisonPill, Props}
import akka.camel
import akka.camel.{Ack, CamelMessage}
import akka.testkit.TestProbe
import com.github.bjornschuller.Bootstrap._
import com.github.bjornschuller.supervision.{SupervisedConsumer, SupervisedProducer}

import scala.concurrent.Await

class SupervisedConsumptionTest extends TestSpec{


  feature("One-way communication with autoAck false") {
    scenario("1.The SupervisedProducer sends a message that  should  be consumed by the SupervisedConsumer.") {
      // create consumer & producer
      val supervisedConsumerRef = actorSystem.actorOf(Props[SupervisedConsumer])
      val supervisedProducerRef = actorSystem.actorOf(Props[SupervisedProducer])

      // Wait for the consumer to be ready


      val testProbe = TestProbe()
      testProbe.send(supervisedProducerRef, "3")

      testProbe.expectNoMsg()

      //TODO how to test if the message is consumed succesfully?

      cleanupActors(supervisedConsumerRef,supervisedProducerRef)
    }
    scenario("2.The SupervisedProducer sends a message that  should  be consumed by the SupervisedConsumer.") {

      // create consumer & producer
      val supervisedConsumerRef = actorSystem.actorOf(Props[SupervisedConsumer])
      val supervisedProducerRef = actorSystem.actorOf(Props[SupervisedProducer])

      val testProbe = TestProbe()
      testProbe.send(supervisedProducerRef, "This will result in NumberFormatException")

      testProbe.expectNoMsg()

      //TODO how to test if the message is redelivered succesfully?

      cleanupActors(supervisedConsumerRef,supervisedProducerRef)

    }
  }
}
