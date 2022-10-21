package io.blindnet.identityclient
package auth

import cats.data.EitherT
import cats.effect.{IO, *}
import io.blindnet.jwt.{Token, TokenPublicKey}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*

import java.util.UUID
import scala.util.Try

case class JwtAuthenticator[T] (
  identityClient: IdentityClient,
  private val baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint,
  private val jwtProcessor: Jwt => IO[Either[String, T]] = jwt => IO.pure(Right(jwt)),
) {
  def withBaseEndpoint(endpoint: PublicEndpoint[Unit, Unit, Unit, Any]): JwtAuthenticator[T] =
    copy(baseEndpoint = endpoint)

  def requireAppJwt: JwtAuthenticator[AppJwt] = require(_.asApp)
  def requireAnyUserJwt: JwtAuthenticator[AnyUserJwt] = require(_.asAnyUser)
  def requireUserJwt: JwtAuthenticator[UserJwt] = require(_.asUser)
  def requireAnonymousJwt: JwtAuthenticator[AnonymousJwt] = require(_.asAnonymous)
  private def require[R](f: Jwt => Option[R]): JwtAuthenticator[R] =
    copy(jwtProcessor = jwt => IO.pure(f(jwt).toRight("Wrong JWT type")))

  def mapJwt[R](f: T => R): JwtAuthenticator[R] =
    copy(jwtProcessor = jwtProcessor.andThen(e => EitherT(e).map(f).value))
  def mapJwtF[R](f: T => IO[R]): JwtAuthenticator[R] =
    copy(jwtProcessor = jwtProcessor.andThen(e => EitherT(e).semiflatMap(f).value))
  def flatMapJwt[R](f: T => Either[String, R]): JwtAuthenticator[R] =
    copy(jwtProcessor = jwtProcessor.andThen(e => EitherT(e).subflatMap(f).value))
  def flatMapJwtF[R](f: T => IO[Either[String, R]]): JwtAuthenticator[R] =
    copy(jwtProcessor = jwtProcessor.andThen(e => EitherT(e).flatMap(t => EitherT(f(t))).value))

  def secureEndpoint: PartialServerEndpoint[Option[String], T, Unit, Exception, Unit, Any, IO] =
    baseEndpoint
      .securityIn(header[Option[String]]("Authorization"))
      .errorOut(oneOf[Exception](
        oneOfVariant(StatusCode.Unauthorized, jsonBody[String].mapTo[AuthException])
      ))
      .serverSecurityLogic(authenticateHeader(_).map(_.left.map(AuthException(_))))

  def authenticateHeader(headerOption: Option[String]): IO[Either[String, T]] =
    headerOption match
      case Some(header) => authenticateHeader(header)
      case None => IO.pure(Left("Missing Authorization header"))

  def authenticateHeader(header: String): IO[Either[String, T]] =
    EitherT.fromEither[IO](extractTokenFromHeader(header))
      .flatMap(token => EitherT(authenticateToken(token)))
      .value

  def authenticateToken(raw: String): IO[Either[String, T]] =
    (for {
      token <- EitherT.fromEither[IO](parseToken(raw))
      appInfo <- EitherT.fromOptionF(identityClient.getAppInfo(token.getAppId), "Application not found")
      _ <- EitherT.fromEither[IO](verifyToken(token, appInfo))
      jwt <- EitherT.pure[IO, String](Jwt.fromJavaToken(token))
      mapped <- EitherT(jwtProcessor(jwt))
    } yield mapped).value

  private def extractTokenFromHeader(header: String): Either[String, String] =
    header match {
      case s"Bearer $jwt" => Right(jwt)
      case _              => Left("Invalid Authorization header")
    }

  private def parseToken(raw: String): Either[String, Token] =
    Try(Token.parse(raw)).toEither.left.map(_.getMessage)

  private def verifyToken(token: Token, appInfo: ApplicationInfo): Either[String, Unit] =
    Try(TokenPublicKey.fromString(appInfo.key).verify(token)).toEither.left.map(_.getMessage)
      .flatMap(v => if v then Right(()) else Left("Invalid JWT signature"))
      .flatMap(_ => if !token.isExpired then Right(()) else Left("Expired JWT"))
}

object JwtAuthenticator {
  def apply(identityClient: IdentityClient): JwtAuthenticator[Jwt] = new JwtAuthenticator(identityClient)
}
