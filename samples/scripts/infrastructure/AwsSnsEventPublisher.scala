package infrastructure

import java.net.URI

import cats.implicits.*

import cats.effect.{ IO, Resource }

import io.circe.*
import io.circe.syntax.*

import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.model.{ PublishRequest, PublishResponse }
import software.amazon.awssdk.services.sns.SnsClient

import domain.{ Event, EventPublisher, Logger }

class AwsSnsEventPublisher(
  snsClient: SnsClient,
  topicArn:  String,
  logger:    Logger[IO]
) extends EventPublisher[IO]:

  override def publish[E <: Event](event: E)(using encoder: Encoder[E]): IO[Unit] =
    for
      message <- IO.pure(event.asJson.noSpaces)
      publishRequest = PublishRequest
                         .builder()
                         .topicArn(topicArn)
                         .message(message)
                         .subject(s"Event: ${ event.name }")
                         .build()
      response <- IO.blocking(snsClient.publish(publishRequest))
      _        <- logger.info(s"Event published to SNS. MessageId: ${ response.messageId() }")
    yield ()

object AwsSnsEventPublisher:

  def resource(
    endpoint:  Option[URI] = None,
    region:    Region = Region.AP_NORTHEAST_1,
    topicArn:  String,
    accessKey: String = "test",
    secretKey: String = "test",
    logger:    Logger[IO]
  ): Resource[IO, AwsSnsEventPublisher] =
    Resource
      .fromAutoCloseable(IO {
        val clientBuilder = SnsClient
          .builder()
          .region(region)
          .credentialsProvider(
            StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey, secretKey)
            )
          )

        // LocalStack用のエンドポイント設定
        endpoint.foreach(clientBuilder.endpointOverride)

        clientBuilder.build()
      })
      .map(new AwsSnsEventPublisher(_, topicArn, logger))

  // LocalStack用の設定
  def localStackResource(
    topicArn:      String,
    localStackUrl: String = "http://localhost:4566",
    logger:        Logger[IO]
  ): Resource[IO, AwsSnsEventPublisher] =
    resource(
      endpoint  = Some(URI.create(localStackUrl)),
      region    = Region.AP_NORTHEAST_1,
      topicArn  = topicArn,
      accessKey = "test",
      secretKey = "test",
      logger    = logger
    )

  // 本番AWS用の設定
  def awsResource(
    topicArn:  String,
    region:    Region = Region.AP_NORTHEAST_1,
    accessKey: String,
    secretKey: String,
    logger:    Logger[IO]
  ): Resource[IO, AwsSnsEventPublisher] =
    resource(
      endpoint  = None,
      region    = region,
      topicArn  = topicArn,
      accessKey = accessKey,
      secretKey = secretKey,
      logger    = logger
    )
