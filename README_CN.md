### 项目结构

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