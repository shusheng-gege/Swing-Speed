# Swing Speed - OPPO Watch 3

OPPO Watch 3（Wear OS）専用バドミントン振拍速度トラッカー。内蔵加速度センサーを使用して振拍を検出し、ラケットヘッド速度を推定し、トレーニングセッションを記録します。

![Platform](https://img.shields.io/badge/platform-Wear%20OS-4285F4)
![Language](https://img.shields.io/badge/language-Java-DD4B39)
![API](https://img.shields.io/badge/minSDK-28-3DDC84)
![License](https://img.shields.io/badge/license-MIT-green)

[English](README.md) | [中文](README_CN.md)

## 特徴

- **リアルタイム振拍速度** — 文字盤が見やすい大型km/h表示
- **セッション統計** — 振拍回数と平均速度を追跡
- **触覚フィードバック** — 振拍検出時にバイブレーションで通知
- **トレーニング履歴** — SQLiteに記録保存、長押しで削除可能
- **スクリーン最適化** — OPPO Watch 3 の 372×430 AMOLED 画面に最適化
- **常時点灯** — トレーニング中はCPUと画面を起動したまま維持

## スクリーンショット

| メイン画面 | 履歴 |
|------------|------|
| ![メイン](docs/screenshot_main.png) | ![履歴](docs/screenshot_history.png) |

## 技術スタック

- **言語**: Java
- **プラットフォーム**: Android (Wear OS)、minSdk 28
- **UI**: Android Views + Material Components
- **センサー**: 加速度センサー (Accelerometer)
- **ストレージ**: SQLite (SQLiteOpenHelper)
- **ビルド**: Gradle + Android Gradle Plugin 8.1

## 動作原理

### 振拍検出アルゴリズム

検出器はフェーズステートマシン（IDLE → ACTIVE）を使用して有効な振拍を識別します：

1. **加速度サンプリング** — 加速度センサーが約50HzでX/Y/Z三軸データを報告
2. **重力補正** — 9.81 m/s² を減算して純作用力を取得
3. **ローパスフィルタ** — 5サンプル移動平均でノイズを平滑化
4. **振拍開始** — 連続3サンプルが 30 m/s² 閾値を超過時にトリガー
5. **振拍終了** — 作用力が閾値の25%以下を60ms間持続で確定（ヒステリシスによるマルチピーク分裂防止）
6. **有効性検証** — 振拍継続時間 150ms–3000ms、最低3サンプル
7. **速度推定** —  ≈ a_peak × r / t × 3.6（km/h）、r = 0.5m（推定振拍半径）

### アーキテクチャ

`
MainActivity                — UI、センサー登録、画面維持
  VibratingCountDetector — フェーズステートマシン振拍検出アルゴリズム
  DatabaseHelper         — SQLite トレーニング記録 CRUD

HistoryActivity             — 履歴リスト、長押し削除
  TrainingRecord         — データモデル (id, timestamp, count, avgSpeed, maxSpeed)
`

## ビルドと実行

### 必要環境
- Android Studio Hedgehog 以降
- Android SDK API 28+
- ADB接続された OPPO Watch 3、または Wear OS エミュレータ

### Android Studio で実行
1. **File → Open** → プロジェクトディレクトリを選択
2. Gradle 同期を待つ
3. **Run → Run 'app'** → ウォッチまたはエミュレータを選択

### コマンドラインビルド
`ash
./gradlew :app:assembleDebug
# 出力: app/build/outputs/apk/debug/app-debug.apk
`

### ADB インストール
`ash
adb install app/build/outputs/apk/debug/app-debug.apk
`


### プロジェクト構成

```
app/
  src/main/
    AndroidManifest.xml
    java/com/opposport/badminton/vibrationapp/
      MainActivity.java              # メイン画面 + センサー
      HistoryActivity.java           # 履歴画面
      VibratingCountDetector.java    # 振拍検出アルゴリズム
      DatabaseHelper.java            # SQLite ヘルパー
      TrainingRecord.java            # データモデル
    res/
      drawable/                      # アイコン、カード背景
      layout/
        activity_main.xml            # メイン画面（スクリーン最適化）
        activity_history.xml         # 履歴画面
        item_history.xml             # 履歴行レイアウト
      mipmap-anydpi-v26/             # アダプティブアイコン (API 26+)
      mipmap-*/                      # ラスターアイコン fallback
      values/
        colors.xml                   # セマンティックカラートークン
        dimens.xml                   # 間隔とタイポグラフィシステム
        strings.xml                  # UI 文字列
        styles.xml                   # テーマとコンポーネントスタイル
  build.gradle                       # App module build config
  proguard-rules.pro
build.gradle                         # Root build file
gradle.properties
gradlew / gradlew.bat
gradle/wrapper/
local.properties
settings.gradle
```
## ライセンス

MIT