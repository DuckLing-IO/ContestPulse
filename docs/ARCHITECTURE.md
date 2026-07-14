# Contest Pulse 架构设计

## 1. 范围与约束

Contest Pulse 是无业务后端的本地优先 Android 应用。客户端从公开 API 或公开网页读取比赛信息，个人收藏、提醒和设置只保存在本机。应用不模拟平台登录、不持有 Cookie、不绕过验证码或反爬措施。

当前实现已覆盖可运行 MVP：Codeforces 官方 API、AtCoder/洛谷/牛客公开页面解析、可预览的自定义 HTTPS 数据源、Room 离线仓库、本地收藏/提醒、DataStore 设置与 WorkManager 周期同步。发布前仍需按 `RELEASE_CHECKLIST.md` 完成真机矩阵、目标 SDK 升级评估与正式签名。

## 2. 架构决策

### 2.1 先单模块、后按证据拆分

初始工程使用一个 `:app` 模块，但代码按以下边界组织：

```text
presentation -> domain <- data
                      <- reminder
                      <- sync
```

- `presentation` 只依赖领域模型和用例，不直接访问 Retrofit、Room、DataStore 或 AlarmManager。
- `domain` 是纯 Kotlin 核心，不依赖 Android UI、网络或数据库实现。
- `data` 实现领域仓库，协调远程数据源、本地缓存、映射、合并与去重。
- `reminder` 实现提醒计划、通知和重调度，不承担比赛同步。
- `sync` 只负责触发同步用例、约束、重试和平台级结果汇总。

单模块可以更快验证模型和 UI 边界，也避免阶段 1 产生大量只有 Gradle 配置的空模块。满足任一条件后再拆模块：

1. 两个以上功能需要独立并行开发且频繁产生包级冲突。
2. 增量构建时间明显受单模块影响。
3. 某个边界已经拥有稳定公开 API 和独立测试集。
4. 需要通过 Gradle 可见性强制阻止反向依赖。

### 2.2 未来多模块映射

```text
:app
:core:common
:core:model
:core:designsystem
:core:network
:core:database
:core:notification
:core:testing
:domain
:data:codeforces
:data:atcoder
:data:luogu
:data:nowcoder
:data:customsource
:feature:contestlist
:feature:contestdetail
:feature:favorites
:feature:settings
```

拆分时包名保持不变，只移动源码和收紧依赖；不在同一次变更里重写业务逻辑。

## 3. 单模块目标目录

```text
io.duckling.contestpulse/
├── app/
│   └── AppStartup.kt
├── core/
│   ├── common/              # Clock、Dispatcher、Result、URL 校验
│   ├── designsystem/        # 主题令牌、组件、动效、预览
│   ├── model/               # 可跨层共享的稳定值对象
│   ├── network/             # OkHttp/Retrofit 配置与安全日志策略
│   └── database/            # Room database、DAO、converter
├── domain/
│   ├── model/               # Contest、Reminder、SyncResult
│   ├── repository/          # 仓库接口
│   └── usecase/             # 查询、同步、收藏、筛选、提醒用例
├── data/
│   ├── local/               # entity、DAO adapter、DataStore
│   ├── remote/              # ContestDataSource 抽象
│   ├── remote/codeforces/   # API、DTO、mapper、data source
│   ├── remote/atcoder/      # 页面 fetcher、parser、mapper
│   ├── remote/luogu/        # 洛谷公开比赛列表适配器
│   ├── remote/nowcoder/     # 牛客系列赛与高校校赛适配器
│   ├── remote/custom/       # 安全抓取、自动/映射解析与预览
│   ├── customsource/        # 自定义来源 DataStore
│   └── repository/          # 聚合仓库实现
├── reminder/
│   ├── notification/        # channel、通知构建、deep link
│   ├── scheduler/           # exact/inexact scheduler
│   └── receiver/            # alarm、boot、package replaced
├── sync/
│   ├── worker/              # WorkManager worker
│   └── scheduler/           # periodic/one-shot policy
├── feature/
│   ├── contestlist/
│   ├── contestdetail/
│   ├── favorites/
│   ├── customsource/
│   └── settings/
└── navigation/
```

