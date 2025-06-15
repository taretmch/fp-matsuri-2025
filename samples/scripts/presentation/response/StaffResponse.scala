package presentation.response

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

import sttp.tapir.*

import domain.Staff

import application.StaffCreatedEvent

import presentation.DomainCodec.given

/** 職員レスポンス */
case class StaffResponse(
  id:       Staff.Id,
  name:     Staff.Name,
  email:    Staff.EmailAddress,
  role:     Staff.Role,
  username: Option[Staff.Username]
)

object StaffResponse:
  given Encoder[StaffResponse] = deriveEncoder[StaffResponse]
  given Decoder[StaffResponse] = deriveDecoder[StaffResponse]
  given Schema[StaffResponse]  = Schema.derived[StaffResponse]

  def fromDomain(staff: Staff): StaffResponse = StaffResponse(
    id       = staff.id,
    name     = staff.name,
    email    = staff.email,
    role     = staff.role,
    username = staff.username
  )

  def fromEvent(event: StaffCreatedEvent): StaffResponse = StaffResponse(
    id       = event.staffId,
    name     = event.staffName,
    email    = event.email,
    role     = event.role,
    username = None
  )
