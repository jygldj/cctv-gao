# 更新日志

> 当前稳定版：**v4.5.26**（计时器改白名单 + 取消起播默认海报，已实测确认）。
> 待实测：**v4.5.25**（换台/播放流畅度优化，见下）、**v4.5.27**（删除首页起跳浮层，见下）。

## v4.5.27 (2026-09-17) - 删除首页起跳前多余的浮层提示【待实测】

> 自 `cctv-zl` 移植（对应其 v4.5.44）。只改网页侧 `js/index.js` 一个文件，原生层零改动。

**现象**：首页点击 CCTV直播 / 央视片库 / 央视栏目，先出现「等待跳转...」约 0.2~1 秒，之后原生计时器才开启，两层提示叠在一起。

**根因**：`appChoose()` 在 `location.href` 之前先弹一个 JS 浮层；而原生在 `onPageStarted` 就已经接管（命中 `isVideoPage` 才显示计时遮罩）。两处做的是同一件事——提示"正在跳转"，JS 层抢先一步，于是先闪一下再被原生盖掉。

**改法**：
- 删掉 `appChoose()` 里两处浮层调用，只留 `window.location.href = dataUrl;`。
- 顺带删掉死分支 `if (_utao_version <= 20)`：`index.html` 引的是 `base.js`（`_utao_version="21"`），条件恒不成立，且分支体与下方逻辑完全相同，删除后行为等价。

**注意（gao 与 zl 的差异）**：gao 的「支持我」入口是 `kuxuan.html`（zl 是 `dsm.html`），本次**均未改动**，保持现状。gao 的 4 个入口皆是网页脱壳，删除浮层后直接由原生计时遮罩接管，等待反馈不会中断。

---

## v4.5.26 (2026-09-17) - 计时器改白名单 + 取消起播前的默认海报【稳定版】

> 自 `cctv-zl` 移植（对应其 v4.5.42 / v4.5.43）。两处都只在**原生层**，网页侧零改动；`LiveActivity`（央视网 / CCTV直播）一行未动。

**① 计时器改为白名单式（对应 cctv-zl v4.5.42）**

**gao 与 zl 的关键差异（勿混用验收标准）**：gao 是网页版高清，**4 个入口（央视网 / CCTV直播 / 央视片库 / 央视栏目）全部是网页脱壳**，等待期需要遮住站点骨架，因此**这 4 个入口本就应当都有计时器**；`cctv-zl` 的片库/栏目则是本地页直连 m3u8，不需要计时器。两条线路的情形不同，不能照搬同一套结论。

**根因（原实现的写法问题）**：`onPageLoadStarted` 写成了「任何一次页面跳转都先安排遮罩」——对纯本地页也照排一个 260ms 的「待显示」定时器（`scheduleLoadingOverlay`）。计时器的唯一用途是给网页脱壳的等待期遮一遮裸骨架；而本地页是工程内联的 HTML（自身底色即 `#0a1f3a`），压根没有站点骨架可挡，遮罩对它们毫无意义。

**改法（`BaseWebViewActivity.java`）**：
- `onPageLoadStarted` 改为**白名单**：只有 `isVideoPage(url)`（`tv.cctv.com` / `yangshipin.cn`）才安排遮罩；其余页面一律不安排，仅做会话状态复位并 `stopFsPoll()`。
- 整条「延迟待显示」链路删除：`scheduleLoadingOverlay()`、`cancelPendingShow()`、字段 `pendingShowRunnable`、常量 `OVERLAY_SHOW_DELAY_MS = 260L`，以及散在 5 处的调用点。
- 保留 `OVERLAY_MIN_SHOW_MS = 420L`（收起防抖：一旦显示就别一闪而过）。
- **在 gao 上的实际效果**：4 个脱壳入口命中 `tv.cctv.com` / `yangshipin.cn` 白名单，计时器**原样保留**（这是预期行为）；首页、支持我这类纯本地页不再弹无谓计时器。同时把「任何跳转都排队遮罩」这一隐患从根因上消除。

**② 取消起播前的「灰底 + 播放按钮」（对应 cctv-zl v4.5.43）**

**表现**：央视片库 / 央视栏目是 m3u8 直连，画面出现前会显示一个居中灰方块 + 播放图标，约 1~2 秒。

**根因**：这是 **Android WebView 自带的「视频默认海报」**。`<video>` 没有 `poster` 属性时，WebView 会取 `WebChromeClient.getDefaultVideoPoster()` 的返回值当海报画在视频区域，系统默认给的就是「灰底 + 大播放图标」。它由内核直接绘制，与页面 CSS / xgplayer 无关——所以此前在 xgplayer 侧怎么调都盖不住。

**排查证据**：
- 截图像素量测：灰块 `#888888`，宽 720 = 屏幕高且水平居中 → 恰是一张方形图按 contain 缩放；图标为黑色圆环 + 三角。
- 项目自身 CSS/JS 中 `#888` / `888888` / `rgb(136,136,136)` 一条都没有 → 不是页面画的。
- Chromium 对照实验：`<video>` 无 `controls` → 全屏干净；有 `controls` → 灰块 + 播放图标；加 1×1 透明 `poster` → 灰底消失。
- 抓真实 `live.html` 的 DOM：`<video autoplay ...>` 无 `controls` → 桌面干净、Android 会灰，与实验一致。

**改法（`BaseWebViewActivity.java`）**：
- 新增 `TRANSPARENT_VIDEO_POSTER`（1×1 `ARGB_8888` 透明位图），覆写 `getDefaultVideoPoster()` 返回它。
- 本覆写只作用于 `BaseWebViewActivity` 的 `WebChromeClient`。注：`LiveActivity`（央视网入口）自带独立的 `WebChromeClient`，本次未作改动；若该入口起播前也出现同类灰块，在其 WebChromeClient 内加同一处覆写即可。
- 那张默认海报本就不可点击，取消后无功能损失。

**未移植项**：`cctv-zl` 中另有一个 `isLocalAssetPage()` 静态方法，自 v4.5.42 起已不参与任何判定（纯自说明的遗留），本次未带入 gao。

