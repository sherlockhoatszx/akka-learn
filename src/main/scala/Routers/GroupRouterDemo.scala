package main.scala.Routers
//demonstrate the use of group routers

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, Routers}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}



object GroupRouterDemo {
  def main(args: Array[String]): Unit = {
    // create the ActorSystem
    val supervisor: ActorSystem[Supervisor.SupervisorMessage] = ActorSystem(
      Supervisor(),
      "Supervisor"
    )

    // send the Start message to the Supervisor
    supervisor ! Supervisor.Start

    // wait a few moments, and then stop the Supervisor
    Thread.sleep(200)
    supervisor.terminate()
  }

}

object Mouth {

  sealed trait MessageToMouth
  case class SpeakText(msg: String) extends MessageToMouth
  private case class ListingResponse(listing: Receptionist.Listing)
    extends MessageToMouth


  val MouthKey: ServiceKey[MessageToMouth] = ServiceKey("Mouth")

  def apply(): Behavior[MessageToMouth] = Behaviors.setup {
    context: ActorContext[MessageToMouth] =>

      context.system.receptionist !
        Receptionist.Register(Mouth.MouthKey, context.self)
      println("我注册了")

      Behaviors.receiveMessage { message =>
        message match {
          case SpeakText(msg) =>
            println(s"Mouth: got a msg: $msg")
            Behaviors.same
        }
      }
  }

}


object Brain {

  sealed trait MessageToBrain
  final case object FindTheMouth extends MessageToBrain
  private case class ListingResponse(listing: Receptionist.Listing)
    extends MessageToBrain


  def apply(): Behavior[MessageToBrain] = Behaviors.setup {
    context: ActorContext[MessageToBrain] =>

      var mouth: Option[ActorRef[Mouth.MessageToMouth]] = None
      val listingAdapter: ActorRef[Receptionist.Listing] =
      context.messageAdapter { listing =>
        println(s"listingAdapter:listing: ${listing.toString}")
        Brain.ListingResponse(listing)
      }
      context.system.receptionist !
        Receptionist.Subscribe(Mouth.MouthKey, listingAdapter)

      Behaviors.receiveMessage { message =>
        message match {
          case FindTheMouth =>
            println(s"Brain: got a FindTheMouth message")
            context.system.receptionist !
              Receptionist.Find(Mouth.MouthKey, listingAdapter)
            Behaviors.same
          case ListingResponse(Mouth.MouthKey.Listing(listings)) =>
            println(s"Brain: got a ListingResponse message")
            val xs: Set[ActorRef[Mouth.MessageToMouth]] = listings
            for (x <- xs) {
              mouth = Some(x)
              mouth.foreach{ m =>
                m ! Mouth.SpeakText("Brain says hello to Mouth")
              }
            }
            Behaviors.same
        }
      }
  }
}


object Supervisor {

  sealed trait SupervisorMessage
  final case object Start extends SupervisorMessage
  def apply(): Behavior[SupervisorMessage] =
    Behaviors.setup[SupervisorMessage] {
      actorContext: ActorContext[SupervisorMessage] =>
        val mouth1: ActorRef[Mouth.MessageToMouth] = actorContext.spawn(
          Mouth(), "Mouth1"
        )

        val mouth2: ActorRef[Mouth.MessageToMouth] = actorContext.spawn(
          Mouth(), "Mouth2"
        )
        val brain: ActorRef[Brain.MessageToBrain] = actorContext.spawn(
          Brain(), "Brain"
        )


        Behaviors.receiveMessage { consoleMessage =>
          consoleMessage match {
            case Start =>
              println(s"Supervisor got a Start message")
              brain ! Brain.FindTheMouth
              Behaviors.same
          }
        }
    }
}





