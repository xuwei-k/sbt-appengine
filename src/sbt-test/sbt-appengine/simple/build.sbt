name := "sample"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-filter" % "0.6.4",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
)

enablePlugins(AppenginePlugin)

(appengineOnStartHooks in appengineDevServer in Compile) += { () =>
  println("hello")
}

(appengineOnStopHooks in appengineDevServer in Compile) += { () =>
  println("bye")
}

appengineDataNucleusSettings

appenginePersistenceApi in appengineEnhance in Compile := "JDO"
