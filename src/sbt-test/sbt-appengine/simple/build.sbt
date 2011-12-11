import sbtappengine.Plugin.{AppengineKeys => gae}

name := "sample"

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-filter" % "0.5.0",
  "javax.servlet" % "servlet-api" % "2.3" % "provided",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
)

seq(appengineSettings: _*)
