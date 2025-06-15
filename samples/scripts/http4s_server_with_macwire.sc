//> using scala 3.7.1
//> using file "domain/Staff.scala"
//> using file "domain/StaffRepository.scala"
//> using file "domain/FromUUID.scala"
//> using file "application/CreateStaffService.scala"
//> using file "domain/EventPublisher.scala"
//> using file "domain/IdGenerator.scala"
//> using file "domain/TimeProvider.scala"
//> using file "domain/Logger.scala"
//> using file "infrastructure/StaffRepositoryImpl.scala"
//> using file "infrastructure/AwsSnsEventPublisher.scala"
//> using file "infrastructure/IdGeneratorImpl.scala"
//> using file "infrastructure/TimeProviderImpl.scala"
//> using file "infrastructure/ConsoleLogger.scala"
//> using file "presentation/StaffsAPI.scala"
//> using file "presentation/DomainCodec.scala"
//> using file "presentation/json/JsEmailAddress.scala"
//> using file "presentation/response/APIError.scala"
//> using file "presentation/response/StaffResponse.scala"
//> using file "presentation/request/CreateStaffRequest.scala"
//> using dep com.softwaremill.sttp.tapir::tapir-http4s-server:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.11.33
//> using dep org.http4s::http4s-ember-server:0.23.30
//> using dep org.http4s::http4s-dsl:0.23.30
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep io.github.iltotore::iron:3.0.1
//> using dep io.github.iltotore::iron-circe:3.0.1
//> using dep io.circe::circe-generic:0.14.14
//> using dep software.amazon.awssdk:sns:2.31.63
//> using dep software.amazon.awssdk:auth:2.31.63
//> using dep org.tpolecat::doobie-core:1.0.0-RC9
//> using dep org.tpolecat::doobie-hikari:1.0.0-RC9
//> using dep org.tpolecat::doobie-mysql:1.0.0-RC9
//> using dep mysql:mysql-connector-java:8.0.33
//> using dep com.softwaremill.macwire::macros:2.6.6

import java.net.URI

import cats.syntax.all.*

import cats.effect.*

import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger as ServerLogger

import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import doobie.*
import doobie.hikari.*

import com.zaxxer.hikari.HikariConfig

import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.model.{ CreateTopicRequest, CreateTopicResponse }
import software.amazon.awssdk.services.sns.SnsClient

import domain.{ EventPublisher, IdGenerator, Logger, Staff, StaffRepository, TimeProvider }

import application.*

import infrastructure.*

import presentation.StaffsAPI

import com.softwaremill.macwire.*

// アプリケーションの設定
case class AppConfig(
  serverHost:    Host,
  serverPort:    Port,
  mysqlUrl:      String,
  mysqlUser:     String,
  mysqlPassword: String,
  awsRegion:     Region,
  snsEndpoint:   Option[URI],
  snsTopicArn:   String
)

// アプリケーションコンポーネント
class AppComponents(config: AppConfig):
  // MySQL Transactor
  private val mysqlConfig = new HikariConfig()
  mysqlConfig.setDriverClassName("com.mysql.cj.jdbc.Driver")
  mysqlConfig.setJdbcUrl(config.mysqlUrl)
  mysqlConfig.setUsername(config.mysqlUser)
  mysqlConfig.setPassword(config.mysqlPassword)
  mysqlConfig.setMaximumPoolSize(Runtime.getRuntime.availableProcessors() * 2)

  val transactorResource: Resource[IO, HikariTransactor[IO]] =
    HikariTransactor.fromHikariConfig[IO](mysqlConfig, logHandler = None)

  // SNS Client
  val snsClient: SnsClient = {
    val builder = SnsClient
      .builder()
      .region(config.awsRegion)
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create("test", "test")
        )
      )

    config.snsEndpoint.foreach(builder.endpointOverride)
    builder.build()
  }

  def createServices(xa: HikariTransactor[IO]): AppServices[IO] = {
    // MacWire の Module として依存関係を定義
    val module = new ServiceModule(xa, snsClient, config.snsTopicArn)
    module.services
  }

