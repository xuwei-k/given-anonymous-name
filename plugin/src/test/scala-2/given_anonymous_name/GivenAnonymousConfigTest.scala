package given_anonymous_name

import given_anonymous_name.GivenAnonymousNamePlugin.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.scalatest.Assertions.*
import scalaprops.Gen
import scalaprops.Property
import scalaprops.Scalaprops

object GivenAnonymousConfigTest extends Scalaprops {

  implicit val configGen: Gen[GivenAnonymousNameConfig] = {
    implicit val s: Gen[String] = Gen.alphaNumString
    Gen.from5(GivenAnonymousNameConfig.apply)
  }

  val test = Property.forAll { (c1: GivenAnonymousNameConfig) =>
    val tmp = Files.createTempFile("", ".json")
    try {
      Files.write(tmp, c1.toJsonString.getBytes(StandardCharsets.UTF_8))
      val c2 = GivenAnonymousName.jsonFileToConfig(tmp.toFile)
      assert(c1 == c2)
      true
    } finally {
      Files.deleteIfExists(tmp)
    }
  }

}
