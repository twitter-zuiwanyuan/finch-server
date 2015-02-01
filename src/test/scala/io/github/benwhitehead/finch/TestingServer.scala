package io.github.benwhitehead.finch

import com.twitter.finagle.Service
import com.twitter.finagle.httpx.Method
import com.twitter.finagle.httpx.path.{->, /, Root}
import com.twitter.util.Future
import io.finch._
import io.finch.request.RequiredBody
import io.finch.response._
import io.github.benwhitehead.finch.request.DelegateService

/**
 * @author Ben Whitehead
 */

object Echo extends HttpEndpoint {
  def service(echo: String) = new Service[HttpRequest, HttpResponse] {
    def apply(request: HttpRequest): Future[HttpResponse] = {
      Ok(echo).toFuture
    }
  }
  def route = {
    case Method.Get -> Root / "echo" / echo => service(echo)
  }
}

object JsonBlob extends HttpEndpoint {
  lazy val logger = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  import io.finch.jackson._
  import io.github.benwhitehead.finch.JacksonWrapper.mapper

  lazy val service = DelegateService[HttpRequest, Map[String, Int]] {
    (1 to 100).map { case i => s"$i" -> i }.toMap
  }

  lazy val handlePost = new Service[HttpRequest, HttpResponse] {
    lazy val reader = for {
      body <- RequiredBody[Map[String, Int]]
    } yield body

    def apply(request: HttpRequest): Future[HttpResponse] = {
      reader(request) flatMap { case body =>
        logger.info("body = {}", body)
        Ok().toFuture
      }
    }
  }
  def route = {
    case Method.Get  -> Root / "json" => service ! TurnIntoHttp[Map[String, Int]]
    case Method.Post -> Root / "json" => handlePost
  }
}

object TestingServer extends SimpleFinchServer {
  override lazy val defaultHttpPort = 19990
  override lazy val config = Config(port = 17070)
  override lazy val serverName = "test-server"
  def endpoint = {
    Echo orElse JsonBlob
  }
}
