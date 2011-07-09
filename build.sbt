sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.1"

libraryDependencies += "com.github.siasia" %% "xsbt-web-plugin" % "0.1.0-0.10.0"

scalacOptions := Seq("-deprecation", "-unchecked")

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  if(v endsWith "-SNAPSHOT") Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/")
  else Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"
