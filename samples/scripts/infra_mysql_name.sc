//> using scala 3.7.1
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep org.tpolecat::doobie-core:1.0.0-RC9
//> using dep org.tpolecat::doobie-hikari:1.0.0-RC9
//> using dep org.tpolecat::doobie-mysql:1.0.0-RC9
//> using dep mysql:mysql-connector-java:8.0.33
//> using dep io.github.iltotore::iron:3.0.1

//> using file domain/Staff.scala
//> using file domain/FromUUID.scala

import java.util.UUID

import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import doobie.mysql.implicits.*

import com.zaxxer.hikari.HikariConfig

import domain.Staff
import domain.Staff.*

private lazy val maxCore: Int = Runtime.getRuntime.availableProcessors()

// MySQL接続設定
val mysqlConfig = new HikariConfig()
mysqlConfig.setDriverClassName("com.mysql.cj.jdbc.Driver")
mysqlConfig.setJdbcUrl("jdbc:mysql://localhost:33306/staff_db?useSSL=false&allowPublicKeyRetrieval=true")
mysqlConfig.setUsername("root")
mysqlConfig.setPassword("password")
mysqlConfig.setMaximumPoolSize(maxCore * 2)

val transactor: Resource[IO, HikariTransactor[IO]] =
  HikariTransactor.fromHikariConfig[IO](mysqlConfig, logHandler = None)

given Meta[Staff.FamilyName] = Meta[String].timap(Staff.FamilyName.applyUnsafe)(_.value)
given Meta[Staff.GivenName]  = Meta[String].timap(Staff.GivenName.applyUnsafe)(_.value)

def run: IO[Unit] = transactor.use { xa =>
  for
    _ <- IO.println("=== Staff.Name の doobie デモ ===")

    // テーブル作成
    _ <- IO.println("\n1. テーブルを作成します...")
    _ <- sql"""
      |CREATE TABLE IF NOT EXISTS `names_demo` (
      |  `id` INT AUTO_INCREMENT PRIMARY KEY,
      |  `family_name` VARCHAR(32) NOT NULL,
      |  `given_name` VARCHAR(32),
      |  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      |)
    """.stripMargin.update.run.transact(xa)
    _ <- IO.println("テーブル作成完了")

    // データの挿入
    _ <- IO.println("\n2. Name データを挿入します...")

    // 名前1: 姓名あり
    name1 = Staff.Name(
              Staff.FamilyName.applyUnsafe("山田"),
              Some(Staff.GivenName.applyUnsafe("太郎"))
            )
    _ <- sql"""
      |INSERT INTO `names_demo` (`family_name`, `given_name`)
      |VALUES (${ name1.familyName }, ${ name1.givenName })
    """.stripMargin.update.run.transact(xa)
    _ <- IO.println(s"✅ 挿入成功: ${ name1.fullName }")

    // 名前2: 姓のみ
    name2 = Staff.Name(
              Staff.FamilyName.applyUnsafe("鈴木"),
              None
            )
    _ <- sql"""
      |INSERT INTO `names_demo` (`family_name`, `given_name`)
      |VALUES (${ name2.familyName }, ${ name2.givenName })
    """.stripMargin.update.run.transact(xa)
    _ <- IO.println(s"✅ 挿入成功: ${ name2.fullName }")

    // データの取得
    _ <- IO.println("\n3. Name データを取得します...")

    // 全件取得
    names <- sql"""
      |SELECT `family_name`, `given_name`
      |FROM `names_demo`
      |ORDER BY `id`
    """.stripMargin.query[Staff.Name].to[List].transact(xa)

    _ <- IO.println(s"取得件数: ${ names.length }")
    _ <- names.traverse { name =>
           IO.println(
             s"  - ${ name.fullName } (姓: ${ name.familyName.value }, 名: ${ name.givenName.map(_.value).getOrElse("なし") })"
           )
         }

    // 特定の姓を持つ名前を検索
    _ <- IO.println("\n4. 特定の姓で検索...")
    searchFamilyName = Staff.FamilyName.applyUnsafe("山田")
    foundNames <- sql"""
      |SELECT `family_name`, `given_name`
      |FROM `names_demo`
      |WHERE `family_name` = ${ searchFamilyName }
    """.stripMargin.query[Staff.Name].to[List].transact(xa)

    _ <- foundNames.traverse { name =>
           IO.println(s"✅ 検索結果: ${ name.fullName }")
         }

    // 複合型の便利な点のデモ
    _ <- IO.println("\n5. 複合型の便利な点...")

    // Write[Staff.Name] を使った挿入（より簡潔）
    name3 = Staff.Name(
              Staff.FamilyName.applyUnsafe("佐藤"),
              Some(Staff.GivenName.applyUnsafe("花子"))
            )
    id <- sql"""
      |INSERT INTO `names_demo` (`family_name`, `given_name`)
      |VALUES (${ name3 })
    """.stripMargin.update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    _ <- IO.println(s"✅ Writeインスタンスを使った挿入成功: ${ name3.fullName } (ID: $id)")

    // Read[Staff.Name] を使った単一レコード取得
    _ <- IO.println("\n6. 単一レコードの取得...")
    singleName <- sql"""
      |SELECT `family_name`, `given_name`
      |FROM `names_demo`
      |WHERE `id` = $id
    """.stripMargin.query[Staff.Name].unique.transact(xa)
    _ <- IO.println(s"✅ ID=$id のレコード: ${ singleName.fullName }")
  yield ()
}

run.unsafeRunSync()

// 実行方法:
// 1. cd samples && docker compose up -d mysql
// 2. scala-cli run scripts/infra_mysql_name.sc
