//> using scala 3.7.1
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep io.github.iltotore::iron:3.0.1
//> using dep io.circe::circe-core:0.14.14
//> using dep io.circe::circe-generic:0.14.14
//> using file "domain/FromUUID.scala"
//> using file "domain/Staff.scala"
//> using file "domain/StaffRepository.scala"
//> using file "domain/EventPublisher.scala"
//> using file "domain/IdGenerator.scala"
//> using file "domain/TimeProvider.scala"
//> using file "domain/Logger.scala"
//> using file "application/CreateStaffService.scala"
//> using file "infrastructure/ConsoleLogger.scala"
//> using file "infrastructure/IdGeneratorImpl.scala"
//> using file "infrastructure/TimeProviderImpl.scala"
//> using file "test/fixtures/InMemoryEventPublisher.scala"
//> using file "test/fixtures/InMemoryStaffRepository.scala"

import cats.effect.*
import cats.effect.unsafe.implicits.global

import domain.*

import application.*

import infrastructure.*

import test.fixtures.*

val logger          = infrastructure.Logger.console[IO]
val staffRepository = new InMemoryStaffRepository[IO]
val eventPublisher  = new InMemoryEventPublisher[IO](logger)
val idGenerator     = infrastructure.IdGenerator.uuid[IO]
val timeProvider    = infrastructure.TimeProvider.system[IO]

val createStaffService = new CreateStaffServiceImpl[IO](
  staffRepository,
  eventPublisher,
  idGenerator,
  timeProvider
)

// テストデータ
val validCommand = CreateStaffCommand(
  name  = Staff.Name(Staff.FamilyName("田中"), Some(Staff.GivenName("太郎"))),
  email = Staff.EmailAddress.Unvalidated(Staff.Email.applyUnsafe("tanaka@example.com")),
  role  = Staff.Role.Manager
)

val duplicateCommand = CreateStaffCommand(
  name  = Staff.Name(Staff.FamilyName("佐藤"), Some(Staff.GivenName("花子"))),
  email = Staff.EmailAddress.Unvalidated(Staff.Email.applyUnsafe("tanaka@example.com")), // 同じメール
  role  = Staff.Role.Member
)

val anotherValidCommand = CreateStaffCommand(
  name  = Staff.Name(Staff.FamilyName("鈴木"), None), // 名前なし
  email = Staff.EmailAddress.Unvalidated(Staff.Email.applyUnsafe("suzuki@example.com")),
  role  = Staff.Role.Member
)

println("=== CreateStaffService デモ ===\n")

// 1. 正常なケース
println("1. 正常な職員作成:")
val result1 = createStaffService.create(validCommand).unsafeRunSync()
result1 match
  case Right(event) =>
    println(s"  ✓ 成功: 職員ID=${ event.staffId.value }")
    println(s"    名前: ${ event.staffName.fullName }")
    println(s"    メール: ${ event.email.value }")
    println(s"    ロール: ${ event.role.displayName }")
  case Left(error) =>
    println(s"  ✗ エラー: $error")

// 2. 重複エラー
println("\n2. 同じメールアドレスで作成（重複エラー）:")
val result2 = createStaffService.create(duplicateCommand).unsafeRunSync()
result2 match
  case Right(event) =>
    println(s"  ✓ 成功: $event")
  case Left(CreateStaffError.EmailAlreadyExists(email)) =>
    println(s"  ✗ 期待通りのエラー: メールアドレス ${ email.value } は既に使用されています")
  case Left(error) =>
    println(s"  ✗ 予期しないエラー: $error")

// 3. 別の正常なケース
println("\n3. 別の職員作成（名前なし）:")
val result3 = createStaffService.create(anotherValidCommand).unsafeRunSync()
result3 match
  case Right(event) =>
    println(s"  ✓ 成功: 職員ID=${ event.staffId.value }")
    println(s"    名前: ${ event.staffName.fullName }")
    println(s"    メール: ${ event.email.value }")
    println(s"    ロール: ${ event.role.displayName }")
  case Left(error) =>
    println(s"  ✗ エラー: $error")

// 発行されたイベントの確認
println("\n4. 発行されたイベント一覧:")
val events = eventPublisher.getEvents.unsafeRunSync()
events.foreach { event =>
  event match
    case StaffCreatedEvent(id, name, email, role, occurredAt) =>
      println(s"  - StaffCreatedEvent:")
      println(s"    ID: ${ id.value }")
      println(s"    名前: ${ name.fullName }")
      println(s"    メール: ${ email.value }")
      println(s"    ロール: ${ role.displayName }")
      println(s"    発生時刻: $occurredAt")
}
