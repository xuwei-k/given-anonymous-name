package given_anonymous_name

import metaconfig.ConfDecoder
import metaconfig.generic.Surface

final case class GivenAnonymousNameScalafixConfig(
  outputPath: String,
  removeFile: Boolean,
  dialectOverride: Map[String, Boolean]
)

object GivenAnonymousNameScalafixConfig {
  val default = GivenAnonymousNameScalafixConfig(
    outputPath = "target/given-anonymous-name/given-anonymous-name.json",
    removeFile = true,
    dialectOverride = Map.empty,
  )
  implicit val surface: Surface[GivenAnonymousNameScalafixConfig] =
    metaconfig.generic.deriveSurface[GivenAnonymousNameScalafixConfig]
  implicit val decoder: ConfDecoder[GivenAnonymousNameScalafixConfig] =
    metaconfig.generic.deriveDecoder(default)

  def configKey: String = "GivenAnonymousName"
}
