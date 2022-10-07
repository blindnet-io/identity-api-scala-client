package io.blindnet.identityclient

import cats.effect.*
import org.http4s.*
import org.http4s.blaze.client.*
import org.http4s.client.*
import org.http4s.implicits.*

case class IdentityClientBuilder (
  baseUri: Uri = uri"https://identity.devkit.blindnet.io",
) {
  def withBaseUri(baseUri: Uri): IdentityClientBuilder = copy(baseUri = baseUri)

  def resource: Resource[IO, IdentityClient] =
    BlazeClientBuilder[IO].resource
      .map(client => IdentityClient(client, baseUri))
}
