package test.fixtures

import cats.effect.Sync

import domain.{ Staff, StaffRepository }

/** テスト用のインメモリ実装 */
class InMemoryStaffRepository[F[_]: Sync] extends StaffRepository[F]:
  private val storage = scala.collection.mutable.Map.empty[Staff.Id, Staff]

  def findByEmail(email: Staff.EmailAddress): F[Option[Staff]] =
    Sync[F].delay {
      storage.values.find(_.email == email)
    }

  def save(staff: Staff): F[Staff] =
    Sync[F].delay {
      storage.put(staff.id, staff)
      staff
    }
