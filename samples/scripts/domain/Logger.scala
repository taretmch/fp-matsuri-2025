package domain

/** ロギングの抽象化インターフェース */
trait Logger[F[_]]:
  def info(message:  String): F[Unit]
  def warn(message:  String): F[Unit]
  def error(message: String): F[Unit]
  def error(message: String, throwable: Throwable): F[Unit]
  def debug(message: String): F[Unit]
