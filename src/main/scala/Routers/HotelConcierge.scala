package main.scala.Routers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

object HotelConcierge {

  val goldenKey = ServiceKey[VIPGuest.Command]("concierge-key")

  sealed trait Command
  private final case class ListingResponse(
      listing: Receptionist.Listing)
      extends Command

  def apply() = Behaviors.setup[Command] { context =>
    println("前台出现了")
    val listingNotificationAdapter =
      context.messageAdapter[Receptionist.Listing](ListingResponse)
    //订阅后，何时触发结果？
    context.system.receptionist ! Receptionist
      .Subscribe(goldenKey, listingNotificationAdapter)

    Behaviors.receiveMessage {
      case ListingResponse(goldenKey.Listing(listings)) =>
        listings.foreach { actor =>
          println(s"${actor.path.name} is in")

        }
        Behaviors.same
    }
  }
}
