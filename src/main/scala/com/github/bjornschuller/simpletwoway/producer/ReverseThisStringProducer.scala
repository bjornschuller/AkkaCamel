package com.github.bjornschuller.simpletwoway.producer

import akka.actor.Actor
import akka.camel.Producer
/*
 * default is used here, that is two way communcation
 */
class ReverseThisStringProducer extends Actor with Producer{
  override def endpointUri: String = "activemq:simpleTestQueue"

}