DTO、Room Entity、领域模型和 UI Model 必须是不同类型。映射方向明确，禁止让远程字段变化直接穿透到数据库和 UI。

## 4. 核心模型

### 4.1 领域模型

| 类型 | 关键字段 | 说明 |
| --- | --- | --- |
| `Contest` | `id`, `source`, `sourceContestId`, `title`, `startTime`, `endTime`, `duration`, `registrationUrl`, `contestUrl`, `status`, `category`, `difficultyLabel`, `ratedRange`, `isRated`, `isFavorite`, `reminderOffsets`, `lastUpdatedAt` | 时间统一为 UTC `Instant`；展示时才转换系统时区 |
| `ContestSource` | `CODEFORCES`, `ATCODER`, `LUOGU`, `NOWCODER`, `OTHER` | 数据源标识，也是隔离、设置和同步状态的维度 |
| `ContestStatus` | `UPCOMING`, `RUNNING`, `FINISHED`, `UNKNOWN` | 由当前时间、开始时间、结束时间统一推导 |
| `ContestFilter` | `sources`, `dateRange`, `favoriteOnly`, `ratedOnly` | 比赛列表持有的筛选意图，不下沉为远程参数假设 |
| `ContestGroup` | `RUNNING`, `TODAY`, `TOMORROW`, `THIS_WEEK`, `LATER` | 使用用户时区和注入的 `Clock` 计算 |
| `Favorite` | `contestId`, `createdAt`, `note` | 独立于比赛实体，避免远程 upsert 覆盖用户状态 |
| `Reminder` | `id`, `contestId`, `triggerAt`, `offset`, `enabled`, `schedulerType`, `systemRequestCode`, `createdAt` | `(contestId, offset)` 唯一，过期触发时间不入库 |
| `ReminderSchedulerType` | `EXACT_ALARM`, `INEXACT_ALARM`, `WORK_MANAGER` | 记录实际采用的调度方式，便于 UI 解释精度 |
| `SourceSyncResult` | `source`, `sourceKey`, `fetchedCount`, `issue` | 内置平台和每个自定义来源独立成功或失败 |
| `SyncError` | `type`, `userMessage`, `retryable` | 不向 UI 暴露堆栈、响应体或敏感 URL |
| `SyncReport` | `results`, `insertedCount`, `updatedCount`, `finishedAt` | 聚合同步报告；允许部分成功 |

统一比赛 ID 优先使用 `"${source}:${sourceContestId}"`。自定义来源以持久化配置 UUID 作为命名空间，比赛记录再使用远端 ID 或 `title + startTime` 稳定哈希；不得在每次同步时生成新的随机 ID。

### 4.2 Room 表

| 表 | 主键/约束 | 设计重点 |
| --- | --- | --- |
| `contests` | `id` 主键；`(source, sourceContestId)` 唯一 | 保存远程规范化数据和 `remoteFingerprint`，不保存收藏状态 |
| `favorites` | `contestId` 主键并外键到 `contests` | 远程同步不能删除用户收藏；删除比赛需显式策略 |
| `reminders` | `id` 主键；`(contestId, offsetMinutes)` 唯一 | 外键级联、系统 request code 稳定且可取消 |
| `sync_status` | `source` 主键 | 保存最近尝试、成功、错误类别和数量 |

需要为 `startTime`、`source`、收藏关联和 reminder 的 `contestId` 建索引。Room schema JSON 纳入版本控制；迁移测试从首个正式 schema 开始。

### 4.3 UI 状态

每个复杂页面使用一个不可变 `UiState` 和单向事件入口。例如比赛列表状态包含：

- 已缓存且稳定 key 的分组列表。
- `isInitialLoading` 与 `isRefreshing`，刷新时不清空旧列表。
- 筛选条件与是否存在结果。
- 最近同步时间和逐平台错误提示。
- 网络不可用但缓存可用的非阻塞提示。

