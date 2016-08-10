name := """tamtams"""

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
   "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// fork in run seems to have problems :
fork in run := false


fork in run := true