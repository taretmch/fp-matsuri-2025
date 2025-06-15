# CLAUDE.md

このファイルは、このリポジトリでコードを扱う際のClaude Code (claude.ai/code) へのガイダンスを提供します。

## 重要な指示

- **言語**: このプロジェクトでは常に日本語で応答してください
- **イベント名**: 正式名称は「関数型まつり」です。「FP祭り」「関数型プログラミング祭り」などの表記は使用しないでください
- **コミット**: 変更を行うたびにgitコミットを実行してください。
- **プレゼンテーションスタイル**: 真面目で専門的なトーンのスライドを作成してください。カジュアルすぎる表現は避け、技術的な正確性を重視してください

## プロジェクト概要

これは関数型まつり 2025のためのSlidevベースのプレゼンテーションプロジェクトです。プレゼンテーションでは、Scalaの関数型プログラミングライブラリを使用した型安全なビジネスアプリケーション開発について議論します。

## 発表内容

関数型まつり 2025での発表では、以下のトピックを扱います：

### メインテーマ：型安全なビジネスアプリケーション開発

1. **ドメイン型とバリデーション**
   - Iron型ライブラリを使用した制約付き型の実装
   - OpenAPIスキーマとの統合による型安全なAPI設計
   - ビジネスルールの型レベルでの表現

2. **実践的なScalaアプリケーション開発**
   - Tapirを使用したRESTful API開発
   - http4sとCirceによるHTTPサーバー実装
   - 型安全性を保ちながらの実装パターン

3. **型安全性とビジネス価値**
   - コンパイル時エラー検出によるバグの削減
   - ドメインモデルの表現力向上
   - 保守性と拡張性の向上

### デモアプリケーション

職員管理システムのAPIを例に、以下の機能を実装：
- 職員作成API（POST /api/staffs）
- ドメイン型によるバリデーション（名前、メールアドレス、給与など）
- OpenAPIスキーマの自動生成

## アーキテクチャ設計方針

### レイヤー構成と責務

#### 1. ドメイン層（domain/）
- **責務**: ビジネスロジックとドメインモデルの定義
- **特徴**: 
  - 純粋な関数とデータ型のみで構成
  - 副作用（IO）を含まない
  - 外部ライブラリへの依存を最小限に
- **例**: Staff、EmailAddress、Username などのエンティティと値オブジェクト

#### 2. アプリケーション層（application/）
- **責務**: ユースケースの実装とビジネスロジックの調整
- **特徴**:
  - ドメイン層のオブジェクトを組み合わせてユースケースを実現
  - 副作用（IO）を含まない
  - インターフェースを通じてインフラ層に依存
- **例**: CreateStaffService、EventPublisher、IdGenerator のインターフェース

#### 3. インフラストラクチャ層（infrastructure/）
- **責務**: 外部システムとの連携、副作用の実装
- **特徴**:
  - IO[A] などのエフェクト型を使用
  - データベース、外部API、ファイルシステムなどへのアクセス
  - アプリケーション層のインターフェースを実装
- **例**: StaffRepositoryImpl、AwsSnsEventPublisher

#### 4. プレゼンテーション層（presentation/）
- **責務**: HTTPエンドポイントの定義とリクエスト/レスポンスの変換
- **特徴**:
  - Tapirを使用したエンドポイント定義
  - DTO（Data Transfer Object）の定義
  - ドメインオブジェクトとDTOの相互変換
  - エフェクトシステムの選択と注入
- **例**: StaffsAPI、CreateStaffRequest、StaffResponse

### 副作用の扱い方

1. **ドメイン層とアプリケーション層では副作用を避ける**
   - ランダム値生成、現在時刻取得などは引数として受け取る
   - 副作用が必要な場合は、インターフェースを定義し、実装はインフラ層で行う

2. **インフラ層で副作用をラップ**
   - Cats Effect の IO[A] を使用して副作用を明示的に扱う
   - リソース管理には Resource[IO, A] を使用

3. **プレゼンテーション層で依存性を注入**
   - アプリケーションの起動時に全ての依存関係を組み立てる
   - 手動での依存性注入、または将来的にはMacWire、ZIOのZLayerなどを検討

### サンプルコードのディレクトリ構造

```
samples/scripts/
├── domain/              # ドメイン層
│   ├── Staff.scala      # エンティティ、値オブジェクト
│   ├── StaffRepository.scala  # リポジトリインターフェース
│   └── FromUUID.scala   # ユーティリティ型クラス
├── application/         # アプリケーション層
│   ├── CreateStaffService.scala  # ユースケース実装
│   ├── EventPublisher.scala      # イベント発行インターフェース
│   └── IdGenerator.scala         # ID生成インターフェース
├── infrastructure/      # インフラストラクチャ層
│   ├── StaffRepositoryImpl.scala  # DBアクセス実装
│   └── AwsSnsEventPublisher.scala # AWS SNS実装
└── presentation/        # プレゼンテーション層
    ├── StaffsAPI.scala  # APIエンドポイント定義
    ├── DomainCodec.scala  # JSON変換定義
    ├── request/         # リクエストDTO
    ├── response/        # レスポンスDTO
    └── json/           # カスタムJSON型定義
```

## よく使う開発コマンド

### 開発
- `npm run dev` - ホットリロード付きの開発サーバーを起動（ブラウザが自動的に開きます）
- `npm run build` - プレゼンテーションを本番用にビルド
- `npm run export` - プレゼンテーションをPDFまたは他の形式にエクスポート

### パッケージ管理
- `npm install` - 依存関係をインストール（pnpmを使用している場合は `pnpm install`）
- `bun install` - Bunを使用して依存関係をインストール（ロックファイルが存在）

