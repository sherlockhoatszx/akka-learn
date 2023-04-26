package main.scala.AkkaHttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json._
import main.scala.CatsValidator.ValidationModel._
import scala.collection.mutable.ListBuffer





object DynamicRouter extends App{

  implicit val system = ActorSystem("DataVHttp")

  //implicit val materializer = ActorMaterializer()
  import system.dispatcher

  case class Definition(path:String,
                        requests:JsValue)

  implicit val format = jsonFormat2(Definition)

  @volatile var state = Map.empty[String,JsValue]

  //fixed route to publish the data dictionary
  val fixedRoute:Route = post{

    pathSingleSlash{
      entity(as[Definition]){
        difn=>
          state = state + (difn.path ->difn.requests)
          complete("ok")
      }
    }
  }

  //dynamic routing based on the current state
  val dynamicRoute:Route = ctx =>{
    val routes = state.map{
      case(segment,dataDict)=>
        post{
          path(segment){
            entity(as[JsValue]){input=>
              val inputDict = input.convertTo[Map[String,String]]
              val DataDict = dataDict.convertTo[Map[String,String]]

              var error_messages=ListBuffer[String]()

              //考虑 inputDict 比原始DataDict多,上游感知
              val IncreaseKey = inputDict.keySet -- DataDict.keySet
              val ret = if (IncreaseKey.size==0) "上游感知ok" else "上游感知notok"

              //下面代码好丑啊
              val validate_result=FormValidatorNec.validateForm(
                username = inputDict.getOrElse("username",""),
                password = inputDict.getOrElse("password",""),
                firstName = inputDict.getOrElse("firstName",""),
                lastName = inputDict.getOrElse("lastName",""),
                age = inputDict.getOrElse("age",0).asInstanceOf[Int]
              ).toEither

              validate_result match{
                case Left(x)=>error_messages+=(x.toString)
                case Right(x)=> println("good")
              }

              error_messages+=ret
              complete(error_messages.toString)
            }
          }
        }
    }
    concat(routes.toList: _*)(ctx)
  }

  val route = fixedRoute ~ dynamicRoute

  Http().newServerAt( "localhost",8080).bind(route)



}
