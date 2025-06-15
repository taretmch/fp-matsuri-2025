//> using scala 3.7.1
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep software.amazon.awssdk:sns:2.31.63
//> using dep software.amazon.awssdk:auth:2.31.63
//> using dep io.github.iltotore::iron:3.0.1
//> using dep io.github.iltotore::iron-circe:3.0.1
//> using dep io.circe::circe-core:0.14.14
//> using dep io.circe::circe-generic:0.14.14
//> using dep io.circe::circe-parser:0.14.14

//> using file "domain/Staff.scala"
//> using file "domain/StaffRepository.scala"
//> using file "domain/FromUUID.scala"
//> using file "domain/EventPublisher.scala"
//> using file "domain/Logger.scala"
//> using file "infrastructure/AwsSnsEventPublisher.scala"
//> using file "infrastructure/ConsoleLogger.scala"

import java.net.URI

import cats.implicits.*

import cats.effect.*

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*

import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.model.*
import software.amazon.awssdk.services.sns.SnsClient

import domain.*
import domain.Staff.*

import infrastructure.*

// デモ用のイベント定義
case class StaffCreatedEvent(
  staffId:   String,
  staffName: String,
  email:     String,
  role:      String,
  timestamp: java.time.Instant = java.time.Instant.now()
) extends Event:
  override def name: String = "StaffCreated"

object StaffCreatedEvent:
  given Encoder[StaffCreatedEvent] = deriveEncoder[StaffCreatedEvent]

// 手動でEventのEncoderを定義
object EventEncoders:
  given Encoder[Event] = Encoder.instance {
    case e: StaffCreatedEvent => e.asJson
    case e                    => Json.obj("type" -> Json.fromString(e.getClass.getSimpleName))
  }

object LocalStackDemo extends IOApp.Simple:

  val localStackUrl = "http://localhost:4566"
  val topicName     = "staff-events"
  val region        = Region.AP_NORTHEAST_1

  // LocalStack用のSNSクライアント作成
  def createSnsClient(): Resource[IO, SnsClient] =
    Resource.fromAutoCloseable(IO {
      SnsClient
        .builder()
        .endpointOverride(URI.create(localStackUrl))
        .region(region)
        .credentialsProvider(
          StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")
          )
        )
        .build()
    })

  // SNSトピックの作成
  def createTopic(snsClient: SnsClient, topicName: String): IO[String] = IO.blocking {
    val createTopicRequest = CreateTopicRequest
      .builder()
      .name(topicName)
      .build()

    val response = snsClient.createTopic(createTopicRequest)
    response.topicArn()
  }

  def run: IO[Unit] =
    createSnsClient().use { snsClient =>
      for
        _ <- IO.println("=== LocalStack (AWS SNS) 連携デモ ===")

        // トピック作成
        _        <- IO.println("1. SNSトピックを作成します...")
        topicArn <- createTopic(snsClient, topicName)
        _        <- IO.println(s"✅ トピック作成成功: $topicArn")

        // EventPublisherの作成とイベント発行
        logger = infrastructure.Logger.console[IO]
        _ <- AwsSnsEventPublisher
               .localStackResource(
                 topicArn      = topicArn,
                 localStackUrl = localStackUrl,
                 logger        = logger
               )
               .use { publisher =>
                 import EventEncoders.given

                 for
                   _ <- IO.println("\n2. イベントを発行します...")

                   // イベント1
                   event1 = StaffCreatedEvent(
                              staffId   = "123e4567-e89b-12d3-a456-426614174000",
                              staffName = "山田太郎",
                              email     = "yamada.taro@example.com",
                              role      = "Manager"
                            )
                   _ <- publisher.publish(event1)
                   _ <- IO.println(s"✅ イベント発行成功: $event1")

                   // イベント2
                   event2 = StaffCreatedEvent(
                              staffId   = "987e6543-e21b-12d3-a456-426614174000",
                              staffName = "佐藤花子",
                              email     = "sato.hanako@example.com",
                              role      = "Member"
                            )
                   _ <- publisher.publish(event2)
                   _ <- IO.println(s"✅ イベント発行成功: $event2")

                   // イベント3
                   event3 = StaffCreatedEvent(
                              staffId   = "456e7890-e21b-12d3-a456-426614174000",
                              staffName = "鈴木一郎",
                              email     = "suzuki.ichiro@example.com",
                              role      = "Member"
                            )
                   _ <- publisher.publish(event3)
                   _ <- IO.println(s"✅ イベント発行成功: $event3")

                   _ <- IO.println("\n3. SNSトピックの属性を確認...")
                   _ <- IO.blocking {
                          val getTopicAttributesRequest = GetTopicAttributesRequest
                            .builder()
                            .topicArn(topicArn)
                            .build()

                          val response   = snsClient.getTopicAttributes(getTopicAttributesRequest)
                          val attributes = response.attributes()

                          IO.println(s"   - DisplayName: ${ attributes.get("DisplayName") }")
                          IO.println(s"   - SubscriptionsConfirmed: ${ attributes.get("SubscriptionsConfirmed") }")
                          IO.println(s"   - SubscriptionsPending: ${ attributes.get("SubscriptionsPending") }")
                        }.flatten
                 yield ()
               }

        _ <- IO.println("\n4. デモ完了")
        _ <- IO.println("注: LocalStackのSNSは実際にはメッセージを配信しません")
        _ <- IO.println("    本番環境では実際のAWS SNSを使用してください")
      yield ()
    }

end LocalStackDemo

// 実行方法:
// 1. cd samples && docker compose up -d localstack
// 2. scala-cli run scripts/infra_aws_demo.sc
