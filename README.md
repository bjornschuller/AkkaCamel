#### Akka Camel and ActiveMQ
Akka is a platform for event-driven, scalable and fault-tolerant architectures on the JVM. It is mainly written in Scala. One of its core features is support for the actor model that provides a higher level of abstraction for writing concurrent and distributed systems.

Since version 0.7, Akka offers a new feature that let actors send and receive messages over a great variety of protocols and APIs. In addition to the native Scala actor API, actors can now exchange messages with other systems over large number of protcols and APIs such as HTTP, SOAP, TCP, FTP, SMTP or JMS, to mention a few. At the moment, approximately 80 protocols and APIs are supported. This new feature is provided by Akka's Camel module.


##### Setting up
The akka-camel module is implemented as an Akka Extension, known as the CamelExtension object. ****Extensions will only be loaded once per ActorSystem, which will be managed by Akka****. The CamelExtension object provides access to the **Camel trait**. The Camel trait in turn provides access to two important Apache Camel objects, the **CamelContext** and the **ProducerTemplate**.

The CamelContext is started when the CamelExtension is created, and it is shut down when the associated ActorSystem is shut down. The same is true for the ProducerTemplate. The CamelExtension is used by both Producer and Consumer actors to interact with Apache Camel internally. 

If you want to use the ActiveMQ component, then you need to add this component to your CamelContext as folows: 

```
camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("brokerUrl"))
```

Important, note when you specify the brokerURL as follows: => ```s"failover:(tcp://localhost:61616)"``` this ensures that when ActiveMQ is down it tries to reconnect. One could test this as follows: 
docker stop activemqtest_activemq_1
docker start activemqtest_activemq_1 (to check if it reconnects)

Of course this is not all, we still need to run an ActiveMQ broker on our local machine. Therefore, you could create a docker-compose.yml file that pulls ActiveMQ from the dockerhub. After creating a docker-compose.yml file that does this, you can do:
 - docker-compose up -d (to run activeMQ)
 - Then do sbt run (to run your app)
Next to this, you need a docker deamon of course (so the ActiveMQ Docker Container can run in this 'docker daemon')

One last important note: your logging should be in place ```("com.typesafe.akka"   %%  "akka-slf4j"  &
 "ch.qos.logback"       % "logback-classic")``` otherwise you will not see the expected command line output during starting ActiveMQ.

##### Producers and Consumers
Akka Camel uses the concept of producers and consumers, and makes it very easy to link them to ActiveMQ. To start  you need the following:

 A. an actor to produce messages - therefore: extend akka.camel.Producer and implement endpointUri
 B. an actor to implement messages - therefore: extend akka.camel.Consumer and implement the same endpointUri + a receive method to for receiving messages.
 
The **endpointUri** is an **abstract method** declared in the **Consumer** & **Producer** **trait**. For the producer Actor it means that any message send to the producer Actor will be produced to the queue specified in this endpointUri method. In contrast, the consumer Actor will consume any message from the queue specified in the endpoinUri method. Next to this, all messages consumed by actors from Camel endpoints are of type Message and are immutable representations of Camel messages.
 
The consumer and producer actors will communicate with eachother using the quename specified in the **endpoint method**. 

When a Consumer actor is created, the Consumer is published at its Camel endpoint (more precisely, the route is added to the CamelContext from the Endpoint to the actor). When a Producer actor is created, a SendProcessor and Endpoint are created so that the Producer can send messages to it. 

Some Camel components can take a while to startup, and in some cases you might want to know when the endpoints are activated and ready to be used. The Camel trait allows you to find out when the endpoint is activated or deactivated. The below code shows that you can get a Future to the activation of the route from the endpoint to the actor, or you can wait in a blocking fashion on the activation of the route. An ActivationTimeoutException is thrown if the endpoint could not be activated within the specified timeout.

```
// get a future reference to the activation of the endpoint of the Consumer Actor
val activationFuture = camel.activationFutureFor(actorRef)(
  timeout = 10 seconds,
  executor = system.dispatcher)
```

Deactivation of a Consumer or a Producer actor happens when the actor is terminated.  For a Consumer, the route to the actor is stopped. For a Producer, the SendProcessor is stopped. A DeActivationTimeoutException is thrown if the associated camel objects could not be deactivated within the specified timeout.
Deactivation works in a similar fashion:

```
system.stop(actorRef)
// get a future reference to the deactivation of the endpoint of the Consumer Actor
val deactivationFuture = camel.deactivationFutureFor(actorRef)(
  timeout = 10 seconds,
  executor = system.dispatcher)
```

