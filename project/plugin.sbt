resolvers ++= Seq(
  Resolver.url("sbt-plugin-releases", new URL(
    "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
      Resolver.ivyStylePatterns),
  "coda" at "http://repo.codahale.com"
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

addSbtPlugin("net.virtual-void" % "sbt-cross-building" % "0.7.0-RC1")
