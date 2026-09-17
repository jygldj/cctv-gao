(function(){
    function loadCssCode(code) {
       try {
           var style = document.createElement('style')
           style.rel = 'stylesheet'
           style.appendChild(document.createTextNode(code))
           var head = document.getElementsByTagName('head')[0]
                   || document.head
                   || document.documentElement;
           if (head) { head.appendChild(style); }
       } catch (e) {}
    }
  function createDiv(html,index,idName,...classNames){
     var myDiv = document.createElement("div");
     if(!idName){
         idName="tv-index-"+index;
     }
     myDiv.id = idName;
     myDiv.style.zIndex=index;
     myDiv.style.visibility="visible";
     myDiv.style.background="#E3EDCD";
     if(classNames.length==0){
         myDiv.className="tv-index";
     }else{
         myDiv.className=classNames.join(" ");
     }
     myDiv.innerHTML = html;
     document.body.appendChild(myDiv);
     return idName;
 };
 var css=`
 body{
   visibility:visible;
 }
 .utv-video-full{
   position: fixed !important;
    z-index: 99990 !important;
    width: 100vw !important;
    height: 100vh !important;
    top: 0 !important;
    left: 0 !important;
    right:0 !important;
    bottom: 0 !important;
    background-color: rgb(0, 0, 0); 
 }
 `;
 loadCssCode(css);
})();

 var _tvLoadRes={
     js(scrJs){
         let script = document.createElement('script');
         script.setAttribute('type', 'text/javascript');
         script.src = scrJs;
         script.async = false;
         (document.body || document.head || document.documentElement).appendChild(script);
     },
     css(scrCss){
         let script = document.createElement('link');
         script.setAttribute('rel', 'stylesheet');
         script.setAttribute('type', 'text/css');
         script.href = scrCss;
         script.async = false;
         document.head.appendChild(script);
     },
     jsBottom(scrJs){
         let script = document.createElement('script');
         script.setAttribute('type', 'text/javascript');
         script.src = scrJs;
         script.async = false;
         document.head.appendChild(script);
     }
 };
 var _browser={
    config:{
         isTvApp:false,
    },
    getURL(src){
       let  baseUrl="https://www.utao.tv/tv-web/";
           if(window.location.href.startsWith("https://www.bestv.com.cn/web/play/")){
               baseUrl="https://www.bestv.com.cn/tv-web/";
           }
       return baseUrl+src;
    },
     app(normal,callback){
           if(!this.config.isTvApp){
               normal();
               return;
           }
           callback();
      }
};
 _tvIsApp=true;
