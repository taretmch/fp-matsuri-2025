package presentation.request

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

import sttp.tapir.*

import domain.Staff

import application.CreateStaffCommand

import presentation.DomainCodec.given

/** 職員作成リクエスト */
case class CreateStaffRequest(
  name:  Staff.Name,
  email: Staff.Email,
  role:  Staff.Role
)

object CreateStaffRequest:
  given Encoder[CreateStaffRequest] = deriveEncoder[CreateStaffRequest]
  given Decoder[CreateStaffRequest] = deriveDecoder[CreateStaffRequest]
  given Schema[CreateStaffRequest]  = Schema.derived[CreateStaffRequest]

  def toCommand(dto: CreateStaffRequest): CreateStaffCommand =
    CreateStaffCommand(
      name  = dto.name,
      email = Staff.EmailAddress(dto.email),
      role  = dto.role
    )
