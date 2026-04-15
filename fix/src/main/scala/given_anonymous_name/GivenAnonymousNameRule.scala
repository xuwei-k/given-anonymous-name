package given_anonymous_name

import java.nio.file.Paths
import metaconfig.Configured
import scala.meta.XtensionCollectionLikeUI
import scalafix.Diagnostic
import scalafix.Patch
import scalafix.lint.LintSeverity
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule
import scalafix.v1.XtensionSeqPatch

sealed abstract class GivenAnonymousNameRule(config: GivenAnonymousNameScalafixConfig, name: String)
    extends SyntacticRule(name) {

  override final def withConfiguration(config: Configuration): Configured[Rule] = {
    config.conf
      .getOrElse(GivenAnonymousNameScalafixConfig.configKey)(this.config)
      .map(newConfig => newInstance(newConfig))
  }

  private[this] lazy val unusedNames: Set[String] = {
    FindResults.loadFromFile(Paths.get(config.outputPath)).values.map(_.value).toSet
  }

  protected def severity: LintSeverity
  protected def newInstance(config: GivenAnonymousNameScalafixConfig): GivenAnonymousNameRule

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree
      .collect(GivenAnonymousName.extractNamedGiven)
      .collect {
        case (tree, treeName) if unusedNames.contains(treeName.value) && !CheckUnusedAnnotation.exists(tree) =>
          Patch.lint(
            Diagnostic(
              id = "",
              message = "maybe unused given name",
              position = treeName.pos,
              severity = severity
            )
          )
      }
      .asPatch
  }
}

class GivenAnonymousNameWarn(config: GivenAnonymousNameScalafixConfig)
    extends GivenAnonymousNameRule(config, "GivenAnonymousNameWarn") {
  def this() = this(GivenAnonymousNameScalafixConfig.default)
  override protected def severity: LintSeverity = LintSeverity.Warning
  override protected def newInstance(config: GivenAnonymousNameScalafixConfig): GivenAnonymousNameRule =
    new GivenAnonymousNameWarn(config)
}

class GivenAnonymousNameError(config: GivenAnonymousNameScalafixConfig)
    extends GivenAnonymousNameRule(config, "GivenAnonymousNameError") {
  def this() = this(GivenAnonymousNameScalafixConfig.default)
  override protected def severity: LintSeverity = LintSeverity.Error
  override protected def newInstance(config: GivenAnonymousNameScalafixConfig): GivenAnonymousNameRule =
    new GivenAnonymousNameError(config)
}
