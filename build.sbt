name := "AkkaCamelActiveMQ"

version := "1.0"

scalaVersion := "2.11.8"

fork in Test := true
(parallelExecution in Test) := false

libraryDependencies ++= {
  val akkaV       = "2.4.10"
  val scalaTestV  = "3.0.0"
  Seq(
    "org.apache.activemq" %   "activemq-camel"                       % "5.14.0",
    "com.typesafe.akka"   %%  "akka-camel"                           % akkaV,
    "com.typesafe.akka"   %%  "akka-actor"                           % akkaV,
    "ch.qos.logback"       % "logback-classic"                       % "1.1.3",
    "com.typesafe.akka"   %%  "akka-stream"             		         % akkaV,
    "com.typesafe.akka"   %%  "akka-slf4j"               		         % akkaV,
    "com.typesafe.akka"   %%  "akka-http-experimental"               % akkaV,
    "com.typesafe.akka"   %%  "akka-http-spray-json-experimental"    % akkaV,
    "com.typesafe.akka"   %%  "akka-http-testkit"       		         % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"                         % akkaV,
    "org.scalatest"       %%  "scalatest"                            % scalaTestV % "test"
  )
}