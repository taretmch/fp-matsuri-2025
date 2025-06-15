//> using scala 3.7.1
//> using dep io.github.iltotore::iron:3.0.1

import scala.util.matching.Regex

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.string.*

object Domain:
  type GivenName = GivenName.T
  object GivenName extends RefinedType[String, MinLength[1] & MaxLength[32]]

  final class EmailConstraint
  object EmailConstraint:
    val EMAIL_REGEXP =
      """^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    given Constraint[String, EmailConstraint] with
      override inline def test(inline value: String): Boolean = EMAIL_REGEXP.matches(value)
      override inline def message: String = "Should be valid email address."

  type Email = Email.T
  object Email extends RefinedType[String, EmailConstraint]

  sealed abstract class EmailAddress(val value: Email, val isValidated: Boolean)
  object EmailAddress:
    case class Unvalidated(override val value: Email) extends EmailAddress(value, false)
    case class Validated(override val value: Email)   extends EmailAddress(value, true)

    def either(email: String): Either[String, EmailAddress] =
      Email.either(email).map(Unvalidated(_))
end Domain

def demo(): Unit =
  import Domain.*

  val refinedGivenName         = GivenName.either("太郎")
  val refinedInvalidGivenName1 = GivenName.either("")
  val refinedInvalidGivenName2 = GivenName.either("あ" * 33) // 32文字を超える

  println(refinedGivenName)
  println(refinedInvalidGivenName1)
  println(refinedInvalidGivenName2)

  val validEmailAddress    = EmailAddress.either("taro.yamada@example.com")
  val invalidEmailAddress1 = EmailAddress.either("taro.yamada@@example.com")
  val invalidEmailAddress2 = EmailAddress.either("taro.yamadaxample.com")

  println(validEmailAddress)
  println(invalidEmailAddress1)
  println(invalidEmailAddress2)

demo()
