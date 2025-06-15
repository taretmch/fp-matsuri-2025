package presentation.json

import io.github.iltotore.iron.circe.given

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

import sttp.tapir.*

import domain.Staff

import presentation.DomainCodec.given

/** メールアドレスJSON表現 */
case class JsEmailAddress(
  email:       Staff.Email,
  isValidated: Boolean
)

object JsEmailAddress:
  given Encoder[JsEmailAddress] = deriveEncoder[JsEmailAddress]
  given Decoder[JsEmailAddress] = deriveDecoder[JsEmailAddress]
  given Schema[JsEmailAddress]  = Schema.derived[JsEmailAddress]

  def fromDomain(emailAddress: Staff.EmailAddress): JsEmailAddress =
    JsEmailAddress(
      email       = emailAddress.value,
      isValidated = emailAddress.isValidated
    )

  def toDomain(dto: JsEmailAddress): Staff.EmailAddress =
    Staff.EmailAddress(dto.email, dto.isValidated)
