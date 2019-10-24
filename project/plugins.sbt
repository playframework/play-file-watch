resolvers ++= DefaultOptions.resolvers(snapshot = true)
// TODO remove when https://github.com/lightbend/mima/issues/422 is fixed
resolvers += Resolver.url(
  "typesafe sbt-plugins",
  url("https://dl.bintray.com/typesafe/sbt-plugins")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.play" % "interplay" % "2.1.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.1")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

