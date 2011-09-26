import sbtappengine.Plugin.{AppengineKeys => gae}

name := "sample"

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-filter" % "0.5.0",
  "javax.servlet" % "servlet-api" % "2.3" % "provided"
)

seq(appengineSettings: _*)
