package com.github.bjornschuller.supervision

import akka.actor.{Actor}
import akka.actor.Status.Failure
import akka.camel.{Ack, CamelMessage, Consumer, Oneway}

/*
 * If an exception is thrown in the receive block then actor is restarted or stopped else it returns an ACK (which also deletes message)
 */
class SupervisedConsumer extends Actor with Consumer{


  // When this is set to false it let this actor positively acknowledge the message receipt else Failure
  override def autoAck = false  // default is true

  override def endpointUri: String = "activemq:supervisedQueue"

  override def receive: Receive = {
    case msg: CamelMessage => {
      println(s"SUPERVISOR CONSUMER RECEIVED THE MESSAGE ===> $msg")
      msg.body.toString.toInt   // leads to exception of String cannot be parsed to Int
      self ! Ack // positively acknowledge receipt of message, which causes the message on queue to be delete
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println(s"In the consumer preStart method with reason ===> $reason")
    self ! Failure(reason)
  }

}
