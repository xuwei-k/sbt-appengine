package sbtappengine

private[sbtappengine] object Compat {
  type Process = scala.sys.process.Process
  val Process = scala.sys.process.Process
}
