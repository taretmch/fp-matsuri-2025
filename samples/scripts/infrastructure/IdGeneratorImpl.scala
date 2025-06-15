package infrastructure

import java.util.UUID

import cats.effect.Sync

import domain.{ FromUUID, IdGenerator }

/** UUIDベースのID生成器の実装 */
class UuidIdGenerator[F[_]: Sync] extends IdGenerator[F]:
  def generate[K: FromUUID]: F[K] =
    Sync[F].delay {
      FromUUID[K].fromUUID(UUID.randomUUID())
    }

object IdGenerator:
  /** UUIDベースのID生成器を作成 */
  def uuid[F[_]: Sync]: IdGenerator[F] = new UuidIdGenerator[F]