##### Consumer un-publishing
When an actor is stopped, the route from the endpoint to that actor is stopped as well. For example, stopping an actor that has been previously published at a specified endpoint will cause a connection failure when trying to access that endpoint.  Stopping the route is done asynchronously; it may be still in progress after the ActorRef.stop method returned.

##### Delivery acknowledgements -- One-way communication & Two-way communication
Camel knows two types of communications namely, one-way and two-way communication. The main difference between both is that for **two-way communcation replies from consumer actors are mandatory for one-way communcation they are optional**.
IMPORTANT to note is that by default, the producer initiates an in-out message exchange with the endpoint. For initiating an in-only exchange, producer actors must override the oneway method to return true or mix in the OneWay trait.

Below the two types of communications are  described in more detail. 

###### Oneway (in-only)
If the producer extends the Oneway trait this tells Camel that the producer won't be participating in any request-reply messaging patterns. Also known as a **publish-subscribe system**. Important to note is that, with in-only message exchanges, by default, an exchange is done when a message is added to the consumer actor's mailbox.
**Any failure or exception that occurs during processing of that message by the consumer actor cannot be reported back to the endpoint in this case.** 

###### Oneway (in-only) -- autoAck
To allow consumer actors to positively or negatively acknowledge the receipt of a message from an in-only message exchange, they need to override the autoack (Scala) method and set it to false. In this case the consumer actor must reply with a special Ack message when message processing is done (i.e., `self.reply(Ack)`). Consumer actors may also reply with a Failure message to indicate a processing failure.

```
    override def autoAck: Boolean = false

```
If set to true, in-only message exchanges are auto-acknowledged when the message is added to the actor's mailbox. If set to false, actors must acknowledge the receipt of the message.

###### Twoway (in-out)
With in-out (Twoway) message exchanges, clients usually know that a message exchange is done when they receive a reply from a consumer actor. The reply message can be a CamelMessage (or any object which is then internally converted to a CamelMessage) on success, and a Failure message on failure. The Producer is waiting for an answer in this case, this is asynchronous however it costs more performance wise. 
When you are using two-way communications between a Camel endpoint and an actor. You are able to implement the ask pattern. The Consumer Actor will reply to the endpoint ones the response is ready. The ask request to the actor can timeout, which will result in the Exchange failing with a TimeoutException set on the failure of the Exchange. 

Please note that, turning auto-acknowledgements on and off is only relevant for in-only message exchanges because, for in-out message exchanges, consumer actors need to reply in any case with an (application-specific) message. 

###### Twoway and timeouts
Endpoints that support two-way communications need to wait for a response from an (untyped) actor or typed actor before returning it to the initiating client. For some endpoint types, timeout values can be defined in an endpoint-specific way which is described in the documentation of the individual Camel components. Another option is to configure timeouts on the level of consumer actors and typed consumer actors.


##### Blocking exchanges

By default, message exchanges between a Camel endpoint and a consumer actor are non-blocking because, internally, the ! (bang) operator is used to commicate with the actor. The route to the actor does not block waiting for a reply. The reply is sent asynchronously. Consumer actors however can be configured to make this interaction blocking. In this case, the !! (bangbang) operator is used internally to communicate with the actor which blocks a thread until the consumer sends a response or throws an exception within receive. 

##### Producing messages with the Producer Actor
As described earlier any message sent to a Producer actor (or UntypedProducerActor) will be sent to the associated Camel endpoint. For this reason, the Producer Actors should not override the default Producer.receive method. However, you customize some of the Producer Actors defaultbehavior by overriding the Producer.transformOutgoingMessage and Producer.transformResponse methods. 

####### Producer.transformOutgoingMessage method
This method is called before the message is sent to the endpoint specified by. The original message is passed as argument. By default, this method simply returns the argument but may be overridden

####### Producer.transformResponse method
Called before the response message is sent to the original sender. The original
message is passed as argument. By default, this method simply returns the argument but.

##### Fault-tolerance -- message redelivery
Message processing inside receive may throw exceptions which usually requires a failure response to Camel (i.e. to the consumer endpoint). This is done with a Failure message that contains the failure reason (an instance of Throwable). Instead of catching and handling the exception inside receive, **consumer actors should be part of supervisor hierarchies and send failure responses from within restart callback methods**. In other words, a JMS consumer actor should acknowledge a message receipt upon successful message processing and trigger a message redelivery on failure. --> you additionally need to configure the JMS connection with a redelivery policy and, optionally, use transacted queues.(find out)

