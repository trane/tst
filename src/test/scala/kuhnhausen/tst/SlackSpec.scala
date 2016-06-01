package kuhnhausen

import tst.Slack
import org.scalatest._
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import com.twitter.finagle.http.{ParamMap, Method, Request, RequestBuilder}
import com.twitter.io.{ Buf, Reader }
import tst.Messages._
import tst.Slack._

class SlackSpec extends FlatSpec with Matchers {

  /*
  "SlackRequest" should "hydrate" in {
    val params = ParamMap(
      ("token", "jJGfagzhcGLFIXoPSSL4ssd3"),
      ("team_id","T0001"),
      ("team_domain","example"),
      ("channel_id","C2147483705"),
      ("channel_name","test"),
      ("user_id","U2147483697"),
      ("user_name","Steve"),
      ("command","/tst"),
      ("text","help"),
      ("response_url","https://hooks.slack.com/commands/1234/5678")
    )

    val req = Request(Method.Get, s"/test${params.toString}")
    val test: Endpoint[String] =
      get("test" :: slackParams) { sr: SlackRequest => Ok(sr.toString) }
    test.toService.apply(req).onSuccess { response =>
      println(s"$response: ${ response.contentString }")
    }
  }
  */



}

