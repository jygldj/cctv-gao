const _appVersion = {
    16: "4.1",
    17: "4.2",
    18: "4.3",
    19: "4.4",
    20: "4.4W",
    21: "5",
    22: "5.1",
    23: "6",
    24: "7",
    25: "7.1",
    26: "8",
    27: "8.1",
    28: "9",
    29: "10",
    30: "11",
    31: "12",
    32: "12L",
    33: "13",
    34: "14",
    35: "15"
};

function getAndroidName(version) {
    if (version < 16) {
        return "小于安卓4.1";
    }
    if (version > 35) {
        return "大于安卓15";
    }
    return "安卓" + _appVersion[version];
}

const _html = {
    init() {
        this.initApp();
    },
    initApp() {
        PetiteVue.createApp({
            apps: [],
            focusId: "",
            info: {
                sys: "当前系统",
                version: "当前版本",
                x5Ok: true
            },
            initData() {
                _data.initData(this);
                const addFocus = () => {
                    const first = document.getElementById("app-0");
                    if (first) {
                        first.classList.add("tv-focus");
                    } else {
                        console.error("#app-0 未渲染，100ms 后重试");
                        setTimeout(addFocus, 100);
                    }
                };
                this.$nextTick(addFocus);
                setTimeout(addFocus, 300);
            },
            appChoose(item) {
                let dataUrl = item.url;
                if (dataUrl === "tv.html") {
                    _apiX.msgStr("activity", "live");
                    return;
                }
                /* 起跳前不弹 JS 浮层：原生在 onPageStarted 即接管（命中 isVideoPage 才显示计时遮罩），
                   多一层浮层只会先闪 0.2~1 秒再被原生盖掉，反而更乱。 */
                window.location.href = dataUrl;
            },
            tvId(value, pre) {
                if (pre) {
                    return pre + value;
                }
                return 'tv-' + value;
            }
        }).mount('#tv-body');

        try {
            new FOCUS({
                event: {
                    keyOkEvent: function () {
                        const f = document.querySelector(".tv-focus");
                        if (f) { f.click(); }
                    },
                    keyBackEvent: function () {
                    }
                }
            });
        } catch (e) {
            console.error("FOCUS 实例化失败", e);
        }

        setTimeout(function () {
            try {
                const cctvCard = document.getElementById('app-0');
                if (cctvCard) {
                    const goLive = function (e) {
                        try {
                            if (e) e.preventDefault();
                            _apiX.msgStr("activity", "live");
                        } catch (err) { console.error(err); }
                    };
                    cctvCard.addEventListener('click', goLive);
                    cctvCard.addEventListener('keydown', function (e) {
                        if (e && (e.key === 'Enter' || e.keyCode === 13 || e.keyCode === 23)) goLive(e);
                    });
                } else {
                    console.error("未找到 #app-0 卡片，apps 可能未渲染");
                }
            } catch (e) { console.error(e); }
        }, 500);
    }
};

let _data = {
    vue: null,
    initData(vue) {
        this.vue = vue;
        this.apps();
    },
    apps() {
        let apps = [];
        const isGecko = _tvFunc.isGecko();
        apps.push({ id: 0, url: "tv.html", name: "央视网", pic: "img/ysw.png" });
        apps.push({ id: 0, url: "https://www.yangshipin.cn/tv/home?pid=600002475", name: "CCTV直播", pic: "img/ysp.png" });
        apps.push({ id: 0, url: "cctv.html", name: "央视片库", pic: "img/yspk.png" });
        apps.push({ id: 0, url: "column.html", name: "央视栏目", pic: "img/lanmu.png" });
        apps.push({ id: 0, url: "kuxuan.html", name: "支持我", pic: "img/dsm.png" });

        apps.forEach((item, index) => {
            item.id = index;
            item.pic = _tvFunc.image(item.pic);
            _data.vue.apps.push(item);
        });
    }
};

$$(function () {
    _html.init();
    if (LA) {
        LA.init({ id: "3Kwwp7VWLvVgtOND", ck: "3Kwwp7VWLvVgtOND" });
    }
});
