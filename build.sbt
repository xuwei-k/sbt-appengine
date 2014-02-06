sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.6.1"

description := "sbt plugin to deploy on appengine"

licenses := Seq("MIT License" -> url("https://github.com/sbt/sbt-appengine/blob/master/LICENSE"))

libraryDependencies <++= (scalaBinaryVersion in sbtPlugin, sbtBinaryVersion in sbtPlugin) { (scalaV, sv) => Seq(
  sv match {
    case "0.11.0" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.0-0.2.8"
    case "0.11.1" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.1-0.2.10"
    case "0.11.2" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.2-0.2.11"
    case "0.11.3" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.3-0.2.11.1"
    case "0.12"   => "com.github.siasia" %% "xsbt-web-plugin" % "0.12.0-0.2.11.1"
    case "0.13"   => "com.earldouglas" % "xsbt-web-plugin" % "0.4.0" extra("scalaVersion" -> scalaV, "sbtVersion" -> sv)
  },
  sv match {
    case "0.12" => "cc.spray" % "sbt-revolver" % "0.6.1" extra("scalaVersion" -> scalaV, "sbtVersion" -> sv)
    case "0.13" => "io.spray" % "sbt-revolver" % "0.7.1" extra("scalaVersion" -> scalaV, "sbtVersion" -> sv) 
  }
)}

scalacOptions := Seq("-deprecation", "-unchecked")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

resolvers += "Maven.org" at "http://repo1.maven.org/maven2"

resolvers += "spray repo" at "http://repo.spray.cc"

publishMavenStyle := false

publishTo <<= (version) { version: String =>
   val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
   val (name, u) = if (version.contains("-SNAPSHOT")) ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                   else ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, url(u))(Resolver.ivyStylePatterns))
}

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")