**验证（本机无 Android SDK，静态校验）**：
- 61 个 Java 文件花括号**词法级**配平全部通过（扫描时跳过注释与字符串）。
- 与 cctv-zl 对应文件的差异收敛到唯一一处，即上述未移植的 `isLocalAssetPage`。
- 已确认 `LiveActivity.java` 自带独立 `WebChromeClient` 与独立遮罩逻辑、不继承 `BaseWebViewActivity`，央视网 / CCTV直播链路零影响。
- 改动前文件已备份至 `F:\github-dx\_backup_cctv-gao_20260917_1822\`（md5 `40b6354b2883b7f53c43935c60dc825f`）。

**实测结论（用户确认）**：
1. 4 个网页脱壳入口的计时器照常工作 ✓ —— 属预期行为，不是残留。
2. 起播前的「灰方块 + 播放按钮」已根除 ✓。
3. 判定为稳定版。

## v4.5.25 (2026-09-16) - 换台与播放流畅度优化【待实测】

**目标**：减少换台的无效等待、减少页面加载期的主线程开销，让出画面更快、体感更顺。

**换台链路（`LiveActivity.java`）**：
1. **换台防抖 1000ms → 250ms** —— 原逻辑给遥控器长按留了 1 秒合并窗口，每次换台都要白等满 1 秒。新增常量 `SWITCH_DEBOUNCE_MS = 250L`：250ms 足以合并长按产生的连续 `ACTION_DOWN`，又几乎无感。
2. **连续按键只落地最后一次** —— `goNext()` 里先 `handler.removeMessages(1)` 再 `sendMessageDelayed`，避免多次按键排队触发多次加载。
3. **同频道不重复整页重载** —— `handleMessage` 中取 `mWebView.getUrl()` 与目标 URL 比对，相同则直接跳过 `loadLiveUrl`，不再白刷一次网页（白屏闪一下）。
4. **打开菜单撤销未落地的换台** —— `showMenu()` 里 `handler.removeMessages(1)`，用户浏览频道时不会被刚按下的换台「带走」。
5. **遮罩显示目标频道名** —— 遮罩文案由 `正在脱壳网页框架… 3.2s` 变为 `CCTV-1 综合 · 正在脱壳网页框架… 3.2s`，黑屏那几秒用户能一眼确认切到了哪个台。频道名在 `loadLiveUrl` 内按 URL 与 `currentLive` 匹配后取值，对不上则留空（首次进入不贴错台）。

**注入脚本缓存（`impl/WebViewClientImpl.java`）**：
- 新增静态缓存 `sEndJs` / `sLoadDetailTvJs` / `sLoadDetailVideoJs` / `sCctvFullscreenJs` / `sPageFsBtnJs` / `sPageToastKillJs` 与 `assetText()` 助手。
- 这些脚本原先在每次 `onPageStarted` / `onPageFinished` 都经 `FileUtil.readExt` 从 assets 重读一遍（assets 为压缩存储，每次 open 都要解压 + 拼串 + 打日志），且发生在 UI 线程。缓存后每个进程只读一次，换台越频繁收益越明显。

**WebView 设置（`BaseActivity.java`）**：
- `setSupportMultipleWindows(false)`、`setDefaultTextEncodingName("UTF-8")`、`setOverScrollMode(OVER_SCROLL_NEVER)` —— 电视端不需要多窗口与越界回弹，关掉可省下加载期一批开销，也避免遥控器误触时页面跟着滑。
- `setWebContentsDebuggingEnabled(BuildConfig.DEBUG)` —— 原先恒为 `true`，release 包也一直开着调试，渲染进程多背一份开销；改为仅 debug 包开启。

**全屏管线（`js/cctvFullscreen.js`）**：
- `MutationObserver` 回调做 120ms 合并（`moBusy` 标志 + `setTimeout`）。原先每个 DOM 变更批次都跑一遍 `tryEnterPlayer()` / `tryFs()`（内部有 `querySelectorAll('a,button')` / `('video')`），加载期变更极密集时会在主线程上反复扫描、直接拖慢出画面。合并后同一波变更只处理一次，200ms 兜底轮询仍在，响应性不变。
- 断开 observer 的 `disconnect()` 加 try/catch 保护。

**布局（`activity_live.xml` / `activity_main.xml`）**：
- `loadingText` 增加 `maxLines="1"` + `ellipsize="middle"`，避免「频道名 + 阶段文案 + 秒数」在个别长台名下折行或溢出。

**验证（本机无 Android SDK，静态校验）**：
- `js/cctvFullscreen.js` `node --check` 通过。
- `BaseActivity.java` / `LiveActivity.java` / `WebViewClientImpl.java` 花括号配平均 BALANCED（59/59、299/299、114/114）。
- `activity_live.xml` / `activity_main.xml` 经 XML 解析器校验为 well-formed。
- `BuildConfig` 可用性已确认：`app/build.gradle` 中 `buildFeatures.buildConfig = true`，`namespace` 为 `com.daoxuan.cctv`（与 `BaseActivity` 同包，无需 import）；release 分支 `minifyEnabled false`，`BuildConfig.DEBUG` 在 release 下为 `false`。

**待用户实测**：Clean → Build → 装包。重点看 ① 连续长按换台的落地延迟是否明显缩短；② 换到已加载频道是否不再白闪；③ 出画面速度；④ 遮罩频道名显示是否正确。

## v4.5.24 (2026-09-16) - 修复脱壳遮罩计时器「一上来就 4000s+」【稳定版】

**用户反馈**：4 个 tv.cctv.com 入口（各省节目）脱壳遮罩的秒数一出现就是 4000s+（截图 5010.4s），明显不对。

**根因（会话基线 `sessionStartAt` 变脏）**：
- 计时秒数 = `now - sessionStartAt`。`sessionStartAt` 是「一次点节目 → 出画面」的连续会话起点，本意是跨页不归零、让遮罩在整条链路上连续计时。
- 当用户在栏目/列表页久留时，连续探测不到 video（`FS_NOVIDEO_MAX` 次）会走 `releaseOverlayForBrowsing()`「列表页放行」——原实现**只收遮罩、刻意不结束会话**（`hideLoadingOverlayInternal(false)`），`sessionStartAt` 于是保留着几分钟前的旧值。
- 之后用户再点节目，`onPageLoadStarted` 里 `if (!sessionActive)` 因会话仍未结束而为假，**不会重置基线**，秒数便从旧基线一路算下来 → 一上来就是 4000s+（实测 5010.4s）。
- 计时循环自身只有一个 `if (sessionStartAt <= 0)` 的兜底，防不住这种「非零但已过期」的脏基线。

**修复（`BaseWebViewActivity` 与 `LiveActivity` 同步）**：
1. **根因：列表页放行时结束会话** —— `releaseOverlayForBrowsing()` 改用 `hideLoadingOverlayInternal(true)`，把 `sessionStartAt / sessionActive / 阶段计数` 一并归零。列表页浏览结束即意味着「这次等待」已结束，下次点节目就是新的一次。
   （「专辑页 → 播放页」那种真正连续的跳转都在视频页之间，不会走到这条路，故不受影响。）
2. **兜底：计时循环内做基线有效期校验** —— 新增常量 `SESSION_STALE_MS = SESSION_MAX_MS + 5000`（23s，正常一次等待上限 18s）。计时循环里若 `sessionStartAt <= 0` 或已过期就重新起算；并夹住负值（`sec < 0 → 0`）。即便将来还有别的路径留下脏基线，也不会再渲染出 4000s+。
3. **进视频页时二次校正** —— `onPageLoadStarted` 的视频页分支除 `!sessionActive` 外，会话基线「缺失/过期」同样重新起算并重置阶段计数，保证文案与计时都从新一轮开始。

**验证**：本机无 Android SDK，静态校验——`BaseWebViewActivity.java` / `LiveActivity.java` 括号配平均 BALANCED；`SESSION_STALE_MS` 定义与引用一致；改动点各仅一处，逻辑自洽。

**实测确认（2026-09-16 17:18）**：用户 Clean → Build → 装包实测，各省节目/直播切台遮罩秒数从 0.1s 起正常增长，无 4000s+，本项封版。v4.5.24 定为当前稳定版。

**遗留（用户已明确不急于今天）**：
- 山西地方台（`sxrtv.com`，`tv.json` 的 `sxtv` 组）：节目页中心播放按钮不消失；部分台播放 1 秒后绿屏。属第三方站点自带播放器行为 + 疑为雷电模拟器解码限制，需真机测试。
- 北京地方台（`btime.com`，`tv.json` 的 `beijing` 组）：直播需登录，非代码可解。用户意见：地方台若不易修复可删除（主线为 央视 / 卫视 / 央视片库 / 央视栏目）。

## v4.5.23 (2026-09-16) - 收尾清理：删自制全屏按钮、剔诊断代码、清冗余文件

**用户实测确认（v4.5.22 落地后）**：
1. **直播入口「全力加载中…」气泡已彻底消失、底部无任何浮层（亦无黄条/红条）** —— 老问题完美解决（`pageToastKill` 已隔离为只注 `yangshipin.cn`，事件驱动压制生效）。本条封版。
2. 4 个 tv.cctv.com 入口速度**有提升、尚未回到昨晚最佳** → 归因战场冗余拖累，本次清理。
3. 个别节目播放前**短暂显示播放按钮**（时间短，影响小）→ 列为观察项，未改。

**本次改动（战场清理）**：

- **`js/cctvFullscreen.js`**：删除右下角自制的「全屏」浮层按钮 `#cctv-fs-force`（`buildFsBtn()` 函数 + 两处调用），并删除已外移的 `pokeToastKiller()` 函数及其调用、`cleanPage` 中的 `fsBtn` 引用。理由：自动脱壳全屏已稳定接管，手动按钮冗余且遮挡画面。**保留**选集按钮 `#cctv-fs-xj`。
- **`js/pageToastKill.js`**：删除全部诊断浮层 / 探针调试代码（`describe` / `noteHit` / `everHits` / `geoRec` / `scanDocForDiag` / `__DXTV_TOAST_DIAG__` / `__DXTV_TOAST_HUD__` / `HUD*` / `pushSeen` / `showToastHud` / `AUTO_HUD` 等），仅保留核心压制逻辑与两个原生点名接口 `__DXTV_KILL_LOADING__` / `__DXTV_ARM_TOAST_WATCH__`。直播气泡已完美解决，调试浮层/HUD 不再需要。
- **`LiveActivity.java`**：删除 `showToastDiagHud()` 与 `scheduleToastDiag()` 两个纯调试方法及其 3 处调用（`onPageLoadFinished` / `startFsPoll` playing 分支 / `loadLiveUrl`）。**保留** `killLoadingToastTail` / `KILL_PAGE_LOADING_JS`（功能性点名清气泡，仍调用 `__DXTV_KILL_LOADING__`）。
- **移出 10 个确认无运行时引用的冗余文件**（按 README 约定备份到工程外 `F:/github-dx/_backup_android-x5_20260916/`，工程内不留 `_backup_*`）：
  - `js/begin.js`（127 字节 `String.prototype.replaceAll` polyfill）
  - `js/xg/index.min.js`(280KB)、`js/xg/hls.min.js`(228KB) —— 死副本，live 页实际用的是 `js/xg/2.31.2/index.min.js` 与 `js/xg/hls/2.2.2/index.min.js`
  - `ya.js` / `node.js` / `huo.js` / `package.json` / `package-lock.json` / `upload.sh` / `ext-run.sh` —— 构建期 Node 脚本（`ya.js` 写死作者本机 `/Users/vonchange/...` 路径，非运行时资源）

**验证（本机无 Android SDK，静态 + 逻辑）**：`pageToastKill.js` `node --check` 通过；全工程（不含 build/ 与文档）已无 `buildFsBtn` / `cctv-fs-force` / `pokeToastKiller` / `__DXTV_TOAST_HUD__` / `__DXTV_TOAST_DIAG__` / `showToastDiagHud` / `scheduleToastDiag` 残留引用（仅剩注释说明）；`LiveActivity` 内 `killLoadingToastTail` / `KILL_PAGE_LOADING_JS` 完好；10 个冗余文件已字节级校验备份并移出工程。

**下一步（待用户实测）**：Clean → Build → 装包。重点看 ① 4 个 tv.cctv.com 入口播放速度是否回到昨晚最佳（清理冗余后应提升）；② 央视片库/央视栏目右下角"全屏"按钮是否已消失；③ 播放前播放按钮问题是否缓解。

## v4.5.22 (2026-09-16) - 止血 + 隔离：4 个 tv.cctv.com 入口回到 v4.5.16，直播气泡单独处理

**用户反馈（关键）**：4 个 tv.cctv.com 入口（央视网 / 央视片库 / 央视栏目 / 第 5 入口）从昨晚 v4.5.16 起就正常，
但 v4.5.17~v4.5.21 把「全力加载中…」的排查脚本 `pageToastKill.js` 打进了**公共注入管线**，
导致这 4 个入口每次播放都背着它那个**每 200ms 全文档扫一遍的常驻循环**，播放被拖慢。
而 CCTV 直播入口（走的是央视频 `yangshipin.cn`）因为公共管线的 `url.contains("tv.cctv.com")` 门槛，
整套脚本（含 `pageToastKill`）**从未注进去**——红条「诊断脚本未注入」就是证据。
→ 之前的几轮修改等于在「没通电的房间按开关」：既拖慢了 4 个好入口，又从没碰到直播气泡。

**本次改动（止血 + 隔离）**：

- **`impl/WebViewClientImpl.java`**：
  1. 从 `injectCctvFullscreenPipeline()`（仅 tv.cctv.com 的脱壳管线）**摘除 `pageToastKill` 的注入** → 4 个入口立刻回到 v4.5.16，不再背扫描开销。
  2. 新增**只针对 `yangshipin.cn`（直播）的专属分支**：在 `onPageFinished` 里单独注入 `pageToastKill`，
     不注脱壳脚本、不影响上面 4 个入口。直播气泡从此由这条独立路径负责。
- **`js/pageToastKill.js`（从「常驻 200ms 全文档扫描」改为「轻量事件驱动」）**：
  - 删除 `setInterval` 常驻循环；改为：video 的 `playing`/`canplay` 触发连扫一小串（burst 6×300ms）
    + MutationObserver（DOM 真变化时轻扫，空闲零开销）+ 两个定时深度扫描（body 就绪 / 气泡常冒出的时刻）
    + 原生 playing 分支的点名（`__DXTV_KILL_LOADING__`，由 `LiveActivity.killLoadingToastTail(8)` 调用）仍强制深扫。
  - `AUTO_HUD` 改为 `false`：诊断浮层不再自动弹，改由直播入口原生回调显式叫出，绝不误伤其它入口。
- **`BaseWebViewActivity.java`（4 个 tv.cctv.com 入口的基类，彻底还原 v4.5.16）**：
  删除 `killLoadingToastTail` / `scheduleToastDiag` / `showToastDiagHud` 三个调试方法、删除 `KILL_PAGE_LOADING_JS` 常量，
  以及 `onPageLoadFinished`/`startFsPoll` 里对应的调用 —— 该入口不再有任何 toast 相关逻辑、无浮层、无红条。
- **`LiveActivity.java`**：保留 `killLoadingToastTail` / `scheduleToastDiag` / `showToastDiagHud` 及 `playing` 分支调用
  （直播修复仍需），并确认 `onPageLoadFinished` 仍会叫出诊断浮层（现在直播页 `__DXTV_TOAST_HUD__` 已存在，显示黄条而非红条）。

