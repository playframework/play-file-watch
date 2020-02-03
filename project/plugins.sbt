resolvers ++= DefaultOptions.resolvers(snapshot = true)

addSbtPlugin("com.typesafe.play" % "interplay" % "3.0.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.2")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")