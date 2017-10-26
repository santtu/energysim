enablePlugins(ScalaJSPlugin)

name := "Energy Simulation"
version := "0.1.0"

scalaVersion := "2.12.2"

lazy val root = project.in(file(".")).
  aggregate(energySimulatorJS, energySimulatorJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

val circeVersion = "0.8.0"

lazy val energySimulator = crossProject.in(file(".")).
  settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % circeVersion) ++ Seq(
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
      "com.outr" %%% "scribe" % "1.4.3"
    )
  ).
  jvmSettings(
    mainClass in Compile := Some("fi.iki.santtu.energysim.Command"),
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.4.0",
      "com.github.scopt" %% "scopt" % "3.7.0",
    )
  ).
  jsSettings(
    scalaJSUseMainModuleInitializer := true,
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    mainClass in Compile := Some("fi.iki.santtu.energysim.Browser"),
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.2"

    //    libraryDependencies ++= Seq(
//      "io.circe" %%% "circe-core",
//      "io.circe" %%% "circe-generic",
//      "io.circe" %%% "circe-parser"
//    ).map(_ % circeVersion)
  )

lazy val energySimulatorJVM = energySimulator.jvm
lazy val energySimulatorJS = energySimulator.js
//
//scalaJSUseMainModuleInitializer := true
//scalaJSMainModuleInitializer := Some(
//  org.scalajs.core.tools.linker.ModuleInitializer.mainMethod(
//    "fi.iki.santtu.energysim.Browser", "main"))

// libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
//libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.4.0"
//
//
//val nlpVersion = "0.13.2"
//
//libraryDependencies  ++= Seq(
//  // Last stable release
//  "org.scalanlp" %% "breeze" % nlpVersion,
//
//  // Native libraries are not included by default. add this if you want them (as of 0.7)
//  // Native libraries greatly improve performance, but increase jar sizes.
//  // It also packages various blas implementations, which have licenses that may or may not
//  // be compatible with the Apache License. No GPL code, as best I know.
////  "org.scalanlp" %% "breeze-natives" % nlpVersion,
//
//  // The visualization library is distributed separately as well.
//  // It depends on LGPL code
//  "org.scalanlp" %% "breeze-viz" % nlpVersion
//)
//
//
//resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"