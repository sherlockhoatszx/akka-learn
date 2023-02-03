package main.scala.ChildActors

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}


object Guardian{
  sealed trait Command
  case class Start(tasks:List[String]) extends Command
  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      println("setting up")
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-alpha")
      Behaviors.receiveMessage {
        case Start(tasks) =>
          manager ! Manager.Delegate(tasks)
          Behaviors.same
      }
    }
}

object Manager{
  sealed trait Command
  case class Delegate(tasks:List[String]) extends Command

  def apply(): Behavior[Command]= Behaviors.setup{
    context =>
      val aggregator:ActorRef[Aggregator.Command]= {
        context.spawn(Aggregator(),"Aggregator")
      }
      // add the pool router
      val pool = Routers.pool(poolSize = 4) {
        // make sure the workers are restarted if they fail
        Behaviors.supervise(Worker(context.self,aggregator)).onFailure[Exception](SupervisorStrategy.restart)
      }


      val router = context.spawn(pool, "worker-pool")

      Behaviors.receiveMessage{
        message=>message match{
          case Delegate(tasks)=>
            println("分配工作")
            tasks.map{task=>
              println(s"创建了工人")
              router ! Worker.Do(task)
              aggregator ! Aggregator.nInstanceTask(tasks.length)

            }
            Behaviors.same
        }
      }
  }
}

object Worker{
  sealed trait Command
  case class Do(task:String) extends Command

  def apply(managerRef:ActorRef[Manager.Command],replyTo:ActorRef[Aggregator.Command]):Behavior[Command]=
    Behaviors.receiveMessage{message=>
    message match{
      case Do(message) =>
        println("工人收到了工作")
        val confirmedMessage:String=message + "*Confirmed"
        println(s"完成了工作$confirmedMessage")
        replyTo ! Aggregator.Collecting(confirmedMessage)
        Behaviors.same
    }
  }


}

object Aggregator{
  sealed trait Command
  case class nInstanceTask(n:Int) extends Command
  case class Collecting(response:String) extends Command
  def apply():Behavior[Aggregator.Command] = Behaviors.receive { (context, message) =>
    message match {
      case nInstanceTask(n: Int) =>
        println(s"统计出了共 $n 个工作")
        active(n, 0, List())
      case _ =>
        println("command not found")
        Behaviors.same
    }
  }


    /*var nInstances:Int=0
    var receivedMessage:Int =0
    var retResponse:List[String]=List()*/
    def active(nInstances: Int, receivedMessage: Int,
               retResponse: List[String] = List()
              ): Behavior[Aggregator.Command] =
      Behaviors.receive { (context,message) =>
        message match {
          case Collecting(ret: String) =>
            println(s"收到了$receivedMessage,反馈")
            //retResponse = retResponse :+ ret
            println("添加到结果里了")
            val newReceivedMessage =receivedMessage + 1
            val newResponses = retResponse :+ ret
            //receivedMessage += 1
            println(newResponses)
            println(s"现在共 存了$newReceivedMessage 个工作")
            println(s"现在共有 $nInstances 工作要完成")
            active(nInstances, newReceivedMessage, newResponses)
            /*if (newReceivedMessage>=nInstances){
             newResponses.foreach(println)
              Behaviors.stopped
            }else{
              Behaviors.same
            }*/
          case _=>
            println("aggregator not found command")
            Behaviors.same

        }
      }
}


object ChildAggregator extends App{
  val system: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "ChildAndAggregatorShow")
  system ! Guardian.Start(List("jack", "tom","Jackson","Jerry","Phil","Ada"))


}
