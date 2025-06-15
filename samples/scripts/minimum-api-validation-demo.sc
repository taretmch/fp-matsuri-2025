//> using scala 3.7.1
//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-http4s-server:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.11.33
//> using dep org.http4s::http4s-ember-server:0.23.30
//> using dep org.http4s::http4s-ember-client:0.23.30
//> using dep org.http4s::http4s-dsl:0.23.30
//> using dep io.circe::circe-generic:0.14.13
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep io.github.iltotore::iron:3.0.1
//> using dep io.github.iltotore::iron-circe:3.0.1
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.33

import java.util.UUID

import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*

import cats.syntax.all.*

import cats.effect.*

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*

import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

// ドメイン定義
// Opaque Type for StaffId
opaque type StaffId = UUID
object StaffId {
  def apply(uuid: UUID): StaffId = uuid
  def generate(): StaffId = UUID.randomUUID()

  extension (id: StaffId) {
    def value: UUID = id
  }

  // Circe Encoder/Decoder
  given Encoder[StaffId] = Encoder[UUID].contramap(_.value)
  given Decoder[StaffId] = Decoder[UUID].map(StaffId.apply)

  // Tapir Schema
  given Schema[StaffId] = Schema.string[StaffId].description("Staff unique identifier")
}

// Enum for Role with business logic
enum Role(val value: String, val displayName: String, val canManage: Boolean):
  case Staff   extends Role("staff", "一般職員", false)
  case Manager extends Role("manager", "管理者", true)

  def canApprove(targetRole: Role): Boolean = this.canManage && !targetRole.canManage

object Role {
  def from(value: String): Option[Role] =
    Role.values.find(_.value == value)

  // Circe Encoder/Decoder
  given Encoder[Role] = Encoder[String].contramap(_.value)
  given Decoder[Role] = Decoder[String].emap(value => from(value).toRight(s"Role $value not found"))

  // Tapir Schema
  given Schema[Role] = Schema
    .string[Role]
    .validate(
      Validator.enumeration(
        Role.values.toList
      )
    )
    .description("Staff role (staff or manager)")
    .encodedExample("staff")
}

type StaffName = StaffName.T
object StaffName extends RefinedType[String, MinLength[1] & MaxLength[50]] {
  // Tapir Schema with validation constraints
  given Schema[StaffName] = Schema
    .string[StaffName]
    .validate(Validator.minLength(1))
    .validate(Validator.maxLength(50))
    .description("Staff name (1-50 characters)")
}

type Email = Email.T
object Email extends RefinedType[String, Match["^[\\w\\.-]+@[\\w\\.-]+\\.\\w+$"]] {
  // Tapir Schema with validation constraints
  given Schema[Email] = Schema
    .string[Email]
    .validate(Validator.pattern("^[\\w\\.-]+@[\\w\\.-]+\\.\\w+$"))
    .description("Valid email address")
    .format("email")
}

type Department = "Engineering" | "Sales" | "Marketing" | "BackOffice"

object Department {
  // Union Typeの値を配列として定義（一箇所で管理）
  val values: Array[Department] = Array("Engineering", "Sales", "Marketing", "BackOffice")

  def either(value: String): Either[String, Department] = value match {
    case dept: Department => Right(dept)
    case _                => Left(s"Department $value not found")
  }

  def apply(value: String): Department = either(value) match {
    case Right(dept) => dept
    case Left(error) => throw new IllegalArgumentException(error)
  }

  // Circe Encoder/Decoder
  given Encoder[Department] = Encoder[String].contramap(_.toString)
  given Decoder[Department] = Decoder[String].emap(either)

  // Tapir Schema
  given Schema[Department] = Schema
    .string[Department]
    .validate(
      Validator.enumeration(
        values.toList
      )
    )
}

// リクエスト/レスポンスの型（ドメインの型を使用）
case class CreateStaffRequest(
  name:       StaffName,
  email:      Email,
  department: Department,
  role:       Role
)

case class StaffResponse(
  id:         StaffId,
  name:       StaffName,
  email:      Email,
  department: Department,
  role:       Role
)

case class ApiError(
  error: String
)

// Circe Encoder/Decoder (自動導出でドメイン型対応)
import Department.given
import Role.given

given Encoder[CreateStaffRequest] = deriveEncoder
given Decoder[CreateStaffRequest] = deriveDecoder

given Encoder[StaffResponse] = deriveEncoder
given Decoder[StaffResponse] = deriveDecoder

given Encoder[ApiError] = deriveEncoder
given Decoder[ApiError] = deriveDecoder

// エンドポイント定義
val createStaffEndpoint = endpoint.post
  .in("api" / "staff")
  .in(jsonBody[CreateStaffRequest])
  .out(jsonBody[StaffResponse])
  .errorOut(
    oneOf[ApiError](
      oneOfVariant(statusCode(sttp.model.StatusCode.BadRequest).and(jsonBody[ApiError]))
    )
  )
  .summary("Create a new staff member")

// ビジネスロジック（ドメイン型を直接受け取る - Tapirでバリデーション済み）
def createStaff(request: CreateStaffRequest): IO[Either[ApiError, StaffResponse]] = IO {
  Right(
    StaffResponse(
      id         = StaffId.generate(),
      name       = request.name,
      email      = request.email,
      department = request.department,
      role       = request.role
    )
  )
}

// サーバー実装
val serverEndpoint = createStaffEndpoint.serverLogic(createStaff)

// Swagger UI エンドポイント
val swaggerEndpoints = SwaggerInterpreter()
  .fromEndpoints[IO](List(createStaffEndpoint), "Staff API", "1.0.0")

val allRoutes = Http4sServerInterpreter[IO]().toRoutes(
  serverEndpoint :: swaggerEndpoints
)

val app = allRoutes.orNotFound

val server = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(app)
  .build

// メイン処理
object Main extends IOApp.Simple {
  def run: IO[Unit] = {
    IO.println("Starting server on http://localhost:8080") *>
      IO.println("Swagger UI: http://localhost:8080/docs") *>
      IO.println(
        "Try: curl -X POST http://localhost:8080/api/staff -H 'Content-Type: application/json' -d '{\"name\": \"山田太郎\", \"email\": \"yamada@example.com\", \"department\": \"Engineering\", \"role\": \"staff\"}'"
      ) *>
      server.useForever
  }
}

Main.main(Array.empty)
