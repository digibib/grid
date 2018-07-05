package com.gu.mediaservice.lib.auth

import akka.actor.ActorSystem
import com.gu.mediaservice.lib.argo.ArgoHelpers
import com.gu.mediaservice.lib.argo.model.Link
import com.gu.mediaservice.lib.auth.Authentication.{AuthenticatedService}
import com.gu.mediaservice.lib.config.CommonConfig
// DEICHMAN MOD : remove authentication
//import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
//import com.gu.pandomainauth.action.{AuthActions, UserRequest}
import com.gu.pandomainauth.model.{AuthenticatedUser, User}
import org.slf4j.{Marker, MarkerFactory}
import play.api.{Logger, MarkerContext}
import play.api.libs.ws.WSClient
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class Authentication(config: CommonConfig, actorSystem: ActorSystem,
                     override val parser: BodyParser[AnyContent],
                     //override val wsClient: WSClient,
                     //override val controllerComponents: ControllerComponents,
                     override val executionContext: ExecutionContext)
  extends ActionBuilder[Authentication.Request, AnyContent]  with ArgoHelpers {

  implicit val ec: ExecutionContext = executionContext

  val loginLinks = List(
    Link("login", config.services.loginUriTemplate)
  )

  // Panda errors
  val notAuthenticatedResult = respondError(Unauthorized, "unauthorized", "Not authenticated", loginLinks)
  val invalidCookieResult    = notAuthenticatedResult
  val expiredResult          = respondError(new Status(419), "session-expired", "Session expired, required to log in again", loginLinks)

  // API key errors
  val invalidApiKeyResult    = respondError(Unauthorized, "invalid-api-key", "Invalid API key provided", loginLinks)

  private val apiKey = ApiKey(s"dummykey", Internal)
  private val headerKey = "X-Gu-Media-Key"
  private val keyStoreBucket: String = config.properties("auth.keystore.bucket")
  val keyStore = new KeyStore(keyStoreBucket, config)

  //keyStore.scheduleUpdates(actorSystem.scheduler)

  //override lazy val panDomainSettings = buildPandaSettings()
  //override lazy val panDomainSettings = new PanDomainAuthSettingsRefresher()

  //final override def authCallbackUrl: String = s"${config.services.authBaseUri}/oauthCallback"


  override def invokeBlock[A](request: Request[A], block: Authentication.Request[A] => Future[Result]): Future[Result] = {
    // Try to auth by API key, and failing that, with Panda
    /* DEICHMAN MOD : always successful authentication
    request.headers.get(headerKey) match {
      case Some(key) =>
        keyStore.lookupIdentity(key) match {
          case Some(apiKey) =>
            val marker: Marker = MarkerFactory.getMarker(apiKey.name)
            val mc: MarkerContext = MarkerContext(marker)
            Logger.info(s"Using api key with name ${apiKey.name} and tier ${apiKey.tier}")(mc)
            if (ApiKey.hasAccess(apiKey, request, config.services))
              block(new AuthenticatedRequest(AuthenticatedService(apiKey), request))
            else
              Future.successful(ApiKey.unauthorizedResult)
          case None => Future.successful(invalidApiKeyResult)
        }
      case None =>
        block(new AuthenticatedRequest(AuthenticatedService(apiKey), request))

        APIAuthAction.invokeBlock(request, (userRequest: UserRequest[A]) => {
          block(new AuthenticatedRequest(PandaUser(userRequest.user), request))
        })
    }
    */
    block(new AuthenticatedRequest(AuthenticatedService(apiKey), request))
  }

  // DEICHMAN MOD: disable authentication
  //final override def validateUser(authedUser: AuthenticatedUser): Boolean = {
  //  1 == 1
    // authedUser.user.email.endsWith("@guardian.co.uk") && authedUser.multiFactor
  //}

  /*private def buildPandaSettings() = {
    new PanDomainAuthSettingsRefresher(
      domain = config.services.domainRoot,
      system = "media-service",
      actorSystem = actorSystem,
      awsCredentialsProvider = config.awsCredentials
    )
  }*/
}

object Authentication {
  sealed trait Principal { def apiKey: ApiKey }
  //case class PandaUser(user: User) extends Principal { def apiKey: ApiKey = ApiKey(s"${user.firstName} ${user.lastName}", Internal) }
  case class AuthenticatedService(apiKey: ApiKey) extends Principal

  type Request[A] = AuthenticatedRequest[A, Principal]

  def getEmail(principal: Principal): String = principal match {
    //case PandaUser(user) => user.email
    case _ => principal.apiKey.name
  }
}
