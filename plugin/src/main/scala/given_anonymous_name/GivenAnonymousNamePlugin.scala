package given_anonymous_name

import java.nio.charset.StandardCharsets
import sbt.*
import sbt.Keys.*
import sbt.plugins.JvmPlugin
import scalafix.sbt.ScalafixPlugin
import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.support.scalajson.unsafe.CompactPrinter

object GivenAnonymousNamePlugin extends AutoPlugin with GivenAnonymousNamePluginCompat {
  object autoImport {
    @transient
    val givenAnonymousName = taskKey[Unit]("analyze code and output intermediate file")
    @transient
    val givenAnonymousNameConfig = taskKey[GivenAnonymousNameConfig]("config for GivenAnonymousName")
  }
  import autoImport.*

  private implicit val instance: JsonFormat[GivenAnonymousNameConfig] = {
    import sjsonnew.BasicJsonProtocol.*
    caseClass5(GivenAnonymousNameConfig.apply, (_: GivenAnonymousNameConfig).asTupleOption)(
      "files",
      "scalafixConfigPath",
      "excludeNameRegex",
      "excludePath",
      "baseDir",
    )
  }

  private[given_anonymous_name] implicit class GivenAnonymousNameConfigOps(private val self: GivenAnonymousNameConfig)
      extends AnyVal {
    def toJsonString: String = {
      val builder = new Builder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      implicitly[JsonFormat[GivenAnonymousNameConfig]].write(self, builder)
      CompactPrinter.apply(
        builder.result.getOrElse(sys.error("invalid json"))
      )
    }
  }

  private def sbtLauncher: Def.Initialize[Task[File]] = Def.task {
    val Seq(launcher) = (LocalRootProject / dependencyResolution).value
      .retrieve(
        dependencyId = "org.scala-sbt" % "sbt-launch" % (givenAnonymousName / sbtVersion).value,
        scalaModuleInfo = None,
        retrieveDirectory = (ThisBuild / csrCacheDirectory).value,
        log = streams.value.log
      )
      .left
      .map(e => throw e.resolveException)
      .merge
      .distinct
    launcher
  }

  // avoid extraProjects and derivedProjects
  // https://github.com/sbt/sbt/issues/6860
  // https://github.com/sbt/sbt/issues/4947
  private def runGivenAnonymousName(
    config: GivenAnonymousNameConfig,
    launcher: File,
    forkOptions: ForkOptions,
  ): Either[Int, Unit] = {
    val buildSbt =
      s"""|name := "tmp-given-anonymous-name"
          |logLevel := Level.Warn
          |scalaVersion := "2.13.18"
          |libraryDependencies ++= Seq(
          |  "com.github.xuwei-k" %% "given-anonymous-name-scalafix" % "${GivenAnonymousNameBuildInfo.version}"
          |)
          |Compile / sources := Nil
          |""".stripMargin

    IO.withTemporaryDirectory { dir =>
      val forkOpt = forkOptions.withWorkingDirectory(dir)
      val in = dir / "in.json"
      IO.write(dir / "build.sbt", buildSbt.getBytes(StandardCharsets.UTF_8))
      IO.write(in, config.toJsonString.getBytes(StandardCharsets.UTF_8))
      val ret = Fork.java.apply(
        forkOpt,
        Seq(
          "-jar",
          launcher.getCanonicalPath,
          Seq(
            "runMain",
            "given_anonymous_name.GivenAnonymousName",
            s"--input=${in.getCanonicalPath}"
          ).mkString(" ")
        )
      )
      if (ret == 0) {
        Right(())
      } else {
        Left(ret)
      }
    }
  }

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin && ScalafixPlugin

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    givenAnonymousName / sources := Seq(
      ((Compile / sources).value ** "*.scala").get(),
      ((Test / sources).value ** "*.scala").get()
    ).flatten,
  )

  override def buildSettings: Seq[Def.Setting[?]] = Def.settings(
    ScalafixPlugin.autoImport.scalafixDependencies += {
      "com.github.xuwei-k" %% "given-anonymous-name-scalafix" % GivenAnonymousNameBuildInfo.version
    },
    givenAnonymousName / forkOptions := Def.uncached(ForkOptions()),
    givenAnonymousNameConfig := Def.taskDyn {
      val s = state.value
      val extracted = Project.extract(s)
      val currentBuildUri = extracted.currentRef.build
      val projects = extracted.structure.units
        .apply(currentBuildUri)
        .defined
        .values
        .filter(
          _.autoPlugins.contains(GivenAnonymousNamePlugin)
        )
        .toList
      val baseDir = (LocalRootProject / baseDirectory).value
      val sourcesTask: Def.Initialize[Task[Seq[File]]] = projects.map { p =>
        LocalProject(p.id) / givenAnonymousName / sources
      }.join.map(_.flatten)

      Def.task {
        val files = sourcesTask.value
        GivenAnonymousNameConfig(
          files = files.map { f =>
            IO.relativize(baseDir, f).getOrElse(sys.error(s"invalid file ${f.getCanonicalFile}"))
          }.toList,
          scalafixConfigPath = ScalafixPlugin.autoImport.scalafixConfig.?.value.flatten.map { f =>
            IO.relativize(baseDir, f).getOrElse(sys.error(s"invalid file ${f.getCanonicalFile}"))
          },
          excludeNameRegex = Set.empty,
          excludePath = Set(
            "glob:**/target/**",
            "glob:**/src_managed/**",
          ),
          baseDir = (LocalRootProject / baseDirectory).value.getCanonicalPath,
        )
      }
    }.value,
    givenAnonymousName := {
      val conf = givenAnonymousNameConfig.value
      val _ = conf.pathMatchers // check syntax error
      val jsonString = conf.toJsonString
      streams.value.log.debug(jsonString)
      runGivenAnonymousName(
        config = conf,
        launcher = sbtLauncher.value,
        forkOptions = (givenAnonymousName / forkOptions).value
      ).fold(e => sys.error(s"${givenAnonymousName.key.label} failed ${e}"), x => x)
    },
  )
}
