package io.blindnet.identityclient

import cats.effect.*
import org.http4s.*
import org.http4s.blaze.client.*
import org.http4s.client.*
import org.http4s.implicits.*

case class IdentityClientBuilder (
  baseUri: Uri = defaultBaseUri,
  client: Option[Client[IO]] = None,
) {
  def withBaseUri(baseUri: Uri): IdentityClientBuilder = copy(baseUri = baseUri)

  def withClient(client: Client[IO]): IdentityClientBuilder = copy(client = Some(client))

  def resource: Resource[IO, IdentityClient] =
    client match
      case Some(client) => Resource.pure(IdentityClient(client, baseUri))
      case None =>
        BlazeClientBuilder[IO].resource
          .map(client => IdentityClient(client, baseUri))
}

object IdentityClientBuilder {
  val defaultBaseUri: Uri = uri"https://identity.devkit.blindnet.io"
}
