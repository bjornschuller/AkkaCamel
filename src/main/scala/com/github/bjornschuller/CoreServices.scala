package com.github.bjornschuller

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

/**
  * Created by bjornschuller on 21/09/16.
  */
trait CoreServices {
  val actorSystem = ActorSystem("ActorSystem")
  implicit val ec = actorSystem.dispatcher

}
