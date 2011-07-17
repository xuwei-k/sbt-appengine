sbtPlugin := true

name := "sbt-appengine"

organization := "com.eed3si9n"

posterousNotesVersion := "0.2"

version <<= (sbtVersion, posterousNotesVersion) { (sv, nv) => "sbt" + sv + "_" + nv }

libraryDependencies <+= (sbtVersion) { (sv) => "com.github.siasia" %% "xsbt-web-plugin" % ("0.1.0-" + sv) }

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

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"
