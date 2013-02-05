import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

object MainBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      "NativeLibs4Java Respository" at "http://nativelibs4java.sourceforge.net/maven/",
      "Sonatype OSS Snapshots Repository" at "http://oss.sonatype.org/content/groups/public",
      "repo.codahale.com" at "http://repo.codahale.com"
    )
  )

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
//      "commons-lang" % "commons-lang" % "2.6",
      "org.apache.commons" % "commons-math3" % "3.0",
      "org.scala-lang" % "scala-reflect" % "2.10.0",
      "org.scala-lang" % "scala-compiler" % "2.10.0",
      "org.scala-lang" % "scala-actors" % "2.10.0",
      "org.imgscalr" % "imgscalr-lib" % "4.2",
//      "org.apache.commons" % "commons-math" % "2.2",
//      "commons-io" % "commons-io" % "2.3",
      "com.frugalmechanic" % "scala-optparse" % "1.1",
      "org.scalatest" %% "scalatest" % "1.9.1" % "test",
//      "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
//      "org.scala-tools" %% "scala-stm" % "0.6",
//      "net.liftweb" % "lift-json_2.9.1" % "2.4-RC1",
      "org.scalanlp" %% "breeze-math" % "0.2-SNAPSHOT",
//      "org.scalanlp" %% "breeze-learn" % "0.1",
//      "org.scalanlp" %% "breeze-process" % "0.1",
//      "org.scalanlp" %% "breeze-viz" % "0.1",
      "org.jsoup" % "jsoup" % "1.7.1",
      "rome" % "rome" % "1.0",
      "com.googlecode.flickrj-android" % "flickrj-android" % "2.0.4",
      "org.json" % "json" % "20090211",
      "org.spire-math" %% "spire" % "0.3.0-RC1",
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "com.chuusai" % "shapeless_2.10.0-RC5" % "1.2.4-SNAPSHOT"
//      "org.slf4j" % "slf4j-api" % "1.6.6"
    )
  )

  def scalaSettings = Seq(
    scalaVersion := "2.10.0",
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

  def libSettings = Project.defaultSettings ++ extraResolvers ++ extraLibraryDependencies ++ scalaSettings ++ assemblySettings

  lazy val root = {
    val longName = "Tangram"
    val settings = libSettings ++ Seq(name := longName)
    Project(id = longName, base = file("."), settings = settings)
  }
}
