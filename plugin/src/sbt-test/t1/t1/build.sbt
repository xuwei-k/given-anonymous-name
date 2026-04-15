import sjsonnew.support.scalajson.unsafe.Parser.parseFromFile
import scala.util.Success

val commonSettings = Def.settings(
  scalaVersion := "3.3.7",
)

commonSettings

lazy val a1 = project.settings(commonSettings)
lazy val a2 = project.settings(commonSettings).dependsOn(a1)
lazy val a3 = project.settings(commonSettings).disablePlugins(ScalafixPlugin).dependsOn(a1)

InputKey[Unit]("checkJson") := {
  val Success(x1) = parseFromFile(file("expect.json"))
  val Success(x2) = parseFromFile(file("target/given-anonymous-name/given-anonymous-name.json"))
  assert(x1 == x2)
}
