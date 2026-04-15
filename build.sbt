import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

def sbt2 = "2.0.0-RC12"
def Scala212 = "2.12.21"
def Scala213 = "2.13.18"
def Scala3 = scala_version_from_sbt_version.ScalaVersionFromSbtVersion(sbt2)

val commonSettings = Def.settings(
  publishTo := (if (isSnapshot.value) None else localStaging.value),
  libraryDependencies += "org.scalatest" %% "scalatest-funsuite" % "3.2.20" % Test,
  Compile / unmanagedResources += (LocalRootProject / baseDirectory).value / "LICENSE.txt",
  Compile / doc / scalacOptions ++= {
    val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
    if (scalaBinaryVersion.value != "3") {
      Seq(
        "-sourcepath",
        (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/xuwei-k/given-anonymous-name/blob/${hash}€{FILE_PATH}.scala"
      )
    } else {
      Seq(
        "-source-links:github://xuwei-k/given-anonymous-name",
        "-revision",
        hash
      )
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Nil
      case _ =>
        Seq(
          "-release:8",
        )
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Seq(
          "-Wunused:all",
        )
      case "2.13" =>
        Seq(
          "-Wunused:imports",
          "-Xsource:3-cross",
        )
      case "2.12" =>
        Seq(
          "-Xsource:3",
        )
    }
  },
  scalacOptions ++= Seq(
    "-deprecation",
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>xuwei-k</id>
        <name>Kenji Yoshida</name>
        <url>https://github.com/xuwei-k</url>
      </developer>
    </developers>
    <scm>
      <url>git@github.com:xuwei-k/given-anonymous-name.git</url>
      <connection>scm:git:git@github.com:xuwei-k/given-anonymous-name.git</connection>
    </scm>
  ),
  organization := "com.github.xuwei-k",
  homepage := Some(url("https://github.com/xuwei-k/given-anonymous-name")),
  licenses := List(License.MIT),
)

commonSettings

publish / skip := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommandAndRemaining("sonaRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val plugin = projectMatrix
  .in(file("plugin"))
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(Seq(Scala212, Scala3))
  .enablePlugins(ScriptedPlugin)
  .dependsOn(common)
  .configure(p =>
    p.id match {
      case "plugin2_12" =>
        p.dependsOn(fix.jvm(Scala212) % Test)
      case _ =>
        p
    }
  )
  .settings(
    commonSettings,
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          sbtVersion.value
        case _ =>
          sbt2
      }
    },
    description := "find unused given names sbt plugin",
    scalapropsSettings,
    scalapropsVersion := "0.10.1",
    addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % _root_.scalafix.sbt.BuildInfo.scalafixVersion),
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}",
    scriptedBufferLog := false,
    sbtPlugin := true,
    name := "given-anonymous-name-plugin",
    Test / dependencyClasspath := (Test / dependencyClasspath).value.reverse,
  )

lazy val common = projectMatrix
  .in(file("common"))
  .settings(
    commonSettings,
    Compile / sourceGenerators += task {
      val dir = (Compile / sourceManaged).value
      val className = "GivenAnonymousNameBuildInfo"
      val f = dir / "given_anonymous_name" / s"${className}.scala"
      IO.write(
        f,
        Seq(
          "package given_anonymous_name",
          "",
          s"object $className {",
          s"""  def version: String = "${version.value}" """,
          "}",
        ).mkString("", "\n", "\n")
      )
      Seq(f)
    },
    name := "given-anonymous-name-common",
    description := "given-anonymous-name common sources",
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(Seq(Scala212, Scala213, Scala3))

lazy val fix = projectMatrix
  .in(file("fix"))
  .enablePlugins(ScalafixRuleResourceGen)
  .settings(
    commonSettings,
    name := "given-anonymous-name-scalafix",
    description := "scalafix rules given-anonymous-name",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % "0.14.6",
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(Seq(Scala213, Scala212))
  .dependsOn(common)

ThisBuild / scalafixDependencies += "com.github.xuwei-k" %% "scalafix-rules" % "0.6.24"