In other words, the idea is as follows: Messages that are successfully processed by the consumer will positively acknowledge the message receipt, causing the endpoint to delete the message. In contrast, when an exception occurs during processing of the message it will cause the supervisor to restart the consumer. Important here is that before restart, the consumer negatively acknowledges the message  receipt which causes the endpoint to redeliver the message.

The callback message called **preRestart** is called when an Actor is started.
The callback method **postStop** is called after 'actor.stop()' is invoked. A reply within preRestart and postStop is possible after receive has thrown an exception. 

##### Fault-tolerance -- supervision strategy
There are basically two different types of supervisor strategy, the **OneForOneStrategy** and the **AllForOneStrategy**. Choosing the former means that the way you want to deal with an error in one of your children will only affect the child actor from which the error originated, whereas the latter will affect all of your child actors. Which of those strategies is best depends a lot on your individual application.

Regardless of which type of SupervisorStrategy you choose for your actor, you will have to specify a Decider, which is a PartialFunction[Throwable, Directive] – this allows you to match against certain subtypes of Throwable and decide for each of them what’s supposed to happen to your problematic child actor (or all your child actors, if you chose the all-for-one strategy).

##### Fault-tolerance Consumers
Message processing inside receive may throw exceptions which usually requires a failure response to Camel (i.e. to the consumer endpoint). This is done with a Failure message that contains the failure reason (an instance of Throwable). Instead of catching and handling the exception inside receive, consumer actors should be part of supervisor hierarchies and send failure responses from within restart callback methods. 

####### Fault-tolerance of the consumer depends on its life cycle (PERMANENT OR TEMPORARY)
If the lifecycle of an Actor Consumer is configured to be **PERMANENT**, a supervisor will restart the consumer when failure with a call to preRestart. If the sender replies with a FAILURE message in the preStart method this will causes the endpoint to redeliver the content of the consumed message and the Consumer Actor can try processing again. In contrast, when you reply with an ACK then the message will be deleted from the endpoint. 

If the lifecycle of an Actor Consumer is configured to be **TEMPORARY**, a supervisor will shut down the consumer upon failure with a call to postStop. Within postStop you can reply with an Ack, to delete the message from the endpoint. Or you can reply with a Failure, so that it retries processing again based on your redelivery policy. 

===== KLAD
When using autoAck false you can positively or negatively acknowledge the
receipt of the Camel message, but that is not the same thing as the
acknowledgement of the JMS message towards the broker.

You can configure how the Camel ActiveMQ component should behave when
picking JMS messages off the queue using options like
acknowledgementModeName,see http://camel.apache.org/activemq.html and
http://camel.apache.org/jms. Just append options to the endointUri defined
in your consumer actor.

example: https://gist.github.com/ketankhairnar/831229
http://danielwestheide.com/blog/2013/03/20/the-neophytes-guide-to-scala-part-15-dealing-with-failure-in-actor-systems.html
https://danielasfregola.com/2015/03/09/how-to-supervise-akka-actors/
https://gist.github.com/krasserm/835076

source:
http://krasserm.blogspot.nl/2011/02/akka-consumer-actors-new-features-and.html
http://cjwebb.github.io/blog/2013/09/01/akka-camel-and-activemq/
http://doc.akka.io/docs/akka/current/scala/camel.html
http://krasserm.blogspot.nl/2010/04/akka-features-for-application.html
http://activemq.apache.org/configuring-transports.html
https://github.com/OpenNetworkingFoundation/BOULDER-Intent-NBI/blob/master/docs/modules/camel.rst
https://github.com/RayRoestenburg/akka-camel-presentation/blob/master/src/test/scala/org/xebia/ConsumerTest.scala





















=======KLAD======

####Synchronous Producer
The main difference between an asynchronous and synchronous producer is that the synchronous producer has two queues: a send queue and a reply queue. The send queue is the queue on which the producer will send a message to the consumer. The reply queue is the queue on which the producer will listen for a reply from the consumer. The producer when it sends a message sets two important pieces of information on the message:

JMSCorrelationID: This is the uniqueID used by the producer to indentify the message
JMSReplyTo: This tells the consumer on which queue to send the message reply

The producer then creates a “ReplyConsumer” on the reply queue and listens for a reply from the consumer that contains that “JMSCorrelationID”. When a message with that ID appears on the reply queue, the ReplyConsumer will receive that message and our synchronous message round trip has been completed!

source: https://myadventuresincoding.wordpress.com/category/activemq/

======KLAD=====