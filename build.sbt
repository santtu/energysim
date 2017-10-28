name := "Energy Simulator"
version := "0.1.0"

scalaVersion := "2.12.2"

// This is to prevent me from getting confused if I accidentally typed
// fastOptJS instead of fastOptJS::webpack -- they write to same files
// but with different effects, resulting in confusion if used
// interchangeably. So force this to be the webpack version.
addCommandAlias("fastOptJS", "fastOptJS::webpack")


// root project is aggregate of the subprojects, JVM and JS libraries
// plus UI
lazy val root = project.in(file(".")).
  aggregate(libraryJS, libraryJVM, ui).
  settings(
    publish := {},
    publishLocal := {}
  )

// UI is a javascript single-page app project, depending on the JS
// energysim library
lazy val ui = (project in file("ui"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    name := "Energy Simulator UI",
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    webpackConfigFile := Some(baseDirectory.value / "my.webpack.config.js"),
    mainClass in Compile := Some("fi.iki.santtu.energysimui.Main"),
    scalaJSUseMainModuleInitializer := true,
    npmDependencies in Compile ++= Seq(
      "react" → "15.6.1",
      "react-dom" → "15.6.1",
      "bootstrap" → "4.0.0-beta.2",
      "jquery" → "3.2.1",
      "popper.js" → "^1.12.6",
    ),
    npmDevDependencies in Compile ++= Seq(
      "webpack-merge" → "^4.1.0",
      "css-loader" → "^0.28.0",
      "style-loader" → "^0.19.0",
      "sass-loader" → "^6.0.0",
      "node-sass" → "^4.0.0",
      "precss" → "^2.0.0",
      "autoprefixer" → "^7.0.0",
    ),
    libraryDependencies ++= Seq(
      "com.github.marklister" %%% "base64" % "0.2.3",
    ),
    webpackMonitoredDirectories += baseDirectory.value / "src" / "main" / "sass",
    includeFilter in webpackMonitoredFiles := "*.sass",
  )
  .dependsOn(libraryJS)

// Library JVM and JS versions is a Scala.JS cross-project, generating
// both JVM and JS versions. The JVM version has a command line
// interface main provided also.
lazy val library = crossProject.in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
      "com.outr" %%% "scribe" % "1.4.3"
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
      "org.scala-js" %%% "scalajs-dom" % "0.9.3",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.1.0",
      "com.github.japgolly.scalajs-react" %%% "extra" % "1.1.0",
    ),
  )

lazy val libraryJVM = library.jvm
lazy val libraryJS = library.js
