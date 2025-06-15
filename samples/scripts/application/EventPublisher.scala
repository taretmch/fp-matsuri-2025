package application

import cats.effect.Sync

import io.circe.Encoder

import domain.{ Event, EventPublisher }

class InMemoryEventPublisher[F[_]: Sync] extends EventPublisher[F]:
  import io.circe.syntax.*

  private val events = scala.collection.mutable.ArrayBuffer.empty[Event]

  def publish[E <: Event](event: E)(using encoder: Encoder[E]): F[Unit] =
    Sync[F].delay {
      events += event
      val encoded = event.asJson.noSpaces
      println(s"イベント発行: ${ event.name } at ${ event.timestamp }")
      println(s"エンコード済み: $encoded")
    }

  def getEvents: F[List[Event]] = Sync[F].delay(events.toList)
