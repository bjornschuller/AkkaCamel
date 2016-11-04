package com.github.bjornschuller.ActiveMQ

import akka.actor.{ActorSystem, Props}
import akka.camel.CamelExtension
import com.github.bjornschuller.simpletwoway.consumer.StringReverseConsumer
import com.github.bjornschuller.simpletwoway.producer.ReverseThisStringProducer
import org.apache.activemq.camel.component.ActiveMQComponent


/***
  * http://camel.apache.org/activemq.html
  * http://localhost:8161/admin/queues.jsp
  */
class ActiveMQ(actorSystem: ActorSystem){


  /**
    *  Below adds the ActiveMQ component to the CamelContext, which is required when you would like to use the ActiveMQ component.
    *  The component name (i.e., addComponent("activemq" ....))
    *  needs to match the protocol specified in the producer and consumer endpoints.
    *
    *  failover: ensures that when down it reconnects
    */
  val brokerURL = s"failover:(tcp://localhost:61616)"
  val camel = CamelExtension(actorSystem)
  val camelContext = camel.context

  val activeMqComponent = ActiveMQComponent.activeMQComponent(brokerURL)
  camelContext.addComponent("activemq", activeMqComponent)

}