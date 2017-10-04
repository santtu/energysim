enablePlugins(ScalaJSPlugin)

name := "Energy Simulation"
scalaVersion := "2.12.2"

scalaJSUseMainModuleInitializer := true

// libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
//libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.4.0"
libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.4.0"

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %%% "circe-core",
  "io.circe" %%% "circe-generic",
  "io.circe" %%% "circe-parser"
).map(_ % circeVersion)

val nlpVersion = "0.13.2"

libraryDependencies  ++= Seq(
  // Last stable release
  "org.scalanlp" %% "breeze" % nlpVersion,

  // Native libraries are not included by default. add this if you want them (as of 0.7)
  // Native libraries greatly improve performance, but increase jar sizes.
  // It also packages various blas implementations, which have licenses that may or may not
  // be compatible with the Apache License. No GPL code, as best I know.
//  "org.scalanlp" %% "breeze-natives" % nlpVersion,

  // The visualization library is distributed separately as well.
  // It depends on LGPL code
  "org.scalanlp" %% "breeze-viz" % nlpVersion
)


resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"