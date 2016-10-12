import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import NativePackagerKeys._

organization := "com.carjump"

name := "cache-demon"

version := "0.0.1"

scalaVersion := "2.11.8"

val Akka = "2.4.11"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-language:reflectiveCalls",
  "-Yno-adapted-args",
  "-target:jvm-1.8"
)

useJGit
enablePlugins(GitVersioning)
enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("com.carjump.Application")

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)

net.virtualvoid.sbt.graph.Plugin.graphSettings

resolvers ++= Seq("maven central"  at "http://repo.maven.apache.org/maven2")

libraryDependencies ++= Seq(
  "net.ceedubs"         %%  "ficus"                   %   "1.1.2", //config
  "com.typesafe.akka"   %%  "akka-stream"             %   Akka,
  "com.typesafe.akka"   %%  "akka-http-experimental"  %   Akka,
  "com.typesafe.akka"   %%  "akka-slf4j"              %   Akka,
  "com.lihaoyi"         %%  "pprint"                  %   "0.4.1",
  "org.typelevel"       %%  "cats-core"               %   "0.7.2",
  "ch.qos.logback"      %   "logback-classic"         %   "1.1.2"
)

libraryDependencies ++= Seq(
  "org.scalatest"     %%  "scalatest"         %   "2.2.5"   % "test",
  "com.typesafe.akka" %%  "akka-testkit"      %   Akka      % "test"
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-deprecation",
  "-unchecked",
  "-Ywarn-dead-code",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:existentials")

javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-Xlint:unchecked",
  "-Xlint:deprecation")

publishMavenStyle := true
