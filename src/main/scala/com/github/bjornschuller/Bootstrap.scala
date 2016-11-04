package com.github.bjornschuller

import akka.actor.Props
import com.github.bjornschuller.ActiveMQ.ActiveMQ
import com.github.bjornschuller.simpletwoway.consumer.StringReverseConsumer
import com.github.bjornschuller.simpletwoway.producer.ReverseThisStringProducer

/**
  * Created by bjornschuller on 21/09/16.
  * localhost:8161/admin/queues.jsp
  */
object Bootstrap extends App with CoreServices {

  val activeMQ = new ActiveMQ(actorSystem)

  // create consumer & producer
  val simpleConsumer = actorSystem.actorOf(Props[StringReverseConsumer])
  val simpleProducer = actorSystem.actorOf(Props[ReverseThisStringProducer])
}
