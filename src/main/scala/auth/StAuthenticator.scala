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
  private val stProcessor: T => IO[Either[String, R]] = (st: T) => IO.pure(Right(st)),
) extends Authenticator[R] {

  def mapSt[S](f: R => S): StAuthenticator[T, S] =
    copy(stProcessor = stProcessor.andThen(e => EitherT(e).map(f).value))
  def mapStF[S](f: R => IO[S]): StAuthenticator[T, S] =
    copy(stProcessor = stProcessor.andThen(e => EitherT(e).semiflatMap(f).value))
  def flatMapSt[S](f: R => Either[String, S]): StAuthenticator[T, S] =
    copy(stProcessor = stProcessor.andThen(e => EitherT(e).subflatMap(f).value))
  def flatMapStF[S](f: R => IO[Either[String, S]]): StAuthenticator[T, S] =
    copy(stProcessor = stProcessor.andThen(e => EitherT(e).flatMap(t => EitherT(f(t))).value))

  def authenticateToken(raw: String): IO[Either[String, R]] =
    (for {
      token <- EitherT.fromOptionF(repo.findByToken(raw), "Invalid token")
      mapped <- EitherT(stProcessor(token))
    } yield mapped).value
}

object StAuthenticator {
  def apply[T <: St](repo: StRepository[T, IO]): StAuthenticator[T, T] = new StAuthenticator(repo)
}
