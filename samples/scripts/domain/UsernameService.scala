package domain

import domain.Staff.Username

/** ユーザー名生成のドメインサービス */
trait UsernameService[F[_]]:
  /** ランダムなユーザー名を生成 */
  def generateRandom: F[Username]

  /** 指定された文字数のランダムなユーザー名を生成 */
  def generateRandomWithLength(length: Int): F[Username]
