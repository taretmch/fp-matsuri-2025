package infrastructure

import java.time.Instant

import cats.effect.Sync

import domain.TimeProvider

/** システム時刻を使用するTimeProviderの実装 */
class SystemTimeProvider[F[_]: Sync] extends TimeProvider[F]:
  def now: F[Instant] = Sync[F].delay(Instant.now())

object TimeProvider:
  /** システム時刻を使用するTimeProviderを作成 */
  def system[F[_]: Sync]: TimeProvider[F] = new SystemTimeProvider[F]
