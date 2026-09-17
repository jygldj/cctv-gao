let _data={
    vue:null,
    initData(vue){
        this.vue=vue;
        this.channels();
        this.channelPage();
    },
    channels(){
        let requestUrl="js/cctv/tv.json";
        _data.vue.focusId="cctv";
        try {
            let text = window._apiNative.getJson(requestUrl, JSON.stringify({}));
            let data = JSON.parse(text);
            data.data.forEach((item,index)=>{
                item.id=index;
                _data.vue.channels.push(item);
            })
        } catch(e) {
            console.error("load tv.json error", e);
            if (typeof _layer !== 'undefined' && _layer.notify) { _layer.notify("加载频道失败 "+e.message); }
        }
    },
    channelPage(channelItem){

    }
};
$$(function(){

    _tvHtmlInit();
});
