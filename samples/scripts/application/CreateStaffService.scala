package application

import cats.{ MonadError, MonadThrow }
import cats.data.EitherT
import cats.syntax.all.*

import cats.effect.*

import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*

import domain.{ Event, EventPublisher, FromUUID, IdGenerator, Staff, StaffRepository, TimeProvider }

case class StaffCreatedEvent(
  staffId:   Staff.Id,
  staffName: Staff.Name,
  email:     Staff.EmailAddress,
  role:      Staff.Role,
  timestamp: java.time.Instant
) extends Event:
  override def name: String = "StaffCreated"

object StaffCreatedEvent:
  import io.circe.generic.semiauto.*

  // Staff関連の型のためのCodecを定義
  given Encoder[Staff.Id]           = Encoder[String].contramap(_.toString)
  given Encoder[Staff.FamilyName]   = Encoder[String].contramap(_.toString)
  given Encoder[Staff.GivenName]    = Encoder[String].contramap(_.toString)
  given Encoder[Staff.Name]         = deriveEncoder[Staff.Name]
  given Encoder[Staff.EmailAddress] = Encoder[String].contramap(_.toString)
  given Encoder[Staff.Role]         = Encoder[String].contramap(_.toString)

  given Encoder[StaffCreatedEvent] = deriveEncoder[StaffCreatedEvent]

case class CreateStaffCommand(
  name:  Staff.Name,
  email: Staff.EmailAddress,
  role:  Staff.Role
)

enum CreateStaffError:
  case EmailAlreadyExists(email: Staff.EmailAddress)
  case Unexpected(ex: Throwable)

/** 職員作成サービスの実装
  *
  * 1. メールアドレスの重複チェック
  * 2. 職員エンティティの作成
  * 3. 職員エンティティの永続化
  * 4. サービス連携 (作成イベント発火)
  */
trait CreateStaffService[F[_]]:
  def create(command: CreateStaffCommand): F[Either[CreateStaffError, StaffCreatedEvent]]

class CreateStaffServiceImpl[F[_]: Sync](
  staffRepository: StaffRepository[F],
  eventPublisher:  EventPublisher[F],
  idGenerator:     IdGenerator[F],
  timeProvider:    TimeProvider[F]
) extends CreateStaffService[F]:

  def create(command: CreateStaffCommand): F[Either[CreateStaffError, StaffCreatedEvent]] =
    val process = for
      // 1. メールアドレスの重複チェック
      existingStaff <- EitherT.liftF(staffRepository.findByEmail(command.email))
      _ <- EitherT.cond[F](
             existingStaff.isEmpty,
             (),
             CreateStaffError.EmailAlreadyExists(command.email)
           )

      // 2. 職員エンティティの作成
      staffId <- EitherT.liftF(idGenerator.generate[Staff.Id])
      newStaff = Staff.create(
                   id       = staffId,
                   name     = command.name,
                   email    = command.email,
                   role     = command.role,
                   username = None
                 )

      // 3. 職員エンティティの永続化
      savedStaff <- EitherT.liftF(staffRepository.save(newStaff))

      // 4. イベントの作成と発行
      now <- EitherT.liftF(timeProvider.now)
      event = StaffCreatedEvent(
                staffId   = savedStaff.id,
                staffName = savedStaff.name,
                email     = savedStaff.email,
                role      = savedStaff.role,
                timestamp = now
              )
      _ <- EitherT.liftF(eventPublisher.publish(event))
    yield event

    process.value.handleError(throwable => CreateStaffError.Unexpected(throwable).asLeft)
