(function () {
    'use strict';
    if (window.__DXTV_YSP_INJECTED__) { return; }
    window.__DXTV_YSP_INJECTED__ = true;

    var KEEP_SELECTORS = ['.bei-list-inner', '[class*="quality"]', '[class*="Quality"]'];
    var SKIP_TAGS = { SCRIPT: 1, STYLE: 1, LINK: 1, META: 1, TITLE: 1, BASE: 1, HEAD: 1, NOSCRIPT: 1, TEMPLATE: 1 };

    var container = null;
    var scheduled = false;

    function isKeepMenu(el) {
        for (var i = 0; i < KEEP_SELECTORS.length; i++) {
            try { if (el.matches && el.matches(KEEP_SELECTORS[i])) return true; } catch (e) {}
        }
        return false;
    }

    function collectKeep() {
        var keep = [];
        function add(el) { if (el && keep.indexOf(el) < 0) keep.push(el); }
        function chain(el) {
            var p = el;
            while (p && p !== document.documentElement) { add(p); p = p.parentNode; }
        }
        var vs = document.querySelectorAll('video');
        for (var k = 0; k < vs.length; k++) chain(vs[k]);
        if (container) add(container);
        for (var i = 0; i < KEEP_SELECTORS.length; i++) {
            try {
                var nodes = document.querySelectorAll(KEEP_SELECTORS[i]);
                for (var j = 0; j < nodes.length; j++) chain(nodes[j]);
            } catch (e) {}
        }
        return keep;
    }

    function hideDeep(node, keep, stat) {
        var kids = node.children;
        if (!kids) return;
        for (var i = 0; i < kids.length; i++) {
            var k = kids[i];
            if (k === container) continue;
            if (SKIP_TAGS[k.tagName]) continue;
            if (isKeepMenu(k)) continue;
            if (keep.indexOf(k) >= 0) { hideDeep(k, keep, stat); continue; }
            if (k.style.display !== 'none') {
                k.style.display = 'none';
                stat.hidden++;
            }
        }
    }

    function doClean() {
        if (!document.body) return;
        if (container && (!container.isConnected || container.parentNode !== document.body)) {
            try { document.body.appendChild(container); } catch (e) {}
        }
        var keep = collectKeep();
        for (var i = 0; i < keep.length; i++) {
            var n = keep[i];
            if (n.style && n.style.display === 'none') n.style.display = '';
        }
        var stat = { hidden: 0 };
        hideDeep(document.body, keep, stat);
    }

    function schedule() {
        if (scheduled) return;
        scheduled = true;
        setTimeout(function () {
            scheduled = false;
            try { doClean(); } catch (e) {}
        }, 300);
    }

    function observe() {
        if (typeof MutationObserver === 'undefined') return;
        try {
            new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
        } catch (e) {}
    }

    function setupVideo(video) {
        container = document.createElement('div');
        container.style.position = 'fixed';
        container.style.top = '0';
        container.style.left = '0';
        container.style.width = '100vw';
        container.style.height = '100vh';
        container.style.zIndex = '2147483647';
        container.style.backgroundColor = 'black';
        container.setAttribute('data-ysp-keep', '1');
        video.style.width = '100%';
        video.style.height = '100%';
        video.style.objectFit = 'contain';
        video.style.transform = 'translateZ(0)';
        container.appendChild(video);
        document.body.appendChild(container);
        document.body.style.overflow = 'hidden';
        document.documentElement.style.overflow = 'hidden';
        doClean();
        observe();
        return container;
    }

    window._data = {
        hzList() {
            var hzList = [];
            var currentLevelHz = null;
            $$(".bei-list-inner").find(".item").each(function (i, item) {
                var id = "" + i;
                $$(item).attr("id", "xhz-" + id);
                var isCurrent = $$(item).hasClass("active");
                var hzName = $$(item).text().trim().replace(/\s*/g, "");
                if (hzName.includes("VIP")) {
                    return;
                }
                var itemData = {
                    id: id, name: hzName, level: _tvFunc.hzLevel(hzName, 2),
                    action: `_data.hzChoose("xhz-${id}","${hzName}")`
                };
                if (isCurrent) {
                    currentLevelHz = itemData;
                }
                hzList.push(itemData);
            });
            var chooseHzId = localStorage.getItem("chooseHz");
            if (chooseHzId && currentLevelHz && "xhz-" + currentLevelHz.id !== chooseHzId) {
                window._data.hzChoose(chooseHzId, localStorage.getItem("chooseHzName"));
            }
            _apiX.msg("videoQuality", hzList);
        },
        hzChoose(id, name) {
            $$("#" + id).click();
            _apiX.toast("画质切换到 " + name);
            localStorage.setItem("chooseHz", id);
            localStorage.setItem("chooseHzName", name);
        }
    };

    _tvFunc.waitForVideoElement().then(function (video) {
        if (!window.__DXTV_PAGE_FS__) {
            setupVideo(video);
        }
        video.muted = false;
        video.volume = 1;
        video.playsInline = false;
        video.setAttribute('playsinline', 'false');
        try {
            video.play();
        } catch (e) {
        }
        window._data.hzList();
        _tvFunc.check(function () {
            var videoPlay = _tvFunc.isVideoPlaying(video);
            if (!videoPlay) {
                _tvFunc.getVideo().play();
            }
            return videoPlay;
        }, function () { window._data.hzList(); }, 1000, 5);
    });
})();
