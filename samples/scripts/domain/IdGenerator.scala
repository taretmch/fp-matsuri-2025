package domain

trait IdGenerator[F[_]]:
  def generate[K: FromUUID]: F[K]
