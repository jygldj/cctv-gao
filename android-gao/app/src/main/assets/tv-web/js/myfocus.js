let TvFocus={
    curFocusId:null,
    focusId:".tv-focus",
    focusClass:"tv-focus",
    menuId: null,
    model:{
        event:{}
    },
    init(menuId){
        this.menuId=menuId;
        this.menuCtrl();
        this.initKeyEvent();
        this.observe();
    },
    menuCtrl(){
        let _this=this;
        window._menuCtrl={
            menu(){
                _this.menu();
                let data = _detailData();
                _apiX.message("menu",data);
            },
            ok(){
                _this.keyOkEvent();
            },
            back(){
                _this.keyBackEvent();
            },
            left(){
                _this.keyLeftEvent();
            },
            right(){
                _this.keyRightEvent();
            },
            down(){
                _this.keyDownEvent();
            },
            up(){
                _this.keyUpEvent();
            },
            focus(id){
                let realId = (id&&id.charAt(0)==="#") ? id.substring(1) : id;
                _this.curFocusId = realId;
                _this.applyFocus(realId);
                if(window.__setFocus){ window.__setFocus(realId); }
            }
        }
    },
    ok(){
        if(_ctrlx.ok){
            _ctrlx.ok();
        }
        return true;
    },
    menu(){
        if(_ctrlx.menu){
            _ctrlx.menu();
        }
        return true;
    },
    back(){
        return true;
    },
    left(){
        if(_isVideo){
            let duration=_tvFunc.getVideo().duration;
            if(duration=="Infinity"){
                return true;
            }
            _tvFunc.getVideo().currentTime-=20;_layer.notifyLess("进度减20秒");
        }
        return true;
    },
    right(){
        if(_isVideo){
            let duration=_tvFunc.getVideo().duration;
            if(duration=="Infinity"){
                return true;
            }
            _tvFunc.getVideo().currentTime+=20;_layer.notifyLess("进度加20秒");
        }
        return true;
    },
    up(){
        let video= _tvFunc.getVideo();
        let currentVolume=_tvFunc.getVideo().volume;
        if(currentVolume<1){if((currentVolume+0.2)>1){video.volume=1}else{video.volume+=0.2}}
        let name= Math.floor(video.volume*100);
        _layer.notifyLess("音量"+name);
        return true;
    },
    down(){
        let video= _tvFunc.getVideo();
        let currentVolume=_tvFunc.getVideo().volume;
        if(currentVolume>0){if((currentVolume-0.2)<0){video.volume=0}else{video.volume-=0.2;}}
        let name= Math.floor(video.volume*100);
        _layer.notifyLess("音量"+name);
        return true;
    },
    keyDownEvent(){
        var elem= this.getFocus();
        if(null==elem){
            this.down();
            return;
        }
        var updownNum= elem.attr("move-updown");
        if(updownNum){
            this.move(this.next(elem,updownNum));
        }
        this.moveBack(elem,"move-down");
    },
    moveBack(elem,attr){
        var attrElem= elem.attr(attr);
        if(attrElem){
            var strs =  attrElem.split(":");
            if(strs.length==1){
                if($$(attrElem).length>0){
                    this.move($$(attrElem).first());
                }else{
                    this.moveBack(elem,attr+"b");
                }
            }
            if(strs.length==2){
                this.move($$(strs[0]).find(strs[1]).first());
            }   
        }
    },
    keyUpEvent(){
        var elem= this.getFocus()
        if(null==elem){
            this.up();
            return;
        }
        var updownNum= elem.attr("move-updown");
        if(updownNum){
            this.move(this.prev(elem,updownNum));
            return;
        }
        var moveUp= elem.attr("move-up");
        if(moveUp){
            this.move($$(moveUp).first());
        }
    },
    keyRightEvent() {
        let focus=this.getFocus();
        if(null==focus){
            this.right();
            return;
        }
        this.move(focus.next());
    },
    found(location){
        if(null==this.menuId){
            return  $$(location);
        }
        let isShow=_layer.isShow(this.menuId);
        if(isShow){
            return $$("#"+this.menuId).find(location);
        }
        return  null;
    },
    getFocus(){
        let el = this.curFocusId ? $$("#"+this.curFocusId) : null;
        if(el && el.length>0){ return el; }
        return this.found(this.focusId);
    },
    applyFocus(id){
        if(!id){ return; }
        $$(".tv-focus").removeClass("tv-focus");
        let el = $$("#"+id);
        if(el && el.length>0){ el.addClass("tv-focus"); }
        this.curFocusId = id;
    },
    observe(){
        let _this=this;
        this._focusObserver = new MutationObserver(function(){
            if(_this._raf){ cancelAnimationFrame(_this._raf); }
            _this._raf = requestAnimationFrame(function(){
                let id=_this.curFocusId;
                if(!id){ return; }
                let el=document.getElementById(id);
                if(el && !el.classList.contains("tv-focus")){ el.classList.add("tv-focus"); }
                let others = document.querySelectorAll(".tv-focus");
                for(let i=0;i<others.length;i++){
                    if(others[i].id!==id){ others[i].classList.remove("tv-focus"); }
                }
            });
        });
        this._focusObserver.observe(document.body, {childList:true, subtree:true, attributes:true, attributeFilter:["class"]});
    },
    keyLeftEvent() {
        let focus=this.getFocus();
        if(null==focus){
            this.left();
            return;
        }
        this.move(focus.prev());
    },
    idFound(elem,num,down){
        let idPre=elem.attr("move-updown-id");
        if(idPre){
            let nowId=elem.attr("id").substring(idPre.length);
            let newNum=Number(nowId)-Number(num);
            if(down){
                newNum=Number(nowId)+Number(num);
            }
            let foundId=idPre+newNum;
            if(null!=document.getElementById(foundId)){
                return $$("#"+foundId);
            }else{
                if(!down){
                    var moveUp= elem.attr("move-up");
                    return $$(moveUp);
                }
            }

            return elem;
        }
        return null;
    },
    next(elem,num){
        let newElem = this.idFound(elem,num,true);
        if(null!=newElem){
             return newElem;
        }
        for(var i=0;i<num;i++){
            if(elem.next()&&elem.next().length>0){
                elem=elem.next();
            }
        }
        return elem;
    },
    prev(elem,num){
        let newElem = this.idFound(elem,num,false);
        if(null!=newElem){
             return newElem;
        }
        for(var i=0;i<num;i++){
            if(elem.prev()&&elem.prev().length>0){
                elem=elem.prev();
            }else{
                var moveUp= elem.attr("move-up");
                return $$(moveUp);
            }
        }
        return elem;
    },
    move(nextElm){
        if(nextElm&&nextElm.length>0){
            this.curFocusId = nextElm.attr("id");
            this.applyFocus(this.curFocusId);
            this.scrollTo();
            this.focusEvent();
        }
    },
    isElementInViewport(el) {
        var rect = el.getBoundingClientRect();
        return (
          rect.top >= 0 &&
          rect.left >= 0 &&
          rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
          rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
      },
    scrollIntoView(el) {
        var parent = el.parentElement;
        var isHeader = el.closest('.tv-header') !== null;

        if (isHeader) {
            parent = el.closest('.tv-header');
            setTimeout(() => {
                document.body.scrollTop = 0;
                document.documentElement.scrollTop = 0;

                var elementRect = el.getBoundingClientRect();
                if (elementRect.right > window.innerWidth) {
                    parent.scrollLeft += (elementRect.right - window.innerWidth) + 60;
                } else if (elementRect.left < 0) {
                    parent.scrollLeft = Math.max(0, parent.scrollLeft + elementRect.left - 60);
                }
            }, 50);
            return;
        }

        while (parent) {
            var style = window.getComputedStyle(parent);
            var overflow = style.getPropertyValue('overflow');
            var overflowY = style.getPropertyValue('overflow-y');

            var isScrollable = (overflow === 'auto' || overflow === 'scroll' ||
                    overflowY === 'auto' || overflowY === 'scroll') &&
                parent.scrollHeight > parent.clientHeight;

            if (isScrollable) break;
            parent = parent.parentElement;
        }

        if (!parent) {
            parent = document.scrollingElement || document.documentElement;
        }

        var elementRect = el.getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var viewportWidth = window.innerWidth;

        requestAnimationFrame(() => {
            if (elementRect.bottom > viewportHeight) {
                var scrollOffset = elementRect.bottom - viewportHeight + 20;
                parent.scrollTop += scrollOffset;
            } else if (elementRect.top < 0) {
                parent.scrollTop += elementRect.top - 20;
            }

            if (elementRect.right > viewportWidth) {
                parent.scrollLeft += elementRect.right - viewportWidth + 40;
            } else if (elementRect.left < 0) {
                parent.scrollLeft = Math.max(0, parent.scrollLeft + elementRect.left - 40);
            }

            parent.style.transform = 'translateZ(0)';
        });
    },
    scrollTo: function() {
        var el = document.querySelector(this.focusId);
        if (!el) return;
        
        var flag = this.isElementInViewport(el);
        if (!flag) {
            setTimeout(() => {
                el.scrollIntoView({ behavior: "smooth", block: "start", inline: "nearest" });
            },50);
        }
    },
    keyOkEvent() {
        let focus=this.getFocus();
        if(null==focus){
            this.ok();
            return;
        }
        focus.trigger("click");
    },
    keyBackEvent(){
        if(_layer.isShow(this.menuId)){
            _layer.hide(this.menuId);
        }else{
            window.location.href= this.backUrl();
        }
    },
    backUrl(){
        return _browser.getURL("index.html");
    },
    keyMenuEvent(){
        this.isShow= _layer.toggle(_tv_menuId);
        if(!this.isShow){
            this.focus=null;
            _apiX.msgStr("menuShow","0");
        }else{
            this.menu();
            this.focus=this.found(this.focusId);
            _apiX.msgStr("menuShow","1");
        }
    },
    focusEvent(){
        let focus=this.getFocus();
        if(null!=focus){
            focus.trigger("focus");
        }
    },
    initKeyEvent() {
        var KEY_BACK = 8,
            KeyQ=81,
            KEY_OK = 13,
            KeyA=65,
            KeyW=87,
            KeyD = 68,
            KeyS=83,
            KeyR=82
        let _this=this;
        document.addEventListener("keydown",function(e){
            var keyCode=e.keyCode;
            switch (keyCode) {
                case 19:
                case 38:
                case KeyW:
                    _this.keyUpEvent()
                    break;
                case 20:
                case 40:
                case KeyS:
                    _this.keyDownEvent()
                    break
                case 21:
                case 37:
                case KeyA:
                    _this.keyLeftEvent()
                    break
                case 4:
                case 22:
                case 39:
                case KeyD:
                    _this.keyRightEvent()
                    break
                case KeyQ:
                    _this.keyBackEvent()
                    break
                case 23:
                case 66:
                case KEY_OK:
                    _this.keyOkEvent();
                    e.preventDefault();
                    break
                case KeyR:
                    _this.keyMenuEvent();
                    break
                default:
                    break
            }
        });
    }
};
window.TvFocus = TvFocus;
