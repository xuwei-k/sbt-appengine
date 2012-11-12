package sbtappengine

import sbt._

/**
 * Sbt compatibility code concrete instances are declared in sbt version specific folders
 */
trait SbtCompat {
  /**
   * Changes javaOptions by using transformator function
   * (overridesJarPath, agentJarPath, reJRebelJar, localDbPath in devServer) => newJavaOptions
   */
  def changeJavaOptions(f: (File, File, String, File) => Seq[String]): Setting[_]
}

object SbtCompat {
  def impl: SbtCompat = SbtCompatImpl
}
