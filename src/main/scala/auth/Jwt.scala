package io.blindnet.identityclient
package auth

import cats.effect.*
import io.blindnet.jwt.Token
import io.circe.*

import java.util.UUID

sealed trait Jwt {
  val appId: UUID

  def asApp: Option[AppJwt] = as(classOf[AppJwt])
  def asAnyUser: Option[AnyUserJwt] = as(classOf[AnyUserJwt])
  def asUser: Option[UserJwt] = as(classOf[UserJwt])
  def asAnonymous: Option[AnonymousJwt] = as(classOf[AnonymousJwt])

  private def as[T](cl: Class[T]): Option[T] =
    if cl.isInstance(this)
    then Some(asInstanceOf[T])
    else None
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
