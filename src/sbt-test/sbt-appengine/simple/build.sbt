import sbtappengine.Plugin.{AppengineKeys => gae}

name := "sample"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  "ws.unfiltered" %% "unfiltered-filter" % "0.9.1",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
)

appengineSettings

(gae.onStartHooks in gae.devServer in Compile) += { () =>
  println("hello")
}

(gae.onStopHooks in gae.devServer in Compile) += { () =>
  println("bye")
}

appengineDataNucleusSettings

gae.persistenceApi in gae.enhance in Compile := "JDO"
