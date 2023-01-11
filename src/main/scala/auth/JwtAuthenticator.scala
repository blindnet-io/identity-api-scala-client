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
  private val jwtProcessor: Jwt => IO[Either[String, T]] = jwt => IO.pure(Right(jwt)),
) extends Authenticator[T] {

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

  def authenticateToken(raw: String): IO[Either[String, T]] =
    (for {
      token <- EitherT.fromEither[IO](parseToken(raw))
      appInfo <- EitherT.fromOptionF(identityClient.getAppInfo(token.getAppId), "Application not found")
      _ <- EitherT.fromEither[IO](verifyToken(token, appInfo))
      jwt <- EitherT.pure[IO, String](Jwt.fromJavaToken(token))
      mapped <- EitherT(jwtProcessor(jwt))
    } yield mapped).value

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