一次性行为（打开 Custom Tab、请求权限、轻量提示）通过明确的 UI effect 通道表达，不把 `Context` 放进 ViewModel。

## 5. 页面与导航

顶层使用一个浮在页面内容上的圆角导航容器，包含三个同时显示图标和标签的入口；选中项由黑白反转的小圆角块包裹：

```text
比赛列表（含筛选）─┐
我的比赛 ─────────┼─> 比赛详情 ─> 外部官方页面（Custom Tabs）
                  │       ├─> 提醒编辑（底部面板）
                  │       └─> 系统日历插入 Intent

设置 ─> 数据源设置 ─> 自定义来源列表 ─> 解析预览/字段映射
     ├> 同步设置/同步状态
     ├> 通知与精确提醒状态
     └> 隐私说明

系统通知 deep link ─> 比赛详情
```

| 路由 | 来源 | 返回行为 |
| --- | --- | --- |
| `contests` | 启动页/底部导航 | 保留列表滚动位置 |
| `favorites` | 底部导航 | 保留筛选与滚动位置 |
| `settings` | 底部导航 | 返回前一顶层状态 |
| `custom-sources` | 设置 | 编辑中先退出编辑，否则返回设置 |
| `contest/{contestId}` | 列表、收藏、通知 | 返回发起页面；找不到数据时显示可恢复状态 |
| `settings/sources` | 设置 | 返回设置 |
| `settings/sync` | 设置 | 返回设置 |
| `settings/privacy` | 设置 | 返回设置 |

提醒编辑优先使用留白充足的底部面板而非独立主页面。外部 URL 在 domain/common 层验证 scheme 和 host 基本合法性后才交给 Custom Tabs。

## 6. 阶段 1 Gradle 依赖

版本由 Version Catalog 锁定，阶段内不使用 alpha、beta 或 RC。

| 能力 | 依赖 | 固定版本 |
| --- | --- | --- |
| 构建 | AGP / Kotlin / Compose Compiler | 8.5.2 / 1.9.24 / 1.5.14 |
| Compose | Compose BOM | 2024.09.03 |
| 生命周期 | lifecycle runtime/viewmodel compose | 2.8.4 |
| 导航 | navigation-compose | 2.7.7 |
| 注入 | Hilt / hilt-navigation-compose | 2.51.1 / 1.2.0 |
| 数据库 | Room runtime/ktx/compiler | 2.6.1 |
| 设置 | DataStore Preferences | 1.1.1 |
| 后台任务 | WorkManager KTX | 2.9.0 |
| 网络 | Retrofit / OkHttp | 2.11.0 / 4.12.0 |
| JSON | Kotlinx Serialization | 1.6.3 |
| HTML | Jsoup | 1.18.1 |
| 外部页面 | AndroidX Browser | 1.8.0 |
| 测试 | JUnit / MockK / Compose UI Test | 4.13.2 / 1.13.11 / BOM 管理 |

AlarmManager、Notification API、Calendar Intent 和 `java.time` 是平台 API，不需要新增三方依赖。

## 7. 数据接入方案

### 7.1 统一数据源契约

每个适配器只负责返回规范化数据或抛出来源异常，`RemoteContestFetcher` 在 `supervisorScope` 中把它转换为显式逐源结果：

```kotlin
interface ContestRemoteDataSource {
    val source: ContestSource
    suspend fun fetchUpcomingContests(): List<Contest>
}
```

聚合器并行调用 DataStore 中启用的数据源并形成 `Success/Failure`。离线仓库按来源独立事务 upsert；只有成功解析的数据源才能更新该来源的快照，失败源的旧数据保留。

### 7.2 Codeforces

