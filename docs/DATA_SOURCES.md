# 数据源维护说明

## Codeforces

- 来源：`GET https://codeforces.com/api/contest.list?gym=false`。
- 类型：官方公开 JSON API，无认证参数。
- 生产入口：`CodeforcesContestDataSource`。
- 只展示 `BEFORE` 与 `CODING`；必需字段缺失、非法时间戳和非正时长会被丢弃。
- 顶层 `status != OK` 视为远端业务错误；HTTP 429 单独归类为频率限制。
- 固定响应：`app/src/test/resources/fixtures/codeforces_contests.json`。

维护时先对照官方方法文档，再更新 DTO/fixture；不要根据第三方博客虚构字段。

## AtCoder

- 来源：`https://atcoder.jp/contests/?lang=en`。
- 类型：官方公开 HTML 页面，不是稳定 API。
- 生产入口：`AtCoderContestDataSource`；全部 selector 和格式规则集中在 `AtCoderContestParser`。
- 当前读取 `#contest-table-upcoming` 和 `#contest-table-daily`。
- 时间使用页面自带的 `+0900` 偏移解析为 `Instant`，没有手动加九小时。
- 比赛 ID 只接受 `/contests/<slug>` 中的字母、数字、下划线和连字符。
- 已识别表格存在数据但没有任何行可解析时，整个平台本次同步失败，Room 旧缓存不被覆盖。
- fixture：`app/src/test/resources/fixtures/atcoder_contests.html`，基于 2026-07-14 观察到的公开结构最小化保存。

页面变化维护步骤：

1. 保存最小化的新 fixture，不把整页追踪脚本或无关内容提交到仓库。
2. 先写失败测试，再集中修改 parser。
3. 覆盖正常、空表、缺列、非法时间、非法链接和长时长。
4. 运行 `testDebugUnitTest`、`lintDebug` 与 `assembleDebug`。
5. 不把解析规则散落到 Repository、ViewModel 或 Compose 页面。

## 洛谷

- 来源：`https://www.luogu.com.cn/contest/list`。
- 类型：洛谷公开 HTML 比赛列表，不假设存在官方 JSON API。
- 生产入口：`LuoguContestDataSource`；结构、数字比赛 ID 与时间区间解析集中在 `LuoguContestParser`。
- 当前读取服务端回退内容中的 `#app > ul` 直接比赛项，只接受 `/contest/<数字 ID>`。
- 页面时间按 `Asia/Shanghai` 解析为 `Instant`，没有手动增加八小时。
- 已结束比赛在解析后过滤；任何已识别比赛项无法完整解析时，本次来源失败并保留旧缓存。
- fixture：`app/src/test/resources/fixtures/luogu_contests.html`，基于 2026-07-14 观察到的公开结构最小化保存。
- `robots.txt` 未禁止 `/contest/`；客户端保持 6 至 24 小时的后台同步频率，不登录、不持有 Cookie。

## 牛客

- 来源：`https://ac.nowcoder.com/acm/contest/vip-index`，分别读取公开的牛客系列赛（`topCategoryFilter=13`）与高校校赛（`topCategoryFilter=14`）。
- 类型：牛客公开 HTML 竞赛页，不假设存在稳定公开 API。
- 生产入口：`NowcoderContestDataSource`；DOM 结构与内嵌 `data-json` 解析集中在 `NowcoderContestParser`。
- 线上 `data-json` 当前为双层 HTML 实体编码；解析器使用有上限的实体归一化后再解 JSON，fixture 保留同样结构以防回归。
- 时间使用页面给出的 epoch milliseconds 转为 `Instant`，并校验开始、结束和正时长。
- 比赛链接只接受 `/acm/contest/<数字 ID>`，且必须与 DOM `data-id` 和内嵌 `contestId` 一致。
- 页面标题、开始/结束时间与内嵌时长必须相互一致；任一比赛项异常都会使本次牛客同步 fail closed。
- Rated 只在页面存在明确 Rated 标签时设为 `true`；页面给出 Rating 上限时映射为 `ratedRange`。
- 自主创建赛不进入近期聚合，避免长期题单、重现赛和异常历史日期污染赛程。
- fixture：`app/src/test/resources/fixtures/nowcoder_contests.html`，基于 2026-07-14 观察到的公开结构最小化保存。
- `ac.nowcoder.com/robots.txt` 明确允许 `/acm/`；客户端不请求登录、Cookie 或个人报名状态。

## 自定义来源

- 配置保存在独立 DataStore，只包含名称、HTTPS 地址、时区、格式和可选 CSS 映射，不保存 Cookie、账号或令牌。
- 自动识别顺序为 iCalendar、JSON、HTML；HTML 内依次尝试 JSON-LD、`data-json` 内嵌对象和带 `datetime` 的语义时间元素。
- 显式 HTML 映射要求比赛条目、标题和开始时间选择器；结束时间、链接和 `DateTimeFormatter` 格式可选。
- 所有结果在保存前必须预览。同步时每个自定义来源具有独立 `sourceKey`，失败不会取消内置平台或其他自定义来源。
- 只允许 HTTPS；最多跟随 3 次 HTTPS 重定向，解压后的正文最多 1 MB，不执行 JavaScript，也不使用 Cookie。
- 每个请求在 DNS 解析后拒绝 loopback、私网、链路本地、组播、CGNAT 和 IPv6 ULA，并将本次连接固定到已校验地址，避免重定向或 DNS 重绑定访问局域网。
- 单个来源最多导入最早的 200 场未来/进行中比赛；结束时间缺失会在预览中警告，但不伪造时长。
- 删除来源会显式清除其本地比赛、收藏、提醒和同步状态。

自定义来源不承诺兼容需要登录、验证码、客户端 JavaScript、无限滚动或反自动化挑战的页面。用户应只添加允许公开读取并符合站点规则的地址；优先使用平台提供的 JSON、ICS 或服务端渲染列表。

## 页面变化维护

洛谷或牛客页面变化时遵循与 AtCoder 相同的 fail-closed 流程：先保存最小 fixture 和失败测试，再集中修改对应 parser。禁止把 selector、字段名或时区逻辑散落到 Repository、ViewModel 或 Compose 页面。

## 内置新平台准入

后续平台只有在公开来源无需登录、不规避限制、字段可稳定映射、有固定测试样本且失败可隔离时才启用；不返回生产假数据。
