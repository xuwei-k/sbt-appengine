package sbtappengine

import sbt._
import Keys._
import cc.spray.revolver.RevolverKeys

object SbtCompatImpl extends SbtCompat with RevolverKeys {
  import Plugin.{AppengineKeys => gae}

  def changeJavaOptions(f: (File, File, String) => Seq[String]) =
    javaOptions in gae.devServer <<= (gae.overridesJarPath, gae.agentJarPath, gae.reJRebelJar) apply f
}
