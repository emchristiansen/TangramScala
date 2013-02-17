addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.6")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.6.0")

//addSbtPlugin("com.nativelibs4java" % "sbt-scalaxy" % "0.3-SNAPSHOT")

//resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers += Resolver.url("sbt-plugin-snapshots", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)

//resolvers += Resolver.sonatypeRepo("snapshots")