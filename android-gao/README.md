# 道玄电视 Android 项目（android-x5）

## 项目简介
基于腾讯 X5（TBS）内核的 Android TV 应用，使用 Java 开发，采用 DataBinding 架构。
提供电视频道（央视网）、直播（央视频）、央视片库点播，以及「特别节目」「支持我」等 Web 内容入口。
应用名：**道玄电视**，包名：`com.daoxuan.cctv`，当前版本：**道玄电视 4.0**。

> 注：Java 源码包名仍沿用历史命名 `tv.utao.x5`（未做包名重构），但对外品牌、资源、文档已全部去「utao / 油桃」化。

## 技术栈
- **语言**：Java
- **架构**：DataBinding
- **WebView**：腾讯 X5（TBS）内核
- **平台**：Android TV（横屏强制 `SCREEN_ORIENTATION_LANDSCAPE`）
- **前端壳**：`assets/tv-web/`（PetiteVue 单页壳 + 原生 JS 入口）

## 项目结构

```
app/src/main/
├── java/tv/utao/x5/            # 源码包（历史命名，未重构）
│   ├── StartActivity.java      # 启动页（初始化 X5、按设置跳转）
│   ├── MainActivity.java       # 主页面（Web 视频点播）
│   ├── LiveActivity.java       # 直播页面
│   ├── BaseActivity.java       # Activity 基类
│   ├── BaseWebViewActivity.java# WebView Activity 基类
│   ├── adapter/ api/ dao/ domain/ impl/ service/ util/ utils/
├── res/
│   ├── layout/                 # 布局（activity_start/main/live、dialog_exit）
│   ├── values/                 # strings / colors / dimens
│   ├── drawable/ drawable-v24/ # 图片与 banner
│   ├── xml/                    # backup_rules / data_extraction_rules
│   └── mipmap-xxxhdpi/         # 启动图标（dxds 完整图标）
└── assets/
    └── tv-web/                 # Web 前端（index.html + js/css/img）
```

## 启动图标方案（重要）
- **唯一密度目录**：`res/mipmap-xxxhdpi/`，仅保留 `ic_launcher.webp` 与 `ic_launcher_round.webp`（均为 `dxds.webp` 完整方图）。
- **已移除** `mipmap-anydpi-v26/` 自适应图标壳：dxds 为不透明完整图标，进自适应壳会被系统遮罩裁切/叠色；移除后 Manifest 直接引用完整图，桌面原样清晰显示。
- Manifest 引用：`android:icon="@mipmap/ic_launcher"`、`android:roundIcon="@mipmap/ic_launcher_round"`。
- 已删除 `mipmap-hdpi / mdpi / xhdpi / xxhdpi` 冗余密度目录，与 sgxxs-app 范本对齐。

## 前端 tv-web 入口（assets/tv-web/index.html）
首页已去四 tab 化，直接渲染主功能五入口（apps）；「历史」功能已从央视片库删除（v4.2），「随喜」不再是顶部导航栏，而是由**支持我页内「去随喜吧」按钮**跳转至 `img/dsm.html`（深墨蓝+金黄打赏码页，含微信/支付宝二维码）。

主功能五入口（apps）：
1. **央视网**（原「电视频道」）
2. **央视频**（原「CCTV 直播」，链接 yangshipin.cn）
3. **央视片库**（未改名）
4. **特别节目**（tebie.html）
5. **支持我**（kuxuan.html，含公众号「道玄文集」；页内「去随喜吧」→ `img/dsm.html`）

> 注：原「1905电影 / 1905国际电影」两频道因在机顶盒端无法播放，已从 `tv-web/js/cctv/tv.yml` 与 `tv2.yml` 中移除。

风格：暗黑金边（背景 #121212 + 文字 #e0e0e0 + 边框/强调 #D4AF37），含农历全局浮层（lunar.js / zhufu.js）。

## 核心页面说明
### 1. StartActivity（启动页）
- 应用入口，初始化并安装 X5 内核
- 按 SharedPreferences 中「启动首页」设置跳转 Main / Live

### 2. MainActivity（主页面）
- WebView 加载视频点播页
- 遥控器按键控制、菜单（选集/画质/倍速）、返回退出对话框

### 3. LiveActivity（直播页面）
- 电视直播、上下左右快速切台、频道列表、返回退出对话框

## 开发规范
### 1. DataBinding
- 所有 Activity 继承 BaseActivity
- `DataBindingUtil.setContentView` 绑定布局，布局根 `<layout>` + `<data>` 定义变量

### 2. 按键处理
- 重写 `dispatchKeyEvent`，区分 ACTION_DOWN / ACTION_UP
- 返回键弹退出对话框（dialog_exit.xml，右侧 1/3 屏宽）
- 退出对话框左侧公告区（打赏二维码 + 「第一时间获取新版上线通知」）已整体移除；右侧提示下新增《增删卜易•六爻占卜系统》可点击链接（https://dxzsby.pages.dev）

### 3. 数据存储
- `ValueUtil.putString / getString`（SharedPreferences 封装）

### 4. 日志 / Toast
- `LogUtil.i/e`；`ToastUtils.show`

## 编译与运行
### 环境
- Android Studio Arctic Fox+
- JDK 8+，Android SDK API 21+，Gradle 7.0+

### 打包
```bash
./gradlew assembleDebug      # 调试
./gradlew assembleRelease    # 发布（需 zsby-release.jks 签名）
```

## 注意事项
1. **X5 内核**：首次启动需下载安装内核文件
2. **横屏强制**：所有 Activity 锁定横屏
3. **SingleTask**：主 Activity `singleTask` 防重复创建
4. **焦点处理**：TV 应用务必保证遥控器导航
5. **资源备份**：`res/` 内禁止保留下划线前缀目录（如 `_backup_*`），AAPT 虽忽略但污染构建；备份统一迁至工程外 `F:/github-dx/_backup_android-x5_*/`
6. **启动/退出抢镜修复**：`index.html` 内联 `.tv-tab{display:none}` 兜底，Vue 接管前隐藏全部频道区块，根治 PetiteVue 异步挂载空窗期「随喜/太极」闪现与退出弹窗透出。勿删此规则。

## 打包产物命名
- `app/build.gradle` 中 `outputFileName = "dxds.apk"`，Release 构建输出固定为 `dxds.apk`（原 `daoxuan-tv-<version>.apk` 已弃用）。

## 更新日志
详见 [CHANGELOG.md](./CHANGELOG.md)。
当前最新：**v4.5.1（2026-08-28）央视片库分页遥控焦点根治（末页失焦/死链）**。

## 许可证
请遵守相关法律法规使用本项目。

## 联系方式
公众号「道玄文集」：https://mp.weixin.qq.com/mp/appmsgalbum?__biz=Mzk0ODc1MzcxMg==&action=getalbum&album_id=4070727578921992197
官网：https://dxzsby.pages.dev
