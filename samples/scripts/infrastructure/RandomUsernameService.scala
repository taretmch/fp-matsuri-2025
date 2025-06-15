package infrastructure

import scala.util.Random

import cats.effect.Sync

import domain.{ Staff, UsernameService }

/** ランダムなユーザー名を生成するサービスの実装 */
class RandomUsernameService[F[_]: Sync] extends UsernameService[F]:

  def generateRandom: F[Staff.Username] =
    Sync[F].delay {
      val seed = Random.alphanumeric.take(16).mkString
      Staff.Username.fromSeed(seed)
    }

  def generateRandomWithLength(length: Int): F[Staff.Username] =
    Sync[F].delay {
      val validLength = length.max(4).min(16)
      val seed        = Random.alphanumeric.take(validLength * 2).mkString
      Staff.Username.fromSeed(seed)
    }

object UsernameService:
  /** ランダムなユーザー名生成サービスを作成 */
  def random[F[_]: Sync]: UsernameService[F] = new RandomUsernameService[F]
