package domain

import domain.Staff

trait StaffRepository[F[_]]:
  def findByEmail(email: Staff.EmailAddress): F[Option[Staff]]
  def save(staff:        Staff):              F[Staff]
