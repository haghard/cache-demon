import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._
import scalariform.formatter.preferences._

organization := "com.carjump"

name := "cache-demon"

val Akka = "2.4.11"

useJGit
enablePlugins(GitVersioning)

version := "0.0.1"

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)

net.virtualvoid.sbt.graph.Plugin.graphSettings

resolvers ++= Seq("maven central"  at "http://repo.maven.apache.org/maven2")

val sharedSettings = Seq(scalaVersion := "2.11.8")

lazy val cacheDemon = project.in(file(".")).aggregate(demon)

lazy val agent = project
  .settings(
    sharedSettings,

    organization := "com.carjump",
    name := "agent",

    assembleArtifact in assemblyPackageScala := false,

    assemblyJarName in assembly := "agent.jar",
    packageOptions in (Compile, packageBin) += Package.ManifestAttributes( "Premain-Class" -> "com.carjump.agent.Agent")
  )

lazy val demon = project
  .dependsOn(agent)
  .settings(
    sharedSettings,
    fork in run := true,

    name := "demon",

    libraryDependencies ++= Seq(
      "net.ceedubs"         %%  "ficus"                   %   "1.1.2",
      "com.typesafe.akka"   %%  "akka-stream"             %   Akka,
      "com.typesafe.akka"   %%  "akka-http-experimental"  %   Akka,
      "com.typesafe.akka"   %%  "akka-slf4j"              %   Akka,
      "com.twitter"         %%  "util-jvm"                %   "6.38.0",
      "com.lihaoyi"         %%  "pprint"                  %   "0.4.1",
      "org.typelevel"       %%  "cats-core"               %   "0.7.2",
      //"org.hdrhistogram"  %   "HdrHistogram"            %   "2.1.9",
      "ch.qos.logback"      %   "logback-classic"         %   "1.1.2",
      "org.scalatest"       %%  "scalatest"               %   "2.2.5"   % "test",
      "com.typesafe.akka"   %%  "akka-testkit"            %   Akka      % "test"
    ),


    scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-Ywarn-dead-code", "-feature",
      "-language:implicitConversions", "-language:postfixOps", "-language:existentials",
      "-feature", "-deprecation", "-language:implicitConversions",
      "-language:higherKinds", "-language:reflectiveCalls", "-Yno-adapted-args", "-target:jvm-1.8"),

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"),

    javaOptions in run += ("-javaagent:" + (packageBin in (agent, Compile)).value),

    mainClass in assembly := Some("com.carjump.Application"),
    assemblyJarName in assembly := "demon.jar"
)
