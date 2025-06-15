package test.fixtures

import cats.syntax.all.*

import cats.effect.Sync

import io.circe.Encoder

import domain.{ Event, EventPublisher, Logger }

/** テスト用のインメモリ実装 */
class InMemoryEventPublisher[F[_]: Sync](logger: Logger[F]) extends EventPublisher[F]:
  import io.circe.syntax.*

  private val events = scala.collection.mutable.ArrayBuffer.empty[Event]

  def publish[E <: Event](event: E)(using encoder: Encoder[E]): F[Unit] =
    for
      _ <- Sync[F].delay { events += event }
      encoded = event.asJson.noSpaces
      _ <- logger.info(s"イベント発行: ${ event.name } at ${ event.timestamp }")
      _ <- logger.debug(s"エンコード済み: $encoded")
    yield ()

  def getEvents: F[List[Event]] = Sync[F].delay(events.toList)