**验证（本机无 Android SDK，静态 + 逻辑）**：`pageToastKill.js` 语法通过、`FAST_MS/FAST_TICKS/tick` 已清理；
三个 Java 文件括号配平（`BaseWebViewActivity` 的 224/223 是因 `data.startsWith("{")` 字符串里的 `{` 误报，非真失衡）；
`BaseWebViewActivity` 内已无任何 `killLoadingToastTail/scheduleToastDiag/showToastDiagHud/KILL_PAGE_LOADING_JS` 残留引用；
`injectToastKiller` 在 `yangshipin.cn` 分支与定义处均就位）。

**下一步（待用户实测）**：Clean → Build → 装包。重点看 ① 4 个 tv.cctv.com 入口播放速度是否回到昨晚水平（不再被拖慢）；
② CCTV 直播入口的气泡是否被压住（若仍在，截一张直播页底部的黄条诊断浮层发我，据此定位气泡在同源 iframe / 跨域 iframe / 伪元素）；
③ 确认 4 个入口不再出现任何浮层或红条。

## v4.5.21 (2026-09-16) - 「全力加载中…」定位到同源 iframe：扫描/压制范围扩到 iframe

**背景（v4.5.20 浮层给出的决定性证据）**：央视片库 / 央视栏目入口的浮层显示——
`marks=4`、最后命中 `<div.vjs-loading-spinner...> "正在加载视频播放器。"`、`曾「文本命中」0 个`、
`居中小浮层候选 0`、`shadowRoot=0`、`iframe=1(同源1/跨域0)`。

由此三点定性：
1. 我们一直压中的只是 **video.js 自己的转圈器**（"正在加载视频播放器。"），**根本不是**用户看到的「全力加载中…」；
2. 主文档里**既无居中浮层、也无 shadow** → 气泡在那 **1 个同源 iframe** 里，主文档扫描够不着（也解释了为什么三轮修改"一点反应都没有"）；
3. 另有一个独立问题：**CCTV直播 / 央视网入口的浮层压根没出现**（原生回调没走到那条分支）。

**本次改动**：

- **`js/pageToastKill.js`（系统性重构，document/window 全参数化）**：
  1. **扫描/压制范围扩到「主文档 + 所有同源 iframe 文档」**：新增 `docRoots()`、`sweep()`→`sweepDoc(doc, deep)`；`ensureStyle(doc)` 对每个文档各注入一份 `!important` 焊死规则；MutationObserver 对每个文档各挂一个。跨域 iframe 取 `contentDocument` 会抛，已 try/catch。
  2. **几何判据改用「元素所在文档的视口」**（`winOf(el).innerWidth/innerHeight`），iframe 内也能正确判「居中」。
  3. **诊断浮层钻进 iframe 普查**：新增 `scanDocForDiag()`，浮层显示出 `ifr#N src=... 内: cand/txt/video` 与逐条候选描述。
  4. **历史留痕**（气泡只闪 1~2 秒，400ms 刷新可能刚好错过）：新增 `seenTexts`（曾含「加载」文案）与 `seenCands`（曾居中小浮层），事后可回看。
  5. **`everHits` 覆盖全部三条判据路径**（原先只在候选法留痕，才造成 `marks>0` 却 `曾文本命中 0` 的误读）。
  6. **新增 `AUTO_HUD` 自启开关**（调试期 true）：脚本注入 1.5s 后自动叫出浮层，不依赖原生回调 → 解决 CCTV直播 / 央视网入口浮层不出现的问题。
- **`BaseWebViewActivity.java` / `LiveActivity.java`**：`showToastDiagHud()` 的注入 JS 升级——**若 `__DXTV_TOAST_HUD__` 不存在，则在屏幕底部显示红条「诊断脚本未注入」**，把"浮层坏了"与"脚本没进来"区分开，避免空图误判。
- **`LiveActivity.java`**：`loadLiveUrl()` 里直接调一次 `showToastDiagHud()`，与 `sessionActive && isVideoPage` 分支**解耦**，保证直播入口浮层必出。

**验证（本机无 Android SDK，静态 + 逻辑）**：
- `pageToastKill.js` `node --check` 通过。
- **新增「同源 iframe」行为测试 15/15 通过**（迷你 DOM + 假定时器）：同源 iframe 内的「全力加载中」气泡被压住（class + display:none）；主文档 body/video 与 iframe body 均未误伤；**跨域 iframe 抛错时不崩溃**；DIAG 正确区分同源/跨域并报出 iframe 内的气泡文案；重复扫描幂等（marks 不虚增）。
- 两个 Java 文件花括号/圆括号配平为 0；`showToastDiagHud` 定义各 1 次、调用（Base 1 / Live 2）、`dxtv-hud-missing` 各 2 处、`loadLiveUrl` 接线、JS 导出名与 Java 调用名一致——全部 `ALL_GOOD`。

**实测验证重点**：
1. Clean → Build 装包（`pageToastKill.js` 是 assets，必须重新打包）。
2. **四个入口都应出现底部浮层**；若某入口出现**红条**，说明该页 `pageToastKill.js` 未注入。
3. 直播入口点节目，在**气泡出现那 1~2 秒**截浮层图：气泡消失 = iframe 内压制已生效；仍存在 = 浮层会列出 iframe 内的 `cand`/`txt`，据此精准定位。

## v4.5.20 (2026-09-16) - 诊断改为「页面内可视浮层」：截图即可，不再需要 adb 日志

**背景**：v4.5.19 的探针只往 Logcat 写 JSON，但用户不便取 adb / Logcat 日志，诊断链路卡在"取不到现场"。同时用户提供新证据（10:34 / 10:35 两张截图）：**同一气泡在江苏卫视直播、不同节目里位置完全一致**（画面正中偏上、胶囊尺寸一致）→ 它是一个**挂在视口上、固定居中的播放器级 UI 组件**，不是跟着视频内容缩放的。这类组件若由伪元素 / 图片渲染文案，或封在 Shadow DOM / 跨域 iframe 里，页面脚本就够不着。

**本次改动**：

- **`js/pageToastKill.js`**：
  1. **抽出 `describe(el)`**：把元素压成一行可读文本（`<div.cls> 420x64@(640,300) fixed z=9 "全力加载中…"`），供诊断浮层与 Logcat 共用。
  2. **新增 `everHits` / `noteHit()`**：凡是**文案对得上**的元素（不管最终压没压住）都留一条记录，最多 8 条。这是二分的核心证据——**有记录=看见了却没压住**（力度问题）；**没记录=压根没看见**（气泡在 shadow / 跨域 iframe / 伪元素里，JS 够不着）。
  3. **`mark()` 记录 `lastHitDesc`**：留下"最后压住的到底是哪个元素"。
  4. **`__DXTV_TOAST_DIAG__()` 增补 `hits` 字段**（everHits 前 6 条）；几何判据抽成 `geoRec()` 与浮层共用，避免两处判据漂移。
  5. **新增 `__DXTV_TOAST_HUD__()` 可视诊断浮层**：屏幕底部一条黑底黄字，每 400ms 刷新现场（marks 命中数 / 曾文本命中的元素 / 居中小浮层候选 / shadow·iframe·video 普查），点「× 关闭」收起。**截图即可**，无需 adb。
     - 保活两招：host 预置 `data-cctv-hide`（`cleanPage()` 会跳过带此属性的元素，免得我们自己的浮层被脱壳清掉）；每拍确认节点还在、没被隐藏，被动过手脚立刻纠正。
     - 样式走 `attachShadow` 隔离，不污染央视页面、也不受其 CSS 影响。
     - 重活（几何候选 + shadow/iframe 普查）每 5 拍（≈2 秒）做一次，免得调试浮层本身拖慢页面。
- **`BaseWebViewActivity.java` / `LiveActivity.java`**：新增 `showToastDiagHud()`，在 `onPageLoadFinished` 的视频页分支（即刚进播放页）调用，**延迟 800ms 与 2600ms 各投一次**——注入脚本与本回调谁先谁后不定，投两次保证叫得出来。Logcat 探针 `scheduleToastDiag()` 保留，作为备选。

**验证（本机无 Android SDK，静态 + 逻辑）**：
- `pageToastKill.js` `node --check` 通过。
- **新增 HUD 冒烟测试 18/18 通过**（最小假 DOM + 假时钟）：脚本无异常加载、导出接口存在、调用不抛异常、节点正确挂在 `<html>` 下、预置 `data-cctv-hide`、用了 shadow root、文本含 `TOAST-DIAG`/`marks=`/`文本命中`/`shadowRoot=`/`★判据全扑空`、重复调用不重复建节点、点「关闭」能拆掉、关后可再打开。
- 两个 Java 文件括号/引号配平为 0；`showToastDiagHud` 各"定义 1 次 / 调用 1 次"；两处调用的 JS 接口名与 `pageToastKill.js` 导出名一致。

**诊断用法（用户侧，三步）**：
1. Android Studio **Clean → Build → 装包**（`pageToastKill.js` 已改，必须重新打包）。
2. 进 **CCTV直播入口** 点一个节目 → 页面底部会出现黑底黄字的「DXTV 诊断浮层」。
3. 气泡出现那 1~2 秒**截图**发我即可（不需要 adb / Logcat）。

**怎么读这张截图（二值判断）**：
- **浮层压根没出现** → 说明 `pageToastKill.js` 没注入到那条链路（直播播放页可能换了域名/iframe），问题在注入层，不在判据层。
- **浮层出现、`marks=0`** → 判据全扑空。看 `曾「文本命中」` 有没有记录：有 → 看见了没压住；无 → 气泡在 shadow DOM / 跨域 iframe / 伪元素里，页面脚本够不着，需改走 WebView 层。
- **浮层出现、`marks>0` 但气泡还在** → "清掉被复活"的力度问题。

## v4.5.19 (2026-09-16) - 「全力加载中…」修确定性缺陷 + 加诊断探针（尚未实测）

**背景**：用户确认该气泡来自**央视频**（yangshipin.cn）播放器，是项目初期就有的老问题，对体验影响小，今天顺手收尾。v4.5.18 的常驻压制**尚未编译安装**——此前两次截图（01:54 / 01:56）反映的是 v4.5.17 的表现，不能判定 v4.5.18 无效。

**本次改动（先修已确认的确定性缺陷 + 上诊断，不等实测）**：

