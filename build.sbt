ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"

Test / fork := true

val circeVersion = "0.14.3"
val http4sVersion = "0.23.12"
val tapirVersion = "1.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "identity-client",
    organization := "io.blindnet",
    organizationName := "blindnet",
    organizationHomepage := Some(url("https://blindnet.io")),
    idePackagePrefix := Some("io.blindnet.identityclient"),
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"                 % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"           % tapirVersion,
      "io.circe"                    %% "circe-core"                 % circeVersion,
      "io.circe"                    %% "circe-generic"              % circeVersion,
      "org.typelevel"               %% "cats-effect"                % "3.3.14",
      "org.http4s"                  %% "http4s-blaze-client"        % http4sVersion,
      "org.http4s"                  %% "http4s-circe"               % http4sVersion,
//      "org.slf4j"                   %  "slf4j-simple"               % "2.0.1",
      "io.blindnet"                 %  "jwt-java"                   % "1.0-SNAPSHOT",
      "io.circe"                    %% "circe-core"                 % circeVersion,
      "io.circe"                    %% "circe-generic"              % circeVersion,
    )
  )
