package infrastructure

import java.util.UUID

import io.github.iltotore.iron.*

import cats.implicits.*

import cats.effect.IO

import doobie.*
import doobie.implicits.*
import doobie.mysql.implicits.*

import domain.*
import domain.Staff.*
import domain.StaffRepository

class StaffRepositoryImpl(transactor: Transactor[IO]) extends StaffRepository[IO]:

  // カスタムMetaインスタンスの定義

  // Staff.Id (opaque type UUID) - MySQLではBINARY(16)として保存
  given Meta[Staff.Id] = Meta[String].timap(str => Staff.Id.fromUUID(UUID.fromString(str)))(_.value.toString)

  // Staff.FamilyName
  given Meta[Staff.FamilyName] = Meta[String].timap(Staff.FamilyName.applyUnsafe)(_.value)

  // Staff.GivenName
  given Meta[Staff.GivenName] = Meta[String].timap(Staff.GivenName.applyUnsafe)(_.value)

  // Staff.Username
  given Meta[Staff.Username] = Meta[String].timap(Staff.Username.applyUnsafe)(_.value)

  // Staff.Email
  given Meta[Staff.Email] = Meta[String].timap(Staff.Email.applyUnsafe)(_.value)

  // Staff.Role
  given Meta[Staff.Role] = Meta[String].tiemap(str => Staff.Role.find(str).toRight(s"Unknown role: $str"))(_.value)

  override def findByEmail(email: Staff.EmailAddress): IO[Option[Staff]] =
    sql"""
      |SELECT BIN_TO_UUID(`id`), `family_name`, `given_name`, `email`, `email_validated`, `role`, `username`
      |FROM `staffs`
      |WHERE `email` = ${ email.value }
    """.stripMargin
      .query[
        (Staff.Id, Staff.FamilyName, Option[Staff.GivenName], Staff.Email, Boolean, Staff.Role, Option[Staff.Username])
      ]
      .map {
        case (id, familyName, givenName, emailValue, isValidated, role, username) =>
          Staff.create(
            id,
            Staff.Name(familyName, givenName),
            Staff.EmailAddress(emailValue, isValidated),
            role,
            username
          )
      }
      .option
      .transact(transactor)

  override def save(staff: Staff): IO[Staff] =
    sql"""
      |INSERT INTO `staffs` (`id`, `family_name`, `given_name`, `email`, `email_validated`, `role`, `username`)
      |VALUES (
      |  UUID_TO_BIN(${ staff.id }),
      |  ${ staff.name.familyName },
      |  ${ staff.name.givenName },
      |  ${ staff.email.value },
      |  ${ staff.email.isValidated },
      |  ${ staff.role },
      |  ${ staff.username }
      |)
      |ON DUPLICATE KEY UPDATE
      |  `family_name`     = VALUES(`family_name`),
      |  `given_name`      = VALUES(`given_name`),
      |  `email`           = VALUES(`email`),
      |  `email_validated` = VALUES(`email_validated`),
      |  `role`            = VALUES(`role`),
      |  `username`        = VALUES(`username`)
    """.stripMargin.update.run.transact(transactor).map(_ => staff)

object StaffRepositoryImpl:
  // テーブル作成用のSQL
  def createTableSql: Update0 =
    sql"""
      |CREATE TABLE IF NOT EXISTS `staffs` (
      |  `id` BINARY(16) PRIMARY KEY,
      |  `family_name` VARCHAR(32) NOT NULL,
      |  `given_name` VARCHAR(32),
      |  `email` VARCHAR(255) NOT NULL UNIQUE,
      |  `email_validated` BOOLEAN NOT NULL DEFAULT FALSE,
      |  `username` VARCHAR(16),
      |  `role` VARCHAR(20) NOT NULL,
      |  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      |  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      |  INDEX `idx_email` (`email`),
      |  INDEX `idx_role` (`role`)
      |)
    """.stripMargin.update

  // テーブル作成メソッド
  def createTable(transactor: Transactor[IO]): IO[Unit] =
    createTableSql.run.transact(transactor).void
