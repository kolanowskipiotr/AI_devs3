val tapirVersion = "1.11.5"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "ai_devs3",
    version := "0.1.0-SNAPSHOT",
    organization := "pl.pko.ai.devs3",
    scalaVersion := "3.5.1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-monix" % "3.9.7",
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.7",
      "ch.qos.logback" % "logback-classic" % "1.5.8",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.8" % Test,
    )
  )
)
