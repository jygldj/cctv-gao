(function () {
    'use strict';
    if (window.__DXTV_PAGEFS_INJECTED__) { return; }
    window.__DXTV_PAGEFS_INJECTED__ = true;

    var SELECTORS = [
        '.videoFull',
        '.y-full',
        '.vjs-fullscreen-control',
        '.dplayer-full',
        '.dplayer-full-icon',
        '.prism-fullscreen-btn',
        '.xgplayer-fullscreen',
        '.xg-icon-fullscreen',
        '.fullscreen-btn',
        '.btn-fullscreen',
        '.icon-fullscreen',
        '.tv-full',
        '.tvFull',
        '.video-full',
        '[title*="全屏"]',
        '[aria-label*="全屏"]'
    ];
    var LOOSE = ['[class*="fullscreen"]', '[class*="full-screen"]', '[class*="fullScreen"]'];

    var MAX_TRY = 60;         /* 60 × 150ms ≈ 9s 主方案窗口，超时交给兜底接管 */
    var TICK = 150;           /* 400ms → 150ms，更快捕捉到全屏时机 */
    /* 遮罩策略：黑幕一直盖到「视频真的撑满」为止。
       此前到点就撤，回收得比视频出画面早 3~4 秒，中间就是裸骨架。
       反馈交给上层原生遮罩（有动画和计时），黑幕只负责别露骨架。 */
    var MAX_PER_EL = 3;
    var GROW_RATIO = 1.3;

    var elapsed = 0;
    var tryMap = [];
    var clickedCount = 0;
    var baseArea = 0;
    var done = false;
    var overlay = null;

    window.__DXTV_PAGEFS_REPORT__ = { clicks: 0, fs: false, note: 'init' };

    function vw() { return window.innerWidth || document.documentElement.clientWidth || 0; }
    function vh() { return window.innerHeight || document.documentElement.clientHeight || 0; }

    function showMask() {
        if (overlay || done) return;
        try {
            overlay = document.createElement('div');
            overlay.setAttribute('data-dxtv-mask', '1');
            overlay.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;' +
                'z-index:2147483646;background:#000;';
            document.body.appendChild(overlay);
        } catch (e) {
            overlay = null;
        }
    }
    function hideMask() {
        if (!overlay) return;
        try { if (overlay.parentNode) overlay.parentNode.removeChild(overlay); } catch (e) {}
        overlay = null;
    }

    function usable(el) {
        if (!el || el === document.body || el === document.documentElement) return false;
        if (el.tagName === 'VIDEO') return false;
        if (el.querySelector && el.querySelector('video')) return false;
        var r = el.getBoundingClientRect();
        if (!r || r.width <= 0 || r.height <= 0) return false;
        if (r.width > vw() * 0.45 && r.height > vh() * 0.45) return false;
        var st = window.getComputedStyle ? window.getComputedStyle(el) : null;
        if (st && (st.display === 'none' || st.visibility === 'hidden')) return false;
        return true;
    }

    function triesOf(el) {
        for (var i = 0; i < tryMap.length; i++) { if (tryMap[i].el === el) return tryMap[i].n; }
        return 0;
    }
    function mark(el) {
        for (var i = 0; i < tryMap.length; i++) {
            if (tryMap[i].el === el) { tryMap[i].n++; return tryMap[i].n; }
        }
        tryMap.push({ el: el, n: 1 });
        return 1;
    }
    function scan(list) {
        for (var i = 0; i < list.length; i++) {
            var nodes;
            try { nodes = document.querySelectorAll(list[i]); } catch (e) { nodes = []; }
            for (var j = 0; j < nodes.length; j++) {
                var el = nodes[j];
                if (triesOf(el) >= MAX_PER_EL) continue;
                if (usable(el)) return el;
            }
        }
        return null;
    }
    function pick() { return scan(SELECTORS) || scan(LOOSE); }

    function videoArea() {
        try {
            var v = document.querySelector('video');
            if (!v) return 0;
            var r = v.getBoundingClientRect();
            return (r.width || 0) * (r.height || 0);
        } catch (e) { return 0; }
    }

    function fire(el) {
        if (clickedCount === 0) { baseArea = videoArea(); }
        var n = mark(el);
        clickedCount++;
        try { el.click(); } catch (e) {
            try {
                var ev = document.createEvent('HTMLEvents');
                ev.initEvent('click', true, true);
                el.dispatchEvent(ev);
            } catch (e2) {}
        }
        try {
            window.__DXTV_PAGEFS_REPORT__.clicks = clickedCount;
            window.__DXTV_PAGEFS_REPORT__.note = 'clicked:' + (el.className || el.tagName);
        } catch (e3) {}
    }

    function fsOk() {
        if (document.fullscreenElement || document.webkitFullscreenElement) return true;
        var a = videoArea();
        if (baseArea > 0) return a >= baseArea * GROW_RATIO;
        var r = null;
        try { r = document.querySelector('video').getBoundingClientRect(); } catch (e) {}
        return !!r && r.width >= vw() * 0.9 && r.height >= vh() * 0.7;
    }

    function finish(ok, note) {
        done = true;
        hideMask();
        try {
            window.__DXTV_PAGEFS_REPORT__.fs = !!ok;
            window.__DXTV_PAGEFS_REPORT__.note = note;
        } catch (e) {}
        if (ok) {
            window.__DXTV_PAGE_FS__ = true;
        }
    }

    function tick() {
        if (done) return;
        if (window.__DXTV_FS_DONE__) { finish(false, 'cctvFullscreen-already'); return; }

        elapsed += TICK;
        if (elapsed >= MAX_TRY) { finish(false, 'timeout'); return; }
        /* 一直盖着，撤罩只发生在 finish()（视频撑满/原生全屏/窗口失败） */
        if (!overlay) { showMask(); }

        /* 成功判定：原生全屏已生效，或已点击且视频确实长大 */
        if (document.fullscreenElement || document.webkitFullscreenElement) {
            finish(true, 'native-fullscreen');
            return;
        }
        if (clickedCount > 0 && fsOk()) { finish(true, 'video-grown'); return; }

        var el = pick();
        if (el) { fire(el); }
        setTimeout(tick, TICK);
    }

    showMask();
    setTimeout(tick, 60);
})();
