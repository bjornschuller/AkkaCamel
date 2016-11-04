package com.github.bjornschuller.supervision

import akka.actor.Actor
import akka.camel.{Oneway, Producer}


class SupervisedProducer extends Actor with Producer with Oneway{

  override def endpointUri: String = "activemq:supervisedQueue"

}
