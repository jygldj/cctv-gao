if(typeof _tvload == "undefined"){
    _tvload=false;
}
(function(){
    if(_tvload){
        return;
    }
    _tvload=true;
    /*  if(window.location.href.startsWith("https://v.qq.com/x/cover/")){
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
        if(url.startsWith("https://www.btime.com")){
            return "tv/bjtv"
        }
 /*       if(url.startsWith("https://www.jxntv.cn/")){
            return "tv/jxntv"
        }*/
        if(url.startsWith("https://www.jlntv.cn/")){
            return "tv/jltv"
        }
    /*    if(url.startsWith("https://www.gdtv.cn/tvChannelDetail")){
            return "tv/gdtv"
        }*/
        if(url.startsWith("https://www.lcxw.cn/")){
            _tvLoadRes.js("https://cdn.bootcdn.net/ajax/libs/hls.js/1.5.13/hls.js");
            return "tv/lctv"
        }
        if(url.startsWith("https://www.fengshows.com/")){
            return "tv/fengshows"
        }
     /*   if(url.startsWith("https://www.rzw.com.cn")){
            return "tv/rztv"
        }*/
        if(url.startsWith("https://www.nmtv.cn")){
            return "tv/nmtv"
        }
        if(url.startsWith("https://www.mgtv.com/live")){
            return "tv/hntv"
        }
        return "tv/common"
    }
    _tvLoadRes.css(_browser.getURL("css/my.css?v=x"));
    _tvLoadRes.js(_browser.getURL("js/zepto.min.js?v=x"));
    _tvLoadRes.js(_browser.getURL("js/common.js?v=x"));
    _tvLoadRes.js(_browser.getURL("js/myfocus.js?v=x"));
    _tvLoadRes.js(_browser.getURL("js/vuex.min.js?v=x"));
    let detailPath=loadDetailByUrl(window.location.href);
    console.log("detailPath:: "+detailPath);
    if(detailPath.startsWith("tv/")){
        _tvLoadRes.js(_browser.getURL(`js/tv/common.js?v=x`));
    }else{
        _tvLoadRes.js(_browser.getURL("js/detailBase.js?v=x"));
    }
    _tvLoadRes.js(_browser.getURL("js/detailBase.js?v=x"));
    _tvLoadRes.js(_browser.getURL(`js/${detailPath}/detail.js?v=x`));

})();
