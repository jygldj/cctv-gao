const Filter = {
    default: 0,
    hot: 1,
    new: 2,
    best: 3
};
(function(){
    /* 直跳播放页开关：实测专辑页只占约 0.4 秒，跳过它省不了多少，
       而接口解析失败要白等 700ms 才回退，默认关闭。想实测对比就改成 true。 */
    const _DIRECT_PLAY = false;
    const _html={
        init(){
           this.initApp();
        },
        initHtml(){
            const html = `
            <div class="tv-body" id="tv-body" @vue:mounted="initData()">
              <div class="tv-header">
                <div  tabindex="0" v-for="item in filters" class="tv-btn" :id="tvId(item,'fi-')" :class="{'tv-active':item==currentChannel.filter}"
                @focus="switchFilter(item)" @click="switchFilter(item)"
                   :move-down="tvId(currentChannel.tag,'#tv-')">{{filterName(item)}}</div>
            </div>
            <div class="tv-header">
                <div  tabindex="0" v-for="item in channels" class="tv-btn" :id="tvId(item.tag)" :class="{'tv-active':item.tag==currentChannel.tag}"
                 @focus="switchChannel(item)" @click="switchChannel(item)"  :move-down="moveDown(item.tag)" :move-up="tvId(item.filter,'#fi-')">{{item.name}}</div>
            </div>
            <div  v-for="item in channels" class="tv-content" :id="tvId(item.tag,'tvd-')" :style="{display:(item.tag==currentChannel.tag ? 'grid' : 'none')}">
               <div tabindex="0"  v-for="(vod,index) in item.vods"  @focus="detail(item,index)"  @click="goto(vod)" :id="tvId(index,item.tag)" :move-updown-id=item.tag class="tv-item"
                 move-updown="5" :move-up="tvId(item.tag,'#tv-')">
                    <span class="tv-idx">{{index+1}}</span>
                    <img v-if="vod.pic" :src=vod.pic :alt=vod.name />
                    <span :class="{'tv-btn':!vod.pic}">{{vod.name}}{{vod.remark}}</span>
               </div>
               <div v-if="item.vods.length>0 && !item.noMore" tabindex="0" :move-updown-id=item.tag class="tv-item tv-next" @click="nextPage(item)" :id="tvId('next',item.tag)" :move-up="tvId(item.tag,'#tv-')">下一页</div>
               <div v-if="item.noMore" class="tv-item tv-nomore">已经到底了</div>
            </div>
           </div>`
           return html;
        },
        initApp(){
            document.body.innerHTML = this.initHtml();
            PetiteVue.createApp({
                channels:[],
                currentChannel:null,
                focusId:"tv",
                filters:[],
                initData(){
                    _data.initData(this);
                    if(this.channels.length>0){
                        this.currentChannel=this.channels[0];
                        this.$nextTick(()=>{
                            let idFirst = "#"+this.tvId(this.currentChannel.tag);
                            $$(idFirst).addClass("tv-focus");
                        });
                    }
                },
                switchFilter(filter){
                    let filterId = this.tvId(filter,"#fi-");
                    let hasFocus= $$(filterId).hasClass("tv-focus");
                    if(this.currentChannel.filter!==filter){
                        this.currentChannel.filter=filter;
                        this.currentChannel.pageNum=0;
                        this.currentChannel.vods=[];
                        _data.channelPage(this.currentChannel);
                    }
                    this.$nextTick(function (){
                        if(hasFocus){
                            $$(filterId).addClass("tv-focus");
                        }
                    })
                },
                switchChannel(item){
                       this.currentChannel=item;
                      let channelId = "#"+this.tvId(item.tag);
                       let hasFocus= $$(channelId).hasClass("tv-focus");
                       if(item.vods.length===0){
                          _data.channelPage(item);
                       }
                       this.$nextTick(function (){
                           if(hasFocus){
                               $$(channelId).addClass("tv-focus");
                           }
                       })

                },
                moveDown(id){
                   return "#tvd-"+id+":.tv-item";
                },
                goto(item){
    //{id:item.media_id,name:item.title,pic:imageUrl,url:url,remark:remark}
                    let isApp= _tvFunc.isApp();
                    console.log("isAPP "+isApp)
                    _layer.wait("正在跳转到 "+item.name+" 请耐心等待。。。");
                    item.url=_tvFunc.url(item.url);
                    console.log(item.url);
                    if(!isApp||_tvFunc.isGecko()){
                        if(item.url.startsWith("https://tv.utao.tv/tv-web/")){
                            item.url=item.url.substring(26);
                        }
                        window.location.href = item.url;
                        return;
                    }
                    /* 直跳播放页：省掉「专辑页 → 自动点立即观看」这一段。
                       实测专辑页本身只要约 0.4 秒，省不了多少；而接口解析一旦失败要白等
                       700ms 才回退原路，反而更慢。故默认关闭，代码保留备用，
                       想试就把它打开（_DIRECT_PLAY = true）实测对比。 */
                    if(_DIRECT_PLAY){
                        let jumped=false;
                        let doJump=function(u){
                            if(jumped){ return; }
                            jumped=true;
                            if(u&&String(u).indexOf("tv.cctv.com")<0){ u=null; }
                            window.location.href = u || item.url;
                        };
                        if(_data.playUrlOf&&item.id&&String(item.id).indexOf("VIDA")===0){
                            try{ _data.playUrlOf(item.id, doJump); }catch(e){}
                            setTimeout(function(){ doJump(null); }, 700);
                            return;
                        }
                    }
                    window.location.href = item.url;
                },
                detail(item,index){
                     if(index+10>item.vods.length){
                        _data.channelPage(item);
                     }
                },
                nextPage(item){
                    _data.channelPage(item);
                },
                filterName(item) {
                    switch(item){
                        case Filter.default:
                            return "默认";
                        case Filter.hot:
                            return "热门";
                        case Filter.new:
                            return "最新";
                        case Filter.best:
                            return "好评"
                    }
                    return "默认";
                },
                tvId(value,pre){
                    if(pre){
                        return pre+value;
                    }
                    return 'tv-'+value;
                }
            }).mount('#tv-body');
        }
    };
    window._tvHtmlInit=function(){
        _html.init();
        TvFocus.init(null);
    };
})();
