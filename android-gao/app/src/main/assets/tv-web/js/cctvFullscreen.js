(function () {
    'use strict';
    if (window.__DXTV_PAGE_FS__) {
        return;
    }
    if (window.__DXTV_FS_INJECTED__) { return; }
    window.__DXTV_FS_INJECTED__ = true;
    var _autoNavigated = false;
    var _menuLoading = false;

    function isPlayerPage() {
        return !!(document.querySelector('video') || document.getElementById('_video_player') || document.querySelector('.video-player'));
    }

    function tryEnterPlayer() {
        if (_autoNavigated || isPlayerPage()) return false;
        var ljgk = document.querySelector('.ljgk a');
        if (ljgk && ljgk.href) {
            _autoNavigated = true;
            /* 告诉原生「专辑页即将跳播放页」：这段空档里原生遮罩不要因为「本页没有 video」而放行，
               否则专辑页到播放页之间会露出网页骨架（实测裸骨架的窗口之一）。 */
            try { window.__DXTV_ENTERING_PLAYER__ = 1; } catch (e) {}
            window.location.href = ljgk.href;
            return true;
        }
        var all = document.querySelectorAll('a,button');
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            if (el.getAttribute('data-cctv-clicked')) continue;
            var txt = (el.textContent || '').trim();
            if (txt === '立即观看' || txt.indexOf('立即观看') >= 0) {
                el.setAttribute('data-cctv-clicked', '1');
                if (el.tagName === 'A' && el.href && el.href !== '#') {
                    _autoNavigated = true;
                    try { window.__DXTV_ENTERING_PLAYER__ = 1; } catch (e2) {}
                    window.location.href = el.href;
                } else {
                    el.click();
                }
                return true;
            }
        }
        var selectors = ['.play_btn', '.play-btn', '.watch_btn', '.watch-btn', '.btn_play', '.btn-play', '.btn_watch', '.btn-watch', '.player_btn', '.player-btn', '.d_player', '[class*="ljgk"]', '[class*="立即观看"]'];
        for (var j = 0; j < selectors.length; j++) {
            var e = document.querySelector(selectors[j]);
            if (e && !e.getAttribute('data-cctv-clicked')) {
                e.setAttribute('data-cctv-clicked', '1');
                e.click();
                return true;
            }
        }
        return false;
    }

    function fsMarked(v) { return v && v.getAttribute('data-cctv-fs'); }

    function closestEl(el, selector) {
        if (!el) return null;
        try { if (el.matches && el.matches(selector)) return el; } catch (e) {}
        var p = el.parentNode;
        while (p && p !== document) {
            try { if (p.matches && p.matches(selector)) return p; } catch (e) {}
            p = p.parentNode;
        }
        return null;
    }

    function findPlayerContainer(v) {
        var chain = ['#_video_player', '.video-player', '.vjs-player', '.video-js', '.player-con', '.player', '#player'];
        for (var i = 0; i < chain.length; i++) {
            var el = closestEl(v, chain[i]);
            if (el) return el;
        }
        var p = v.parentNode;
        while (p && p !== document.body) {
            if (p.tagName === 'DIV' && p.children.length <= 3) {
                return p;
            }
            p = p.parentNode;
        }
        return v.parentNode;
    }

    function pickVideo() {
        var vs = document.querySelectorAll('video');
        if (!vs || vs.length === 0) return null;
        var best = null, bestArea = 0;
        for (var i = 0; i < vs.length; i++) {
            var v = vs[i];
            if (fsMarked(v)) continue;
            var r = v.getBoundingClientRect();
            var area = (r.width || 0) * (r.height || 0);
            if (area <= 0) continue;
            if (area > bestArea) { bestArea = area; best = v; }
        }
        if (best) return best;
        for (var j = 0; j < vs.length; j++) { if (!fsMarked(vs[j])) return vs[j]; }
        return null;
    }

    /* 播放器「全力加载中…」气泡的压制已完全移出本文件（v4.5.22 起）：
       由 WebViewClientImpl 在 onPageFinished 里**只对 yangshipin.cn（直播）**单独注入
       js/pageToastKill.js 负责。tv.cctv.com 这 4 个入口不再注入任何气泡脚本，
       所以这里也不再需要"戳一下压制器"。 */

    function cleanPage(v, container) {
        if (document.body.getAttribute('data-cctv-cleaned')) return;
        document.body.setAttribute('data-cctv-cleaned', '1');
        var xjBtn = document.getElementById('cctv-fs-xj');
        function shouldKeep(el) {
            if (el === document.documentElement || el === document.body || el === document.head) return true;
            if (el === v || el === container) return true;
            if (xjBtn && (el === xjBtn || xjBtn.contains(el))) return true;
            var p = container ? container.parentNode : (v ? v.parentNode : null);
            while (p && p !== document.body) {
                if (p === el) return true;
                p = p.parentNode;
            }
            return false;
        }
        var all = document.querySelectorAll('*');
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            if (shouldKeep(el)) continue;
            if (el.style.display === 'none') continue;
            if (el.getAttribute('data-cctv-hide')) continue;
            el.setAttribute('data-cctv-hide', '1');
            el.style.display = 'none';
        }
    }

    function makeFs(v) {
        if (!v || fsMarked(v)) return false;
        v.setAttribute('data-cctv-fs', '1');
        v.setAttribute('x5-video-player-type', 'h5');
        v.setAttribute('x5-video-player-fullscreen', 'true');
        v.setAttribute('x5-video-orientation', 'landscape');
        v.setAttribute('playsinline', 'false');
        v.setAttribute('webkit-playsinline', 'false');
        v.muted = false;
        try { v.play(); } catch (e) {}

        var container = findPlayerContainer(v);
        if (container && container !== document.body && container !== v) {
            container.setAttribute('data-cctv-container', '1');
            container.style.cssText += ';position:fixed !important;top:0 !important;left:0 !important;width:100vw !important;height:100vh !important;z-index:2147483647 !important;background:#000 !important;overflow:hidden !important;';
            v.style.cssText += ';width:100% !important;height:100% !important;object-fit:contain !important;background:#000 !important;';
        } else {
            v.style.cssText += ';position:fixed !important;top:0 !important;left:0 !important;width:100vw !important;height:100vh !important;object-fit:contain !important;z-index:2147483647 !important;background:#000 !important;';
        }

        document.body.style.overflow = 'hidden';
        document.documentElement.style.overflow = 'hidden';
        cleanPage(v, container);
        buildXjBtn(v);
        window.__DXTV_FS_DONE__ = true;
        /* 补设主方案标记，避免 detail.js 再包一层全屏容器造成重复堆叠 */
        try { window.__DXTV_PAGE_FS__ = true; } catch (e) {}
        return true;
    }

    function buildXjBtn(v) {
        var old = document.getElementById('cctv-fs-xj');
        if (old) old.remove();
        var btn = document.createElement('div');
        btn.id = 'cctv-fs-xj';
        btn.textContent = '≡ 选集';
        btn.style.cssText = 'position:fixed;right:3vw;bottom:3vh;z-index:2147483647;padding:8px 16px;background:rgba(230,0,0,0.85);color:#fff;font-size:16px;border-radius:6px;cursor:pointer;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,0.5);';
        btn.onclick = function (e) { if (e) { e.stopPropagation(); e.preventDefault(); } showMenu(); return false; };
        document.body.appendChild(btn);
    }

    function forceFsNow() {
        if (!document.body) { return false; }
        var vs = document.querySelectorAll('video');
        if (!vs || vs.length === 0) { return false; }
        var v = null, bestA = -1;
        for (var i = 0; i < vs.length; i++) {
            var a = 0;
            try {
                var r = vs[i].getBoundingClientRect();
                a = (r.width || 0) * (r.height || 0);
            } catch (e) { a = 0; }
            if (a > bestA) { bestA = a; v = vs[i]; }
        }
        if (!v) { return false; }
        try { v.removeAttribute('data-cctv-fs'); } catch (e) {}
        return makeFs(v);
    }
    try { window.__dxtvForceFs = forceFsNow; } catch (e) {}

    /* v4.5.23：删除了右下角那个自制的「全屏」按钮（#cctv-fs-force）。
       自动脱壳全屏已由主方案（pageFsBtn 抢点页面原生全屏键）+ 本文件 makeFs 兜底稳定接管，
       这个手动按钮纯属冗余，还常驻遮挡屏幕右下角。保留 __dxtvForceFs 桥接以备不时之需。 */

    function fmtTitle(item, idx) {
        if (item.title) return item.title;
        return '第' + (idx + 1) + '集';
    }

    function parseAndBuild(data, nextId, callback) {
        var list = (data && data.data && data.data.list) ? data.data.list : [];
        var xjs = [];
        var container = document.createElement('div');
        container.id = 'cctv-xj-container';
        container.style.cssText = 'display:none !important;';
        list.forEach(function (item, idx) {
            var title = fmtTitle(item, idx);
            var xjItem = { id: String(item.id), title: title, name: title, index: idx, url: item.url || '', vodId: String(nextId), site: 'cctv', isVip: 'false', remark: '' };
            xjs.push(xjItem);
            var el = document.createElement('div');
            el.id = 'xj-' + xjItem.id;
            el.setAttribute('data-cctv-xj', '1');
            el.addEventListener('click', function () {
                if (item.url) { window.location.href = item.url; }
            });
            container.appendChild(el);
        });
        document.body.appendChild(container);
        window.__cctv_xjs__ = xjs;
        _menuLoading = false;
        callback && callback(xjs);
    }

    function loadXjs(callback) {
        if (_menuLoading) return;
        _menuLoading = true;
        var nextId = window.next_id;
        var itemId = window.itemid1;
        if (!nextId) {
            _menuLoading = false;
            callback && callback([]);
            return;
        }
        var mode = document.querySelector('script[src*="index_dhp.js"]') ? 1 : 0;
        var api = 'https://api.cntv.cn/NewVideo/getVideoListByAlbumIdNew?id=' + nextId + '&serviceId=tvcctv&pub=1&mode=' + mode + '&part=0&n=100&sort=asc';
        var header = JSON.stringify({ 'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0', 'tv-ref': 'https://tv.cctv.com/' });
        try {
            if (window._api && window._api.getJson) {
                var text = window._api.getJson(api, header);
                parseAndBuild(JSON.parse(text), nextId, callback);
                return;
            }
        } catch (e) {
            try { if (window._api && window._api.toast) _api.toast('分集拉取失败'); } catch (ex) {}
        }
        if (typeof fetch !== 'undefined') {
            fetch(api, { headers: { 'User-Agent': 'Mozilla/5.0', 'tv-ref': 'https://tv.cctv.com/' } })
                .then(function (r) { return r.json(); })
                .then(function (data) { parseAndBuild(data, nextId, callback); })
                .catch(function (e) { _menuLoading = false; callback && callback([]); });
            return;
        }
        _menuLoading = false;
        callback && callback([]);
    }

    function showMenu() {
        if (!window._api || !window._api.message) {
            try { if (window._api && window._api.toast) _api.toast('原生桥未就绪'); } catch (e) {}
            return;
        }
        var xjs = window.__cctv_xjs__;
        if (!xjs || xjs.length === 0) {
            loadXjs(function (list) {
                if (list.length > 0) { openMenu(); }
                else { try { _api.toast('暂无分集'); } catch (e) {} }
            });
            return;
        }
        openMenu();
    }

    function openMenu() {
        var xjs = window.__cctv_xjs__;
        var currentId = String(window.itemid1 || '');
        var currentXj = xjs[0];
        for (var i = 0; i < xjs.length; i++) {
            if (xjs[i].id === currentId) { currentXj = xjs[i]; break; }
        }
        var menu = {
            now: { xj: currentXj, hz: {}, rate: {} },
            xjs: xjs,
            hzs: [],
            rates: [],
            jds: [],
            video: true,
            tab: 'xj',
            isVip: false
        };
        window._api.message('menu', JSON.stringify(menu));
    }

    function tryFs() {
        var v = pickVideo();
        if (v) return makeFs(v);
        return false;
    }

    if (typeof window.$$ === 'undefined') {
        window.$$ = function (selector) {
            var el = document.querySelector(selector);
            return {
                length: el ? 1 : 0,
                trigger: function (eventName) {
                    if (!el) return this;
                    try {
                        var ev = document.createEvent('HTMLEvents');
                        ev.initEvent(eventName, true, true);
                        el.dispatchEvent(ev);
                    } catch (e) {}
                    if (el.onclick) el.onclick();
                    if (el.click) el.click();
                    return this;
                },
                click: function () {
                    if (el && el.click) el.click();
                    return this;
                }
            };
        };
    }

    tryEnterPlayer();
    tryFs();

    if (typeof MutationObserver !== 'undefined') {
        try {
            /* 页面加载期 DOM 变更极密集，若每个批次都跑一遍 tryEnterPlayer/tryFs，
               会在主线程上反复 querySelectorAll('a,button') / ('video')，直接拖慢出画面。
               这里做 120ms 合并：同一波变更只处理一次，且 200ms 的兜底轮询仍在跑，
               响应性不变、开销大幅下降。 */
            var moBusy = false;
            var moWork = function () {
                moBusy = false;
                var fsV = document.querySelector('video[data-cctv-fs="1"]');
                if (fsV && fsV.isConnected) { try { mo.disconnect(); } catch (eD) {} return; }
                if (fsV && !fsV.isConnected) { fsV.removeAttribute('data-cctv-fs'); }
                tryEnterPlayer();
                tryFs();
            };
            var mo = new MutationObserver(function () {
                if (moBusy) return;
                moBusy = true;
                setTimeout(moWork, 120);
            });
            mo.observe(document.body, { childList: true, subtree: true });
        } catch (e) {}
    }

    var MAX_POLL = 150;
    var t = 0, timer = setInterval(function () {
        t++;
        tryEnterPlayer();
        tryFs();
        if (t >= MAX_POLL || window.__DXTV_FS_DONE__) { clearInterval(timer); }
    }, 200);
})();
