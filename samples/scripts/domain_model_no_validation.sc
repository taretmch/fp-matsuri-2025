//> using scala 3.7.1

import java.util.UUID

object Domain:
  opaque type Id = UUID
  object Id:
    def generate(): Id = UUID.randomUUID()
    def fromUUID(uuid: UUID): Id = uuid
  opaque type GivenName = String
  object GivenName:
    def apply(value: String): GivenName = value
  opaque type FamilyName = String
  object FamilyName:
    def apply(value: String): FamilyName = value

  case class Name(
    familyName: FamilyName,
    givenName:  Option[GivenName]
  ):
    def fullName: String =
      givenName match
        case Some(gname) => s"$familyName $gname"
        case None        => familyName

  // パラメータを持たせることができる
  enum Role(val value: String, val displayName: String):
    case Manager extends Role("manager", "管理者")
    case Member  extends Role("member", "一般職員")

  sealed abstract class EmailAddress(val value: String, val isValidated: Boolean)
  object EmailAddress:
    case class UnvalidatedEmailAddress(override val value: String) extends EmailAddress(value, false):
      def toValidated: ValidatedEmailAddress = ValidatedEmailAddress(value)
    case class ValidatedEmailAddress(override val value: String) extends EmailAddress(value, true)

    def toValidated(email: EmailAddress): ValidatedEmailAddress =
      email match
        case v: ValidatedEmailAddress   => v
        case u: UnvalidatedEmailAddress => u.toValidated

  opaque type Username = String
  object Username:
    def apply(value: String): Username = value

  case class LoginId(
    username: Option[Username],
    email:    EmailAddress
  )

  case class Staff(
    id:       Id,
    name:     Name,
    username: Option[Username],
    email:    EmailAddress,
    role:     Role
  ):
    val loginId: LoginId = LoginId(username, email)
end Domain

// または
object Domain2:
  // 以下2つは実行時 String として扱われるので、match 式でのパターンマッチングに制限がある
  opaque type UnvalidatedEmailAddress = String
  opaque type ValidatedEmailAddress   = String
  type EmailAddress                   = UnvalidatedEmailAddress | ValidatedEmailAddress

  type RoleAsUnionTypes = "Manager" | "Member"

  // named tuple
  type Name = (familyName: Domain.FamilyName, givenName: Option[Domain.GivenName])

  // Union Types の場合
  case class UsernameAndEmailAddress(username: Domain.Username, email: Domain.EmailAddress)
  type LoginId = Domain.EmailAddress | UsernameAndEmailAddress
end Domain2

def demoDomain(): Unit =
  import Domain.*

  println("=== ID のデモ ===")
  val id1 = Id.generate()
  val id2 = Id.fromUUID(UUID.randomUUID())
  print(s"Generated ID: $id1, From UUID: $id2\n")

  println("=== 名前のデモ ===")

  val givenName  = GivenName("太郎")
  val familyName = FamilyName("山田")

  val name = Name(
    familyName = familyName,
    givenName  = Some(givenName)
  )
  println(name)
  println(name.fullName)

  // こちらはエラーになる (※同一スコープでない場合)
  // val invalidAssign: GivenName = FamilyName("山田")
  // => [error] Found:    domain_model$_.this.Domain.FamilyName
  // => [error] Required: domain_model$_.this.Domain.GivenName

  println("=== メールアドレスのデモ ===")
  val unvalidatedEmail = EmailAddress.UnvalidatedEmailAddress("taro.yamada@example.com")
  val validatedEmail   = unvalidatedEmail.toValidated
  println(unvalidatedEmail)
  println(unvalidatedEmail.isValidated) // false
  println(validatedEmail)
  println(validatedEmail.isValidated) // true

  println("=== ロールのデモ ===")
  Role.values.foreach { role =>
    println(s"${ role } : ${ role.value }, Display Name: ${ role.displayName }")
  }

  println("=== 職員のデモ ===")
  val staff = Staff(
    id       = Id.generate(),
    name     = name,
    username = None,
    email    = unvalidatedEmail,
    role     = Role.Manager
  )
  println(staff)

def demoDomain2(): Unit =
  import Domain2.*

  val name: Name = (Domain.FamilyName("山田"), Some(Domain.GivenName("太郎")))
  println("=== ドメインモデルのデモ (named tuple) ===")
  println(name)
  println(name.familyName)
  println(name.givenName)

  println("=== ログインIDのデモ (Union Types) ===")
  val email:    Domain.EmailAddress = Domain.EmailAddress.UnvalidatedEmailAddress("taro.yamada@example.com")
  val username: Domain.Username     = Domain.Username("tyamada")
  val loginId1: LoginId             = UsernameAndEmailAddress(username, email)
  val loginId2: LoginId             = email
  println(loginId1)
  println(loginId2)

demoDomain()
demoDomain2()
