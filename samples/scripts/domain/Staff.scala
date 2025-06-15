package domain

import java.util.UUID

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.string.*

import cats.syntax.traverse.*

// 職員エンティティ
case class Staff(
  id:       Staff.Id,
  name:     Staff.Name,
  username: Option[Staff.Username],
  email:    Staff.EmailAddress,
  role:     Staff.Role
):
  val loginId: Staff.LoginId = username match
    case Some(username) => Staff.UsernameAndEmailAddress(username, email)
    case None           => email

object Staff:
  // ID
  opaque type Id = UUID
  object Id:
    def fromUUID(uuid: UUID): Id = uuid

    extension (id: Id) def value: UUID = id

    given FromUUID[Id] = new FromUUID[Id]:
      def fromUUID(uuid: UUID): Id = Id.fromUUID(uuid)

  // 名前
  type NameConstraint = MinLength[1] & MaxLength[32]

  // 姓
  type FamilyName = FamilyName.T
  object FamilyName extends RefinedType[String, NameConstraint]

  // 名
  type GivenName = GivenName.T
  object GivenName extends RefinedType[String, NameConstraint]

  // 姓名
  case class Name(familyName: FamilyName, givenName: Option[GivenName]):
    def fullName: String = givenName match
      case Some(gname) => s"${ familyName } ${ gname }"
      case None        => familyName

  // ユーザー名
  type Username = Username.T
  object Username extends RefinedType[String, MinLength[4] & MaxLength[16]]:
    def fromSeed(seed: String): Username =
      val normalized = seed.filter(_.isLetterOrDigit).toLowerCase.take(8)
      val padded     = normalized.padTo(8, 'x')
      applyUnsafe(padded)

  // メールアドレスの制約 (WHATWG準拠の簡易版)
  final class EmailConstraint
  object EmailConstraint:
    val EMAIL_REGEXP =
      """^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    given Constraint[String, EmailConstraint] with
      override inline def test(inline value: String): Boolean = EMAIL_REGEXP.matches(value)
      override inline def message: String = "Should be valid email address."

  type Email = Email.T
  object Email extends RefinedType[String, EmailConstraint]

  // メールアドレス
  sealed abstract class EmailAddress(val value: Email, val isValidated: Boolean)
  object EmailAddress:
    case class Unvalidated(override val value: Email) extends EmailAddress(value, false):
      def toValidated: Validated = Validated(value)
    case class Validated(override val value: Email) extends EmailAddress(value, true)

    def apply(email: Email): EmailAddress =
      Unvalidated(email)

    def apply(email: Email, validated: Boolean): EmailAddress =
      if validated then Validated(email) else Unvalidated(email)

    def either(email: String): Either[String, EmailAddress] =
      Email.either(email).map(Unvalidated(_))

    def toValidated(email: EmailAddress): Validated =
      email match
        case validated: Validated => validated
        case u: Unvalidated       => u.toValidated

  // ロール
  enum Role(val value: String, val displayName: String):
    case Manager extends Role("manager", "管理者")
    case Member  extends Role("member", "一般職員")

  object Role:
    def find(value: String): Option[Role] = values.find(_.value == value)

  // ログインID
  case class UsernameAndEmailAddress(username: Username, email: EmailAddress)
  type LoginId = EmailAddress | UsernameAndEmailAddress

  // ファクトリメソッド
  def create(id: Id, name: Name, email: EmailAddress, role: Role, username: Option[Username]): Staff =
    Staff(
      id       = id,
      name     = name,
      username = username,
      email    = email,
      role     = role
    )

  def either(
    id:         Id,
    familyName: String,
    givenName:  Option[String],
    email:      String,
    role:       Role,
    username:   Option[String]
  ): Either[String, Staff] =
    for
      fname     <- FamilyName.either(familyName)
      gname     <- givenName.traverse(GivenName.either(_))
      emailAddr <- EmailAddress.either(email)
      uname     <- username.traverse(Username.either(_))
    yield create(id, Name(fname, gname), emailAddr, role, uname)

end Staff
