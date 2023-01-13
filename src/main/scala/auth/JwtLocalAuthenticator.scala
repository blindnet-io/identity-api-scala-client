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

case class JwtLocalAuthenticator[T] (
  private val identityKey: String,
  private val processor: AppJwt => IO[Either[String, T]] = jwt => IO.pure(Right(jwt)),
) extends Authenticator[T] {

  def mapJwt[R](f: T => R): JwtLocalAuthenticator[R] =
    copy(processor = processor.andThen(e => EitherT(e).map(f).value))
  def mapJwtF[R](f: T => IO[R]): JwtLocalAuthenticator[R] =
    copy(processor = processor.andThen(e => EitherT(e).semiflatMap(f).value))
  def flatMapJwt[R](f: T => Either[String, R]): JwtLocalAuthenticator[R] =
    copy(processor = processor.andThen(e => EitherT(e).subflatMap(f).value))
  def flatMapJwtF[R](f: T => IO[Either[String, R]]): JwtLocalAuthenticator[R] =
    copy(processor = processor.andThen(e => EitherT(e).flatMap(t => EitherT(f(t))).value))

  def authenticateToken(raw: String): IO[Either[String, T]] =
    (for {
      token <- EitherT.fromEither[IO](parseToken(raw))
      _ <- EitherT.fromEither[IO](verifyToken(token, identityKey))
      jwt <- EitherT.fromOption[IO](Jwt.fromJavaToken(token).asApp, "Wrong JWT type")
      mapped <- EitherT(processor(jwt))
    } yield mapped).value

  private def parseToken(raw: String): Either[String, Token] =
    Try(Token.parse(raw)).toEither.left.map(_.getMessage)

  private def verifyToken(token: Token, identityKey: String): Either[String, Unit] =
    Try(TokenPublicKey.fromString(identityKey).verify(token)).toEither.left.map(_.getMessage)
      .flatMap(v => if v then Right(()) else Left("Invalid JWT signature"))
      .flatMap(_ => if !token.isExpired then Right(()) else Left("Expired JWT"))
}

object JwtLocalAuthenticator {
  def apply(identityKey: String): JwtLocalAuthenticator[AppJwt] = new JwtLocalAuthenticator(identityKey)
}
