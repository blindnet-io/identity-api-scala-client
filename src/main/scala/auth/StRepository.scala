package io.blindnet.identityclient
package auth

abstract class StRepository[T <: St, F[_]] {
  def findByToken(token: String): F[Option[T]]
}
