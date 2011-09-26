package sbtappengine

import sbt._
import sbt.Process._

object Plugin extends sbt.Plugin {
  import Keys._
  import Project.Initialize
  import com.github.siasia.WebPlugin
  import WebPlugin._
  
  object AppengineKeys {
    lazy val requestLogs    = InputKey[Unit]("appengine-request-logs", "Write request logs in Apache common log format.")
    lazy val rollback       = InputKey[Unit]("appengine-rollback", "Rollback an in-progress update.")
    lazy val deploy         = InputKey[Unit]("appengine-deploy", "Create or update an app version.")
    lazy val deployIndexes  = InputKey[Unit]("appengine-deploy-indexes", "Update application indexes.")
    lazy val deployCron     = InputKey[Unit]("appengine-deploy-cron", "Update application cron jobs.")
    lazy val deployQueues   = InputKey[Unit]("appengine-deploy-queues", "Update application task queue definitions.")
    lazy val deployDos      = InputKey[Unit]("appengine-deploy-dos", "Update application DoS protection configuration.")
    lazy val cronInfo       = InputKey[Unit]("appengine-cron-info", "Displays times for the next several runs of each cron job.")
    
    lazy val sdkVersion     = SettingKey[String]("appengine-sdk-version")
    lazy val sdkPath        = SettingKey[File]("appengine-sdk-path")
    lazy val classpath      = SettingKey[Classpath]("appengine-classpath")
    lazy val apiJarName     = SettingKey[String]("appengine-api-jar-name")
    lazy val apiLabsJarName = SettingKey[String]("appengine-api-labs-jar-name")
    lazy val jsr107CacheJarName = SettingKey[String]("appengine-jsr107-cache-jar-name")
    lazy val binPath        = SettingKey[File]("appengine-bin-path")
    lazy val libPath        = SettingKey[File]("appengine-lib-path")
    lazy val libUserPath    = SettingKey[File]("appengine-lib-user-path")
    lazy val libImplPath    = SettingKey[File]("appengine-lib-impl-path")
    lazy val apiJarPath     = SettingKey[File]("appengine-api-jar-path")
    lazy val appcfgName     = SettingKey[String]("appengine-appcfg-name")
    lazy val appcfgPath     = SettingKey[File]("appengine-appcfg-path")
    lazy val emptyMap       = TaskKey[Seq[(File, String)]]("appengine-empty-map")    
  }
  private val gae = AppengineKeys
  
  private def appcfgTask(action: String, outputFile: Option[String],
                         args: TaskKey[Seq[String]],
                         depends: TaskKey[Seq[(File, String)]] = gae.emptyMap) =
    (args, temporaryWarPath, gae.appcfgPath, streams, depends) map { (args, w, appcfgPath, s, m) =>
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
    if (sdk == null) sys.error("You need to set APPENGINE_SDK_HOME")
    new File(sdk)
  }

  private def buildSdkVersion(libUserPath: File): String = {
    val pat = """appengine-api-1.0-sdk-(\d\.\d\.\d(?:\.\d)*)\.jar""".r
    (libUserPath * "appengine-api-1.0-sdk-*.jar").get.toList match {
      case jar::_ => jar.name match {
        case pat(version) => version
        case _ => sys.error("invalid jar file. " + jar)
      }
      case _ => sys.error("not found appengine api jar.")
    }
  }

  private def isWindows = System.getProperty("os.name").startsWith("Windows")
  private def osBatchSuffix = if (isWindows) ".cmd" else ".sh"

  lazy val baseAppengineSettings: Seq[Project.Setting[_]] = Seq(
    webappUnmanaged  <<= (temporaryWarPath) { (dir) => dir / "WEB-INF" / "appengine-generated" *** },
    unmanagedClasspath  <<= (unmanagedClasspath, gae.classpath) map { (orig, cp) => orig ++ cp },

    gae.requestLogs <<= inputTask { (args: TaskKey[Seq[String]])   => appcfgTask("request_logs", Some("request.log"), args) },
    gae.rollback <<= inputTask { (args: TaskKey[Seq[String]])      => appcfgTask("rollback", None, args) },
    gae.deploy <<= inputTask { (args: TaskKey[Seq[String]])        => appcfgTask("update", None, args, prepareWebapp) },
    gae.deployIndexes <<= inputTask { (args: TaskKey[Seq[String]]) => appcfgTask("update_indexes", None, args, prepareWebapp) },
    gae.deployCron <<= inputTask { (args: TaskKey[Seq[String]])    => appcfgTask("update_cron", None, args, prepareWebapp) },
    gae.deployQueues <<= inputTask { (args: TaskKey[Seq[String]])  => appcfgTask("update_queues", None, args, prepareWebapp) },
    gae.deployDos <<= inputTask { (args: TaskKey[Seq[String]])     => appcfgTask("update_dos", None, args, prepareWebapp) },
    gae.cronInfo <<= inputTask { (args: TaskKey[Seq[String]])      => appcfgTask("cron_info", None, args) },

    gae.sdkVersion <<= (gae.libUserPath) { (dir) => buildSdkVersion(dir) },
    gae.sdkPath := buildAppengineSdkPath,
    gae.classpath <<= (gae.apiJarPath) { (jar: File) => Attributed.blankSeq(Seq(jar)) },
    gae.apiJarName <<= (gae.sdkVersion) { (v) => "appengine-api-1.0-sdk-" + v + ".jar" },
    gae.apiLabsJarName <<= (gae.sdkVersion) { (v) => "appengine-api-labs-" + v + ".jar" },
    gae.jsr107CacheJarName <<= (gae.sdkVersion) { (v) => "appengine-jsr107cache-" + v + ".jar" },
    
    gae.binPath <<= gae.sdkPath(_ / "bin"),
    gae.libPath <<= gae.sdkPath(_ / "lib"),
    gae.libUserPath <<= gae.libPath(_ / "user"),
    gae.libImplPath <<= gae.libPath(_ / "impl"),
    gae.apiJarPath <<= (gae.libUserPath, gae.apiJarName) { (dir, name) => dir / name },
    gae.appcfgName := "appcfg" + osBatchSuffix,
    gae.appcfgPath <<= (gae.binPath, gae.appcfgName) { (dir, name) => dir / name },
    gae.emptyMap := Nil  
  )

  lazy val webSettings = appengineSettings
  lazy val appengineSettings: Seq[Project.Setting[_]] = WebPlugin.webSettings ++
    inConfig(Compile)(baseAppengineSettings) ++
    inConfig(Test)(Seq(
      unmanagedClasspath <++= (gae.classpath) map { (cp) => cp },
      gae.classpath <<= (gae.classpath in Compile,
        gae.libImplPath in Compile, gae.libPath in Compile) { (cp, impl, lib) =>
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
