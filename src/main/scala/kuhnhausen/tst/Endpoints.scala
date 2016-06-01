package kuhnhausen.tst

import io.finch._
import kuhnhausen.tst.Slack._
import kuhnhausen.tst.Messages._

object Endpoints {
  val teamParams: Endpoint[Team] = (param("team_id") :: param("team_domain")).as[Team]
  val channelParams: Endpoint[Channel] = (param("channel_id") :: param("channel_name")).as[Channel]
  val userParams: Endpoint[User] = (param("user_id") :: param("user_name")).as[User]
  val tokenParam: Endpoint[Token] = param("token").as[Token]
  val textParam: Endpoint[Text] = param("text").as[Text]
  val respParam: Endpoint[ResponseURL] = param("response_url").as[ResponseURL]
  val cmdParam: Endpoint[Command] = param("command").as[Command]
  val slackParams: Endpoint[SlackRequest] = (tokenParam :: teamParams ::
      channelParams :: userParams :: cmdParam :: textParam :: respParam).as[SlackRequest]
  val messageResp: Endpoint[Message] = slackParams.mapAsync(slackRequestToMessage(_))

}
