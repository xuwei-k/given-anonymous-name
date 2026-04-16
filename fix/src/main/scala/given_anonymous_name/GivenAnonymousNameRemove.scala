package given_anonymous_name

import java.nio.file.Paths
import metaconfig.Configured
import scala.meta.XtensionClassifiable
import scala.meta.XtensionCollectionLikeUI
import scala.meta.tokens.Token
import scalafix.Patch
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule
import scalafix.v1.XtensionOptionPatch
import scalafix.v1.XtensionSeqPatch

class GivenAnonymousNameRemove(config: GivenAnonymousNameScalafixConfig)
    extends SyntacticRule("GivenAnonymousNameRemove") {

  def this() = this(GivenAnonymousNameScalafixConfig.default)

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    config.conf
      .getOrElse(GivenAnonymousNameScalafixConfig.configKey)(this.config)
      .map(newConfig => new GivenAnonymousNameRemove(newConfig))
  }

  private[this] lazy val unusedNames: Set[String] = {
    FindResults.loadFromFile(Paths.get(config.outputPath)).values.map(_.value).toSet
  }

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree
      .collect(GivenAnonymousName.extractNamedGiven)
      .collect {
        case (tree, treeName) if unusedNames.contains(treeName.value) && !CheckUnusedAnnotation.exists(tree) =>
          Seq(
            if (tree.paramClauseGroups.nonEmpty) {
              Patch.empty
            } else {
              tree.tokens
                .filter(_.is[Token.Colon])
                .find(_.pos.start >= tree.name.pos.end)
                .map(Patch.removeToken)
                .asPatch
            },
            if (tree.tokens.find(_.is[Token.KwGiven]).get.pos.startLine < tree.decltpe.pos.startLine) {
              tree.tokens.find(_.is[Token.LF]).map(x => Patch.replaceToken(x, " ")).asPatch
            } else {
              Patch.empty
            },
            Patch.removeTokens(tree.name.tokens)
          ).asPatch
      }
      .asPatch
  }
}
