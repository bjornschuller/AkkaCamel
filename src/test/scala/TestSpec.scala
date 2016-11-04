import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.TestProbe
import akka.util.Timeout
import com.github.bjornschuller.ActiveMQ.ActiveMQ
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._
class TestSpec extends  FeatureSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures with Eventually{

  implicit val system = ActorSystem("TestSystem")
  implicit val ec = system.dispatcher
  implicit val timeOut = Timeout(5.seconds)
  implicit val waitDuration = 5.seconds

  new ActiveMQ(system)


  def cleanupActors(actors: ActorRef*): Unit = {
    val probe = TestProbe()
    actors.foreach { ref =>
      ref ! PoisonPill
      probe.watch(ref)
      probe.expectTerminated(ref)
    }
  }

  override def afterAll() = {
    system.terminate()
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
