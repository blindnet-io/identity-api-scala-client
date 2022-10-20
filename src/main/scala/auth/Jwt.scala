package io.blindnet.identityclient
package auth

import cats.effect.*
import io.blindnet.jwt.Token
import io.circe.*

import java.util.UUID
import scala.reflect.TypeTest

sealed trait Jwt {
  val appId: UUID

  def asApp: Option[AppJwt] = as[AppJwt]
  def asAnyUser: Option[AnyUserJwt] = as[AnyUserJwt]
  def asUser: Option[UserJwt] = as[UserJwt]
  def asAnonymous: Option[AnonymousJwt] = as[AnonymousJwt]

  private def as[T <: Jwt](using TypeTest[Jwt, T]): Option[T] =
    this match
      case t: T => Some(t)
      case _    => None
}

object Jwt {
  def fromJavaToken(j: Token): Jwt =
    j.getType match
      case Token.Type.APPLICATION => AppJwt(j.getAppId)
      case Token.Type.USER => UserJwt(j.getAppId, j.getUserId)
      case Token.Type.ANONYMOUS => AnonymousJwt(j.getAppId)
}

case class AppJwt(appId: UUID) extends Jwt

sealed trait AnyUserJwt extends Jwt
case class UserJwt(appId: UUID, userId: String) extends AnyUserJwt
case class AnonymousJwt(appId: UUID) extends AnyUserJwt
