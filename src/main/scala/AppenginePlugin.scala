package sbtappengine

import sbt._
import sbt.Process._

object AppenginePlugin {
  import Keys._
  import Project.Initialize
  import com.github.siasia.WebPlugin._

  val Appengine = config("appengine")

  val requestLogs   = InputKey[Unit]("request-logs", "Write request logs in Apache common log format.")
  val rollback      = InputKey[Unit]("rollback", "Rollback an in-progress update.")
  val deploy        = InputKey[Unit]("deploy", "Create or update an app version.")
  val deployIndexes = InputKey[Unit]("deploy-indexes", "Update application indexes.")
  val deployCron    = InputKey[Unit]("deploy-cron", "Update application cron jobs.")
  val deployQueues  = InputKey[Unit]("deploy-queues", "Update application task queue definitions.")
  val deployDos     = InputKey[Unit]("deploy-dos", "Update application DoS protection configuration.")
  val cronInfo      = InputKey[Unit]("cron-info", "Displays times for the next several runs of each cron job.")

  val sdkVersion    = SettingKey[String]("sdk-version")
  val appengineSdkPath = SettingKey[File]("appengine-sdk-path")
  val appengineClasspath = SettingKey[Classpath]("appengine-classpath")
  val appengineApiJarName = SettingKey[String]("appengine-api-jar-name")
  val appengineApiLabsJarName = SettingKey[String]("appengine-api-labs-jar-name")
  val appengineJSR107CacheJarName = SettingKey[String]("appengine-jsr107-cache-jar-name")
  val jsr107CacheJarName = SettingKey[String]("jsr107-cache-jar-name")
  val appengineBinPath = SettingKey[File]("appengine-bin-path")
  val appengineLibPath = SettingKey[File]("appengine-lib-path")
  val appengineLibUserPath = SettingKey[File]("appengine-lib-user-path")
  val appengineLibImplPath = SettingKey[File]("appengine-lib-impl-path")
  val appengineApiJarPath = SettingKey[File]("appengine-api-jar-path")
  val appcfgName    = SettingKey[String]("appcfg-name")
  val appcfgPath    = SettingKey[File]("appcfg-path")
  val emptyMap      = TaskKey[Seq[(File, String)]]("empty-map")

  private def appcfgTask(action: String, outputFile: Option[String],
                         args: TaskKey[Seq[String]], depends: TaskKey[Seq[(File, String)]] = emptyMap) =
    (args, temporaryWarPath, appcfgPath, streams, depends) map { (args, w, appcfgPath, s, m) =>
      val terminal = (Some(jline.Terminal.getTerminal)
        filter {_.isInstanceOf[jline.UnixTerminal]}
        map {_.asInstanceOf[jline.UnixTerminal] })
      val command: ProcessBuilder = <x>
        {appcfgPath.absolutePath} {args.mkString(" ")} {action} {w.absolutePath} {outputFile.mkString}
      </x>
      s.log.debug("Executing command " + command)
      terminal.foreach(_.restoreTerminal)
      try {
        val exitValue = command.run(true).exitValue() // don't buffer output
        if(exitValue == 0) None
        else Some("Nonzero exit value: " + exitValue)
        ()
      } finally {
        terminal.foreach(_.initializeTerminal)
      }
    }

  private def buildAppengineSdkPath: File = {
    val sdk = System.getenv("APPENGINE_SDK_HOME")
    if (sdk == null) error("You need to set APPENGINE_SDK_HOME")
    new File(sdk)
  }

  private def buildSdkVersion(libUserPath: File): String = {
    val pat = """appengine-api-1.0-sdk-(\d\.\d\.\d(?:\.\d)*)\.jar""".r
    (libUserPath * "appengine-api-1.0-sdk-*.jar").get.toList match {
      case jar::_ => jar.name match {
        case pat(version) => version
        case _ => error("invalid jar file. " + jar)
      }
      case _ => error("not found appengine api jar.")
    }
  }

