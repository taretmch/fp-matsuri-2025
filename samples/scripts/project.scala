//> using scala 3.7.1

// 共通の依存関係
//> using dep org.typelevel::cats-core:2.13.0
//> using dep org.typelevel::cats-effect:3.6.1
//> using dep io.github.iltotore::iron:3.0.1
//> using dep io.github.iltotore::iron-circe:3.0.1
//> using dep io.circe::circe-core:0.14.14
//> using dep io.circe::circe-generic:0.14.14
//> using dep io.circe::circe-parser:0.14.14

// Tapir & HTTP4s
//> using dep com.softwaremill.sttp.tapir::tapir-core:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-iron:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-http4s-server:1.11.33
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.11.33
//> using dep org.http4s::http4s-ember-server:0.23.30
//> using dep org.http4s::http4s-dsl:0.23.30

// Database
//> using dep org.tpolecat::doobie-core:1.0.0-RC9
//> using dep org.tpolecat::doobie-hikari:1.0.0-RC9
//> using dep org.tpolecat::doobie-mysql:1.0.0-RC9
//> using dep mysql:mysql-connector-java:8.0.33

// AWS
//> using dep software.amazon.awssdk:sns:2.31.63
//> using dep software.amazon.awssdk:auth:2.31.63

// DI
//> using dep com.softwaremill.macwire::macros:2.6.6

// ドメイン層のファイル
//> using file domain/FromUUID.scala
//> using file domain/Staff.scala
//> using file domain/StaffRepository.scala
//> using file domain/EventPublisher.scala
//> using file domain/IdGenerator.scala
//> using file domain/TimeProvider.scala
//> using file domain/Logger.scala

// アプリケーション層のファイル
//> using file application/CreateStaffService.scala

// インフラストラクチャ層のファイル
//> using file infrastructure/StaffRepositoryImpl.scala
//> using file infrastructure/AwsSnsEventPublisher.scala
//> using file infrastructure/IdGeneratorImpl.scala
//> using file infrastructure/TimeProviderImpl.scala
//> using file infrastructure/ConsoleLogger.scala
//> using file infrastructure/RandomUsernameService.scala

// プレゼンテーション層のファイル
//> using file presentation/StaffsAPI.scala
//> using file presentation/DomainCodec.scala
//> using file presentation/json/JsEmailAddress.scala
//> using file presentation/response/APIError.scala
//> using file presentation/response/StaffResponse.scala
//> using file presentation/request/CreateStaffRequest.scala

// テスト用フィクスチャ
//> using file test/fixtures/InMemoryStaffRepository.scala
//> using file test/fixtures/InMemoryEventPublisher.scala
