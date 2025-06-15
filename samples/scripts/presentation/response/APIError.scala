package presentation.response

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

import sttp.tapir.*

/** APIエラーレスポンス */
case class APIError(
  error:   String,
  details: Option[String] = None
)

object APIError:
  given Encoder[APIError] = deriveEncoder[APIError]
  given Decoder[APIError] = deriveDecoder[APIError]
  given Schema[APIError]  = Schema.derived[APIError]
