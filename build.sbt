sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

version := "0.3.1"

libraryDependencies <+= (sbtVersion) { sv =>
  "com.github.siasia" %% "xsbt-web-plugin" % (sv + "-0.2.10")
}

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo <<= version { (v: String) =>
  if(v endsWith "-SNAPSHOT") Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/")
  else Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

resolvers += "Maven.org" at "http://repo1.maven.org/maven2"
