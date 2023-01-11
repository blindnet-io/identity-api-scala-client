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

trait Authenticator[T] {

  def or(other: Authenticator[T]): Authenticator[T] = {
    val that = this

    new Authenticator[T] {
      def authenticateToken(raw: String): IO[Either[String, T]] =
        other.authenticateToken(raw).flatMap {
          case Right(t) => IO(Right(t))
          case Left(e) => that.authenticateToken(raw)
        }
    }
  }

  def secureEndpoint(baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any]): PartialServerEndpoint[Option[String], T, Unit, AuthException, Unit, Any, IO] =
    baseEndpoint
      .securityIn(header[Option[String]]("Authorization"))
      .errorOut(statusCode(StatusCode.Unauthorized).and(jsonBody[String].mapTo[AuthException]))
      .serverSecurityLogic(authenticateHeader(_).map(_.left.map(AuthException(_))))

  def authenticateHeader(headerOption: Option[String]): IO[Either[String, T]] =
    headerOption match
      case Some(header) =>
        EitherT.fromEither[IO](extractTokenFromHeader(header))
          .flatMap(token => EitherT(authenticateToken(token)))
          .value
      case None => IO.pure(Left("Missing Authorization header"))
    
  def extractTokenFromHeader(header: String): Either[String, String] =
    header match {
      case s"Bearer $token" => Right(token)
      case _              => Left("Invalid Authorization header")
    }

  def authenticateToken(raw: String): IO[Either[String, T]]
}
