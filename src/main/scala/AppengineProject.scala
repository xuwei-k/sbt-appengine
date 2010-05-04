package sbt

import java.io.File
import sbt.Process._

abstract class AppengineProject(info: ProjectInfo) extends DefaultWebProject(info) {
  val servlet = "javax.servlet" % "servlet-api" % "2.5" % "provided"

  override def unmanagedClasspath = super.unmanagedClasspath +++ appengineClasspath
  override def testUnmanagedClasspath = super.testUnmanagedClasspath +++ appengineTestClasspath

  override def webappUnmanaged =
    (temporaryWarPath / "WEB-INF" / "appengine-generated" ***)

  def appengineClasspath: PathFinder = appengineApiJarPath
  def appengineTestClasspath: PathFinder = (appengineLibImplPath * "*.jar") +++ (appengineLibPath / "testing" * "*.jar")

  def appengineApiJarName = "appengine-api-1.0-sdk-" + sdkVersion + ".jar"
  def appengineApiLabsJarName = "appengine-api-labs-" + sdkVersion + ".jar"
  def appengineJSR107CacheJarName = "appengine-jsr107cache-" + sdkVersion + ".jar"
  def jsr107CacheJarName = "jsr107cache-1.1.jar"

  def appengineLibPath = appengineSdkPath / "lib"
  def appengineLibUserPath = appengineLibPath / "user"
  def appengineLibImplPath = appengineLibPath / "impl"
  def appengineApiJarPath = appengineLibUserPath / appengineApiJarName
  def appengineApiLabsJarPath = appengineLibUserPath / appengineApiLabsJarName
  def jsr107cacheJarsPath = appengineLibUserPath / appengineJSR107CacheJarName +++ appengineLibUserPath / jsr107CacheJarName

  def appengineToolsJarPath = (appengineLibPath / "appengine-tools-api.jar")


  def appcfgName = "appcfg" + osBatchSuffix
  def appcfgPath = appengineSdkPath / "bin" / appcfgName

  def isWindows = System.getProperty("os.name").startsWith("Windows")
  def osBatchSuffix = if (isWindows) ".cmd" else ".sh"

  def sdkVersion = {
    val pat = """appengine-api-1.0-sdk-(\d\.\d\.\d(?:\.\d)*)\.jar""".r
    (appengineLibUserPath * "appengine-api-1.0-sdk-*.jar").get.toList match {
      case jar::_ => jar.name match {
        case pat(version) => version
        case _ => error("invalid jar file. " + jar)
      }
      case _ => error("not found appengine api jar.")
    }
  }

  def appengineSdkPath = {
    val sdk = System.getenv("APPENGINE_SDK_HOME")
    if (sdk == null) error("You need to set APPENGINE_SDK_HOME")
    Path.fromFile(new File(sdk))
  }

  def appcfgTask(action: String) = execTask {
    <x>
      {appcfgPath.absolutePath} {action}
    </x>
  }

  lazy val javaCmd = (Path.fromFile(new java.io.File(System.getProperty("java.home"))) / "bin" / "java").absolutePath

  def appengineAgentPath = appengineLibPath / "agent" / "appengine-agent.jar"

  def devAppserverJvmOptions:Seq[String] = List()
  lazy val devAppserverInstance = new DevAppserverRun
  lazy val devAppserverStart = devAppserverStartAction
  lazy val devAppserverStop = devAppserverStopAction
  def devAppserverStartAction = task{ args => devAppserverStartTask(args) dependsOn(prepareWebapp) }
  def devAppserverStopAction = devAppserverStopTask
  def devAppserverStartTask(args: Seq[String]) = task {devAppserverInstance(args)}
  def devAppserverStopTask = task{
    devAppserverInstance.stop()
    None
  }

  class DevAppserverRun() extends Runnable with ExitHook {
    ExitHooks.register(this)
    def name = "dev_appserver-shitdown"
    def runBeforeExiting() { stop() }

    val jvmOptions =
      List("-ea", "-javaagent:"+appengineAgentPath.absolutePath,
           "-cp", appengineToolsJarPath.absolutePath) ++ devAppserverJvmOptions

    private var running: Option[Process] = None

    def run() {
      running.foreach(_.exitValue())
      running = None
    }

    def apply(args: Seq[String]): Option[String] = {
      if (running.isDefined)
        Some("This instance of dev_appserver is already running.")
      else {
        val builder: ProcessBuilder =
          Process(javaCmd :: jvmOptions :::
                  "com.google.appengine.tools.development.DevAppServerMain" ::
                  args.toList ::: temporaryWarPath.relativePath :: Nil)
        running = Some(builder.run)
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

}

trait DataNucleus extends AppengineProject {
  override def appengineClasspath = super.appengineClasspath +++ appengineApiLabsJarPath +++ appengineORMJarsPath

  override def prepareWebappAction = super.prepareWebappAction dependsOn(enhance)

  def appengineORMJarsPath = appengineLibUserPath / "orm" * "*.jar"
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
      mainClasses.get.map(_.projectRelativePath))
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
