import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

import com.typesafe.sbt.SbtStartScript

object MainBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      "NativeLibs4Java Respository" at "http://nativelibs4java.sourceforge.net/maven/",
      "Sonatype OSS Snapshots Repository" at "http://oss.sonatype.org/content/groups/public",
      "repo.codahale.com" at "http://repo.codahale.com"
    )
  )

  val scalaVersionString = "2.10.1"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      "nebula" %% "nebula" % "0.1-SNAPSHOT",
      //"org.scala-saddle" %% "saddle" % "1.1.+",
//      "commons-lang" % "commons-lang" % "2.6",
      //"com.nativelibs4java" %% "scalaxy-debug" % "0.3-SNAPSHOT" % "provided",
      "org.rogach" %% "scallop" % "0.9.1",
      "org.apache.commons" % "commons-math3" % "3.2",
      "commons-io" % "commons-io" % "2.4",
      "org.scala-lang" % "scala-reflect" % scalaVersionString,
      "org.scala-lang" % "scala-compiler" % scalaVersionString,
      "org.scala-lang" % "scala-actors" % scalaVersionString,
      //"org.imgscalr" % "imgscalr-lib" % "4.2",
//      "org.apache.commons" % "commons-math" % "2.2",
//      "commons-io" % "commons-io" % "2.3",
      //"com.frugalmechanic" % "scala-optparse" % "1.1",
      "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
//      "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
//      "org.scala-tools" %% "scala-stm" % "0.6",
//      "net.liftweb" % "lift-json_2.9.1" % "2.4-RC1",
      "org.scalanlp" %% "breeze-math" % "0.2.3",
//      "org.scalanlp" %% "breeze-learn" % "0.1",
//      "org.scalanlp" %% "breeze-process" % "0.1",
//      "org.scalanlp" %% "breeze-viz" % "0.1",
      "org.jsoup" % "jsoup" % "1.7.2",
      "rome" % "rome" % "1.0",
      "com.googlecode.flickrj-android" % "flickrj-android" % "2.0.7",
      "org.json" % "json" % "20090211",
      "org.spire-math" %% "spire" % "0.4.0",
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "com.chuusai" % "shapeless_2.10.0-RC5" % "1.2.4-SNAPSHOT"
//      "org.slf4j" % "slf4j-api" % "1.6.6"
    )
  )

  def scalaSettings = Seq(
    scalaVersion := scalaVersionString,
    scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-language:existentials",
//      "-language:reflectiveCalls",
      "-optimize",
      "-unchecked",
      "-feature",
      "-deprecation"
    )
  )

  // def compiletsSettings = Seq(
  //   autoCompilets := true,
  //   addDefaultCompilets()
  // )

  def libSettings = Project.defaultSettings ++ extraResolvers ++ extraLibraryDependencies ++ scalaSettings ++ assemblySettings ++ SbtStartScript.startScriptForJarSettings

  lazy val root = {
    val longName = "Tangram"
    val settings = libSettings ++ Seq(name := longName)
    Project(id = longName, base = file("."), settings = settings)
  }
}
