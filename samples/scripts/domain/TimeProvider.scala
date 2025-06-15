package domain

import java.time.Instant

/** 時刻取得の抽象化
  * 
  * 副作用（現在時刻の取得）を型で表現し、
  * テスタビリティと純粋性を保つ
  */
trait TimeProvider[F[_]]:
  def now: F[Instant]
