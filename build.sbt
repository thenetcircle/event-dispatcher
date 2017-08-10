lazy val commonSettings = Seq(
  scalaVersion := "2.12.2"
)

lazy val eventBus = (project in file("."))
  .settings(
    name := "event-bus",
    version := "2.0.1",
    commonSettings,
    libraryDependencies ++= Dependencies.rootDeps
  )
