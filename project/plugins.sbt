resolvers ++= DefaultOptions.resolvers(snapshot = true)

addSbtPlugin("com.typesafe.play" % "interplay" % "2.0.5")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.18")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

