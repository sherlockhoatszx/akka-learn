package main.scala.Routers
import akka.NotUsed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import scala.util.Random
object HotelGuardian extends App {
  val guardian: Behavior[NotUsed] = Behaviors.setup { context =>
    // controller for the sensors
    val qiantai = context.spawn(HotelConcierge(), "前台")
    val guest1 = context.spawn(VIPGuest(), "Mr.A")


    guest1 ! VIPGuest.EnterHotel
    Behaviors.empty

  }
}