- **`js/pageToastKill.js`**：
  1. **几何兜底不再要求"必须有文本"**（原 `geomHit` 写死 `if (!t || t.length > 20) return false`）。若气泡文案由伪元素 `::before/::after`、背景图或 canvas 渲染，`textContent` 为空，原本几何法永不可达——这是 v4.5.17/前版"一丝效果都没有"的高嫌疑根因之一。改为只把"文本过长 (>40 字)"当作误伤大容器的保险。
  2. **深扫预算改为"点名强制必扫"**。原 `deepLeft = 12` 从页面加载就烧，约 7 秒耗尽；而"点节目 → 出画面"本身要 5~9 秒，原生在 `playing` 时连打 8 枪（`killLoadingToastTail`）却因预算归零全成空枪。新增 `deepForce` 标志：原生 `__DXTV_KILL_LOADING__` / `__DXTV_ARM_TOAST_WATCH__` 点名时置位，sweep 强制一次全文档深扫，不被预算卡死；预算放宽到 60 覆盖加载初期。
  3. **新增诊断函数 `__DXTV_TOAST_DIAG__()`**：playing 后约 2.2 秒 dump 现场——`marks`（命中计数，0 即判据全扑空）、居中小浮层候选 `cands`、带 shadowRoot 的宿主 `shadows`、iframe 列表 `iframes`（含是否跨域）、video 归属 `videos`。
- **`BaseWebViewActivity.java` / `LiveActivity.java`**：`startFsPoll()` 判定 `playing` 后新增 `scheduleToastDiag()`，延迟 2.2 秒调用 `__DXTV_TOAST_DIAG__()` 并把结果 `Log.i("DXTV-TOAST", ...)` 打到 Logcat。

**验证（本机无 Android SDK，静态 + 逻辑）**：
- `pageToastKill.js` `node --check` 通过。
- 两个 Java 文件括号/引号配平为 0；`scheduleToastDiag` 各"定义 1 次 / 调用 1 次"。

**诊断用法（用户侧）**：装包后进 CCTV 直播入口点节目，等气泡出现那 1~2 秒，`adb logcat -s DXTV-TOAST`（或 Android Studio Logcat 筛 `DXTV-TOAST`）即可看到现场 JSON。
- `marks: 0` → 判据全扑空，气泡在 **shadow DOM / iframe / 伪元素** 里（H1/H2/H3），按返回里的 `shadows` / `iframes` / `cands` 继续定位；
- `marks > 0` 但仍残留 → 是"清掉被复活"的力度问题（H5），长值守已覆盖，大概率是别处。

**已知边界**：跨域 iframe 内页面脚本够不着，需改走 WebView 层（`shouldInterceptRequest` 改资源 / 评估原生 View 覆盖补丁）。诊断出来再定。

## v4.5.18 (2026-09-16) - 「全力加载中…」改为常驻压制 + 独立注入（v4.5.17 打了空枪）

**背景**：v4.5.17 的清理实测**依旧无效**——CCTV直播入口下，遮罩计时已结束、视频正常播放，播放器自己的「全力加载中…」气泡仍要赖 1~2 秒才消失。

**为什么 v4.5.17 打了空枪（三个原因）**：

1. **只有「打一枪」的能力**。原生在判定 `playing` 那一刻调一次 `__DXTV_KILL_LOADING__()` 就 `stopFsPoll()`；页面侧也只有「脱壳后 5 秒内每 250ms」。而那个气泡归播放器自己管——**清掉后它还会被显示回来**，5 秒窗口一过就没人看着了，正好落在它复活的那段。
2. **「有标记就跳过」会造成假死**。旧实现用 `data-cctv-hide` 标记并据此跳过。播放器恢复显示时往往只是覆盖 `class`（`el.className = 'xxx'`），我们留下的 `data` 属性却还在——于是标记还在、气泡照样亮，再也不会重新压制。
3. **逻辑落在一条会整体短路的分支里**。`js/cctvFullscreen.js` 开头是 `if (window.__DXTV_PAGE_FS__) return`——脱壳主方案 `pageFsBtn.js`（抢着点播放器全屏按钮）一旦先得手，兜底脚本整个不执行，写在它里面的清气泡逻辑自然也没注册上。

**改动**：

- **新增 `js/pageToastKill.js`**：把气泡压制从脱壳链路里**解耦**成独立模块，由原生产线**无条件注入**（不管哪条链路脱壳成功，都在值守）。
  - 三层机制：① **MutationObserver** 抓「新插入」和「被改回可见」（`childList` + `attributes:['style','class','display']`）；② **定时兜底扫**（前 20 秒每 200ms，之后低频长期值守）；③ 命中后打 class `dxtv-kill-toast` + 一条 `!important` 样式规则**焊死**——播放器把内联 `display` 清成 `''` 时这条规则仍然生效。
  - 三条判据：**文案法**（关键词匹配 + 零宽字符归一化，旧的精确 `indexOf` 会被央视文案里夹的零宽字符打空）、**容器内文本法**（不依赖 class 名，专治「气泡躲在播放器容器内部」）、**几何兜底**（居中 + 个头小 + `fixed/absolute` + 无 video，且只在主视频真的在播时启用）。
  - `mark()` 改为**每次都校正**而非「有标记就跳过」，专治上面第 2 条那种假死；同时只在真需要时才改 DOM，避免与观察者互踢成死循环。
  - 护栏：绝不碰 `body`/`html`/`video`/`canvas`；向上收拢遇到 video 或播放器容器即停；文案归一化后超 24 字的容器一律跳过。
- **`js/cctvFullscreen.js`**：删掉 v4.5.17 那份寿命只有 5 秒的 `hideCctvLoadingToast()` / `armLoadingToastWatch()`，改为 `pokeToastKiller()` 戳一下独立模块，避免两份实现漂移。
- **`impl/WebViewClientImpl.java`**：`injectCctvFullscreenPipeline()` 新增 `injectToastKiller()`，**无条件注入** `pageToastKill.js`（首次 + 350ms 补注入各一次，脚本自带幂等保护）。
- **`BaseWebViewActivity.java` / `LiveActivity.java`**：`startFsPoll()` 判定 `playing` 时，把原来的「一枪」换成 `killLoadingToastTail(8)`——每 260ms 打一次、连打 8 次（≈2 秒），正好盖住「遮罩已收、气泡还在」的那段窗口；更长的值守由页面模块接续。

**验证（本机无 Android SDK，静态 + 逻辑验证）**：

- `js/pageToastKill.js`、`js/cctvFullscreen.js` `node --check` 均通过。
- 新增行为测试（真 DOM 结构 + 假时钟 + 假 MutationObserver）**17/17 通过**，关键场景：播放器覆盖 `class` 复活气泡后仍被压住；气泡结构被整块 remove + 重建后新节点同样被压住；文案夹零宽字符正常命中；长正文含「加载中」不误伤；居中浮层（class 与文案都认不出）由几何法兜住；连续节拍稳态不抖动；模拟「收罩后那 1~2 秒」连打 12 拍全程压住。
- 三个 Java 文件括号/引号配平正常；`killLoadingToastTail` 各「定义 1 次 / 递归 1 次 / 调用 1 次」、`injectToastKiller`「定义 1 次 / 调用 2 次」。

**设计取舍**：与 v4.5.17 一致——清掉后保持不显示（自用场景下这个气泡是冗余的，收罩前的等待期由 App 自己的遮罩负责反馈）。

**已知边界**：若该气泡位于**跨域 iframe** 内，页面脚本触达不到（同源 iframe 可以）。若装包后仍有残留，请留意气泡是否紧贴视频画面边缘/在播放器内部，据此判断是否需要改走原生层面处理。

## v4.5.17 (2026-09-16) - 清掉直播播放器残留的「全力加载中…」气泡

**背景**：v4.5.16 装包达标（4 个入口节目 5~9 秒出画面）。遗留一个一直没提的老问题：**CCTV直播入口**下的节目，视频已经正常播放，但播放器自己的「全力加载中…」小气泡还要转 1~2 秒才消失（截图证据）。其他入口没有这个现象。

**定位**：

- 全项目搜 `全力加载中` **零命中**——它不在 App 代码里，是**央视（直播）播放器自己的提示**。
- `js/cctvFullscreen.js` 的 `cleanPage()` 只隐藏「播放器容器**之外**」的元素，容器内部的子元素一律保留（否则会把播放器本身也清掉）。直播播放器的这个气泡是**动态插进容器内部**（或晚于脱壳才出现）的，于是躲过了清理，压在已经开播的画面上。
- 点播播放器的同类提示恰好落在「被清理」的范围内，所以只有直播入口中招——这正是"其他栏目没有"的原因。

**改动**：

- `js/cctvFullscreen.js` 新增 `hideCctvLoadingToast()`：按文本「全力加载中」定位，取「自身没有子元素再含该文案」的**最内层**节点，再向上收拢到「文本只剩这段提示」的**最外层包裹**（也就是那个黑色圆角气泡），置 `display:none` 并打 `data-cctv-hide`（与 `cleanPage()` 同一套幂等约定）。向上收拢有三重护栏：文案超过 24 字停止、遇到含 `video` 的祖先停止、永不越过 `body`。**绝不会误伤播放器容器或 video**。
- 同文件新增 `armLoadingToastWatch(v)`：`makeFs()` 完成即清一次，之后 **5 秒内每 250ms 复查**（气泡常常晚于脱壳才被插进来），并监听 `video` 的 `playing` 事件再清一次。同时把入口挂到 `window.__DXTV_KILL_LOADING__`。
- `BaseWebViewActivity.java` / `LiveActivity.java` 的 `startFsPoll()`：原生判定 `playing` 的那一刻，补一次 `evaluateJavascript` 调 `__DXTV_KILL_LOADING__()`——**原生比页面更早、更准地知道画面出来了**，这是"更准的一击"。

**设计取舍**：清掉后即保持不显示（自用场景下，这个气泡是冗余的；且收罩前的等待期由 App 自己的全屏遮罩负责反馈，不会有"看不到加载状态"的空窗）。若日后希望保留播放器在**后续重新缓冲**时的提示，把 `hideCctvLoadingToast()` 里那句 `data-cctv-hide` 标记去掉即可，只清不锁。

**验证（本机无 Android SDK，仍是静态 + 逻辑验证）**：

- 全项目搜索 `全力加载中`：仅出现在本次新增的 JS 逻辑注释/字符串里，确认原代码中不存在。
- `js/cctvFullscreen.js` `node --check` 通过。
- 从文件里抽出 `hideCctvLoadingToast()`，用假 DOM 跑 6 组共 10 项断言 **10/10**：标准气泡被清；气泡在含 `video` 的播放器容器内时**只清气泡、不碰容器与 video**；文案夹在大段正文里时**不误伤大容器**；页面上无该文案时**零动作**；`...`/`…` 两种省略号都命中；连调三次幂等稳定。
- 两个 Java 文件括号/引号配平通过；`KILL_PAGE_LOADING_JS` 各文件定义 1 次、调用 1 次。

