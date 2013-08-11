sbt-appengine is a sbt 0.10+ port of the awesome [sbt-appengine-plugin][1] by [yasushi][2].

requirements
------------

export environment variables (`JREBEL_PATH` is optional).

    export APPENGINE_SDK_HOME=/Applications/appengine-java-sdk-1.6.2.1
    export JREBEL_PATH=/Applications/ZeroTurnaround/JRebel/jrebel.jar

setup
-----

put the following in the `project/plugins.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-appengine" % "0.5.0")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.cc",
  Resolver.url("sbt-plugin-releases",
    url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
)
```

for `build.sbt`:

```scala
libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % "7.6.8.v20121106" % "container"

appengineSettings
```

for `build.scala`:

```scala
import sbtappengine.Plugin._
import AppengineKeys._

lazy val example = Project("web", file("web"),
  settings = buildSettings ++ appengineSettings ++
             Seq( // your settings here
             ))
```

usage
-----

### development server

you can now deploy your application like this:

    > appengine-deploy

to (re)start the development server in the background:

    > appengine-dev-server

to redeploy development server continuously:

    > ~ appengine-dev-server

### hot loading!

to hot reload development server continuously, set `JREBEL_PATH` and:

    > appengine-dev-server
    > ~ package-war

by default development server runs in debug mode. IDE can connect to it via port 1044.


### backend support

you can deploy your backend application(s) like this:

    > appengine-deploy-backends
    
to start a backend instance in the cloud:

    > appengine-start-backend <backend-name>
    
to stop a backend instance:

    > appengine-stop-backend <backend-name>


### `appengineDevServer` lifecyle hooks

```scala
(gae.onStartHooks in gae.devServer in Compile) += { () =>
  println("hello")
}

(gae.onStopHooks in gae.devServer in Compile) += { () =>
  println("bye")
}
```

### DataNucleous enhancer support (experimental)

sbt-appengine provides experimental support for DataNucleous enhancer. to use this, include the following in `build.sbt`:

```scala
appengineSettings

appengineDataNucleusSettings

gae.persistenceApi in gae.enhance in Compile := "JDO"
```

this will call the enhancer automatically on `packageWar` task. since DataNucleous expects plain Java fields, the entity class looks a bit ugly in Scala:

```scala
import javax.jdo.annotations._
import com.google.appengine.api.datastore.Key
import scala.annotation.target.field

@PersistenceCapable
case class Counter(
  @(PrimaryKey @field)
  @(Persistent @field)(valueStrategy = IdGeneratorStrategy.IDENTITY)
  var key: Key,
  @(Persistent @field)
  var count: Int)
```

sample
------

- [simple sample][3]

note
----

When trying to launch the dev server with `appengine-dev-server`, you might run
into the following exception: `java.lang.RuntimeException: Unable to restore the previous TimeZone`.
[This issue][4] has been resolved in the latest App Engine SDK.

  [1]: https://github.com/Yasushi/sbt-appengine-plugin
  [2]: https://github.com/Yasushi
  [3]: https://github.com/sbt/sbt-appengine/tree/master/src/sbt-test/sbt-appengine/simple
  [4]: http://code.google.com/p/googleappengine/issues/detail?id=6928
