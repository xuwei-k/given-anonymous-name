package given_anonymous_name

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.Hocon
import metaconfig.generic.Surface
import scala.meta.Defn
import scala.meta.Mod
import scala.meta.Name
import scala.meta.Source
import scala.meta.Term
import scala.meta.Tree
import scala.meta.Type
import scala.meta.XtensionClassifiable
import scala.meta.XtensionCollectionLikeUI
import scala.meta.inputs.Input
import scala.meta.parsers.Parse

object GivenAnonymousName {
  private[this] implicit val surface: Surface[GivenAnonymousNameConfig] =
    metaconfig.generic.deriveSurface[GivenAnonymousNameConfig]

  private[this] def defaultScalafixConfigFile = ".scalafix.conf"

  private[this] implicit val decoder: ConfDecoder[GivenAnonymousNameConfig] = {
    val empty = GivenAnonymousNameConfig(
      files = Nil,
      scalafixConfigPath = None,
      excludeNameRegex = Set.empty,
      excludePath = Set.empty,
      baseDir = "",
    )
    metaconfig.generic.deriveDecoder[GivenAnonymousNameConfig](empty)
  }

  private[given_anonymous_name] def jsonFileToConfig(json: File): GivenAnonymousNameConfig = {
    val c = Conf.parseFile(json)(Hocon)
    implicitly[ConfDecoder[GivenAnonymousNameConfig]].read(c).get
  }

  def main(args: Array[String]): Unit = {
    val in = {
      val key = "--input="
      args.collectFirst { case arg if arg.startsWith(key) => arg.drop(key.length) }
        .getOrElse(sys.error("missing --input"))
    }
    val conf = jsonFileToConfig(new File(in))
    val config = getScalafixConfig(conf)
    val result = conf.files.flatMap { file =>
      val input = Input.File(new File(conf.baseDir, file))
      val dialect = applyDialectOverride(
        config.dialectOverride,
        scala.meta.dialects.Scala3
      )
      val tree = implicitly[Parse[Source]].apply(input, dialect).get
      run(tree, file)
    }
    writeResult(result, conf)
  }

  private def getScalafixConfig(conf: GivenAnonymousNameConfig): GivenAnonymousNameScalafixConfig = {
    val scalafixConfig = new File(conf.baseDir, conf.scalafixConfigPath.getOrElse(defaultScalafixConfigFile))
    if (scalafixConfig.isFile) {
      Conf.parseFile(scalafixConfig)(Hocon).get.as[GivenAnonymousNameScalafixConfig].get
    } else {
      GivenAnonymousNameScalafixConfig.default
    }
  }

  /**
   * [[https://github.com/scalacenter/scalafix/commit/2529c4d42ef25511c6576d17c1cc287a5515d9d2]]
   */
  private def applyDialectOverride(
    dialectOverride: Map[String, Boolean],
    dialect: scala.meta.Dialect
  ): scala.meta.Dialect = {
    dialectOverride.foldLeft(dialect) {
      case (cur, (k, v)) if k.nonEmpty =>
        val upper = s"${k.head.toUpper}${k.drop(1)}"
        cur.getClass.getMethods
          .find(method =>
            (
              method.getName == s"with${upper}"
            ) && (
              method.getParameterTypes.toSeq == Seq(classOf[Boolean])
            ) && (
              method.getReturnType == classOf[scala.meta.Dialect]
            )
          )
          .fold(cur)(
            _.invoke(cur, java.lang.Boolean.valueOf(v)).asInstanceOf[scala.meta.Dialect]
          )
      case (cur, _) =>
        cur
    }
  }

  private object ExtractNamedGiven {
    def unapply(value: Tree): Option[(Defn.GivenAlias, Name)] =
      extractNamedGiven.lift.apply(value)
  }

  private[given_anonymous_name] val extractNamedGiven: PartialFunction[Tree, (Defn.GivenAlias, Name)] = {
    case x @ Defn.GivenAlias.After_4_12_0(_, _: Term.Name, _, _, _) if !x.mods.exists(_.is[Mod.Override]) =>
      (x, x.name)
  }

  private[this] def aggregate(values: Seq[FindResult], config: GivenAnonymousNameConfig): List[FindResult.Use] = {
    val allDefineNames = values.collect { case a: FindResult.Define => a.value }.toSet
    val allNames = values.collect { case a: FindResult.Use => a }
    val pathMatchers = config.pathMatchers

    allNames
      .groupBy(_.value)
      .view
      .collect { case (_, Seq(v)) => v }
      .filter(x => allDefineNames(x.value))
      .filterNot(x => config.isExcludeName(x.value))
      .filter(x => pathMatchers.forall(matcher => !matcher.matches(Paths.get(x.path))))
      .toList
      .sortBy(x => (x.path, x.value))
  }

  private[this] def writeResult(values: Seq[FindResult], config: GivenAnonymousNameConfig): String = {
    val result = FindResults(aggregate(values = values, config = config))
    val json = implicitly[ConfEncoder[FindResults]].write(result).show
    val bytes = json.getBytes(StandardCharsets.UTF_8)
    val path = new File(config.baseDir, getScalafixConfig(config).outputPath).toPath
    Files.createDirectories(path.getParent)
    Files.write(path, bytes)
    json
  }

  private[this] def run(tree: Tree, path: String): List[FindResult] = {
    tree.collect {
      case ExtractNamedGiven(_, x) =>
        FindResult.Define(
          value = x.value,
        )
      case x: Term.Name =>
        FindResult.Use(
          value = x.value,
          path = path,
        )
      case x: Type.Name =>
        FindResult.Use(
          value = x.value,
          path = path,
        )
      case x: scala.meta.Name =>
        FindResult.Use(
          value = x.value,
          path = path,
        )
    }
  }
}
