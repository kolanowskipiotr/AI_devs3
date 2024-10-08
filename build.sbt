ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "AI_devs3",
    idePackagePrefix := Some("pl.pko.ai.devs3"),
    javacOptions ++= Seq("-source", "21", "-target", "21"),
    libraryDependencies := Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.5",
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server-sync" % "1.11.5",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.11.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-monix" % "3.9.7",
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.7",
      "io.circe" %% "circe-generic" % "0.14.7"
    )
  )
