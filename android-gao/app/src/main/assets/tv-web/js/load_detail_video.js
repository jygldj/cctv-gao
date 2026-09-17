if(typeof _tvload == "undefined"){
     _tvload=false;
}
(function(){
    if(_tvload){
        return;
    }
    _tvload=true;
    function loadDetailByUrl(url){
        if(url.startsWith("https://www.yangshipin.cn/tv/home")){
            return "cctv";
        }
        if(url.startsWith("https://tv.cctv.com/")){
            return "cctvideo";
        }
        return null;
    }
    let detailPath=loadDetailByUrl(window.location.href);
    if(null!=detailPath){
        let baseUrl = window.location.origin + "/tv-web/";
        _tvLoadRes.css(baseUrl + "css/my.css?v=x");
        _tvLoadRes.js(baseUrl + "js/zepto.min.js?v=x");
        _tvLoadRes.js(baseUrl + "js/common.js?v=x");
        _tvLoadRes.js(baseUrl + "js/myfocus.js?v=x");
        _tvLoadRes.js(baseUrl + "js/vuex.min.js?v=x");
        _tvLoadRes.js(baseUrl + "js/detailBase.js?v=x");
        _tvLoadRes.js(baseUrl + "js/" + detailPath + "/detail.js?v=x");
    }
})();
