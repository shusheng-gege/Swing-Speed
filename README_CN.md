# Swing Speed - OPPO Watch 3

专为 OPPO Watch 3（Wear OS）打造的羽毛球挥拍测速应用。利用手表内置加速度传感器检测挥拍动作，估算球拍速度，并记录训练数据。

![Platform](https://img.shields.io/badge/platform-Wear%20OS-4285F4)
![Language](https://img.shields.io/badge/language-Java-DD4B39)
![API](https://img.shields.io/badge/minSDK-28-3DDC84)
![License](https://img.shields.io/badge/license-MIT-green)

[English](README.md) | [日本語](README_JP.md)

## 功能特性

- **实时挥拍速度** — 大字体 km/h 显示，抬腕即看
- **训练统计** — 记录挥拍次数与平均速度
- **震动反馈** — 每次检测到挥拍即时震动确认
- **训练历史** — SQLite 本地存储，支持长按删除记录
- **方形屏优化** — 专为 OPPO Watch 3 的 372×430 AMOLED 屏幕设计
- **屏幕常亮** — 训练期间保持屏幕与 CPU 唤醒

## 截图

| 主界面 | 历史记录 |
|--------|----------|
| ![主界面](docs/screenshot_main.png) | ![历史](docs/screenshot_history.png) |

## 技术栈

- **语言**: Java
- **平台**: Android (Wear OS)，最低 API 28
- **UI**: Android Views + Material Components
- **传感器**: 加速度传感器 (Accelerometer)
- **存储**: SQLite (SQLiteOpenHelper)
- **构建**: Gradle + Android Gradle Plugin 8.1

## 工作原理

### 挥拍检测算法

检测器使用相位状态机（`IDLE` → `ACTIVE`）识别有效挥拍：

1. **加速度采样** — 加速度传感器以约 50Hz 频率报告 X/Y/Z 三轴数据
2. **重力补偿** — 减去 9.81 m/s² 获取净作用力
3. **低通滤波** — 5 样本移动平均平滑噪声
4. **挥拍开始** — 连续 3 个样本超过 30 m/s² 阈值时触发
5. **挥拍结束** — 作用力持续 60ms 低于阈值 25% 时确认（迟滞防多峰分裂）
6. **有效性验证** — 挥拍持续 150ms–3000ms 且至少 3 个样本
7. **速度估算** — `v ≈ a_peak × r / t × 3.6`（km/h），r = 0.5m（估算挥拍半径）

### 架构

```
MainActivity                — UI、传感器注册、屏幕唤醒
  VibratingCountDetector    — 相位状态机挥拍检测算法
  DatabaseHelper            — SQLite 训练记录 CRUD

HistoryActivity             — 历史记录列表，长按删除
  TrainingRecord            — 数据模型 (id, timestamp, count, avgSpeed, maxSpeed)
```

## 构建运行

### 环境要求
- Android Studio Hedgehog 或更新版本
- Android SDK API 28+
- OPPO Watch 3 通过 ADB 连接，或 Wear OS 模拟器

### Android Studio 运行
1. **File → Open** → 选择本项目目录
2. 等待 Gradle 同步完成
3. **Run → Run 'app'** → 选择手表或模拟器

### 命令行构建
```bash
./gradlew :app:assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

### ADB 安装
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/
  src/main/
    AndroidManifest.xml
    java/com/opposport/badminton/vibrationapp/
      MainActivity.java              # 主界面 + 传感器
      HistoryActivity.java           # 历史记录
      VibratingCountDetector.java    # 挥拍检测算法
      DatabaseHelper.java            # SQLite 辅助类
      TrainingRecord.java            # 数据模型
    res/
      drawable/                      # 图标矢量、卡片背景
      layout/
        activity_main.xml            # 主界面（方形屏优化）
        activity_history.xml         # 历史界面
        item_history.xml             # 历史行布局
      mipmap-anydpi-v26/             # 自适应图标 (API 26+)
      mipmap-*/                      # 栅格图标 fallback
      values/
        colors.xml                   # 语义化颜色 tokens
        dimens.xml                   # 间距与字号系统
        strings.xml                  # UI 字符串
        styles.xml                   # 主题与组件风格
  build.gradle                       # App module build config
  proguard-rules.pro
build.gradle                         # Root build file
gradle.properties
gradlew / gradlew.bat
gradle/wrapper/
local.properties
settings.gradle
```

## 许可证

MIT