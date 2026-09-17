if(typeof _tvload == "undefined"){
     _tvload=false;
}
(function(){
    if(_tvload){
        return;
    }
    _tvload=true;
/*    if(window.location.href.startsWith("https://www.iqiyi.com/")){
        return ;
    }*/
    function loadDetailByUrl(url){
        if(url.startsWith("https://www.yangshipin.cn/tv/home")){
            return "cctv";
        }
        if(url.startsWith("https://tv.cctv.com/live")){
            return "tv/cctv";
        }
        if(url.startsWith("https://tv.cctv.com/")){
            return "cctvideo";
        }
        //各大tv
        if(url.startsWith("https://live.jstv.com")){
            return "tv/jstv"
        }
        if(url.startsWith("https://live.kankanews.com/")){
            return "tv/sh"
        }
        if(url.startsWith("https://live.fjtv.net/")){
            return "tv/fjtv"
        }
        if(url.startsWith("https://www.btime.com")){
            return "tv/bjtv"
        }
        return "tv/common"
    }
    _tvLoadRes.loadCssCode(_api.getCss("css/my.css?v=x"));
    _tvLoadRes.jsC(_api.getJs("js/zepto.min.js?v=x"));
    _tvLoadRes.jsC(_api.getJs("js/common.js?v=x"));
    _tvLoadRes.jsC(_api.getJs("js/myfocus.js?v=x"));
    _tvLoadRes.jsC(_api.getJs("js/vuex.min.js?v=x"));
    _tvLoadRes.jsC(_api.getJs("js/detailBase.js?v=x"));
    let detailPath=loadDetailByUrl(window.location.href);
    console.log("detailPath:: "+detailPath);
    _tvLoadRes.jsC(_api.getJs(`js/${detailPath}/detail.js?v=x`));
})();