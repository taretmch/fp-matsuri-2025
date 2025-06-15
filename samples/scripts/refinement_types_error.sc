//> using scala 3.7.1
//> using dep io.github.iltotore::iron:3.0.1

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

// 年齢：正の整数
type Age = Int :| Positive

// メールアドレス：正規表現でバリデーション
type Email = String :| Match["^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"]

// 正常なケース
val validAge:   Age   = 25
val validEmail: Email = "user@example.com"

// コンパイル時エラーになるケース（コメントアウトを外すとエラー）

// 負の年齢はコンパイルエラー
// val invalidAge: Age = -5
// error: Could not satisfy a constraint for type scala.Int.
// Value: -5
// Message: Should be strictly positive

// 不正なメールアドレス形式はコンパイルエラー
// val invalidEmail: Email = "not-an-email"
// error: Could not satisfy a constraint for type java.lang.String.
// Value: not-an-email
// Message: Should match ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$

println("=== コンパイル時の型安全性 ===")
println(s"有効な年齢: $validAge")
println(s"有効なメール: $validEmail")
println("\nコメントアウトを外すとコンパイルエラーになります")
