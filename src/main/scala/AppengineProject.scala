package sbt

import java.io.File
import sbt.Process._

abstract class AppengineProject(info: ProjectInfo) extends DefaultWebProject(info) {
  val servlet = "javax.servlet" % "servlet-api" % "2.5" % "provided"

  override def unmanagedClasspath = super.unmanagedClasspath +++ appengineClasspath

  def appengineClasspath: PathFinder = appengineApiJarPath

  def appengineApiJarName = "appengine-api-1.0-sdk-" + sdkVersion + ".jar"
  def appengineApiLabsJarName = "appengine-api-labs-" + sdkVersion + ".jar"
  def appengineJSR107CacheJarName = "appengine-jsr107cache-" + sdkVersion + ".jar"
  def jsr107CacheJarName = "jsr107cache-1.1.jar"

  def appengineLibPath = appengineSdkPath / "lib"
  def appengineLibUserPath = appengineLibPath / "user"
  def appengineApiJarPath = appengineLibUserPath / appengineApiJarName
  def appengineApiLabsJarPath = appengineLibUserPath / appengineApiLabsJarName
  def jsr107cacheJarsPath = appengineLibUserPath / appengineJSR107CacheJarName +++ appengineLibUserPath / jsr107CacheJarName

  def appengineToolsJarPath = (appengineLibPath / "appengine-tools-api.jar")


  def appcfgName = "appcfg" + osBatchSuffix
  def devAppserverName = "dev_appserver" + osBatchSuffix

  def appcfgPath = appengineSdkPath / "bin" / appcfgName
  def devAppserverPath = appengineSdkPath / "bin" / devAppserverName

  def isWindows = System.getProperty("os.name").startsWith("Windows")
  def osBatchSuffix = if (isWindows) ".cmd" else ".sh"

  def sdkVersion = {
    val pat = """.*/appengine-api-1.0-sdk-(\d\.\d\.\d)\.jar""".r
    (appengineLibUserPath * "appengine-api-1.0-sdk-*.jar").get.toList match {
      case jar::_ => jar.absolutePath match {
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

  def defaultDevAppserverPort:Option[Int] = None
  def defaultDevAppserverAddress:Option[Int] = None

  lazy val devAppserver = execTask {
    <x>
      {devAppserverPath.absolutePath}
        {defaultDevAppserverAddress match { case Some(a) => "-a " + a; case None => }}
        {defaultDevAppserverPort match { case Some(p) => "-p " + p; case None => }}
        {temporaryWarPath.relativePath}
    </x>
  } dependsOn(prepareWebapp) describedAs("start dev_appserver")

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