**注意**：仍需 Clean → Build 装包验证。重点看 **CCTV直播入口**点节目：画面出来的同时「全力加载中…」应立即消失，不再有 1~2 秒残留；同时确认其他三个入口（央视片库 / 央视栏目 / 央视网）行为不变。

## v4.5.16 (2026-09-16) - 修「骨架露三次」：拆会话的支线堵死 + 会话内跳转立即盖罩

**背景**：v4.5.14 装包实测（央视片库同一节目）：裸露骨架出现三次，顺序为「专辑页 → 播放页 → 播放器黑屏带大播放键」。截图里有一处决定性证据：**播放页上的遮罩文案是「正在打开节目页… 0.9s」**——该文案只可能出现在「本次会话第一次进央视页」时，而 0.9s 说明计时被重新起算。也就是说：会话在专辑页就被拆掉了，播放页是「另起一次」加载。

**根因（一条主因 + 两处补刀）**：

1. **主因：`onProgressChanged(100)` 把会话拆了**。`WebChromeClient` 里写着 `if (newProgress >= 100) hideLoadingOverlay();`，**没有任何会话判断**；而 `hideLoadingOverlay()` 内部会把 `sessionActive / sessionStartAt / cctvStageCount` 一并清零。于是每一次网页框架加载完（progress=100）都会发生「收遮罩 + 销毁会话」：专辑页一次、播放页一次、视频就绪前再露一次 = 骨架露三次。v4.5.14 说的「会话期间只有 playing / cleaned / 超时三种收罩信号」，被这条支线整个绕过了。
2. **补刀一：v4.5.15 加的 `cancelPendingShow()`**。让「收罩」成为终态本身是对的，但它被 progress 支线在会话内抢先调用时，会把 260ms 后本该弹出的遮罩一并作废——遮罩可能根本不弹，这一步放大了退步。
3. **补刀二：`cleaned` 放行判据过宽**。`makeFs()` 把容器撑满就立刻置 `__DXTV_FS_DONE__`，而此时视频常常还在取流/缓冲；旧判据一见 `cleaned` 就收罩，露出的正是「黑屏 + 大播放键」。`cleaned` 只说明「网页骨架被清掉了」，不等于「画面出来了」。

**改动**：

- `BaseWebViewActivity.java`：`onProgressChanged(100)` 的收罩加 `if (!sessionActive)` 守卫——会话期间绝不因「网页框架到位」收罩，更不允许它销毁会话。收罩权只留给三处：主视频真出画面（`playing`）、脱壳干净且无 video（`cleaned`）、会话超时。
- `BaseWebViewActivity.java`：`onPageLoadStarted` 对央视页改为**立即盖罩**（不再走 260ms 防抖）。防抖只对本地页有意义（本地页几十毫秒加载完，弹罩会闪）；会话内「专辑页 → 播放页」的跳转窗口正是骨架最容易露的地方，必须零延迟。
- `BaseWebViewActivity.java` / `LiveActivity.java`：`hideLoadingOverlayInternal(boolean endSession)` 把「收遮罩」与「结束会话」拆成两件事；新增 `releaseOverlayForBrowsing()`——列表页 `novideo` 放行**只收遮罩、保留会话**，用户随后点进播放页时计时与文案能续上，而不是从 0 重来。
- 同上：`FS_PROBE_JS` 新增 **`clean_wait` 态**——已脱壳（`__DXTV_FS_DONE__`）但主视频仍未出画面时报 `clean_wait`，原生给 **3 秒**（`FS_CLEANED_GRACE_MS`）宽限等首帧，仍不出画面才放行；`cleaned` 收窄为「脱壳干净**且页面里连 video 都没有**」。
- `js/cctvFullscreen.js`：`tryEnterPlayer()` 在按下「立即观看」跳转前打上 `window.__DXTV_ENTERING_PLAYER__ = 1`；`FS_PROBE_JS` 见到该标记一律报 `loading`——专辑页 → 播放页的跳转空档里本页确实没有 video，但绝不能按 `novideo` 放行。

**验证（本机无 Android SDK，做的是静态 + 逻辑验证）**：

- 两个 Java 文件括号/引号配平检查通过，无遗留的无参 `hideLoadingOverlayInternal()` 调用。
- 把 `FS_PROBE_JS` 从 Java 字面量里抽出来，在 Node 里用假 DOM 跑 10 个场景，两个文件各 **10/10** 命中预期状态（专辑页→`novideo`、跳转中→`loading`、缓冲中已脱壳→`clean_wait`、在播→`playing`、小窗在播→`waiting`/`clean_wait` 等）。
- `js/cctvFullscreen.js` `node --check` 通过。

**注意**：仍需 Clean → Build 装包验证。重点看三件事：① 央视片库点同一节目，遮罩**一次连续盖到出画面**，秒数一路累加不归零，文案只按「正在打开节目页… → 正在进入播放页… → 正在加载视频…」推进；② 中途不再出现任何裸骨架或黑屏播放键；③ 若停在真正的栏目/列表页，遮罩约 3.6 秒后放行，且不打断后续计时。

## v4.5.15 (2026-09-16) - 遮罩状态机兜底：收罩必取消「待显示」，杜绝回首页偶发黑遮罩

**背景**：复查 v4.5.14 连续计时链路时发现一处潜在卡死。原本 `hideLoadingOverlay()` 只取消「待收罩」(pendingHide)，没取消「待显示」(pendingShow)。视频流程里 `onPageLoadFinished` 会顺手把 pendingShow 取消掉，所以平时不触发；但只要走 `onProgressChanged(100)` 或 JS `hideLoading` 这两条支线收罩、而 `onPageLoadFinished` 没赶上取消 pendingShow，260ms 后遮罩会被重新弹出来且再无人收——表现为回首页 / 片库 / 栏目页偶尔黑遮罩去不掉。

**改动**：`BaseWebViewActivity.hideLoadingOverlay()` 开头补 `cancelPendingShow()`，让「收罩」成为真正的终态：一旦决定收，任何排队的「待显示」一并作废。（`LiveActivity` 不排 pendingShow，无需改。）

**注意**：属防御性兜底，正常视频链路无行为变化；Clean → Build 后重点确认回本地页不再有偶发遮罩残留。

## v4.5.14 (2026-09-16) - 遮罩改为「一次连续计时」，全程不再露骨架

**背景**：v4.5.13 装包实测（央视片库同一节目三次计时）：节目页 → 播放页 → **第二次计时后露出骨架约 1 秒** → 视频就绪 3~4s。用户诉求：把三段计时合并为一次连续计时，中途不撤罩。

**根因（两处，缺一不可）**：

1. **页面侧有一处"提前收罩"的通知**：`js/load_detail_tv.js` 里有一段轮询，检测到视频在播就调用 `_apiX.msg('hideLoading')` 让原生收遮罩。它用的是 `document.querySelector('video')`——**只取页面里第一个 video 元素**。而央视播放页里常夹着推荐位、广告位的小 video，一旦它满足 `readyState>=2 && currentTime>0.1 && !paused`，就被误判成"主视频已经在播"，于是遮罩在**主视频还在缓冲**时被收掉——正好是"网页框架就绪、画面未到"的空档，骨架裸露约 1 秒。
2. **遮罩按「页面」分段而非按「一次点击」分段**：原实现每次 `onPageLoadStarted` 重置计时、每次 `onPageLoadFinished` 收罩，于是"专辑页 / 播放页 / 视频就绪"是三段独立过程，段与段之间必然出现空档。

**改动**：

- `BaseWebViewActivity.java` / `LiveActivity.java`：引入**连续会话**（`sessionActive` + `sessionStartAt`）。进入视频页（`tv.cctv.com` / `yangshipin.cn`）开启会话，回到本地页（首页/片库/栏目）结束会话；**会话期间 `onPageLoadFinished` 一律不收罩**，只认三种收罩信号：主视频真的在播（`playing`）、页面已脱壳干净（`cleaned`，即 `__DXTV_FS_DONE__` 立住）、会话超时（18s）。
- 同上：遮罩计时改为**整段连续累加**，文案随阶段切换（打开节目页 → 进入播放页 → 加载视频）但**秒数不归零**，实测时看到的就是"从点击到出画面"的总耗时。
- 同上：页面侧来的 `hideLoading` 请求在**会话进行中被忽略**——收罩时机统一由"主视频真的在播"这一条判据决定，从这个意义上堵死了中途撤罩。
- 同上：`FS_PROBE_JS` 新增 **`cleaned` 态**（`window.__DXTV_FS_DONE__` 为真 → 页面只剩播放器本体，露出的是黑画面而不是网页骨架，可以安全收罩）。
- 同上：`novideo` 放行（防列表页被挡死）**只在会话第 1 页生效**；一旦已经进到播放页，就绝不再提前收罩，只等视频或超时。
- `js/load_detail_tv.js`：收罩判据加严——遍历所有 `video`，要求**主视频占据视口 35% 以上面积**且在播，小窗/推荐位/广告位一律不算。

## v4.5.13 (2026-09-16) - 修「裸露骨架」，并确认真正的瓶颈是视频就绪

**背景**：v4.5.12 装包实测（央视片库同一节目）：节目页 0.4s → 播放页 0.5s → **裸露骨架 1~2s** → 视频页 5s。这组数据推翻了上一轮的两个判断：

1. **网页框架根本不慢**（专辑页 0.4s + 播放页 0.5s = 0.9s），真正的大头是**视频就绪要 5 秒**，占了全程的 78%。所以「跳过专辑页」的收益远小于预估。
2. 裸骨架有确切成因，不是玄学：`FS_NOVIDEO_MAX` 原为 5 次，配合 300ms 轮询是 1.5s 窗口；v4.5.12 把轮询间隔缩到 150ms 时**忘了同步改这个次数**，窗口被顺带砍到 0.75s——正好卡在「专辑页 → 播放页」自动跳转的空档上，遮罩先收、跳转未发生，骨架就露出来了。

**改动**：
- `BaseWebViewActivity.java` / `LiveActivity.java`：`FS_NOVIDEO_MAX` 5 → 24（24 × 150ms ≈ 3.6s），足以覆盖一次自动跳转，又不至于把真正的列表页挡死。
- 同上：探测脚本 `FS_PROBE_JS` 新增 **`loading` 态**。原先「没有 video 元素」一律判为「不是播放页」，但播放页在播放器初始化完成前本来就没有 video，于是被误判、提前收罩。现在先查播放器容器（`_video_player` / `.video-player` / `.vjs-player` / `.video-js`）：容器已就位就算 `loading`，继续等，只有连容器都没有才计入 novideo。
- `js/home.js`：直跳播放页改为**默认关闭**（新增 `_DIRECT_PLAY` 开关，置 `true` 可再开）。理由：实测专辑页只占约 0.4 秒，省不了多少；而接口解析失败要白等 700ms 才回退原路，反而更慢。代码保留，想实测对比直接改开关即可。

