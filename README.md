# Listo

自由に作れるリストでタスクや買い物をメモ・管理する Android アプリ。仕様は [SPEC.md](SPEC.md) を参照。

- パッケージ名：`io.github.rupsilonomicron.listo`（配布後は変更不可）
- 対応：Android 8.0（API 26）以上 / targetSdk 35
- 構成：Kotlin + Jetpack Compose（Material 3）+ Room + Jetpack Glance

## ビルド

ローカルに Android 開発環境は不要。`main` への push で GitHub Actions がビルドする。

- **debug apk**：Actions の実行結果ページ → Artifacts の `listo-debug`
- **release apk**（署名済み）：Secrets 登録後、Artifacts の `listo-release`
- **GitHub Release**：`v0.1.0` のようなタグを push すると、release apk を添付したリリースが作られる

## 署名用 Secrets

リポジトリの Settings → Secrets and variables → Actions に、本人が次の4つを登録する。

| 名前 | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | keystore ファイルを Base64 にした文字列 |
| `KEYSTORE_PASSWORD` | keystore のパスワード |
| `KEY_ALIAS` | 鍵のエイリアス |
| `KEY_PASSWORD` | 鍵のパスワード |

keystore はリポジトリにコミットしない。**紛失すると配布済みアプリを更新できなくなる**ので、安全な場所にバックアップを保管すること。

## インストール

apk を端末で開いてインストールする。初回は端末側で「提供元不明のアプリ」のインストール許可が必要。
