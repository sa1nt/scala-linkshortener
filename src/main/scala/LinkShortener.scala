/**
 * Created by roscoe.pringle on 8/22/2014.
 */

import akka.actor.{Actor => AkkaActor, ActorRef, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import org.apache.commons.validator.UrlValidator
import scredis.Redis
import spray.http.StatusCodes
import spray.routing._
import scala.concurrent.Await
import scalaz.concurrent._
import scala.util.Random

/*
class HelloActor extends AkkaActor {
  def receive = {
    case "hello" =>
      sender ! "hello back at you"
    case _       =>
      sender ! "not sure?"
  }
}

class MainActor extends AkkaActor {
  def receive = {
    case _ => sender ! "hello"
  }

  def tellSomething (a : ActorRef, m : String): Unit = {
    a ! m
  }

}*/

object GrasswireUrlShortener extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("actorsystem1")
  implicit val redis = Redis("application.conf", "production.scredis")

  /*val actorOf = system.actorOf(Props[HelloActor], name="HelloActor")
  val mainActor = system.actorOf(Props[MainActor], name="MainActor")*/

  startServer("0.0.0.0", 8085) {
    /*path(Rest) { r =>
      get {
        complete {
          mainActor.tellSomething()
          "asdfasdfassd"

        }
      }
    }*/

    path(Rest) { r =>
      get {
        //test(r)
        redirectShortUrl(r)

      } ~ post {
        createShortUrl(r)
      }
    }
  }

  /*def test(path: String)(implicit redis: scredis.Redis) = (ctx: RequestContext) => {
    system.log.info("test called")
      ctx.reject()
  }*/

  def redirectShortUrl(path: String)(implicit redis: scredis.Redis) = (ctx: RequestContext) => {
    Future.now {
      redis.withClient(_.get[String](path))
    }.runAsync(_.map(ctx.redirect(_, StatusCodes.MovedPermanently))
      .getOrElse(ctx.complete(StatusCodes.NotFound)))
  }

  def createShortUrl(path: String)(implicit redis: scredis.Redis) = (ctx: RequestContext) => {
    Task {
      val validator = new UrlValidator(List("http", "https").toArray)
      if (validator.isValid(path)) {
        val random = Random.alphanumeric.take(7).mkString
        redis.withClient(_.set(random, path))
        random
      } else {
        throw new Exception("The supplied url is invalid.")
      }
    }.runAsync(_.fold(l => ctx.reject(ValidationRejection("Invalid url provided", Some(l))), r => ctx.complete(s"http://mydomain.com/$r")))
  }
}