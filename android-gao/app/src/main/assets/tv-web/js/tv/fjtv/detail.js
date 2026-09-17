(function(){
    _tvFunc.fixedW("body");
    _tvFunc.check(function (){return null!=document.getElementById("m2o_player");},function (){
        document.getElementById("m2o_player").classList.add("utv-video-full");
    });
    _tvFunc.volume100();
})();