  def isWindows = System.getProperty("os.name").startsWith("Windows")
  def osBatchSuffix = if (isWindows) ".cmd" else ".sh"

  lazy val settings = webSettings ++ inConfig(Appengine)(Seq(
    prepareWebapp    <<= (prepareWebapp in Compile).identity,
    temporaryWarPath <<= (temporaryWarPath in Compile).identity,
    webappResources  <<= (webappResources in Compile).identity,
    webappUnmanaged  <<= (temporaryWarPath) { (dir) => dir / "WEB-INF" / "appengine-generated" *** },
    unmanagedClasspath  <++= (appengineClasspath) map { (cp) => cp },

    requestLogs <<= inputTask { (args: TaskKey[Seq[String]])   => appcfgTask("request_logs", Some("request.log"), args) },
    rollback <<= inputTask { (args: TaskKey[Seq[String]])      => appcfgTask("rollback", None, args) },
    deploy <<= inputTask { (args: TaskKey[Seq[String]])        => appcfgTask("update", None, args, prepareWebapp) },
    deployIndexes <<= inputTask { (args: TaskKey[Seq[String]]) => appcfgTask("update_indexes", None, args, prepareWebapp) },
    deployCron <<= inputTask { (args: TaskKey[Seq[String]])    => appcfgTask("update_cron", None, args, prepareWebapp) },
    deployQueues <<= inputTask { (args: TaskKey[Seq[String]])  => appcfgTask("update_queues", None, args, prepareWebapp) },
    deployDos <<= inputTask { (args: TaskKey[Seq[String]])     => appcfgTask("update_dos", None, args, prepareWebapp) },
    cronInfo <<= inputTask { (args: TaskKey[Seq[String]])      => appcfgTask("cron_info", None, args) },

    sdkVersion <<= (appengineLibUserPath) { (dir) => buildSdkVersion(dir) },
    appengineSdkPath := buildAppengineSdkPath,
    appengineClasspath <<= (appengineApiJarPath) { (jar: File) => Attributed.blankSeq(Seq(jar)) },
    appengineApiJarName <<= (sdkVersion) { (v) => "appengine-api-1.0-sdk-" + v + ".jar" },
    appengineApiLabsJarName <<= (sdkVersion) { (v) => "appengine-api-labs-" + v + ".jar" },
    appengineJSR107CacheJarName <<= (sdkVersion) { (v) => "appengine-jsr107cache-" + v + ".jar" },
    jsr107CacheJarName := "jsr107cache-1.1.jar",

    appengineBinPath <<= (appengineSdkPath) { (dir) => dir / "bin" },
    appengineLibPath <<= (appengineSdkPath) { (dir) => dir / "lib" },
    appengineLibUserPath <<= (appengineLibPath) { (dir) => dir / "user" },
    appengineLibImplPath <<= (appengineLibPath) { (dir) => dir / "impl" },
    appengineApiJarPath <<= (appengineLibUserPath, appengineApiJarName) { (dir, name) => dir / name },
    appcfgName := "appcfg" + osBatchSuffix,
    appcfgPath <<= (appengineBinPath, appcfgName) { (dir, name) => dir / name },
    emptyMap := Nil
  )) ++
  inConfig(Test)(Seq(
    unmanagedClasspath <++= (appengineClasspath) map { (cp) => cp },
    appengineClasspath <<= (appengineClasspath in Appengine,
      appengineLibImplPath in Appengine, appengineLibPath in Appengine) { (cp, impl, lib) =>
      val impljars = (impl * "*.jar").get
      val testingjars = (lib / "testing" * "*.jar").get
      cp ++ Attributed.blankSeq(impljars ++ testingjars)
    }
  ))