**结论**：骨架裸露这一项已修。剩下的 5 秒是央视播放器取流 + 缓冲，属于我们控制不了的硬成本——若还要压，只能走「预取 m3u8 直链、用本地播放器播」这条路（工程量大）。

**注意**：需 Clean → Build 装包。重点确认：① 全程不再有裸骨架；② 若停在真正的列表页，遮罩应在约 3.6 秒后放行，不会把人挡死。

---

## v4.5.12 (2026-09-15) - 片库直跳播放页 + 遮罩分段计时

**背景**：v4.5.11 后实测（央视片库同一节目三次：0.6s / 2.8s / 5.4s），遮罩已能撑到视频阶段，但最坏仍要 5.4 秒。查链路发现两处纯白等：① 片库点节目要先加载**央视专辑页**、再由 `cctvFullscreen` 自动点「立即观看」才进播放页——中间那次专辑页是一次完整的央网页加载；② `cctvFullscreen` 是等 800ms 才注入的，而它同时是「专辑页→播放页」跳转的执行者，等于每次点节目都白等 0.8 秒才有人去点按钮。

**改动**：
- `js/cctvideo/home.js`：新增 `playUrlOf(albumId, cb)`，用 `getVideoListByAlbumIdNew` 专辑接口直接解析出「第一集播放页」URL。
- `js/home.js`：`goto()` 在 App 环境下先用 `playUrlOf` 拿播放页 URL **直跳**，跳过专辑页；**700ms 内没解析出来就回退到原专辑页逻辑**，并校验返回 URL 必须是 `tv.cctv.com` 域，避免反而更慢或跳飞。浏览器环境保持原样。
- `impl/WebViewClientImpl.java`：兜底脚本由「延迟 800ms 注入」改为**立即注入** + 350ms 幂等补注（脚本内 `__DXTV_FS_INJECTED__` / `fsMarked` 自带去重）。省去每次跳转的 0.8 秒等待。
- `BaseWebViewActivity.java` / `LiveActivity.java`：遮罩计时由「整段计时」改为**分段计时**——`正在打开节目页…`（第 1 次进央视页）/ `正在进入播放页…`（第 2 次）/ `正在加载视频…`（等视频可播），每段从该阶段起点独立起算。这样实测时看文案和秒数就能定位耗时究竟在哪一段。回本地页（片库/栏目/首页）时阶段计数归零。
- 视频就绪轮询间隔 300ms → 150ms（就绪后最多还要等一个轮询周期才收罩）。

**注意**：需 Clean → Build 装包验证。重点看遮罩文案是否只出现两段（`正在打开节目页…` → `正在加载视频…`），那就说明直跳生效、专辑页已被跳过；若仍出现三段，说明接口没解析出来走了回退，把现象记下来。

---

## v4.5.11 (2026-09-15) - 遮罩改等「视频真的能播」，不再早收 3~4 秒

**背景**：v4.5.10 让遮罩覆盖了全部入口，但实测发现遮罩不到 1 秒就退，网页骨架仍裸露，节目内容还要再等 3~4 秒才出画面。根因是收罩判据用错了——`onPageFinished` 只代表**网页框架**加载完，视频初始化、取流、缓冲都还在后面；`pageFsBtn.js` 的 DOM 黑幕也是到点（1.2 秒）就撤，撤得比视频出画面还早。

**改动**：
- `BaseWebViewActivity.java` / `LiveActivity.java`：收罩判据由「网页加载完」改为「视频真的能播」。新探测脚本 `FS_PROBE_JS` 要求 video 元素 `readyState >= 2`（已有当前帧）、占据视口 35% 以上、且处于播放状态（或 `currentTime > 0`）。
- 探测脚本返回三态，避免无脑干等：`playing` 立即收罩；`novideo`（页面压根没有 video）连续 5 次后收罩——`tv.cctv.com` 下还有栏目页/列表页，那里没视频，若一律等到超时会把用户白挡 12 秒；`waiting`（有视频但未就绪）继续等，最长 12 秒兜底。
- 修掉 v4.5.10 遗留的 bug：`onPageLoadFinished` 里先调了 `cancelPendingShow()`，使紧随其后的 `pendingShowRunnable != null` 判断恒为 false，视频页「强制保底显示」从未生效。现在改为按遮罩是否已显示来判断。
- 遮罩文案随阶段切换：央视页「正在脱壳网页框架… x.xs」→ 视频阶段「正在加载视频… x.xs」，其余入口「正在加载… x.xs」。
- `js/pageFsBtn.js`：DOM 黑幕不再到点就撤（原 `MASK_TIMEOUT` 1200ms 已删除），改为一直盖到「视频真撑满」或整个窗口失败为止——它撤得比视频出画面早，中间正是裸骨架。用户可见的反馈交给上层原生遮罩（有动画和计时），黑幕只负责不露骨架。

**注意**：仍需 Clean → Build 后装包验证。重点确认三处：① 节目是否「遮罩一收就开始播」；② `tv.cctv.com` 的栏目/列表页不会被遮罩白挡（应在约 1.8 秒内放行）；③ 若视频 12 秒仍未播，遮罩会兜底收起，不至于卡死。

---

## v4.5.10 (2026-09-15) - 三个入口补遮罩：央视直播/片库/栏目不再裸露网页骨架

**背景**：遮罩此前只在「央视网」入口出现——只有 `LiveActivity.loadLiveUrl()` 一处调用 `showLoadingOverlay()`。首页 `index.js` 的其余入口都是 `location.href` 直接跳转（央视直播走外链 yangshipin.cn，片库走 `cctv.html`，栏目走 `column.html`），全程没有任何遮罩，加载期间网页骨架裸露。同理，从片库/栏目点节目跳 `tv.cctv.com` 也是裸奔。

**改动**：
- `BaseWebViewActivity.java`：新增 `onPageLoadStarted()` 覆盖，任何一次页面跳转都安排遮罩，一处覆盖全部入口（含外链）。
- `BaseWebViewActivity.java`：遮罩防抖——延迟 260ms 仍未加载完才显示（本地页几十毫秒就完成，避免闪一下），一旦显示至少停留 420ms 再收（避免一闪而过）。遮罩文案按场景区分：央视页「正在脱壳网页框架… x.xs」，其余「正在加载… x.xs」。
- `BaseWebViewActivity.java` / `LiveActivity.java`：央视页新增脱壳轮询 `startFsPoll()`——`onPageFinished` 只代表网页框架到位、脱壳尚未开始，因此每 300ms 检查 `__DXTV_PAGE_FS__` / `__DXTV_FS_DONE__` / `document.fullscreenElement`，立住才收罩，最长 12 秒兜底。这一条对 `LiveActivity` 尤其必要：v4.5.8 把 JS 黑遮罩从 5 秒缩到 1.2 秒后，原生遮罩若仍在加载完成即收，1.2 秒后同样露骨架。
- 两处兜底收罩时长 10000ms → 15000ms，须晚于 12 秒的脱壳轮询，否则央视页还没脱完就被强制收掉。

**注意**：仍需 Clean → Build 后装包验证。重点看两件事：本地页（片库/栏目）切换时遮罩是否自然不闪；央视页遮罩是否覆盖到视频真正出现为止。

---

## v4.5.9 (2026-09-15) - 补漏：直播页「大约需要4秒」改实时计时

**背景**：v4.5.8 只改了 `activity_main.xml`（首页），漏了 `activity_live.xml`（直播/播放页）。而实际脱壳发生在直播页——`LiveActivity` 继承的是 `BaseActivity` 而非 `BaseWebViewActivity`，**自带一套独立的 `showLoadingOverlay`/`hideLoadingOverlay`**，不复用基类逻辑，所以计时改造没覆盖到，实测仍显示写死的「正在脱壳网页框架，大约需要4秒」。速度提升已生效，仅文案未同步。

**改动**：
- `res/layout/activity_live.xml`：硬编码文案「正在脱壳网页框架，大约需要4秒」→「正在脱壳网页框架…」。
- `LiveActivity.java`：新增 `startLoadingTextTick()` / `stopLoadingTextTick()`（与 `BaseWebViewActivity` 同款），`showLoadingOverlay()` 里起表、`hideLoadingOverlay()` 里停表，每 200ms 刷新为「正在脱壳网页框架… 2.3s」；补 `android.os.SystemClock` 与 `java.util.Locale` 导入及 `loadingTickRunnable` / `loadingStartAt` 字段。

**复核**：全项目 `loadingText` 仅存在于 `activity_main.xml` 与 `activity_live.xml` 两处，现已全部改为实时计时，无第三处遗漏。

**注意**：仍需在 Android Studio 里 Clean → Build 后装包验证。

---

## v4.5.8 (2026-09-15) - 脱壳提速：全屏机制去死等 + 实时计时 + 手动全屏按钮

**背景**：遮罩写「正在脱壳网页框架，大约需要4秒」，但脱壳实际由三段串行等待构成——`pageFsBtn.js` 5 秒黑遮罩、兜底 `cctvFullscreen.js` 延迟 5 秒才注入、再最长 20 秒轮询。最坏可超 30 秒，其中 5 秒还是纯黑无反馈死等。因 `__DXTV_PAGE_FS__` 由该链路立起，优化全屏即优化脱壳。

**改动**：
- `js/pageFsBtn.js`（方案A/C）：`MASK_TIMEOUT` 5000→1200，消除 5 秒纯黑屏，改为只在点击瞬间短暂遮罩；`TICK` 400→150 提速探测；`MAX_TRY` 40000→60，收敛为约 9 秒主窗口，超时交兜底；新增原生全屏判定 `document.fullscreenElement`；启动延迟 300→60ms。
- `impl/WebViewClientImpl.java`（方案B/D）：央视外页脱壳注入不再受 `progress==100` 门槛限制；兜底注入延迟 5000ms→800ms；成功判定同时认 `__DXTV_FS_DONE__`。抽出 `injectCctvFullscreenPipeline()`。
- `js/cctvFullscreen.js`（方案C/F）：轮询 500→200ms（`MAX_POLL` 40→150，30 秒窗口不变）；修掉「视频一出现就停止重试」的隐患，改为全屏真正成功或到窗口末尾才停；`makeFs` 成功后补设 `__DXTV_PAGE_FS__`，避免 `detail.js` 再包一层全屏容器造成重复堆叠；新增 `window.__dxtvForceFs()` 强制全屏接口与可遥控聚焦、可点击的「全屏」浮层按钮（自动脱壳失手时手动救场，4 秒后半透明）。
- `activity_main.xml` + `BaseWebViewActivity.java`（方案E）：写死的「大约需要4秒」改为实时计时「正在脱壳网页框架… 2.3s」，每 200ms 刷新，遮罩隐藏即停表。