// MacWire を使った依存関係の定義
class ServiceModule(xa: HikariTransactor[IO], snsClient: SnsClient, snsTopicArn: String):
  // インフラストラクチャ層の実装
  lazy val logger:          Logger[IO]          = infrastructure.Logger.console[IO]
  lazy val staffRepository: StaffRepository[IO] = wire[StaffRepositoryImpl]
  lazy val eventPublisher:  EventPublisher[IO]  = new AwsSnsEventPublisher(snsClient, snsTopicArn, logger)
  lazy val idGenerator:     IdGenerator[IO]     = infrastructure.IdGenerator.uuid[IO]
  lazy val timeProvider:    TimeProvider[IO]    = infrastructure.TimeProvider.system[IO]

  // アプリケーション層 - wire で依存関係を注入
  lazy val createStaffService: CreateStaffService[IO] = wire[CreateStaffServiceImpl[IO]]

  lazy val services: AppServices[IO] = wire[AppServices[IO]]

case class AppServices[F[_]](
  createStaffService: CreateStaffService[F]
)

// メインアプリケーション
object Main extends IOApp.Simple:

  def run: IO[Unit] =
    val config = AppConfig(
      serverHost = host"0.0.0.0",
      serverPort = port"8080",
      mysqlUrl = sys.env
        .getOrElse("MYSQL_URL", "jdbc:mysql://localhost:33306/staff_db?useSSL=false&allowPublicKeyRetrieval=true"),
      mysqlUser     = sys.env.getOrElse("MYSQL_USER", "root"),
      mysqlPassword = sys.env.getOrElse("MYSQL_PASSWORD", "password"),
      awsRegion     = Region.AP_NORTHEAST_1,
      snsEndpoint   = sys.env.get("SNS_ENDPOINT").map(URI.create).orElse(Some(URI.create("http://localhost:4566"))),
      snsTopicArn   = sys.env.getOrElse("SNS_TOPIC_ARN", "arn:aws:sns:ap-northeast-1:000000000000:staff-events")
    )

    val components = new AppComponents(config)

    components.transactorResource.use { xa =>
      // テーブル作成
      for
        _ <- StaffRepositoryImpl.createTable(xa)

        // LocalStackの場合、SNSトピックを作成
        _ <- if (config.snsEndpoint.isDefined) {
               IO.blocking {
                 try {
                   val createTopicRequest = CreateTopicRequest
                     .builder()
                     .name("staff-events")
                     .build()
                   components.snsClient.createTopic(createTopicRequest)
                   IO.println("Created SNS topic in LocalStack")
                 } catch {
                   case _: Exception => IO.println("SNS topic might already exist")
                 }
               }.flatten
             } else IO.unit

        // サービスの作成
        services = components.createServices(xa)

        // プレゼンテーション層
        staffsEndpoints = List(
                            StaffsAPI.createStaffEndpoint.serverLogic(
                              StaffsAPI.createStaffServerLogic(services.createStaffService)
                            )
                          )

        // Swagger UIエンドポイント
        swaggerEndpoints = SwaggerInterpreter()
                             .fromEndpoints[IO](
                               List(StaffsAPI.createStaffEndpoint),
                               "Staff Management API",
                               "1.0.0"
                             )

        // すべてのエンドポイント
        allEndpoints = staffsEndpoints ++ swaggerEndpoints

        // HTTPルート
        routes = Http4sServerInterpreter[IO]().toRoutes(allEndpoints)

        httpApp = ServerLogger.httpApp(logHeaders = true, logBody = false)(
                    routes.orNotFound
                  )

        server = EmberServerBuilder
                   .default[IO]
                   .withHost(config.serverHost)
                   .withPort(config.serverPort)
                   .withHttpApp(httpApp)
                   .build

        _ <- IO.println("Starting Staff Management API Server (with MacWire)...")
        _ <- IO.println(s"Server: http://${ config.serverHost }:${ config.serverPort }")
        _ <- IO.println(s"Swagger UI: http://${ config.serverHost }:${ config.serverPort }/docs")
        _ <- IO.println("")
        _ <- IO.println("Try creating a staff member:")
        _ <- IO.println("""curl -X POST http://localhost:8080/api/staffs \
  -H 'Content-Type: application/json' \
  -d '{
    "name": {
      "familyName": "山田",
      "givenName": "太郎"
    },
    "email": "yamada@example.com",
    "role": "member"
  }'""")
        _ <- server.use(_ => IO.never)
      yield ()
    }

Main.main(Array.empty)
