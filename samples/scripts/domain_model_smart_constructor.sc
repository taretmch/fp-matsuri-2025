//> using scala 3.7.1

import scala.util.matching.Regex

object Domain:
  opaque type GivenName = String
  object GivenName:
    def apply(value: String): Either[String, GivenName] =
      if value.isEmpty then Left("GivenName must not be empty.")
      else if value.length > 32 then Left("GivenName must be less than or equal to 32.")
      else Right(value)

  sealed abstract class EmailAddress(val value: String, val isValidated: Boolean)
  case class UnvalidatedEmailAddress(override val value: String) extends EmailAddress(value, false)
  case class ValidatedEmailAddress(override val value: String)   extends EmailAddress(value, true)
  object EmailAddress:
    val EMAIL_REGEXP =
      """^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    def apply(value: String): Either[String, EmailAddress] =
      if EMAIL_REGEXP.matches(value) then Right(UnvalidatedEmailAddress(value))
      else Left("Invalid email address.")
end Domain

def demo(): Unit =
  import Domain.*

  val validGivenName    = GivenName("太郎")
  val invalidGivenName1 = GivenName("")
  val invalidGivenName2 = GivenName("あ" * 33) // 32文字を超える

  println(validGivenName)
  println(invalidGivenName1)
  println(invalidGivenName2)

  val validEmailAddress    = EmailAddress("taro.yamada@example.com")
  val invalidEmailAddress1 = EmailAddress("taro.yamada@@example.com")
  val invalidEmailAddress2 = EmailAddress("taro.yamadaxample.com")

  println(validEmailAddress)
  println(invalidEmailAddress1)
  println(invalidEmailAddress2)

demo()
