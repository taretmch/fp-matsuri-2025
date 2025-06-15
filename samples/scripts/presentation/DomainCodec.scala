package presentation

import java.util.UUID

import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

import sttp.tapir.*

import domain.Staff

import presentation.json.JsEmailAddress

/** ドメイン型のCirce/Tapirインスタンス定義 */
object DomainCodec:
  // Staff.Id (Opaque Type) のインスタンス
  given Encoder[Staff.Id] = Encoder[UUID].contramap(_.value)
  given Decoder[Staff.Id] = Decoder[UUID].map(Staff.Id.fromUUID)
  given Schema[Staff.Id]  = Schema.string[Staff.Id].description("職員ID").format("uuid")

  // Staff.Username (RefinedType) のインスタンス
  given Encoder[Staff.Username] = Encoder[String].contramap(_.value)
  given Decoder[Staff.Username] = Decoder[String].emap(Staff.Username.either)
  given Schema[Staff.Username] = Schema
    .string[Staff.Username]
    .description("ユーザー名")
    .validate(Validator.minLength(4))
    .validate(Validator.maxLength(16))

  // Staff.EmailAddress (sealed class) のインスタンス - JsEmailAddressを使用
  given Encoder[Staff.Email] = Encoder[String].contramap(_.value)
  given Decoder[Staff.Email] = Decoder[String].emap(Staff.Email.either)
  given Schema[Staff.Email] = Schema
    .string[Staff.Email]
    .description("メールアドレス")
    .format("email")

  given Encoder[JsEmailAddress] = deriveEncoder[JsEmailAddress]
  given Decoder[JsEmailAddress] = deriveDecoder[JsEmailAddress]
  given Schema[JsEmailAddress]  = Schema.derived[JsEmailAddress]

  given Encoder[Staff.EmailAddress] =
    Encoder[JsEmailAddress].contramap(JsEmailAddress.fromDomain)

  given Decoder[Staff.EmailAddress] =
    Decoder[JsEmailAddress].map(JsEmailAddress.toDomain)

  given Schema[Staff.EmailAddress] =
    Schema
      .derived[JsEmailAddress]
      .map(jsEmail => Some(JsEmailAddress.toDomain(jsEmail)))(JsEmailAddress.fromDomain)
      .description("メールアドレス")

  // Staff.Role (enum) のインスタンス
  given Encoder[Staff.Role] = Encoder[String].contramap(_.value)
  given Decoder[Staff.Role] = Decoder[String].emap { value =>
    Staff.Role.find(value).toRight(s"Invalid role: $value")
  }
  given Schema[Staff.Role] = Schema
    .string[Staff.Role]
    .description("職員ロール")
    .validate(Validator.enumeration(Staff.Role.values.toList, v => Some(v.value)))

  // Staff.FamilyName / GivenName (RefinedType) のインスタンス
  given Schema[Staff.FamilyName] = Schema
    .string[Staff.FamilyName]
    .description("姓")
    .validate(Validator.minLength(1))
    .validate(Validator.maxLength(32))

  given Schema[Staff.GivenName] = Schema
    .string[Staff.GivenName]
    .description("名")
    .validate(Validator.minLength(1))
    .validate(Validator.maxLength(32))

  // Staff.Name (case class) のインスタンス - deriveを使用
  given Encoder[Staff.Name] = deriveEncoder[Staff.Name]
  given Decoder[Staff.Name] = deriveDecoder[Staff.Name]
  given Schema[Staff.Name]  = Schema.derived[Staff.Name]
