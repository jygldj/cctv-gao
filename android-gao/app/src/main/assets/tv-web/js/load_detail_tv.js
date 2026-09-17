if(typeof _tvload == "undefined"){
     _tvload=false;
}
function extractDomain(url) {
    const match = url.match(/^(https?:\/\/[^/?#]+)/i);
    return match ? match[1] : null;
}
function decodeUnicodeBase64(base64Str) {
    return decodeURIComponent(escape(atob(base64Str)));
}
_data={
    hzList(video){
        let hzName =  _tvFunc.getVideoQuality(video);
        let hzList=[];
        let itemData={name:hzName,level:_tvFunc.hzLevel(hzName,1)};
        hzList.push(itemData);
        _apiX.msg("videoQuality",hzList);
        return hzList;
    }
};
(function () {
    /* 视频真的在播才通知原生收遮罩。
       注意必须逐个校验「是不是主视频」：央视播放页里常夹着推荐位、广告位的小 video，
       原先用 document.querySelector('video') 只取第一个命中的元素，被小视频误判成
       「已经播了」，遮罩提前收掉，裸骨架就露出来（实测约 1 秒）。 */
    var sent = false;
    var t0 = Date.now();
    var timer = setInterval(function () {
        if (sent || Date.now() - t0 > 60000) { clearInterval(timer); return; }
        try {
            var vs = document.getElementsByTagName('video');
            var vw = window.innerWidth || 1, vh = window.innerHeight || 1;
            for (var i = 0; i < vs.length; i++) {
                var v = vs[i];
                if (!v || v.readyState < 2) { continue; }
                /* 主视频要占据视口足够大的面积，小窗/推荐位一律不算 */
                var r = v.getBoundingClientRect();
                if (r.width < vw * 0.35 || r.height < vh * 0.35) { continue; }
                if (v.paused || v.currentTime <= 0.1) { continue; }
                sent = true;
                clearInterval(timer);
                if (window._apiX && typeof window._apiX.msg === 'function') {
                    window._apiX.msg('hideLoading', '1');
                }
                break;
            }
        } catch (e) {}
    }, 300);
})();

(function(){
    if(_tvload){
        return;
    }
    _tvload=true;

    function loadLocalText(path){
        try{
            if(window._api && typeof window._api.getJson === "function"){
                return window._api.getJson(path, JSON.stringify({})) || "";
            }
        }catch(e){
            console.error("loadLocalText error "+path, e);
        }
        return "";
    }
    function execJs(content, onload){
        if(!content || content.length===0){
            if(onload) onload(false);
            return false;
        }
        let script = document.createElement('script');
        script.textContent = content;
        if(onload){ script.onload = function(){ onload(true); }; script.onerror=function(){ onload(false); }; }
        document.body.appendChild(script);
        return true;
    }
    function loadLocalJs(path, onload){
        return execJs(loadLocalText(path), onload);
    }
    function loadLocalCss(path){
        let content = loadLocalText(path);
        if(!content || content.length===0){ return false; }
        let style = document.createElement('style');
        style.textContent = content;
        document.head.appendChild(style);
        return true;
    }

    function loadDetailByUrl(url){
        if(url.startsWith("https://tv.cctv.com/live")){
            return "tv/cctv";
        }
        if(url.startsWith("https://www.yangshipin.cn")){
            return "tv/ysptv"
        }
        if(url.startsWith("https://live.jstv.com")){
            return "tv/jstv"
        }

        if(url.startsWith("https://www.btime.com")){
            return "tv/bjtv"
        }
        if(url.startsWith("https://www.jlntv.cn/")){
            return "tv/jltv"
        }
        if(url.startsWith("https://www.lcxw.cn/")){
            _tvLoadRes.js("https://cdn.bootcdn.net/ajax/libs/hls.js/1.5.13/hls.js");
            return "tv/lctv"
        }
        if(url.startsWith("https://www.fengshows.com/")){
            return "tv/fengshows"
        }
        if(url.startsWith("https://www.nmtv.cn")){
            return "tv/nmtv"
        }
        if(url.startsWith("https://www.mgtv.com/live")){
            return "tv/hntv"
        }
        if(url.startsWith("https://web.guangdianyun.tv")){
            return "tv/gdytv"
        }
        return "tv/common"
    }
    let detailPath=loadDetailByUrl(window.location.href);

    function loadFromLocal(){
        var domain = extractDomain(window.location.href);
        if (domain && typeof _tvLoadRes !== "undefined" && _tvLoadRes && _tvLoadRes.js) {
            _tvLoadRes.css(domain + "/tv-web/css/my.css");
            _tvLoadRes.js(domain + "/tv-web/js/zepto.min.js?v=r1");
            _tvLoadRes.js(domain + "/tv-web/js/common.js?v=r1");
            _tvLoadRes.js(domain + "/tv-web/js/pageFsBtn.js?v=r1");
            _tvLoadRes.js(domain + "/tv-web/js/" + detailPath + "/detail.js?v=r1");
            return;
        }
        loadLocalCss("css/my.css");
        loadLocalJs("js/zepto.min.js", function(ok1){
            if(!ok1){ console.error("zepto load failed"); }
            loadLocalJs("js/common.js", function(ok2){
                if(!ok2){ console.error("common.js load failed"); }
                loadLocalJs("js/pageFsBtn.js", function(){
                    loadLocalJs("js/" + detailPath + "/detail.js");
                });
            });
        });
    }
    function loadFromRemote(){
        let fullUrl=window.location.href;
        let domain=extractDomain(fullUrl);
        _tvLoadRes.css(domain+`/tv-web/css/my.css`);
        _tvLoadRes.js(domain+`/tv-web/js/zepto.min.js?v=r1`);
        _tvLoadRes.js(domain+`/tv-web/js/common.js?v=r1`);
        _tvLoadRes.js(domain+`/tv-web/js/pageFsBtn.js?v=r1`);
        _tvLoadRes.js(domain+`/tv-web/js/${detailPath}/detail.js`);
    }
    let isApp = (typeof _tvIsApp !== "undefined" && _tvIsApp) || (typeof window._api !== "undefined" && window._api);
    if(isApp){
        let _waitTimes=0;
        let _timer=setInterval(function(){
            _waitTimes++;
            try{
                if(window._api && typeof window._api.getJson === "function"){
                    clearInterval(_timer);
                    loadFromLocal();
                    return;
                }
            }catch(e){
                console.error("wait _api error", e);
            }
            if(_waitTimes>=50){
                clearInterval(_timer);
                loadFromRemote();
            }
        },100);
    } else {
        loadFromRemote();
    }
})();
