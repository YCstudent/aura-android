# 优雅素问 - Android 客户端

> 智能健康管理助手 | Intelligent Health Management Assistant

基于 Kotlin + Jetpack Compose 开发的健康管理应用，提供个人健康档案管理、家庭成员关怀、AI 数字人健康咨询、用药提醒、就医预约等一站式健康服务。

*Kotlin + Jetpack Compose health management app. One-stop health services including personal health records, family care, AI health consultation, medication reminders, and appointment scheduling.*

## 功能特性 | Features

### 用户系统 | Account
- 手机号注册 / 登录 / 密码重置
- 邮箱 / 手机号绑定与换绑（短信验证码）
- 账号注销
- 隐私政策 & 用户协议

*Phone registration, login, password reset; email & phone binding; account deletion; privacy policy & user agreement.*

### 个人中心 | Profile
- 头像上传与裁剪（自定义裁剪器，双指缩放 + 拖拽，圆形裁剪）
- 基本信息编辑（用户名、个性签名、性别、出生日期）
- 健康档案（身高、体重、血型、疾病史、过敏史、手术史、用药史、慢性病、备注）
- 个人资料完善度提示

*Avatar upload & crop (custom cropper with pinch-zoom, drag, circular crop); basic info editing; health profile (height, weight, blood type, medical/allergy/surgery/medication/chronic history, notes); profile completion tips.*

### 首页 | Home
- 健康档案快捷卡片
- 家庭成员卡片展示（头像、姓名）
- 用药提醒预览
- 快捷功能入口（病历、用药、预约、医院地图）

*Health profile quick card; family member cards with avatars; medication reminder preview; shortcut grid (records, medications, appointments, hospital map).*

### 家庭管理 | Family
- 家庭群组创建与邀请
- 家庭成员添加 / 编辑 / 删除
- 邀请码加入
- 成员健康数据查看

*Family group creation & invitation; member add/edit/delete; invite code join; member health data viewing.*

### 数字人 AI | AI Consultation
- AI 健康咨询对话（流式 SSE 响应）
- 多轮对话，上下文关联
- 会话管理（新建、重命名、删除、搜索）
- 侧边栏对话历史
- 语音输入
- Markdown 富文本渲染
- 欢迎引导页

*AI health consultation chat (streaming SSE); multi-turn conversations with context; session management (create, rename, delete, search); sidebar history; voice input; Markdown rendering; welcome guide.*

### 病历管理 | Medical Records
- 个人及家庭成员病历列表
- 病历创建与编辑
- 图片拍照 / 相册上传
- OCR 识别（阿里云）

*Personal & family member medical records; record creation & editing; photo capture / gallery upload; OCR recognition.*

### 用药提醒 | Medication Reminders
- 个人及家庭成员用药管理
- 定时提醒通知（WorkManager 后台任务）
- 用药请求发送与确认

*Personal & family member medication management; scheduled reminder notifications (WorkManager); medication request sending & confirmation.*

### 预约管理 | Appointments
- 预约列表查看
- 新建预约
- 预约状态跟踪

*Appointment list viewing; new appointment creation; status tracking.*

### 医院地图 | Hospital Map
- 高德地图集成
- 附近医院搜索
- 医院详情展示

*Amap integration; nearby hospital search; hospital detail view.*

### 其他 | More
- 收件箱（系统通知）
- 日程管理
- 健康报告
- 设置

*Inbox (system notifications); schedule management; health reports; settings.*

## 截图 | Screenshots

> 待补充 | Coming soon

## 技术栈 | Tech Stack

| 类别 Category | 技术 Technology |
| --- | --- |
| 语言 Language | Kotlin |
| UI 框架 UI Framework | Jetpack Compose |
| 架构 Architecture | MVVM + Repository Pattern |
| 依赖注入 DI | Hilt |
| 网络请求 HTTP | Retrofit + OkHttp |
| 图片加载 Image | Coil |
| 本地存储 Storage | SharedPreferences + Room |
| 异步处理 Async | Kotlin Coroutines + Flow |
| 后台任务 Background | WorkManager |
| 地图 Map | 高德地图 Amap |

## 项目结构 | Project Structure

