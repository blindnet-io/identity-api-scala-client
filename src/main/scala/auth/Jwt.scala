package io.blindnet.identityclient
package auth

import cats.effect.*
import io.blindnet.jwt.Token
import io.circe.*

import java.util.UUID

sealed trait Jwt {
  val appId: UUID

  def asApp: Either[String, AppJwt] = as(classOf[AppJwt])
  def asAnyUser: Either[String, AnyUserJwt] = as(classOf[AnyUserJwt])
  def asUser: Either[String, UserJwt] = as(classOf[UserJwt])
  def asAnonymous: Either[String, AnonymousJwt] = as(classOf[AnonymousJwt])

  private def as[T](cl: Class[T]): Either[String, T] =
    if cl.isInstance(this)
    then Right(asInstanceOf[T])
    else Left("Wrong JWT type")
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
