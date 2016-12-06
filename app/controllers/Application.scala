package controllers

import com.sun.istack.internal.logging.Logger

object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index("Your new application is ready."))
  }

  def tweets = Action.async {

    val loggingIteratee = Iteratee.foreach[Array[Byte]] { array =>
      Logger.info(array.map(_.toChar).mkString)
    }

      val credentials: Option[(ConsumerKey, RequestToken)] = for {
        apiKey <- Play.configuration.getString("twitter.apiKey")
        apiSecret <- Play.configuration.getString("twitter.apiSecret")
        token <- Play.configuration.getString("twitter.token")
        tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
      } yield (
        ConsumerKey(apiKey, apiSecret),
        RequestToken(token, tokenSecret)
        )

    credentials.map{case (consumerKey, requestToken) =>
      WS
      .url("https://stream.twitter.com/1.1/statuses/filter.json")
      .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" -> "reactive")
      .get{ response =>
        Logger.info("Status: "+ response.status)
        loggingIteratee
      }
      .map { response =>
        Ok("Stream closed")
      }
    } getOrElse{
      Future.successful{
        InternServerError("Tweeter credentials missing")
      }
    }
  }

  private def currentApi(implicit request: RequestHeader) = {
    toJson(Map(
      "root" -> request.uri
    ))
  }
}
