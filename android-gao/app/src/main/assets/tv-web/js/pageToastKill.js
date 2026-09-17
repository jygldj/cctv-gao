/**
 * 网页播放器「全力加载中…」气泡压制器（独立注入 · 轻量事件驱动）
 *
 * 【v4.5.23 收尾清理】气泡已实测修好，本文件删去了整块「诊断浮层 / DIAG」调试代码
 *   （describe / noteHit / everHits / geoRec / scanDocForDiag / __DXTV_TOAST_DIAG__ /
 *    __DXTV_TOAST_HUD__ / AUTO_HUD 等），只保留核心压制逻辑与两个原生点名接口。
 *
 * 适用范围
 *   **只由 WebViewClientImpl 在 onPageFinished 里对 yangshipin.cn（CCTV 直播入口）单独注入**。
 *   tv.cctv.com 的 4 个入口不注入本脚本（那边 v4.5.16 已验证达标，不该背任何额外开销）。
 *
 * 背景
 *   这个提示归播放器自己管：它会压在「已经开播」的画面上 1~2 秒才自己收起。
 *   我们自己的全屏遮罩已经收了，屏幕上就剩它在那儿转，很扎眼。
 *   它和 cleanPage() 也不冲突——cleanPage 只清「播放器容器之外」的元素，
 *   容器内部的这个气泡本来就不该被清（否则连播放器一起清掉了）。
 *
 * 为什么单独一个文件
 *   直播页走的是 yangshipin.cn，与脱壳链路（pageFsBtn.js / cctvFullscreen.js）无关，
 *   所以独立成一个文件、由直播分支单独注入，互不影响。
 *
 * 压制策略（轻量、事件驱动，不拖慢播放）
 *   v4.5.17 曾用「脱壳后 5 秒每 250ms 清一次」——既压不住又拖慢了其它入口，已废弃。
 *   现改为：监听 video 的 playing / canplay 在画面出来后连扫一小串 + MutationObserver 抓
 *   「新插入 / 被改回可见」+ 两个定时深度扫描（body 就绪 / 气泡常冒出的时刻）+ 一条
 *   !important 样式规则焊死。没有「常驻全文档扫描」，对播放零负担。
 *   播放器会反复把气泡显示回来，所以仍靠上述组合 + 样式规则兜底，而不是只打一枪。
 *
 * 安全护栏（宁可漏杀，不可误伤）
 *   · 只处理「最内层」含提示文案的小节点，并向上收拢到那个圆角气泡为止
 *   · 收拢时遇到 video / 播放器容器 / body / html 就停
 *   · 绝不给 body / html / video / canvas / audio 打标记
 *   · 文案归一化后超过 24 字（几何法 40 字）的容器一律跳过（避免误伤整块正文）
 *   · 几何法（兜底）要求「居中小浮层 + fixed/absolute + 无 video」
 */
