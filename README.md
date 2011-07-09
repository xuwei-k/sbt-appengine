sbt-appengine is a sbt 0.10 port of the awesome [sbt-appengine-plugin][1] by [yasushi][2].

usage
=====
export environment variables (actually, JRebel support is not ported yet).

    export APPENGINE_SDK_HOME=~/appengine-java-sdk-1.5.0
    export JREBEL_JAR_PATH=~/jrebel/jrebel.jar

put the following in the `project/plugins/build.sbt`:

    libraryDependencies += "com.eed3si9n" %% "sbt-appengine" % "0.1"

or, specify sbt-assembly.git as a dependency in `project/plugins/project/build.scala`:

    import sbt._

    object Plugins extends Build {
      lazy val root = Project("root", file(".")) dependsOn(
        uri("git://github.com/eed3si9n/sbt-appengine.git")
      )
    }

then, define the project as follows:

    lazy val example = Project("web", file("web"),
      settings = Defaults.defaultSettings ++ sbtappengine.AppenginePlugin.webSettings)

you can now deploy your application like this:

    > appengine:deploy

  [1]: https://github.com/Yasushi/sbt-appengine-plugin
  [2]: https://github.com/Yasushi
