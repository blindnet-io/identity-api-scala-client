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
) extends Authenticator[T] {

  def authenticateToken(raw: String): IO[Either[String, T]] =
    if raw == token then value.map(Right(_))
    else IO.pure(Left("Invalid token"))
}
