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

case class StAuthenticator[T <: St, R] (
  repo: StRepository[T, IO],
  private val baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint,
  private val stMapper: T => IO[Either[String, R]] = (st: T) => IO.pure(Right(st)),
) {
  def withBaseEndpoint(endpoint: PublicEndpoint[Unit, Unit, Unit, Any]): StAuthenticator[T, R] =
    copy(baseEndpoint = endpoint)

  def mapSt[S](f: R => S): StAuthenticator[T, S] =
    copy(stMapper = stMapper.andThen(e => EitherT(e).map(f).value))
  def mapStF[S](f: R => IO[S]): StAuthenticator[T, S] =
    copy(stMapper = stMapper.andThen(e => EitherT(e).semiflatMap(f).value))
  def flatMapSt[S](f: R => Either[String, S]): StAuthenticator[T, S] =
    copy(stMapper = stMapper.andThen(e => EitherT(e).subflatMap(f).value))
  def flatMapStF[S](f: R => IO[Either[String, S]]): StAuthenticator[T, S] =
    copy(stMapper = stMapper.andThen(e => EitherT(e).flatMap(t => EitherT(f(t))).value))

  def secureEndpoint: PartialServerEndpoint[Option[String], R, Unit, (StatusCode, String), Unit, Any, IO] =
    baseEndpoint
      .securityIn(header[Option[String]]("Authorization"))
      .errorOut(statusCode)
      .errorOut(jsonBody[String])
      .serverSecurityLogic(authenticateHeader(_).map(_.left.map((StatusCode.Unauthorized, _))))

  def authenticateHeader(headerOption: Option[String]): IO[Either[String, R]] =
    headerOption match
      case Some(header) => authenticateHeader(header)
      case None => IO.pure(Left("Missing Authorization header"))

  def authenticateHeader(header: String): IO[Either[String, R]] =
    EitherT.fromEither[IO](extractTokenFromHeader(header))
      .flatMap(token => EitherT(authenticateToken(token)))
      .value

  def authenticateToken(raw: String): IO[Either[String, R]] =
    (for {
      token <- EitherT.fromOptionF(repo.findByToken(raw), "Invalid token")
      mapped <- EitherT(stMapper(token))
    } yield mapped).value

  private def extractTokenFromHeader(header: String): Either[String, String] =
    if header.startsWith("Bearer ")
    then Right(header.substring(7))
    else Left("Invalid Authorization header")
}

object StAuthenticator {
  def apply[T <: St](repo: StRepository[T, IO]): StAuthenticator[T, T] = new StAuthenticator(repo)
}