### scala-cli 用プロジェクト設定
- `samples/scripts/project.scala` - すべての.scalaファイルの依存関係を一元管理
- 各.scファイルには個別に`using`ディレクティブが必要

### デモ実行
- `scala-cli run samples/scripts/domain_model_no_validation.sc` - 職員ドメインモデル（バリデーションなし）のデモ
- `scala-cli run samples/scripts/domain_model_refined.sc` - Refinement Typesを使用したバリデーションのデモ
- `scala-cli run samples/scripts/refinement_types_error.sc` - コンパイル時型安全性のデモ
- `scala-cli run samples/scripts/domain_model_smart_constructor.sc` - スマートコンストラクタパターンのデモ
- `scala-cli run samples/scripts/domain_model.sc` - 完全なドメインモデルのデモ
- `scala-cli run samples/scripts/usecase_from_scratch.sc` - CreateStaffServiceのユースケースデモ
- `scala-cli run samples/scripts/infra_aws_demo.sc` - LocalStackを使用したAWS SNS連携デモ
- `scala-cli run samples/scripts/infra_mysql_demo.sc` - MySQLリポジトリ実装のデモ
- `scala-cli run samples/scripts/infra_mysql_name.sc` - DoobieのMeta型クラスのデモ
- `scala-cli run samples/scripts/minimum-api-validation-demo.sc` - 最小限のAPI実装デモ（バリデーション付き）
- `scala-cli run samples/scripts/http4s_server.sc` - 完全なHTTPサーバー実装（手動DI）
- `scala-cli run samples/scripts/http4s_server_with_macwire.sc` - MacWireを使用したHTTPサーバー実装
- `scala-cli fmt samples/scripts/*.sc` - Scalaコードをフォーマット（.scalafmt.confを使用）

## アーキテクチャと構造

### コアファイル
- `slides.md` - Slidev固有のフロントマターと構文を使用したメインプレゼンテーションコンテンツ
- `package.json` - プロジェクト設定と依存関係
- `netlify.toml` & `vercel.json` - 各プラットフォーム用のデプロイ設定

### 主要な依存関係
- **@slidev/cli** - Slidevのコアコマンドラインツール
- **@slidev/theme-default** & **@slidev/theme-seriph** - デフォルトテーマ
- **slidev-theme-academic** - アカデミックプレゼンテーションテーマ（現在使用中）
- **vue** - インタラクティブコンポーネント用の基盤フレームワーク

### プレゼンテーションの構造
メインプレゼンテーション（`slides.md`）の設定:
- テーマ: ダークカラースキームの `academic`
- トランジション: `slide-left`
- MDC（マークダウンコンポーネント）有効
- 様々なレイアウト: intro、default、section、two-colsなど

### カスタムコンポーネント
- `components/Counter.vue` - スライドで使用できるVueコンポーネントの例
- `snippets/external.ts` - コード例用の外部TypeScriptスニペット

## Slidev固有の構文

`slides.md`を編集する際:
- `---` を使用してスライドを区切る
- 上部のフロントマターでプレゼンテーションを設定
- スライドごとにレイアウトを指定可能（例: `layout: intro`）
- Vueコンポーネントを直接埋め込み可能
- コードブロックは構文ハイライトをサポート
- `<v-clicks>` で段階的な表示
- スライドごとの `<style>` ブロックでスタイリング

## デザインシステム

プレゼンテーション全体で一貫したデザインを維持するための指針：

### カラーパレット
- **プライマリカラー**: `#1e293b`（濃い青灰色）- タイトルスライドとセクションスライドの背景
- **セカンダリカラー**: `#ffffff`（白）- 通常のコンテンツスライドの背景
- **テキストカラー**: 
  - 濃い背景上: `white`
  - 白背景上: デフォルト（黒系）

### スライドレイアウトの標準化

#### 1. タイトルスライド
- レイアウト: `layout: cover`
- 背景: `#1e293b`
- テキスト: 白
- フォントサイズ: タイトル`text-5xl`、サブタイトル`text-2xl`

#### 2. セクションスライド（Part 1-5）
- レイアウト: `layout: center` + `class: text-center`
- 背景: `#1e293b`
- テキスト: 白（`h1, h2, p, ul, li`要素すべて）
- 用途: 各パートの開始を示すスライド

#### 3. 自己紹介スライド
- レイアウト: `layout: intro`
- 背景: 白（デフォルト）
- テキスト: デフォルト色

#### 4. コンテンツスライド
- レイアウト: `layout: default`または指定なし
- 背景: 白（デフォルト）
- テキスト: デフォルト色
- コードブロック: シンタックスハイライト付き

#### 5. 特殊レイアウト
- `layout: two-cols`: 2カラム表示
- `layout: image-right`: 右側に画像配置
- 各レイアウトはデフォルトの白背景を使用

### スタイル記述のガイドライン

1. **セクションスライドの統一スタイル**:
```css
.slidev-layout {
  background: #1e293b;
}
h1, h2, p, ul, li {
  color: white;
}
```

2. **フォントサイズの調整**:
- 必要に応じて`font-size`プロパティで個別調整
- タイトルスライドなど特殊なケースでは、Tailwind CSSクラスを使用

3. **余白とスペーシング**:
- デフォルトのレイアウトマージンを尊重
- 特別な調整が必要な場合のみカスタムスタイルを適用

### 注意事項
- 新しいスライドを追加する際は、既存のデザインパターンに従う
- 特殊なスタイリングが必要な場合は、`<style>`ブロックで個別に定義
- アクセシビリティを考慮し、十分なコントラストを維持