  /*
  def appengineToolsJarPath = (appengineLibPath / "appengine-tools-api.jar")
  */
  /* Overrides jar present as of sdk 1.4.3 */
  /*
  def appengineOverridePath = appengineSdkPath / "lib" / "override"
  def overridesJarName = "appengine-dev-jdk-overrides.jar"
  def appengineOverridesJarPath = appengineOverridePath / overridesJarName

  lazy val javaCmd = (Path.fromFile(new java.io.File(System.getProperty("java.home"))) / "bin" / "java").absolutePath

  def appengineAgentPath = appengineLibPath / "agent" / "appengine-agent.jar"

  def devAppserverJvmOptions:Seq[String] = List()
  lazy val devAppserverInstance = new DevAppserverRun
  lazy val devAppserverStart = devAppserverStartAction
  lazy val devAppserverStop = devAppserverStopAction
  def devAppserverStartAction = task{ args => devAppserverStartTask(args) dependsOn(prepareWebapp) }
  def devAppserverStopAction = devAppserverStopTask
  def devAppserverStartTask(args: Seq[String]) = task {devAppserverInstance(args)}
  def devAppserverStopTask = task {
    devAppserverInstance.stop()
    None
  }

  class DevAppserverRun extends Runnable with ExitHook {
    ExitHooks.register(this)
    def name = "dev_appserver-shutdown"
    def runBeforeExiting() { stop() }

    val jvmOptions =
      List("-ea", "-javaagent:"+appengineAgentPath.absolutePath,
           "-cp", appengineToolsJarPath.absolutePath,
           "-Xbootclasspath/p:"+appengineOverridesJarPath) ++ devAppserverJvmOptions

    private var running: Option[Process] = None

    def run() {
      running.foreach(_.exitValue())
      running = None
    }

    def apply(args: Seq[String]): Option[String] = {
      if (running.isDefined)
        Some("An instance of dev_appserver is already running.")
      else {
        val builder: ProcessBuilder =
          Process(javaCmd :: jvmOptions :::
                  "com.google.appengine.tools.development.DevAppServerMain" ::
                  args.toList ::: temporaryWarPath.absolutePath :: Nil,
                  Some(temporaryWarPath.asFile))
        running = Some(builder.run())
        new Thread(this).start()
        None
      }
    }

    def stop() {
      running.foreach(_.destroy)
      running = None
      log.debug("stop")
    }
  }
  */
}

/*
trait DataNucleus extends AppengineProject {
  override def prepareWebappAction = super.prepareWebappAction dependsOn(enhance)

  val appengineORMJarsPath = AppenginePathFinder(appengineLibUserPath / "orm" * "*.jar")
  def appengineORMEnhancerClasspath = (appengineLibPath / "tools" / "orm" * "datanucleus-enhancer-*.jar")  +++ (appengineLibPath / "tools" / "orm" * "asm-*.jar")

  lazy val enhance = enhanceAction
  lazy val enhanceCheck = enhanceCheckAction
  def enhanceAction = enhanceTask(false) dependsOn(compile) describedAs("Executes ORM enhancement.")
  def enhanceCheckAction = enhanceTask(true) dependsOn(compile) describedAs("Just check the classes for enhancement status.")
  def usePersistentApi = "jdo"
  def enhanceTask(checkonly: Boolean) =
    runTask(Some("org.datanucleus.enhancer.DataNucleusEnhancer"),
      appengineToolsJarPath +++ appengineORMEnhancerClasspath +++ compileClasspath ,
      List("-v",
           "-api", usePersistentApi,
           (if(checkonly) "-checkonly" else "")) ++
      mainClasses.get.map(_.absolutePath))
}

trait JRebel extends AppengineProject {
  override def devAppserverJvmOptions =
    if (jrebelPath.isDefined)
      List("-javaagent:" + jrebelPath.get.absolutePath,
           "-noverify") ++ jrebelJvmOptions ++ super.devAppserverJvmOptions
    else
      super.devAppserverJvmOptions

  def jrebelJvmOptions:Seq[String] = List()
  def jrebelPath = {
    val jrebel = System.getenv("JREBEL_JAR_PATH")
    if (jrebel == null) {
      log.error("You need to set JREBEL_JAR_PATH")
      None
    } else
      Some(Path.fromFile(new File(jrebel)))
  }

}
*/
