# Contest Pulse 开发路线

任务按依赖关系执行。每个阶段只在编译、Lint 和相关测试通过后关闭；不通过伪实现、空异常处理或生产假数据跳过验收。

## 阶段 0：架构设计

- [x] 明确无后端、无账号、仅本地个人数据的产品边界。
- [x] 选择单模块分层起步并定义未来多模块映射。
- [x] 列出核心领域模型、Room 表和 UI 状态。
- [x] 定义顶层导航和详情/设置子路由。
- [x] 记录 Codeforces API 与 AtCoder 页面解析方案。
- [x] 建立风险排序与缓解策略。

验收：`docs/ARCHITECTURE.md` 能指导实现，不依赖口头约定。

## 阶段 1：项目骨架

- [x] 创建 Gradle Kotlin DSL 工程和 Version Catalog。
- [x] 配置 Kotlin、Compose、Hilt、Room、Retrofit、Navigation、DataStore、WorkManager。
- [x] 创建 Hilt Application 与 Compose Activity。
- [x] 创建三入口导航壳和阶段占位页面。
- [x] 创建颜色、排版、间距、圆角、阴影和 motion tokens。
- [x] 创建无 ripple 的按压反馈和 spring 页面转场。
- [x] 集中文案资源并支持深浅色启动背景。
- [x] 完善 `.gitignore`、README 和架构文档。
- [x] 通过 `assembleDebug`。
- [x] 通过 `testDebugUnitTest`。
- [x] 通过 `lintDebug`。

验收：全新 clone 配置 SDK 后可构建；应用启动后能在三个顶层页面间导航；没有真实数据或伪造同步结果。

## 阶段 2：本地模型与假数据 UI

- [x] 实现 `Contest`、枚举和值对象，时间统一为 `Instant` / `Duration`。
- [x] 实现状态判断、倒计时格式化、排序、分组、筛选和稳定 ID。
- [x] 定义 repository 接口和列表/详情/收藏用例。
- [x] 创建明确标识示例数据的内存仓库。
- [x] 实现首页下一场比赛重点卡片与分组时间线。
- [x] 实现比赛详情、收藏页和比赛列表内可折叠筛选。
- [x] 实现首次加载、空和无结果状态；网络错误状态留待真实数据阶段。
- [x] 实现 ViewModel、不可变 UiState 和单向事件。
- [x] 补齐 domain 单测与 Compose UI 测试，并完成测试 APK 编译。

验收：无网络依赖即可完整评审信息层级、深浅色和交互；示例数据不会进入 release 数据源。

## 阶段 3：Codeforces 与本地缓存

- [x] 定义 Codeforces Retrofit service、DTO 和响应错误模型。
- [x] 实现 DTO 到领域模型映射及非法字段处理。
- [x] 创建 Room entities、DAO、database、converter 和 schema 导出。
- [x] 分离 contests、favorites、reminders、sync_status 表。
- [x] 实现 cache-first repository 与事务 upsert。
- [x] 实现手动/下拉刷新且刷新期间保留旧列表。
- [x] 实现 HTTP/业务错误、超时和 429 分类。
- [x] 使用固定 JSON 编写 mapper 与错误隔离测试。
- [x] 添加数据库关系、级联和清理仪器测试；首个 schema 无迁移路径。

验收：Codeforces 成功时可离线再次打开；失败时保留缓存；不需要任何平台账号。

## 阶段 4：AtCoder 与多源隔离

- [x] 实现 HTTPS HTML fetcher。
- [x] 实现单一 `AtCoderContestParser`，集中所有 selector 和格式规则。
- [x] 保存最小化 HTML fixture，并记录观察日期和维护说明。
- [x] 覆盖正常、空表、非法时间、长时长和结构变化测试。
- [x] 实现 source-level supervisor 并发和结果汇总。
- [x] 实现结构失败不覆盖旧缓存。
- [x] 在设置 UI 展示单平台错误而不阻塞其他来源。

验收：断开 AtCoder 或提供损坏 fixture 时，Codeforces 与本地功能仍正常。

## 阶段 5：收藏与提醒

