resolvers ++= DefaultOptions.resolvers(snapshot = true)

addSbtPlugin("com.typesafe.play" % "interplay" % "1.1.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

