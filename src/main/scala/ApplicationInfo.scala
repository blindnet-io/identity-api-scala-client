package io.blindnet.identityclient

import cats.effect.IO
import io.circe.*
import io.circe.generic.semiauto.*
import org.http4s.*
import org.http4s.circe.*

import java.util.UUID

case class ApplicationInfo(
  id: UUID,
  name: String,
  key: String,
)

object ApplicationInfo {
  implicit val decoder: Decoder[ApplicationInfo] = deriveDecoder[ApplicationInfo]
  implicit val entityDecoder: EntityDecoder[IO, ApplicationInfo] = jsonOf[IO, ApplicationInfo]
}