```
app/src/main/java/com/edistrive/aura/
├── data/                        数据层 Data Layer
│   ├── model/                   数据模型 Models
│   │   ├── Models.kt           用户、家庭、病历、用药等核心模型
│   │   └── ConversationModels.kt  数字人对话模型
│   ├── network/                 网络层 Network
│   │   ├── ApiService.kt       Retrofit API 接口
│   │   └── NetworkModule.kt    Hilt 网络模块配置
│   ├── local/                   本地存储 Local Storage
│   │   ├── AuthPreferences.kt  用户凭证存储
│   │   └── DisclaimerPreferences.kt  免责声明状态
│   └── notification/            通知 Notification
│       ├── AuraNotificationChannels.kt  通知渠道
│       ├── MedicationReminderScheduler.kt  用药提醒调度
│       └── MedicationReminderWorker.kt  后台提醒任务
├── ui/                          UI 层 UI Layer
│   ├── screens/                 页面 Screens
│   │   ├── auth/              登录注册 Auth
│   │   ├── family/            家庭管理 Family
│   │   ├── medical/           病历管理 Medical Records
│   │   ├── medication/        用药提醒 Medications
│   │   ├── digital/           数字人 AI Digital Human
│   │   └── health/            健康报告 Health Reports
│   ├── components/             通用组件 Shared Components
│   │   ├── StyledDatePicker.kt  日历风格日期选择器
│   │   ├── StyledDialog.kt      自定义对话框
│   │   └── IosTopBar.kt         自定义导航栏
│   ├── theme/                  主题 Theme
│   │   ├── Theme.kt            亮/暗色主题
│   │   └── AuraTokens.kt       品牌色 & 字体
│   └── navigation/             导航 Navigation
│       ├── Routes.kt           路由定义
│       ├── AuraNavHost.kt      导航图
│       └── MainTabsScreen.kt   底部 Tab 导航
├── ui/state/                   状态管理 ViewModels
│   ├── AuthViewModel.kt        认证
│   ├── ProfileViewModel.kt     个人中心
│   ├── HomeViewModel.kt        首页
│   ├── ConversationViewModel.kt 数字人对话
│   ├── FamilyViewModel.kt      家庭管理
│   ├── MedicalRecordsViewModel.kt  病历
│   ├── MedicationViewModel.kt  用药提醒
│   ├── ScheduleViewModel.kt    日程
│   └── ...                     其他 ViewModel
└── util/                        工具类 Utilities
    ├── DateFormat.kt            日期格式化
    ├── LocationProvider.kt      定位
    └── MapService.kt            地图服务
```


## 构建 | Build

```bash
cd android

# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# 安装到设备 Install to device
./gradlew installDebug
```

## 开发环境 | Dev Requirements

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34
- Kotlin 1.9.20+

## 品牌 | Brand

| 项 | 值 |
| --- | --- |
| 中文名 | 优雅素问 |
| 包名 | com.edistrive.aura |
| 主题色 | #1A8080 |
| 字体 | HarmonyOS Sans |

## 联系方式 | Contact

- 📧 2917321268@qq.com
- 📧 jhcx3303761@163.com
- 📧 jjhcx330jinheng@gmail.com

## 开发进度 | Progress

- [x] 项目结构搭建
- [x] 网络层配置
- [x] 数据模型定义
- [x] 登录 / 注册 / 密码重置
- [x] 主页 & 底部导航
- [x] 个人中心（含头像裁剪、健康档案）
- [x] 数字人 AI 对话（流式 SSE）
- [x] 家庭成员管理
- [x] 病历管理 + OCR
- [x] 用药提醒
- [x] 预约管理
- [x] 医院地图（高德）
- [x] 收件箱通知
- [x] 日程管理
- [x] 健康报告
- [ ] 消息推送（Push）
- [ ] 多语言支持

## 免责声明 | Disclaimer

本应用提供健康管理辅助功能，不替代专业医疗建议、诊断或治疗。如有健康问题，请咨询专业医疗机构。

*This app provides health management assistance and does not replace professional medical advice, diagnosis, or treatment. Please consult healthcare professionals for medical concerns.*