来源使用 Codeforces 官方公开方法 [`contest.list`](https://codeforces.com/apiHelp/methods)。实现要点：

1. 调用 `GET https://codeforces.com/api/contest.list?gym=false`，不使用账号、Cookie 或认证参数。
2. Retrofit DTO 只描述官方响应：顶层 `status/comment/result` 与 contest 字段。
3. 仅映射 `BEFORE` 和进行中阶段；`startTimeSeconds` 使用 epoch seconds 转 `Instant`，`durationSeconds` 转 `Duration`。
4. 对 `status != OK`、HTTP 429、5xx、超时、非法时间戳和缺失必需字段分类处理。
5. OkHttp 设置有限超时；重试只针对幂等且可恢复错误，不在客户端快速重放 429。
6. 同步成功后由 fingerprint 判断新增或变更；比赛时间变化触发已有提醒的重调度用例。

Codeforces 集成测试使用 MockWebServer 风格的固定 JSON 响应；单元测试不依赖实时 API。

### 7.3 AtCoder

AtCoder 当前公开的[比赛列表页](https://atcoder.jp/contests/)提供开始时间、名称、时长和 Rated Range，但不把它当作稳定 API。实现要点：

1. 独立 `AtCoderContestFetcher` 只下载 HTTPS HTML；独立 `AtCoderContestParser` 只解析字符串。
2. 选择器、表头定位、时间格式和 URL 规范化集中在 parser 内，Repository 不接触 Jsoup selector。
3. 通过表头语义定位列，不依赖单一 `nth-child`；必须确认处于 Upcoming Contests 区域。
4. 页面提供 `+0900` 偏移时按 `OffsetDateTime` 解析，再转 UTC `Instant`，禁止手动加九小时。
5. 比赛链接中的 slug 作为 `sourceContestId`；缺失时才使用稳定哈希。
6. 解析器对空表、缺列、非法 URL、非法时间和异常数量设置结构检查。任何结构检查失败都使本次 AtCoder 同步失败，不写入半成品。
7. `src/test/resources/fixtures/atcoder_contests.html` 保存最小化且注明观察日期的 HTML fixture；测试不得访问实时网站。
8. 页面变化时保留旧缓存，UI 显示平台级非阻塞错误，其他平台继续更新。

解析频率保持克制，尊重网站可用性；不登录、不持有 Cookie、不绕过访问限制。

### 7.4 洛谷与牛客

洛谷使用公开比赛列表 `https://www.luogu.com.cn/contest/list`。解析器只接受服务端回退列表中的数字比赛 ID 和明确的开始/结束时间，并使用 `Asia/Shanghai` 转为 UTC `Instant`。

牛客使用公开竞赛列表 `https://ac.nowcoder.com/acm/contest/vip-index`，读取牛客系列赛和高校校赛两个公开分类。解析器交叉校验 DOM ID、链接 ID 与内嵌 contest ID，并直接使用页面提供的 epoch milliseconds；自主创建赛不进入近期赛程。

两者均为独立 HTML 适配器，不持有 Cookie、不模拟登录。已识别结构但无法映射时抛出解析错误，由平台级隔离保留旧缓存。fixture 和维护规则见 `DATA_SOURCES.md`。

### 7.5 自定义 HTTPS 来源

自定义来源不是任意浏览器自动化。配置先进入 `SafeCustomSourceHttpClient`，经过 HTTPS、重定向、响应大小和公网地址校验，再由 `CustomContestParser` 识别 JSON、iCalendar 或结构化 HTML。无法可靠识别时，用户必须提供 CSS 字段映射并通过预览确认。

每个配置生成 `custom:<uuid>` 的 `sourceKey`；比赛在现有 `ContestSource.OTHER` 分组下持久化，但 `sourceContestId` 使用配置 UUID 命名空间，避免多个自定义站点冲突。同步状态使用 `sourceKey` 区分来源，单一自定义页面失败不会影响其他来源。删除配置前先取消该来源提醒，再通过外键级联清除收藏和比赛。

## 8. 同步、缓存和错误隔离

推荐同步流程：

1. 读取用户启用的数据源。
2. `supervisorScope` 并行抓取，每个 source 有独立超时和错误映射。
3. 对成功列表执行字段规范化、稳定 ID 和单源去重。
4. 跨源不自动合并同名比赛；避免赞助赛或镜像赛误合并。只标记可能重复供以后审查。
5. Room 事务内 upsert 成功数据、更新同步状态并清理过期且未收藏的数据。
6. 远程开始时间变化时找出受影响提醒并交给 reminder 用例重排。
7. 返回逐平台结果；部分成功属于成功完成，不让 WorkManager 无限重试已成功平台。

WorkManager 只负责数据同步，AlarmManager 负责用户可见提醒。两者不能互相替代业务语义。

## 9. 提醒策略

提醒创建前必须检查：通知权限、触发时间仍在未来、同比赛同 offset 不重复、URL/deep link 可恢复。优先策略：

1. 用户明确开启且系统允许精确闹钟时使用 exact alarm。
2. 不允许时使用 inexact alarm；必要时用一次性 WorkManager 作为更宽松降级。
3. UI 显示实际精度，不承诺后台严格准点。
4. 开机、应用更新和比赛时间变化后，从数据库读取有效提醒并幂等重排。
5. 取消收藏不应暗中遗留闹钟；删除提醒先取消系统任务，再更新本地状态并处理失败恢复。

## 10. Design System

- 页面水平留白 24dp，卡片内容至少 16dp，布局以 8dp 节奏组织。
- 浅色以白、黑和系统灰为主；深色使用黑色背景与 `#1C1C1E` surface。
- 圆角最小 12dp，卡片默认 20dp；不用默认 Material elevation。
- 交互不显示 ripple，统一使用缩放与透明度按压反馈，触控目标至少 48dp。
- UI 过渡只使用 spring；系统关闭动画时不产生按压缩放。
- 文案全部位于资源文件，列表使用稳定 key，状态切换使用可访问语义。

Material 3 只提供 Compose 基础组件能力；颜色、排版、圆角、阴影和交互均由项目令牌显式定义。

## 11. 最高风险与缓解

| 等级 | 风险 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| 1 | 内置或自定义 HTML 结构变化 | 单一数据源暂时失效或产生错误数据 | parser 单一入口、fixture/保存前预览、旧缓存保留、来源级隔离 |
| 2 | 精确提醒权限与厂商后台限制 | 通知延迟或丢失 | 实际能力检测、精确/非精确降级、状态说明、重启重排测试 |
| 3 | 比赛时间变化 | 旧提醒在错误时间触发 | fingerprint、事务后差异集、幂等取消与重排 |
| 4 | 远程 upsert 覆盖本地状态 | 收藏或备注丢失 | contests/favorites/reminders 分表，远程 mapper 不包含用户字段 |
| 5 | 多源部分失败处理不当 | 一个平台拖垮全局同步 | supervisor scope、逐源结果、成功源独立落库 |
| 6 | 时区和夏令时 | 分组、倒计时和日历时间错误 | UTC Instant 持久化、注入 Clock/ZoneId、边界测试 |
| 7 | 用户输入 URL 访问局域网或超大内容 | 隐私风险、内存或流量消耗 | HTTPS、公网 DNS 校验与固定、私网阻止、重定向/1 MB/200 场上限 |
| 8 | 目标 SDK 与依赖升级 | 发布阻塞或行为变化 | 发布阶段单独升级、阅读迁移说明、真机回归，不随功能提交混升 |

## 12. 测试边界

- domain：状态判断、时区、排序分组、去重、倒计时、提醒时间、过期过滤。
- data：Codeforces DTO、三个内置 HTML fixture、自定义 JSON/ICS/HTML、地址安全、来源失败隔离与缓存保留。
- database：upsert、收藏关联、提醒唯一约束/级联、过期清理和迁移。
- reminder：权限能力矩阵、request code 稳定性、重启/更新时间变化后的幂等重排。
- UI：加载、内容、空、缓存离线、单源错误、全源错误、收藏回滚、筛选和深色模式。

每个阶段的完成定义都包含 `assembleDebug`、相关测试和 `lintDebug` 通过。
