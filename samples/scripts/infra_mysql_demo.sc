//> using scala 3.7.1
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep org.tpolecat::doobie-core:1.0.0-RC9
//> using dep org.tpolecat::doobie-hikari:1.0.0-RC9
//> using dep org.tpolecat::doobie-mysql:1.0.0-RC9
//> using dep mysql:mysql-connector-java:8.0.33
//> using dep io.github.iltotore::iron:3.0.1
//> using file "domain/FromUUID.scala"
//> using file "domain/Staff.scala"
//> using file "domain/StaffRepository.scala"
//> using file "infrastructure/StaffRepositoryImpl.scala"

import java.util.UUID

import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import doobie.mysql.implicits.*

import com.zaxxer.hikari.HikariConfig

import domain.*
import domain.Staff.*

import infrastructure.*

private lazy val maxCore: Int = Runtime.getRuntime.availableProcessors()

// MySQL接続設定
val mysqlConfig = new HikariConfig()
mysqlConfig.setDriverClassName("com.mysql.cj.jdbc.Driver")
mysqlConfig.setJdbcUrl("jdbc:mysql://localhost:33306/staff_db?useSSL=false&allowPublicKeyRetrieval=true")
mysqlConfig.setUsername("root")
mysqlConfig.setPassword("password")
mysqlConfig.setMaximumPoolSize(maxCore * 2)

// Transactorの作成
val transactor: Resource[IO, HikariTransactor[IO]] =
  HikariTransactor.fromHikariConfig[IO](mysqlConfig, logHandler = None)

def run: IO[Unit] = transactor.use { xa =>
  for
    // テーブル作成
    _ <- IO.println("=== MySQL連携デモ ===")
    _ <- IO.println("1. テーブルを作成します...")
    _ <- StaffRepositoryImpl.createTable(xa)
    _ <- IO.println("テーブル作成完了")

    // リポジトリの初期化
    repository = StaffRepositoryImpl(xa)

    // デモデータの作成
    _ <- IO.println("\n2. 職員データを作成します...")

    // 職員1: 管理者
    staff1Id = Staff.Id.fromUUID(UUID.randomUUID())
    staff1 = Staff.create(
               id = staff1Id,
               name = Staff.Name(
                 Staff.FamilyName.applyUnsafe("山田"),
                 Some(Staff.GivenName.applyUnsafe("太郎"))
               ),
               email    = Staff.EmailAddress(Staff.Email.applyUnsafe("yamada.taro@example.com")),
               role     = Staff.Role.Manager,
               username = Some(Staff.Username.applyUnsafe("yamada_t"))
             )
    _ <- repository.save(staff1)
    _ <- IO.println(s"✅ 職員作成成功: ${ staff1.name.fullName } (ID: ${ staff1.id.value })")

    // 職員2: 一般職員
    staff2Id = Staff.Id.fromUUID(UUID.randomUUID())
    staff2 = Staff.create(
               id = staff2Id,
               name = Staff.Name(
                 Staff.FamilyName.applyUnsafe("佐藤"),
                 Some(Staff.GivenName.applyUnsafe("花子"))
               ),
               email    = Staff.EmailAddress(Staff.Email.applyUnsafe("sato.hanako@example.com")),
               role     = Staff.Role.Member,
               username = None
             )
    _ <- repository.save(staff2)
    _ <- IO.println(s"✅ 職員作成成功: ${ staff2.name.fullName } (ID: ${ staff2.id.value })")

    // データ検索のデモ
    _ <- IO.println("\n3. データ検索のデモ...")

    // メールアドレスで検索
    searchEmail = Staff.EmailAddress.Unvalidated(Staff.Email.applyUnsafe("yamada.taro@example.com"))
    found <- repository.findByEmail(searchEmail)
    _ <- found match
           case Some(staff) =>
             for
               _ <- IO.println(s"✅ メールアドレスで検索成功: ${ staff.name.fullName }")
               _ <- IO.println(s"   - ID: ${ staff.id.value }")
               _ <- IO.println(s"   - Role: ${ staff.role.displayName }")
               _ <- IO.println(s"   - Username: ${ staff.username.map(_.value).getOrElse("なし") }")
             yield ()
           case None =>
             IO.println("❌ 職員が見つかりません")

    // 存在しないメールアドレスで検索
    _ <- IO.println("\n4. 存在しないメールアドレスで検索...")
    notFoundEmail = Staff.EmailAddress.Unvalidated(Staff.Email.applyUnsafe("notfound@example.com"))
    notFound <- repository.findByEmail(notFoundEmail)
    _ <- notFound match
           case Some(_) => IO.println("❌ 予期しない結果: 職員が見つかりました")
           case None    => IO.println("✅ 期待通り: 職員が見つかりませんでした")
  yield ()
}

run.unsafeRunSync()
