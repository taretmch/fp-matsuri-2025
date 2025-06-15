package infrastructure

import java.time.LocalDateTime

import cats.effect.Sync

import domain.Logger

/** コンソールへ出力するロガーの実装 */
class ConsoleLogger[F[_]: Sync] extends Logger[F]:

  private def log(level: String, message: String): F[Unit] =
    Sync[F].delay {
      val timestamp = LocalDateTime.now()
      println(s"[$timestamp] [$level] $message")
    }

  def info(message: String): F[Unit] =
    log("INFO", message)

  def warn(message: String): F[Unit] =
    log("WARN", message)

  def error(message: String): F[Unit] =
    log("ERROR", message)

  def error(message: String, throwable: Throwable): F[Unit] =
    Sync[F].delay {
      val timestamp = LocalDateTime.now()
      println(s"[$timestamp] [ERROR] $message")
      throwable.printStackTrace()
    }

  def debug(message: String): F[Unit] =
    log("DEBUG", message)

object Logger:
  /** コンソールロガーを作成 */
  def console[F[_]: Sync]: Logger[F] = new ConsoleLogger[F]
