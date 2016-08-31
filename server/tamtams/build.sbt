name := """tamtams"""

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

// for reactivemongo dependency injection to work
routesGenerator := InjectedRoutesGenerator


libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
   "org.reactivemongo" %% "reactivemongo-play-json" % "0.11.14"
)

resolvers ++= Seq(
  Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns),
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
)

// is this really useful? taken from reactivemongo
scalacOptions in ThisBuild ++= Seq("-feature", "-language:postfixOps")

// fork in run seems to have problems :
fork in run := false


//fork in run := true