enablePlugins(ScalaJSPlugin)

name := "Energy Simulator"
version := "0.1.0"

scalaVersion := "2.12.2"

lazy val root = project.in(file(".")).
  aggregate(energySimulatorJS, energySimulatorJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val energySimulator = crossProject.in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
      "com.outr" %%% "scribe" % "1.4.3"
    ),
  )
  .jvmSettings(
    mainClass in Compile := Some("fi.iki.santtu.energysim.Command"),
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.4.0",
      "com.github.scopt" %% "scopt" % "3.7.0",
    )
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
//    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    mainClass in Compile := Some("fi.iki.santtu.energysim.Browser"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.3",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.1.0",
    ),
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    webpackConfigFile := Some(baseDirectory.value / "my.webpack.config.js"),
    npmDependencies in Compile ++= Seq(
      "react" → "15.6.1",
      "react-dom" → "15.6.1",
      "bootstrap" → "4.0.0-beta.2",
      "jquery" → "3.2.1",
      "popper.js" → "^1.12.6",
    ),
  )

lazy val energySimulatorJVM = energySimulator.jvm
lazy val energySimulatorJS = energySimulator.js
