import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import NativePackagerKeys._

organization := "com.haghard"

name := "cache-demon"

version := "0.0.1"

scalaVersion := "2.11.8"

val Akka = "2.4.14"
val AkkaHttp = "10.0.0"

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

mainClass in Compile := Some("com.carsharing.Application")

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
  "com.typesafe.akka"   %%  "akka-slf4j"              %   Akka,
  "com.typesafe.akka"   %%  "akka-http"               %   AkkaHttp,
  "com.typesafe.akka"   %%  "akka-http-spray-json"    %   AkkaHttp,
  "com.twitter"         %%  "util-jvm"                %   "6.39.0",
  "com.lihaoyi"         %%  "pprint"                  %   "0.4.3",
  "org.openjdk.jol"     %   "jol-core"                %   "0.6",
  "org.typelevel"       %%  "cats-core"               %   "0.8.0",
  "com.rklaehn"         %%  "radixtree"               %   "0.4.0",
  "ch.qos.logback"      %   "logback-classic"         %   "1.1.2"
)

libraryDependencies ++= Seq(
  "org.scalatest"     %%  "scalatest"         %   "3.0.1"   % "test",
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