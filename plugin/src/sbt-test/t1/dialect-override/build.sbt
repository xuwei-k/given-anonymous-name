scalaVersion := "3.9.0"

InputKey[Unit]("updateScalafixConfigDialectOverrride") := {
  IO.write(
    file(".scalafix.conf"),
    "dialectOverride.allowCaptureChecking = true"
  )
}

InputKey[Unit]("checkScalaFile") := {
  val actual = IO.read(file("src/main/scala/A.scala"))
  val expect = IO.read(file("expect.txt"))
  assert(actual == expect, actual)
}
