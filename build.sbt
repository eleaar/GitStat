name := """GitStat"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  cache,
  ws
)

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "com.github.nscala-time" %% "nscala-time" % "1.6.0"
)

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "jquery" % "1.8.2",
  "org.webjars" % "bootstrap" % "3.3.1",
  "org.webjars" % "highcharts" % "4.0.4",
  "org.webjars" % "highstock" % "2.0.4"
)

libraryDependencies ++= Seq(
  "org.scalatestplus" %% "play" % "1.2.0",
  "org.mockito" % "mockito-core" % "1.10.8",
  "de.leanovate.play-mockws" %% "play-mockws" % "0.13",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4"
).map(_ % "test")