**注意**：改动横跨 JS 与 Java 两侧，装包前务必 Clean → Build。

---

## v4.5.7 (2026-08-30) - 首页 5 入口图片再缩 20%（12.8vw → 10.24vw）

**改动**：`index.html` 内联 `<style>` 将 `#tv-index-content .tv-item img` 由 `12.8vw` 再缩 20% 至 `10.24vw`（12.8 × 0.8 = 10.24）。累计缩放链：16vw → 12.8vw → 10.24vw。仅作用首页 5 入口图标，**不动全局 `my.css` 的 `.tv-item img`（16vw）**，避免误伤片库（cctv.html）与栏目页（column.html）缩略图。图标保持正方形、成比例缩放不变形。

---

## v4.5.6 (2026-08-30) - 首页 5 入口图片缩小 20%

**改动**：`index.html` 内联 `<style>` 新增 `#tv-index-content .tv-item img { width: 12.8vw; height: 12.8vw; }`（原 16vw，缩 20%）。采用 `#tv-index-content` 作用域精准覆盖，仅作用首页 5 入口图标，**不动全局 `my.css` 的 `.tv-item img`（16vw）**，避免误伤片库（cctv.html）与栏目页（column.html）缩略图。图标保持正方形（16vw → 12.8vw 成比例缩放）。

---

## v4.5.5 (2026-08-29) - 恢复黄历观影祝福模块 + 农历模块与首页 5 入口增大行距

**恢复农历模块**：`index.html` 在 `lunar.js` 之后重新引用 `js/zhufu.js`（黄历观影祝福系统）。该脚本自初始化（监听 `DOMContentLoaded`，依赖 `window.Lunar`），在 `body` 末尾注入 `.almanac-container` 农历模块，无需额外代码改动。

**增大行距**：
- 农历模块（`js/zhufu.js` `almanacStyles`）：`body` 整体 `line-height` 1.3→1.5；`.header/.date-display/.god-positions/.god-item` 1.2→1.6，`.blessing` 1.25→1.7；各区块 `margin` 同步放大（标题 6→12px、日期/祝福 5→10px、吉神 8→14px），容器与上方 5 入口间距 `margin: 0 auto`→`2.5vh auto`。
- 首页 5 入口（`index.html` 内联 `<style>`，仅作用于 `#tv-index-content`）：`.tv-item` 纵向 `margin` 0.8→1.4vh，标签 `span` `line-height` 1.9、`margin-top` 1vh，图标与文字呼吸感更足。

---

## v4.5.4 (2026-08-29) - 栏目页遥控器焦点根治（v-show 换 :style + move-updown-id 静态化 + 直接落焦点）

**问题**：实测「央视栏目」进入任一级节目列表（vods 视图）后，遥控器方向键无反应，鼠标操作正常。

**根因**：
1. `column.js` 使用 `v-show` 切换 cats/vods 两视图，但当前 `vuex.min.js` 构建版不保证支持 `v-show`，导致两视图可能同时渲染、互相干扰；`TvFocus.rescueFocus()` 依 `style.display` 判可见，会误把隐藏视图当可见。
2. `move-updown-id` 用动态绑定 `:move-updown-id="'col-'"`，PetiteVue 编译后可能未落属性，使 `TvFocus.idFound()` 失效。
3. `_focus()` 仅通过 `window.__setFocus` 间接落焦点，若 `PetiteVue` 代理或时序异常，焦点框无法稳定加到 DOM。

**修复**（`js/cctvideo/column.js`）：
- `v-show` 全部替换为 `:style`（cats 视图 `flex/grid`，vods 视图 `none`），与已验证的片库 `js/home.js` 视图切换模式一致，确保 `rescueFocus()` 只扫当前可见内容。
- `move-updown-id="col-"` / `move-updown-id="vod-"` 改为静态属性，彻底规避动态绑定不确定性。
- `_focus()` 改为元素就绪后**直接** `TvFocus.curFocusId = id; TvFocus.applyFocus(id)`，再兜底调用 `__setFocus`；即使 Vue 代理异常也能落焦点。
- 顶部补 `const _ctrlx = {};`，避免 myfocus.js 的 `ok()/menu()` 在 `_ctrlx` 未定义时触发 ReferenceError。

---

## v4.5.3 (2026-08-29) - 片库去综艺空分类 + 栏目页 vods 遥控器焦点修复

**片库去综艺**：`js/cctvideo/home.js` `channels()` 删除 `zy`「综艺」频道（央视接口该分类已无节目），片库从 5 类降为 4 类——电视剧 / 动画片 / 纪录片 / 特别节目。

**栏目页 vods 焦点修复**（`js/cctvideo/column.js`，对齐已验证的片库 `js/home.js` 焦点模式）：
- cols / vods 的 `tv-item` 补 `:move-updown-id`（`'col-'` / `'vod-'`）前缀定位属性，使 `TvFocus.idFound()` 走前缀+数字偏移 O(1) 精确定位，避免 DOM 兄弟遍历在异步渲染场景下的不确定性。
- 重写 `_focus()`：增加 `getElementById(id)` 元素存在性检测，元素未就绪时每 100ms 重试（最多 2.5s），根治 PetiteVue 异步渲染 100 个带图单集卡片完成前调用导致焦点框落空。
- `openColumn()` 切 `view='vods'` 后立即 `_focus('back-cats')` 落焦点到返回栏，消除「进入单集列表空档期」遥控器全失焦。

---

## v4.5.2 (2026-08-29) - 首页新增「央视栏目」单入口（替换特别节目）

**新入口**：首页「特别节目」替换为「央视栏目」（`index.js` `apps()`），图标 `img/lanmu.png`，跳转 `column.html`。入口内分 4 类——新闻 19 / 少儿 18 / 综合 60 / 综艺 48，共 145 个栏目（`js/cctvideo/columns.js`，`window._CCTV_COLUMNS`，含栏目名 + TOPC id）。

**栏目页**（`column.html` + `js/cctvideo/column.js`）：两视图——`cats` 类别清单（4 类标签栏 + 栏目卡片文字块）、`vods` 单集清单（缩略图 `move-updown="5"` 五列）。数据走央视接口 `NewVideo/getVideoListByColumn?id=TOPC...&p=1&n=100&sort=desc&mode=0&serviceId=tvcctv`（PAGE=100 单次拉最新集，不分页）；单集点击 `live.html?guid=...&name=...` 复用现有取流播放链路，无需另建 hls 解析。返回键覆盖 `TvFocus.keyBackEvent`：vods→栏目清单，cats→首页。

**删除特别节目**：`index.js` 删 `PASSWORD_CONFIG` 密码验证与「特别节目」密码分支；删 `tebie.html`、`js/cctv/tebie.js`、`js/cctv/tebie.json`、`img/tebie.png`（已备份至 `_backup_tebie_20260829/`）。

**取消农历观影祝福**：`index.html` 删 `js/zhufu.js` 引用（黄历观影祝福系统下线），保留 `lunar.js`；`zhufu.js` 文件暂留未删，恢复仅需加回一行引用。

---

## v4.5.1 (2026-08-28) - 央视片库分页遥控焦点根治（末页失焦/死链）

**现象**：机顶盒遥控器在央视片库分页区三类病症——① 末行下移到不了「下一页」（仅鼠标可点）；② 进入末页光标停在「下一页」，OK 后四向键全死、仅返回键退主页；③ 列表区内按上键回不到顶部三行（筛选/年代/频道）。

**根因与修复**：
- **末行→下一页断链**（`myfocus.js` `idFound()` line 248）：下行目标不存在时改返 `null`（原返 `elem`），使 `next()` 进入 DOM 兄弟遍历兜底，从末行稳定落到 `tvnext`。网格内下移（foundId 存在）、上移、顶部导航均不变。
- **列表区回不了前三行**（`home.js` line 37）：`move-up` 此前被误改为无 `#` 的 `tv-tv`（jQuery 当标签选择器选不到），复原为 `tvId(item.tag,'#tv-')`（=`#tv-tv` 真实频道行）。上键链路恢复：列表首项→频道行→年代行→筛选行。
- **末页死按钮方案**（`home.js` line 37）：末页「下一页」按钮保留显示、文案切 `{{hasNext(item)?'下一页':'没了'}}`，OK 由 `loadMore` 首行 `noMore` 保护天然无反应，避免隐藏按钮致焦点悬空。
- **病毒包扩散**（`home.js` line 132 新增 `hasNext(item)`）：原「没了」文案绑 `noMore` 持久标志，翻回上一页仍误显「没了」并挡死翻页。改为 `hasNext = (page+1)*PAGE < allVods.length || !noMore`——仅真末屏显示「没了」，其余各页「下一页」职能完好。
- **末页光标落点**（`cctvideo/home.js` `_focusPager` line 152）：翻入末页光标自动落「上一页」（`tvId('prev')`）；第 1 页无「上一页」按钮时（line 186 `page>0` 守卫）改落「下一页」，防悬空死链。
- **翻回被挡死**（`cctvideo/home.js` `loadMore` line 170）：`noMore` 保护由一刀切 `if(noMore) return` 收窄为「本地无下一页 **且** 数据源到底」才 return，本地有数据可正常切片翻回。
- **悬空根治（覆盖一切边角）**（`myfocus.js` `getFocus` line 168 + `rescueFocus` line 176）：按键入口兜底——`curFocusId` 指向元素已不存在即自动转移至当前可见频道的「上一页→下一页→列表项」，根治所有「焦点指向已删元素」场景。

**收官纯洁化**：删除「已经到底了」独立假按钮与 `noMore()` 死方法、`_bak` 备份、过时注释与战役口语；三文件 `node --check` 全过。

**实测结论**：不点末页「下一页」→回路全程畅通；点了偶现「没了」露头（罕见、无害），主公令见好就收、不再修改。

---

## v4.5.0 (2026-08-27) - 央视片库「年代筛选 + 真翻页」

**年代筛选（5年区间，早年合并）**：顶部新增年代栏 `全部|1999前|2000-2004|2005-2009|2010-2014|2015-2019|2020-2024|2025-2026`。央视接口仅支持单年 `year=N`、不支持区间参数（实测 `yearStart/yearEnd`/`fromYear` 等全无效），故区间由前端并发拉该区间各单年（`fc=频道&year=Y`）合并；`1999前` 拉全量后客户端过滤 `year<=1999`。

**真翻页（替换语义，恒一页）**：重构为统一 `allVods` 模型——`channelPage` 重置当前视图，`_loadRemote`（全部视图）远程真分页追加到 `allVods`、`_loadYearRange`（区间视图）合并全区间进 `allVods`；`loadMore`/`prevPage` 用切片（`allVods.slice(page*100,+100)`）显示当前屏，`vods` 恒约一页，切频道/切年代即释放，不跨视图堆积、缓存仅首屏写（不写续页）。