(function () {
    'use strict';
    if (window.__DXTV_TOASTKILL_INJECTED__) { return; }
    window.__DXTV_TOASTKILL_INJECTED__ = true;

    /* 提示文案关键词。归一化后做子串匹配，所以「全力加载」「加载中」都算命中；
       loading/buffering 兜住换成英文文案的情况。 */
    var KEYS = ['全力加载', '加载中', '正在加载', '缓冲中', '正在缓冲', '请稍候', 'loading', 'buffering'];
    var CLS = 'dxtv-kill-toast';
    var STYLE_ID = 'dxtv-kill-toast-style';
    var MARK = 'data-dxtv-toastkill';

    /* class/id 里带这些词的，是气泡的高概率候选（先用选择器筛，省得全文档遍历） */
    var HINT_SEL = '[class*="loading"],[class*="Loading"],[class*="buffer"],[class*="Buffer"],' +
        '[class*="tip"],[class*="Tip"],[class*="toast"],[class*="Toast"],' +
        '[class*="wait"],[class*="Wait"],[id*="loading"],[id*="Loading"],[id*="buffer"]';

    var deepLeft = 60;      /* 「全文档文本匹配」的次数预算（覆盖加载初期 + 多次点名） */
    var deepForce = false;  /* 原生点名（playing）时强制至少一次深扫，不被预算卡死 */
    var pendingKind = 0;    /* 0 无 / 1 轻量 / 2 深度 */
    var pendingTimer = null;

    /* 去掉零宽字符与全部空白：央视页面文案里夹零宽字符是常事，
       直接 indexOf 精确匹配会莫名落空（v4.5.17 失效的疑似原因之一）。 */
    function norm(s) {
        return String(s == null ? '' : s)
            .replace(/[\u200b-\u200f\u202a-\u202e\u2060\ufeff]/g, '')
            .replace(/[\s\u00a0]+/g, '');
    }

    /* 元素所在文档 / 视口（主文档与 iframe 文档通用） */
    function docOf(el) {
        try { return (el && el.ownerDocument) ? el.ownerDocument : document; } catch (e) { return document; }
    }
    function winOf(el) {
        try {
            var d = docOf(el);
            return (d && d.defaultView) ? d.defaultView : window;
        } catch (e) { return window; }
    }

    function ensureStyle(doc) {
        try {
            if (!doc || !doc.documentElement) { return; }
            if (doc.getElementById(STYLE_ID)) { return; }
            var st = doc.createElement('style');
            st.id = STYLE_ID;
            /* !important 的类规则：播放器把内联 display 清成 '' 时，这条规则仍然生效 */
            st.textContent = '.' + CLS + '{display:none !important;visibility:hidden !important;' +
                'opacity:0 !important;pointer-events:none !important;}';
            (doc.head || doc.documentElement).appendChild(st);
        } catch (e) {}
    }

    /* 所有可访问的文档：主文档 + 同源 iframe 文档（跨域 iframe 取 contentDocument 会抛，忽略） */
    function docRoots() {
        var docs = [document];
        try {
            var ifs = document.querySelectorAll('iframe');
            for (var i = 0; i < ifs.length; i++) {
                try {
                    var d = ifs[i].contentDocument;
                    if (d && d.body) { docs.push(d); }
                } catch (e) {}
            }
        } catch (e2) {}
        return docs;
    }

    function textHit(el) {
        var t = norm(el.textContent);
        if (!t || t.length > 24) { return false; }
        var low = t.toLowerCase();
        for (var i = 0; i < KEYS.length; i++) {
            if (low.indexOf(KEYS[i].toLowerCase()) >= 0) { return true; }
        }
        return false;
    }

    /* 子元素里还有同一个提示 → 说明自己是大容器，不是气泡本体 */
    function hasInnerHit(el) {
        var ch = el.children;
        for (var i = 0; i < ch.length; i++) {
            if (textHit(ch[i])) { return true; }
        }
        return false;
    }

    function forbidden(el) {
        if (!el) { return true; }
        var doc = docOf(el);
        if (el === doc.body || el === doc.documentElement) { return true; }
        var tn = el.tagName;
        if (tn === 'VIDEO' || tn === 'CANVAS' || tn === 'AUDIO') { return true; }
        try {
            if (el.querySelector && el.querySelector('video,canvas')) { return true; }
        } catch (e) { return true; }
        return false;
    }

    /* 从命中的文本节点向上收拢到「只装这段提示」的那个圆角气泡 */
    function wrapperOf(el, container) {
        var doc = docOf(el);
        var top = el;
        var guard = 0;
        while (top.parentNode && top.parentNode !== doc.body && guard < 8) {
            var p = top.parentNode;
            guard++;
            if (p === container) { break; }
            if (p.tagName === 'BODY' || p.tagName === 'HTML') { break; }
            var pt = norm(p.textContent);
            if (!pt || pt.length > 24) { break; }
            try { if (p.querySelector && p.querySelector('video')) { break; } } catch (e) { break; }
            top = p;
        }
        return top;
    }

    /**
     * 打标记 + 焊死。
     *
     * 刻意「每次都校正一遍」，而不是「有标记就直接跳过」：播放器恢复显示时往往只是
     * 覆盖 class（el.className = 'xxx'），我们留下的 data 属性却还在——若据此跳过，
     * 就成了「标记还在、气泡照样亮」的假死。所以每次都要确认 class 与内联 display
     * 仍在压着。同时只在真的需要时才改，免得和 MutationObserver 互相踢（改属性 →
     * 触发观察 → 又改 → 死循环）。
     */
    function mark(el, why) {
        if (forbidden(el)) { return false; }
        try {
            /* 只在首次写入：之后每轮只是「校正 class / display」，不再动属性，
               免得稳态下每拍都改一次 DOM。 */
            if (!el.getAttribute(MARK)) { el.setAttribute(MARK, why || '1'); }
        } catch (e0) { return false; }

        var hasCls = false;
        try { hasCls = !!(el.classList && el.classList.contains(CLS)); } catch (e1) {}
        if (!hasCls) {
            try { el.classList.add(CLS); } catch (e2) {
                try {
                    var cur = el.getAttribute('class') || '';
                    if (cur.indexOf(CLS) < 0) { el.setAttribute('class', cur + ' ' + CLS); }
                } catch (e3) {}
            }
        }

        var off = false;
        try {
            off = !!(el.style && el.style.getPropertyValue &&
                el.style.getPropertyValue('display') === 'none');
        } catch (e4) {}
        if (!off) {
            try { el.style.setProperty('display', 'none', 'important'); } catch (e5) {}
        }
        return true;
    }

    /* 已压住且播放器没动过 → 稳态，直接跳过，省掉反复取值 */
    function stillKilled(el) {
        try {
            if (!el.getAttribute(MARK)) { return false; }
            if (!(el.classList && el.classList.contains(CLS))) { return false; }
            if (!(el.style && el.style.getPropertyValue &&
                el.style.getPropertyValue('display') === 'none')) { return false; }
            return true;
        } catch (e) { return false; }
    }

    /* 几何兜底：所在视口正中、个头很小、浮层定位、里面没有 video ——
       这样的东西除了加载提示没别的。注意 vw/vh 取自元素所在文档的视口，
       所以在 iframe 里也能正确判断「居中」。 */
    function geomHit(el) {
        try {
            if (forbidden(el)) { return false; }
            if (el.children.length > 8) { return false; }
            /* 注意：文案若由「伪元素 ::before/::after / 背景图 / canvas」渲染，
               textContent 会是空的——所以「有文本」不能再当必要条件，否则几何兜底
               永远碰不到这种气泡。这里只把「文本过长」当作误伤大容器的保险。 */
            var t = norm(el.textContent);
            if (t && t.length > 40) { return false; }
            var r = el.getBoundingClientRect();
            if (r.width <= 0 || r.height <= 0) { return false; }
            var win = winOf(el);
            var vw = win.innerWidth || 1, vh = win.innerHeight || 1;
            if (r.width > vw * 0.5 || r.height > 130 || r.height < 14) { return false; }
            if (Math.abs((r.left + r.width / 2) - vw / 2) > vw * 0.2) { return false; }
            if (Math.abs((r.top + r.height / 2) - vh / 2) > vh * 0.2) { return false; }
            var cs = win.getComputedStyle ? win.getComputedStyle(el) : null;
            if (!cs) { return false; }
            if (cs.position !== 'fixed' && cs.position !== 'absolute') { return false; }
            if (cs.display === 'none' || cs.visibility === 'hidden') { return false; }
            return true;
        } catch (e) { return false; }
    }

    /* 对「一个文档」做一轮扫描与压制（主文档 / iframe 文档通用） */
    function sweepDoc(doc, deep) {
        try {
            if (!doc || !doc.body) { return; }
            ensureStyle(doc);
            var container = null;
            try { container = doc.querySelector('[data-cctv-container="1"]'); } catch (eC) {}
            var i, el;

            /* ① 候选法：class/id 带关键词的浮层，用「文案或几何」二者之一确认。
                  开销小，可以高频跑。 */
            var hints;
            try { hints = doc.body.querySelectorAll(HINT_SEL); } catch (e1) { hints = []; }
            for (i = 0; i < hints.length; i++) {
                el = hints[i];
                if (stillKilled(el)) { continue; }
                if (forbidden(el)) { continue; }
                if (textHit(el) && !hasInnerHit(el)) {
                    mark(wrapperOf(el, container), 'hint-text');
                    continue;
                }
                if (geomHit(el)) { mark(el, 'hint-geom'); }
            }

            /* ② 容器内文本法：气泡躲在播放器容器内部时，这一步永远能覆盖到，
                  不依赖它的 class 名好不好认。范围小，成本低。 */
            if (container) {
                var inner;
                try { inner = container.querySelectorAll('div,span,p,i,em,b,strong,label'); } catch (e3) { inner = []; }
                for (i = 0; i < inner.length; i++) {
                    el = inner[i];
                    if (stillKilled(el)) { continue; }
                    if (!textHit(el) || hasInnerHit(el)) { continue; }
                    mark(wrapperOf(el, container), 'inner-text');
                }
            }

            /* ③ 全文档文本法：最全但最贵。日常只在加载初期按预算跑；
                  原生点名（playing）会置 deepForce 强制一次，避免「点名也打空枪」。
                  点名时气泡常已躲过轻量路径，必须深扫兜底。 */
            if (!deep) { return; }
            if (deepForce) { deepForce = false; }
            else if (deepLeft <= 0) { return; }
            else { deepLeft--; }
            var cands;
            try { cands = doc.body.querySelectorAll('div,span,p,i,em,b,strong,label,section'); } catch (e5) { cands = []; }
            for (i = 0; i < cands.length; i++) {
                el = cands[i];
                if (stillKilled(el)) { continue; }
                if (el.tagName === 'A') { continue; }
                if (!textHit(el) || hasInnerHit(el)) { continue; }
                mark(wrapperOf(el, container), 'deep-text');
            }
        } catch (e7) {}
    }

    function sweep(deep) {
        var docs = docRoots();
        for (var d = 0; d < docs.length; d++) { sweepDoc(docs[d], deep); }
    }

    function schedule(deep, delay) {
        var kind = deep ? 2 : 1;
        if (kind > pendingKind) { pendingKind = kind; }
        if (pendingTimer) { return; }
        pendingTimer = setTimeout(function () {
            var k = pendingKind;
            pendingKind = 0;
            pendingTimer = null;
            sweep(k >= 2);
        }, delay == null ? 100 : delay);
    }

    /* 给某文档里所有 video 挂「开始播放」监听：画面一出就连扫一小串，盖住
       「遮罩收了、气泡还赖 1~2 秒」的窗口。 */
    function attachVideoWatch(doc) {
        try {
            var vs = doc.querySelectorAll('video');
            for (var i = 0; i < vs.length; i++) {
                (function (v) {
                    try {
                        v.addEventListener('playing', function () { burst(true, 6, 300); });
                        v.addEventListener('canplay', function () { schedule(true, 0); });
                        if (!v.paused) { schedule(true, 0); }
                    } catch (eA) {}
                })(vs[i]);
            }
        } catch (eB) {}
    }

    /* 连扫 count 次、间隔 gap 毫秒（不依赖 schedule 的合并，保证真打出一串）。 */
    function burst(deep, count, gap) {
        var n = 0;
        (function step() {
            sweep(deep);
            n++;
            if (n < count) { setTimeout(step, gap); }
        })();
    }

    function start() {
        /* 不再常驻全文档扫描（那会拖慢播放）。改为事件驱动：
             ① 两个定时深度扫描：body 就绪后、以及气泡常冒出的时刻；
             ② video 的 playing / canplay：画面一出就连扫一小串盖住那段窗口；
             ③ MutationObserver：只在 DOM 真变化时轻扫一次（空闲零开销）。
           原生 playing 分支的点名（__DXTV_KILL_LOADING__）仍会强制深扫，无需常驻。 */
        schedule(true, 800);
        schedule(true, 2500);

        try { attachVideoWatch(document); } catch (e0) {}
        try {
            var ifs = document.querySelectorAll('iframe');
            for (var d = 0; d < ifs.length; d++) {
                try {
                    var dd = ifs[d].contentDocument;
                    if (dd) { attachVideoWatch(dd); }
                } catch (e1) {}
            }
        } catch (e2) {}

        /* 直播页可能晚些才出现 iframe，1.5 秒后再挂一次 iframe 内的视频监听 */
        try {
            setTimeout(function () {
                try {
                    var ifs2 = document.querySelectorAll('iframe');
                    for (var k = 0; k < ifs2.length; k++) {
                        try {
                            var dd2 = ifs2[k].contentDocument;
                            if (dd2) { attachVideoWatch(dd2); }
                        } catch (eI) {}
                    }
                } catch (eT) {}
            }, 1500);
        } catch (e4) {}

        try {
            if (typeof MutationObserver !== 'undefined') {
                var docs = docRoots();
                for (var d2 = 0; d2 < docs.length; d2++) {
                    try {
                        if (!docs[d2].body) { continue; }
                        var mo = new MutationObserver(function () { schedule(false, 80); });
                        mo.observe(docs[d2].body, {
                            childList: true,
                            subtree: true,
                            attributes: true,
                            attributeFilter: ['style', 'class', 'display']
                        });
                    } catch (eM) {}
                }
            }
        } catch (e3) {}
    }

    /* 原生接口：startFsPoll 判定 playing 时点名调用（比页面事件更早、更准）。
       每次点名都强制一次全文档深扫——否则点名时刻预算已耗尽，八枪全是空枪。 */
    try { window.__DXTV_KILL_LOADING__ = function () { deepForce = true; schedule(true, 0); }; } catch (e3) {}
    try { window.__DXTV_ARM_TOAST_WATCH__ = function () { deepForce = true; schedule(true, 0); }; } catch (e4) {}

    if (document.body) {
        start();
    } else {
        try { document.addEventListener('DOMContentLoaded', start); } catch (e5) {}
        try { setTimeout(start, 300); } catch (e6) {}
    }
})();
