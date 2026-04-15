package given_anonymous_name

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.util.regex.Pattern

/**
 * @param scalafixConfigPath `.scalafix.conf` file path
 * @param excludePath [[https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-]]
 */
final case class GivenAnonymousNameConfig(
  files: List[String],
  scalafixConfigPath: Option[String],
  excludeNameRegex: Set[String],
  excludePath: Set[String],
  baseDir: String,
) extends GivenAnonymousNameConfigCompat {
  def pathMatchers: Seq[PathMatcher] = {
    val fs = FileSystems.getDefault
    excludePath.map(fs.getPathMatcher).toSeq
  }

  private val regex: Seq[Pattern] = excludeNameRegex.map(Pattern.compile).toList

  def isExcludeName(name: String): Boolean = regex.exists { p =>
    p.matcher(name).matches
  }
}
