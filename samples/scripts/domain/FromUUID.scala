package domain

import java.util.UUID

// UUID型からエンティティIDへの変換を提供する型クラス
trait FromUUID[K]:
  def fromUUID(uuid: UUID): K

object FromUUID:
  def apply[K](using instance: FromUUID[K]): FromUUID[K] = instance
