package sbtappengine

private[sbtappengine] object Compat {
  type Process = sbt.Process
  val Process = sbt.Process

  implicit class ForkOptionsOps(val self: sbt.ForkOptions) extends AnyVal {
    def withRunJVMOptions(runJVMOptions: Seq[String]): sbt.ForkOptions =
      self.copy(runJVMOptions = runJVMOptions)
    def withOutputStrategy(outputStrategy: sbt.OutputStrategy): sbt.ForkOptions =
      self.copy(outputStrategy = Option(outputStrategy))
  }
}