**其他**：`n=200`→`n=100`（接口上限，超量静默截断）；去 `detail` 自动预加载（纯「上一页/下一页」按钮驱动）；序号改全局 `vod.seq`（每屏显示真实全局序号）；加「上一页」按钮（首屏禁用）。

## v4.4.9 (2026-08-27) - 央视片库「下一页」去重：缓存与翻页拆离（方案B）

**根因**：原 `channelPage` 将「首屏缓存秒显」与「翻页远程加载」混在同一函数，且缓存命中分支无条件把缓存整批 push 进 vods、强制 `pageNum=1` 并返回。点「下一页」(`nextPage`) 或滚动触发自动预加载 (`detail`) 时同样调用 `channelPage`，误命中首屏缓存→重复 append 同一批 106 条、`pageNum` 永远锁在 1 → 内容 107=1。实测央视接口 `p` 翻页正常（p=1/2/3 内容各异），问题纯在前端缓存误命中。

**修复（方案B·拆离）**：
- `js/cctvideo/home.js`：拆出 `loadMore()/_loadRemote()`。首屏 `channelPage` 仍走缓存秒显；`nextPage`/`detail` 改调 `loadMore`，**永远走远程 `p=pageNum+1`、不读缓存**，拉真实下一页。
- 写缓存仅限首屏（`pageNum===1`），翻页累积数据不写入，避免下次进页面秒显爆量。
- 清理冗余变量 `channelName`。

---

## v4.4.8 (2026-08-27) - 放弃火锅随喜页机顶盒遥控，改纯点击导航

**放弃机顶盒遥控（回退早前方案）**：`kuxuan.html`/`dsm.html` 移除 `window.__cctvKey` 与 `keydown` 双路遥控代码及 `tv-focus` 焦点样式；`BaseWebViewActivity.dispatchKeyEvent` 回退为仅 `live.html` 派发（火锅随喜页走原生通道，不加载遥控）。机顶盒上这两页无遥控焦点，仅以触屏/鼠标点击交互。

**导航重构（支持我→打赏码→火锅）**：首页「支持我」入口 `index.js` 由 `kuxuan.html` 改为 `dsm.html`（打赏码）。`dsm.html` 新增无 tabindex 的「返回火锅页」触屏入口（→`kuxuan.html`），机顶盒 DPAD 不可聚焦故进不了火锅页。`kuxuan.html` 两按钮名称互换、位置不变：左「去随喜吧」→`dsm.html`，右「再辣点儿哈！」→辣椒特效。

**按钮间距**：`kuxuan.html` 两按钮 `gap` 维持 80px（模拟器已确认合适）。

---

## v4.4.7 (2026-08-27) - 高清不降级 + 代码瘦身 + 进出列表加速

**高清不降级（#77）**：移除 `degradeToSd` 自动降级（原 `error`/`waiting≥8s` 即降标清，机顶盒缓冲常超 8 秒→一进就标清）。现固定 2000 高清直链，永不降级（`live.js` 删 `_quality`/`_autoSd`/`_stallTimer`/`sdUrl` 及 `player.on error/waiting` 降级监听）。

**代码瘦身（#78）**：删 `index.html` 埋点脚本 `js/com/js-sdk-pro.min.js` 及 `index.js` 的 `LA.init` 初始化。注意：`lunar.js`(428KB)+`zhufu.js`(16KB) 为**首页农历/黄历观影祝福模块**所依赖（zhufu.js 用 `window.Lunar`），须保留；`vuex.min.js` 实为 PetiteVue 框架本体，须保留。真实减重约 20KB（仅埋点）。

**进出列表加速（#79）**：`cctv.html` 节目数据 `sessionStorage` 缓存——退出播放 `history.back` 重建页面时秒显缓存，不再等远程 200 条 JSON；后台静默刷新。

---

## v4.4.6 (2026-08-27) - 选集换源 + 面板自动隐藏 + 单一事件源治理

**选集换源成功（核心修复）**：切集改用 `player.switchURL()`（xgplayer-hls 专用切源 API）。
此前 `player.load()` 在已播放态不重载 HLS，导致选集点中后画面不切换（静默失效）。

**面板自动隐藏**：选集换源成功后延迟 3 秒自动隐藏面板（B 案：switchURL 异步、无法确认画面已切换成功，故延迟隐藏），无需返回键。

**单一事件源治理（化繁为简）**：移除全部兜底——
- `BaseWebViewActivity.onBackPressed` 返回拦截
- `live.js` 的 `document keydown` 监听
- `__cctvKey` 内的返回键分支（history.back / closePanel）

仅保留 `MainActivity.dispatchKeyEvent → window.__cctvKey` 单一派发；返回键交回 Android 默认退出播放页。

**交互模型**：播放页唯一焦点「选集」（默认金边高亮）→ OK 弹出面板 → 方向键选集 → OK 换源 → 3 秒后面板自动隐藏。高清优先固定 2000 直链，卡顿临时降 1200（切集即恢复，不再永久锁死）。

**清理**：删除本轮堆叠的 `[道玄][将军]` 考古注释与被注释死代码；修正 `BaseWebViewActivity` 派发注释。

---

## v4.4.5 (2026-08-26) - 选集遥控派发根治

- **根因**：`BaseWebViewActivity.dispatchKeyEvent` 用 `wv.evaluateJavascript("javascript:if(window.__cctvKey)...")` 派发，但 `evaluateJavascript` 不应带 `javascript:` 前缀（那是 `loadUrl` 写法）→ 整条报错、`__cctvKey` 从未被调用。
- **修复**：去掉 `javascript:` 前缀，派发生效；统一单一事件源（仅 Android 层 `dispatchKeyEvent` 经 `__cctvKey` 派发），保留 120ms 同键去抖。

---

## v4.4.4 (2026-08-26) - 清理 live.html 的 utao 残留 common.js 引用

- 删除 `live.html` 对不存在的 `js/common.js` 的 `<script>` 引用（消除 404 报错）。取流走 Java 层 `addJavascriptInterface` 注入的 `window._api`，与 common.js 无关。

---

## v4.4.3 (2026-08-26) - 画质真修复（高清直链）+ 选集去抖

- 高清固定 `toBr(raw,'2000')` 直链（HEAD 200 实测有效），弃用主链自适应；sd 固定 `toBr(raw,'1200')`。
- `window.__cctvKey` 加 120ms 同键去抖，消除系统 WebView 与 Android 层双触发导致的选集乱跳。

---

## v4.4.2 (2026-08-26) - 高清优先修正 + 遥控选集聚焦

- 高清优先用主链交给 hls.js 自适应；仅在 error/缓冲≥8s 时降 1200。
- 暴露 `window.__cctvKey(code)`，由 `dispatchKeyEvent` 直接派发，绕开 X5 不派发 keydown 的坑。

---

## v4.4.1 (2026-08-26) - 播放页遥控链路根治

- 真因：X5 内核默认不把遥控 DPAD/OK 合成网页 keydown，网页监听收不到；且非面板态 `return` 吞掉返回键。
- `BaseWebViewActivity` 新增 `dispatchKeyEvent`，对 `live.html` 页把按键映射为 webCode 经 `evaluateJavascript` 派发；首页不影响。`home.js` 首页改零等待直跳播放页。

---

## v4.4.0 (2026-08-26) - 机顶盒专项：去黄底 + 选集焦点 + 高清优先 + 加速

- 原生布局 WebView 容器黄底改为深蓝 `#0A1F3A`（activity_*.xml + `setBackgroundColor`）。
- 移除 `getFullscreen().request()` 自动全屏，保留「选集」HTML 浮层常驻可见。
- 画质：高清优先 + 卡顿降级；`home.js` 直带 guid 加速进入播放页。

---

## v4.3.9 (2026-08-26) - 选集页内切源（player.load）

- `playEpisode` 改 `player.load(hls)` 页内切源（丝滑不重载）；黄底兜底（务必 Clean→Build 装包）。

## v4.3.8 (2026-08-26) - 切集根治：原生桥取流 + 整页重载

- `live.html` 引 `common.js` 获 `_apiX.getJson` 原生桥取流；切集改整页重载 `live.html?url=` 复用已验证路径。

## v4.3.7 (2026-08-26) - 选集切集不换源修复

- 播放页取流改走原生桥 `window._api.getJson`（带 tv-ref、绕 CORS），替代被 CORS 拦截的裸 XHR。

## v4.3.6 (2026-08-26) - 片库选集下沉播放页（央视网式）

- 列表页去选集层；播放页内「选集」按钮 + 底部面板 + 自动连播下一集。

## v4.3.5 (2026-08-26) - 选集层被 wait 浮层卡住修复

- `_layer.wait()` 生成的浮层 z-index 9999999 盖住选集层；`home.js` 保存 waitId 并 `_layer.close(waitId)`。

## v4.3.4 (2026-08-26) - 栏目扩展：新增「综艺」栏

## v4.3.3 (2026-08-26) - 央视片库选集菜单

## v4.3.2 (2026-08-26) - 清理 utao 残留米黄全局背景（rset.css #FAF9DE→#0a1f3a）

## v4.3.1 (2026-08-26) - 回滚取流改动 + 放弃亮剑修复（个别 CDN 403，非客户端可解）

## v4.3.0 (2026-08-25) - 复活央视片库流畅播放（脱壳→直链）

- `goto()` 加 `site==="cctv"` 分支走 `playCctvVod`；`live.js` 取值 `decodeURIComponent` + 片库 VOD 设 `isLive:false`。

---

## v4.2.5 (2026-08-25) - 移除央视网 1905 两频道（实测无法播放）

## v4.2.3 (2026-08-25) - 历史功能 Java 死支清理（删片库向 History 死法，保留直播记台）

## v4.2.4 (2026-08-25) - 清理 HistoryDao 五孤儿方法

## v4.2.2 (2026-08-25) - 仓库孤儿清理（自循环死簇删除）

## v4.2.1 (2026-08-24) - 特别节目返回流程修复（WebView 历史栈逐级返回优先）

## v4.2.0 (2026-08-24) - 支持我页整治 + 随喜页定型 + 代码清理

## v4.1.0 (2026-08-23) - 启动/退出体验修复 + 频道精简（去 1905、退出对话框整改、dxds.apk）

## v4.0.0 (2026-08-22) - 道玄电视·去 utao 化重构（包名统一、图标替换、五入口重命名、古风水墨）

---

## v1.0.1 (2025-10-03) - TV 遥控器优化（退出对话框焦点导航、按键优先级）

## v1.0.0 (2025-10-03) - 初版：退出对话框 + 启动首页切换 + 智能启动逻辑
