ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"

Test / fork := true

lazy val root = (project in file("."))
  .settings(
    name := "identity-client",
    organization := "io.blindnet",
    organizationName := "blindnet",
    organizationHomepage := Some(url("https://blindnet.io")),
    idePackagePrefix := Some("io.blindnet.identityclient")
  )
