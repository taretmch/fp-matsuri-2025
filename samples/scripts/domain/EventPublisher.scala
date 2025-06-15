package domain

import java.time.Instant

import io.circe.Encoder

trait Event:
  def timestamp: Instant
  def name:      String

trait EventPublisher[F[_]]:
  def publish[E <: Event](event: E)(using encoder: Encoder[E]): F[Unit]
