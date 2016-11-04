import akka.actor.Props
import akka.camel.CamelMessage
import akka.pattern.ask
import akka.testkit.TestProbe
import com.github.bjornschuller.Bootstrap._
import com.github.bjornschuller.simpletwoway.consumer.StringReverseConsumer
import com.github.bjornschuller.simpletwoway.producer.ReverseThisStringProducer
import scala.concurrent.duration._

import scala.concurrent.Await
/**
  * Scenario 1 and 2  uses the testProbe to send a message to the producer. The TestProbe has a send method
  * that sends a message to an actor while using the probe's TestActor as the sender. This way replies will
  * be available for inspection
  */
class ReverseThisStringProducerConsumerTest extends TestSpec{

  feature("Two-way communication tests") {
    scenario("1. The String Producers sends a message of type String which should be consumed by the SimpleConsumer") {
      // create consumer & producer
      val simpleConsumerRef = actorSystem.actorOf(Props[StringReverseConsumer])
      val simpleProducerRef = actorSystem.actorOf(Props[ReverseThisStringProducer])

      val message = "THIS IS AN OK MESSAGE"

      val testProbe = TestProbe()
      testProbe.send(simpleProducerRef, message)

      testProbe.receiveN(1).head match{
        case msg @ CamelMessage(body, headers) if body == message.reverse =>
          system.log.info(s"RECEIVED =====> $msg")
          true
        case _ =>
          fail("TEST FAILED: Did not received a reply or did not received the expected reply")
      }

      cleanupActors(simpleConsumerRef,simpleProducerRef)
    }
    scenario("2. The SimpleProducer sends a number of type Int which should result into a Failure Response from the consumer") {
      // create consumer & producer
      val simpleConsumerRef = actorSystem.actorOf(Props[StringReverseConsumer])
      val simpleProducerRef = actorSystem.actorOf(Props[ReverseThisStringProducer])

      val number = 123214141

      val testProbe = TestProbe()
      testProbe.send(simpleProducerRef, number)

      testProbe.receiveN(1).head match{
        case msg: CamelMessage if msg.body == "MESSAGE IS NOT A STRING" =>
          system.log.info(s"RECEIVED =====> $msg")
          true
        case _ =>
          system.log.info(s"RECEIVED =====> ")
          fail("TEST FAILED: Did not received a reply or did not received the expected reply")
      }
      cleanupActors(simpleConsumerRef,simpleProducerRef)
    }
    scenario("3.Using the ask pattern since we are expecting an answer back") {
      // create consumer & producer
      val simpleConsumerRef = actorSystem.actorOf(Props[StringReverseConsumer])
      val simpleProducerRef = actorSystem.actorOf(Props[ReverseThisStringProducer])

      val message = "Reverse this via the ASK pattern Bruhhh"

      // send a request response to the consumer, using the producer
      val future = simpleProducerRef ? message

      // Wait for the result
      val response = Await.result(future.mapTo[CamelMessage],10 seconds)

       response match{
        case msg: CamelMessage if msg.body == message.reverse =>
          system.log.info(s"RECEIVED =====> $msg")
          true
        case _ =>
          system.log.info(s"RECEIVED =====> ")
          fail("TEST FAILED: Did not received a reply or did not received the expected reply")
      }
      cleanupActors(simpleConsumerRef,simpleProducerRef)
    }
  }



}
