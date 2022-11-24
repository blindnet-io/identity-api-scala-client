package io.blindnet.identityclient
package auth

import cats.data.EitherT
import cats.effect.*
import io.blindnet.jwt.{Token, TokenPublicKey}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*

import java.util.UUID
import scala.util.Try

case class ConstAuthenticator[T] (
  token: String,
  value: IO[T],
  private val baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint,
) {
  def withBaseEndpoint(endpoint: PublicEndpoint[Unit, Unit, Unit, Any]): ConstAuthenticator[T] =
    copy(baseEndpoint = endpoint)

  def secureEndpoint: PartialServerEndpoint[Option[String], T, Unit, (StatusCode, String), Unit, Any, IO] =
    baseEndpoint
      .securityIn(header[Option[String]]("Authorization"))
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .serverSecurityLogic(authenticateHeader(_).map(_.left.map((StatusCode.Unauthorized, _))))

  def authenticateHeader(headerOption: Option[String]): IO[Either[String, T]] =
    headerOption match
      case Some(header) => authenticateHeader(header)
      case None => IO.pure(Left("Missing Authorization header"))

  def authenticateHeader(header: String): IO[Either[String, T]] =
    EitherT.fromEither[IO](extractTokenFromHeader(header))
      .flatMap(token => EitherT(authenticateToken(token)))
      .value

  def authenticateToken(raw: String): IO[Either[String, T]] =
    if raw == token then value.map(Right(_))
    else IO.pure(Left("Invalid token"))

  private def extractTokenFromHeader(header: String): Either[String, String] =
    header match {
      case s"Bearer $token" => Right(token)
      case _              => Left("Invalid Authorization header")
    }
}
