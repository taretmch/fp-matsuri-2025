package presentation

import cats.syntax.all.*

import cats.effect.*

import sttp.tapir.*
import sttp.tapir.json.circe.*

import domain.Staff

import application.{ CreateStaffError, CreateStaffService }

import presentation.request.CreateStaffRequest
import presentation.response.{ APIError, StaffResponse }
import presentation.DomainCodec.given

/** 職員管理API定義 */
object StaffsAPI:
  // 職員作成エンドポイント
  val createStaffEndpoint: PublicEndpoint[CreateStaffRequest, APIError, StaffResponse, Any] =
    endpoint.post
      .in("api" / "staffs")
      .in(jsonBody[CreateStaffRequest])
      .out(jsonBody[StaffResponse])
      .errorOut(
        oneOf[APIError](
          oneOfVariant(
            statusCode(sttp.model.StatusCode.BadRequest)
              .and(jsonBody[APIError])
          ),
          oneOfVariant(
            statusCode(sttp.model.StatusCode.InternalServerError)
              .and(jsonBody[APIError])
          )
        )
      )
      .summary("新規職員を作成")
      .description("新規職員を作成します。メールアドレスは一意である必要があります。")

  // サーバーロジック実装
  def createStaffServerLogic[F[_]: Async](
    service: CreateStaffService[F]
  ): CreateStaffRequest => F[Either[APIError, StaffResponse]] = { dto =>
    val command = CreateStaffRequest.toCommand(dto)

    service.create(command).map {
      case Right(event) =>
        Right(StaffResponse.fromEvent(event))
      case Left(CreateStaffError.EmailAlreadyExists(email)) =>
        Left(
          APIError(
            error   = "Email already exists",
            details = Some(s"The email address ${ email.value.value } is already registered")
          )
        )
      case Left(CreateStaffError.Unexpected(ex)) =>
        Left(
          APIError(
            error   = "Internal server error",
            details = Some(ex.getMessage)
          )
        )
    }
  }