- [ ] 实现收藏的显式乐观更新、持久化失败回滚；当前 Room 本地事务与离线查询已完成。
- [x] 实现预设/自定义 reminder offset 与唯一约束。
- [x] 创建通知渠道和 Android 13+ 通知权限流程。
- [x] 实现 exact/inexact alarm 能力检测与调度器。
- [x] 实现通知点击 deep link 到详情。
- [x] 实现取消、开机、应用更新、时区和系统时间变化后的幂等重排。
- [x] 实现精度降级说明和系统设置入口。
- [x] 覆盖过期过滤、稳定 request code，并编译提醒关系/级联数据库测试。

验收：提醒可创建、取消、重启恢复；无精确权限时有明确降级而非静默失败。

## 阶段 6：后台同步与设置

- [ ] 实现一次性和周期 WorkManager 调度；当前前台立即同步与周期 Worker 已分离实现。
- [x] 配置网络类型、6/12/24 小时频率和指数退避。
- [x] 实现启动时一小时新鲜度的 stale-while-revalidate 判断。
- [x] DataStore 保存启用来源、网络偏好、后台开关和频率。
- [x] 实现同步状态页和逐源最近成功/错误信息。
- [x] 部分成功视为 Worker 成功，避免失败源造成全局重试循环。
- [ ] 增加 worker、约束和设置迁移测试。

验收：后台同步节能、可关闭、状态可解释；同步与提醒职责分离。

## 阶段 7：体验、合规与发布准备

- [x] 使用安全日历插入 Intent，不读取用户全部日历。
- [x] 完成主要离线、通知权限、精确提醒和后台限制状态。
- [x] 建立 TalkBack 语义、动态字体、48dp 触控目标和黑白对比度基线。
- [x] 接入系统减少动画设置，交互动效统一使用 spring。
- [x] 验证所有外链 scheme/host，生产网络不记录敏感请求。
- [x] 增加隐私说明和平台非隶属声明。
- [ ] 升级到发布时要求的 target SDK 并完成行为回归。
- [x] 执行 release shrink 构建并检查 R8/资源压缩。
- [ ] 执行真机与通知矩阵测试（当前没有连接设备）。
- [ ] 增加 README 截图、数据来源维护说明和发布检查表。

验收：release 构建通过，无敏感日志、无明文流量、无未说明的权限或数据传输。

## 阶段 8：洛谷与牛客

- [x] 验证公开页面无需登录，且 robots 规则允许比赛列表路径。
- [x] 实现 `LuoguContestDataSource` 与集中式 HTML 解析器。
- [x] 实现 `NowcoderContestDataSource`，覆盖牛客系列赛与高校校赛。
- [x] 使用页面明确的时区或绝对时间戳，统一映射为 UTC `Instant`。
- [x] 校验数字比赛 ID、官方链接、正时长与结构完整性。
- [x] 增加洛谷、牛客最小 fixture 与正常/空/损坏结构测试。
- [x] 接入 Hilt 多绑定、默认来源、设置开关与比赛页平台筛选。
- [x] 更新 README、数据源维护说明与发布矩阵。

验收：任一新平台结构损坏时只记录该来源解析失败并保留旧缓存；四个平台可独立开关，筛选和详情外链使用统一领域模型。

## 阶段 9：自定义数据来源与同步可解释性

- [x] 修复牛客线上双层 HTML 实体编码并更新回归 fixture。
- [x] 首页区分部分失败与全部失败，并明确显示失败来源。
- [x] 使用独立 `sourceKey` 隔离多个自定义来源的同步状态与缓存清理。
- [x] 使用 DataStore 保存自定义来源配置，支持编辑、启停和删除。
- [x] 实现 JSON、iCalendar、JSON-LD、内嵌 JSON、语义 HTML 自动识别。
- [x] 实现 CSS 字段映射、时区和自定义时间格式。
- [x] 保存前强制解析预览，不对低置信结果静默启用。
- [x] 实现 HTTPS、公网 DNS、DNS 固定、重定向、响应大小与导入数量限制。
- [x] 增加自定义解析与私网地址单元测试。

验收：用户能从设置添加公开赛程地址并核对预览；任一自定义来源失败不影响其他来源，危险或不受支持的网址会明确拒绝。
