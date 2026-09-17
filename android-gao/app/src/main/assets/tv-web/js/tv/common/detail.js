
function setupVideo(video) {
    const container = document.createElement('div');
    container.style.position = 'fixed';
    container.style.top = '0';
    container.style.left = '0';
    container.style.width = '100vw';
    container.style.height = '100vh';
    container.style.zIndex = '2147483647';
    container.style.backgroundColor = 'black';
    video.style.width = '100%';
    video.style.height = '100%';
    video.style.objectFit = 'contain';
    video.style.transform = 'translateZ(0)';
    container.appendChild(video);
    document.body.appendChild(container);
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';

}
(function(){
    _tvFunc.waitForVideoElement().then(video => {
        let param=_tvFunc.getQueryParams();
        let type="0";
        if(param["utaot"]){
            type=param["utaot"];
        }
        if(type==="0"){
            if(!window.__DXTV_PAGE_FS__){
                setupVideo(video);
            }
        }
        if(type==="1"){
            if(!window.__DXTV_PAGE_FS__){
                _tvFunc.fixedW("body");
                _tvFunc.fullscreen("video");
                $$("video").css("position","fixed !important");
            }
        }
        video.muted = false;
        video.volume = 1;
        video.playsInline = false;
        video.setAttribute('playsinline', 'false');
        try {
            video.play();
        } catch (e) {
        }
        $$("body").css("min-width","100%");
        $$("html").css("min-width","100%");
        _tvFunc.check(function (){
            let videoPlay=_tvFunc.isVideoPlaying(video);
            if(!videoPlay){
                try{
                    _tvFunc.getVideo().play();
                }catch(e){}
            }
            return videoPlay},function (){_data.hzList(video);},1000,5);

    });
})();
$$(function (){
    let url = window.location.href;
    if(url.indexOf("u-link=1")>0){
        _tvFunc.check(function (){
            let utaoLoc =  sessionStorage.getItem("u-loc");
            if(utaoLoc){
                if(url==utaoLoc){
                    window.location.href=extractDomain(utaoLoc)+"/tv-web/live.html?url="+sessionStorage.getItem("u-m3u8");
                    return true
                }
            }
            return false},function (){},1000,10);
    }
    let ujs=   _tvFunc.getQueryParams()["ujs"];
    if(ujs){
        let ujsContent=decodeUnicodeBase64(ujs.replace(/ /g, '+'));
        eval(ujsContent);
    }
    const viewportMeta = document.querySelector('meta[name="viewport"]');
    if (viewportMeta) {
        viewportMeta.setAttribute('content', `width=device-width, initial-scale=1`);
    }

});
