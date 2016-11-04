package com.github.bjornschuller.simpletwoway.consumer

import akka.actor.Actor
import akka.camel.{Ack, CamelMessage, Consumer}
import com.github.bjornschuller.CoreServices
/*
 * default is used here, that is two way communcation
 */
class StringReverseConsumer extends Actor with Consumer with CoreServices {

  override def endpointUri: String = "activemq:simpleTestQueue"

  override def receive: Receive = {
    case msg: CamelMessage if msg.body.isInstanceOf[String] =>{
      actorSystem.log.info(s"Consumed message with body: ${msg.body}")
      sender() ! msg.body.toString.reverse
    }
    case _  => {
      actorSystem.log.info(s"Cannot consume message since it is not of type String")
      sender() ! "MESSAGE IS NOT A STRING"
    }
  }


}
