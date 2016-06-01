name := "tic-slack-toe"

// If the CI supplies a "build.version" environment variable, inject it as the rev part of the version number:
version := "0.0.1"

scalaVersion := "2.11.8"

organization := "kuhnhausen"

libraryDependencies ++= Seq(
    "org.scalatest"       %% "scalatest"     % "2.2.6" % "test",
    "com.github.finagle"  %% "finch-core"    % "0.10.0",
    "com.github.finagle"  %% "finch-circe"   % "0.10.0",
    "io.circe"            %% "circe-generic" % "0.3.0"
)

resolvers ++= Seq(  "oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
                    "oss-releases"  at "https://oss.sonatype.org/content/repositories/releases",
                    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
