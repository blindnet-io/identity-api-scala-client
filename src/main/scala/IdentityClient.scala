package io.blindnet.identityclient

import cats.effect.*
import org.http4s.*
import org.http4s.client.*

import java.util.UUID

class IdentityClient(
  client: Client[IO],
  baseUri: Uri,
) {
  def getAppInfo(id: UUID): IO[Option[ApplicationInfo]] =
    val req = Request[IO](
      method = Method.GET,
      uri = baseUri.addPath(s"applications/$id")
    )

    client.expectOption[ApplicationInfo](req)
}
