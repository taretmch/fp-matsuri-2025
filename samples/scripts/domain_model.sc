//> using scala 3.7.1
//> using dep "io.github.iltotore::iron:3.0.1"
//> using dep "org.typelevel::cats-core:2.13.0"
//> using file "domain/Staff.scala"
//> using file "domain/FromUUID.scala"

import java.util.UUID

import cats.syntax.traverse.*

import domain.{ FromUUID, Staff }

println("=== ドメインモデルのデモ ===")

// 成功例（フルネーム）
val staff1 = Staff.either(
  id         = Staff.Id.fromUUID(UUID.randomUUID()),
  familyName = "山田",
  givenName  = Some("太郎"),
  email      = "taro.yamada@example.com",
  role       = Staff.Role.Manager,
  username   = Some("tyamada")
)
println(staff1)

staff1 match
  case Right(staff) =>
    println(s"職員作成成功: ${ staff.name.fullName }")
    println(s"ID: ${ staff.id }")
    println(s"ロール: ${ staff.role.displayName }")
    println(s"メール検証済み: ${ staff.email.isValidated }")
    staff.loginId match
      case Staff.UsernameAndEmailAddress(username, email) =>
        println(s"ログインID: ユーザー名 '${ username.value }' またはメールアドレス")
      case email: Staff.EmailAddress =>
        println(s"ログインID: メールアドレスのみ")
  case Left(error) =>
    println(s"エラー: $error")

println()

// 成功例（姓のみ）
val staff2 = Staff.either(
  id         = Staff.Id.fromUUID(UUID.randomUUID()),
  familyName = "佐藤",
  givenName  = None,
  email      = "sato@example.com",
  role       = Staff.Role.Member,
  username   = None
)

println(staff2)

staff2 match
  case Right(staff) =>
    println(s"職員作成成功（姓のみ）: ${ staff.name.familyName }")
    println(s"名: ${ staff.name.givenName.getOrElse("なし") }")
  case Left(error) =>
    println(s"エラー: $error")

println()

// 失敗例：無効なメールアドレス
val staff3 = Staff.either(
  id         = Staff.Id.fromUUID(UUID.randomUUID()),
  familyName = "田中",
  givenName  = Some("花子"),
  email      = "invalid-email",
  role       = Staff.Role.Member,
  username   = None
)

println(staff3)

staff3 match
  case Right(_)    => println("成功（想定外）")
  case Left(error) => println(s"バリデーションエラー: $error")

println()

// メール検証の例
staff1.map { staff =>
  val validatedEmail = Staff.EmailAddress.toValidated(staff.email)
  println(s"メール検証後: ${ validatedEmail.isValidated }")
}
