import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "Energy Simulator"
version := "0.2.0"

scalaVersion := "2.12.6"

// generate all JS files in the format the `site` subdirectory expects them
// to be generated
lazy val generateJs = taskKey[Unit]("generate all JS files")

generateJs := {
  (webpack in (ui, Compile, fastOptJS)).value
  (fastOptJS in (worker, Compile)).value
}

// root project is aggregate of the subprojects, JVM and JS libraries
// plus UI and background webworker
lazy val root = project.in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(libraryJS, libraryJVM, ui, worker)
  .settings(
    publish := {},
    publishLocal := {},
    // generate unidoc for only JVM project, if both libraryJS and
    // libraryJVM are used unidoc will barf since it'll have duplicate
    // definitions
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(libraryJS),
  )

// UI is a javascript single-page app project, depending on the JS
// energysim library
lazy val ui = (project in file("ui"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    name := "Energy Simulator UI",
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    mainClass in Compile := Some("fi.iki.santtu.energysimui.Main"),
    scalaJSUseMainModuleInitializer := true,
    npmDependencies in Compile ++= Seq(
      "react" → "16.4.1",
      "react-dom" → "16.4.1",
      "bootstrap" → "4.1.1",
      "jquery" → "3.3.1",
      "popper.js" → "^1.14.3",
    ),
    npmDevDependencies in Compile ++= Seq(
      "webpack-merge" → "^4.1.0",
      // "css-loader" → "^0.28.0",
      // "style-loader" → "^0.19.0",
      // "sass-loader" → "^6.0.0",
      // "node-sass" → "^4.0.0",
      // "precss" → "^2.0.0",
      // "autoprefixer" → "^7.0.0",
    ),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "com.github.marklister" %%% "base64" % "0.2.4",
      "com.payalabs" %%% "scalajs-react-bridge" % "0.6.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.6",
    ),
    webpackMonitoredDirectories += baseDirectory.value / "src" / "main" / "sass",
    includeFilter in webpackMonitoredFiles := "*.sass",
  )
  .dependsOn(worker)

// Worker is a separate project that has no NPM dependencies so it
// doesn't use scalajs-bundler at all, just plain scalajs output. It
// implements a WebWorker simulation backend that the UI element uses.
lazy val worker = (project in file("worker"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "Simulation Worker",
    mainClass in Compile := Some("fi.iki.santtu.energysimworker.SimulationWorker"),
    scalaJSUseMainModuleInitializer := true,
  )
  .dependsOn(libraryJS)


// Library JVM and JS versions is a Scala.JS cross-project, generating
// both JVM and JS versions. The JVM version has a command line
// interface main provided also.
lazy val library = crossProject(JSPlatform, JVMPlatform)
  .in(file("library"))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.9.3",
      "io.circe" %%% "circe-generic" % "0.9.3",
      "io.circe" %%% "circe-parser" % "0.9.3",
      "org.scalatest" %%% "scalatest" % "3.0.5" % "test",
      "com.outr" %%% "scribe" % "2.5.1"
    ),
    name := "Energy Simulator",
  )
  .jvmSettings(
    mainClass in Compile := Some("fi.iki.santtu.energysim.Command"),
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.4.0",
      "com.github.scopt" %% "scopt" % "3.7.0",
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
//      "org.scala-js" %%% "scalajs-dom" % "0.9.3",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.2.0",
      "com.github.japgolly.scalajs-react" %%% "extra" % "1.2.0",
    ),
  )

lazy val libraryJVM = library.jvm
lazy val libraryJS = library.js
