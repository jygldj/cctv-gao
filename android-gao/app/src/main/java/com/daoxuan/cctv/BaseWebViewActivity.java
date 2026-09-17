package com.daoxuan.cctv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.daoxuan.cctv.databinding.ActivityMainBinding;
import com.daoxuan.cctv.databinding.ItemHzBinding;
import com.daoxuan.cctv.databinding.ItemJdBinding;
import com.daoxuan.cctv.databinding.ItemRateBinding;
import com.daoxuan.cctv.databinding.ItemXjBinding;
import com.daoxuan.cctv.domain.ConfigDTO;
import com.daoxuan.cctv.domain.DetailMenu;
import com.daoxuan.cctv.domain.HzItem;
import com.daoxuan.cctv.domain.JdItem;
import com.daoxuan.cctv.domain.RateItem;
import com.daoxuan.cctv.domain.SysInfo;
import com.daoxuan.cctv.domain.XjItem;
import com.daoxuan.cctv.impl.BaseBindingAdapter;
import com.daoxuan.cctv.impl.BaseViewHolder;
import com.daoxuan.cctv.impl.IBaseBindingPresenter;
import com.daoxuan.cctv.service.UpdateService;
import com.daoxuan.cctv.util.AppVersionUtils;
import com.daoxuan.cctv.util.DataCleanManager;
import com.daoxuan.cctv.util.FileUtil;
import com.daoxuan.cctv.util.HttpUtil;
import com.daoxuan.cctv.util.JsonUtil;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.util.Util;
import com.daoxuan.cctv.util.ValueUtil;
import com.daoxuan.cctv.util.WebService;
import com.daoxuan.cctv.utils.ToastUtils;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.util.ArrayList;



public class BaseWebViewActivity extends BaseActivity {
    protected String TAG = "BaseWebViewActivity";
    private static final String mHomeUrl =
            "file:///android_asset/tv-web/index.html";

    /* 本地页视频的「默认海报」替换值。
       WebView 在 <video> 没有 poster 属性时会拿 WebChromeClient.getDefaultVideoPoster()
       的返回值当海报画上去，系统默认给的是「灰底 + 大播放图标」——片库/栏目直连 m3u8 时，
       它就是起播前那 1~2 秒的播放按钮（那张图是方的，所以画出来是个居中灰方块）。
       返回 1x1 全透明位图即取消该海报，露出的就是页面自身底色（本地页统一 #0a1f3a）。
       范围仅限本类：首页 / 央视片库 / 央视栏目 / 支持我 / live.html。
       LiveActivity（央视网、CCTV直播）另有一套 WebChromeClient，不继承本类，原样不动。 */
    private static final Bitmap TRANSPARENT_VIDEO_POSTER =
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    protected  ActivityMainBinding binding;

    @Override
    protected void createInit() {
        bind();
        // 资源读取优先走 assets，不再每次启动全量拷贝，启动更快也避免首屏读到半拷贝文件
        UpdateService.initBaseFolder(this);
        initWebView();
        mWebView.loadUrl(mHomeUrl);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            String url = null;
            try { url = mWebView.getUrl(); } catch (Exception ignore) { /* noop */ }
            if (url != null && url.contains("live.html")) {
                int webCode = -1;
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_CENTER: webCode = 23; break;
                    case KeyEvent.KEYCODE_ENTER:       webCode = 13; break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:   webCode = 37; break;
                    case KeyEvent.KEYCODE_DPAD_UP:     webCode = 38; break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:  webCode = 39; break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:   webCode = 40; break;
                    default: webCode = -1;
                }
                if (webCode >= 0) {
                    final int code = webCode;
                    final WebView wv = mWebView;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            wv.evaluateJavascript("if(window.__cctvKey)window.__cctvKey(" + code + ");", null);
                        }
                    });
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void bind(){
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setMenuTitleHandler(new MenuTitleHandler());
        ViewGroup container = binding.webviewWrapper;
        container.addView(mWebView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        focusChange();
    }


    protected void initWebChromeClient() {
        mWebView.setWebChromeClient(new WebChromeClient() {
            /* 取消 WebView 自带的视频默认海报（灰底 + 大播放图标）。
               它由 WebView 在起播前直接画在视频区域，不属于页面 DOM，
               因此页面样式与播放器配置都管不到，只能在原生层替换。 */
            @Override
            public Bitmap getDefaultVideoPoster() {
                return TRANSPARENT_VIDEO_POSTER;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                LogUtil.i("WebChromeClient", "onProgressChanged, newProgress:" + newProgress + ", view:" + view);
                if (newProgress >= 100) {
                    /* progress==100 只代表「网页框架到位」，视频取流/缓冲还在后面。
                       会话期间若在这里收罩，不只是拆掉遮罩——hideLoadingOverlay() 还会把
                       会话状态一并清零（sessionActive / sessionStartAt / cctvStageCount），
                       于是下一次页面跳转重新起算计时、文案退回「正在打开节目页…」。
                       实测「骨架露三次」正是这条支线在专辑页/播放页各拆一次会话造成的。 */
                    if (!sessionActive) {
                        hideLoadingOverlay();
                    }
                }
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                LogUtil.i("WebChromeClient", "onShowCustomView");
                binding.fullscreen.addView(view);
                binding.fullscreen.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                LogUtil.i("WebChromeClient", "onPermissionRequest " + request.getOrigin());
                LogUtil.i("WebChromeClient", request.getOrigin() + " " + Arrays.toString(request.getResources()));
                request.deny();
            }

            @Override
            public void onHideCustomView() {
                LogUtil.i("WebChromeClient", "onHideCustomView");
                binding.fullscreen.removeAllViews();
                binding.fullscreen.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected Object getJsInterface() {
        return new JsInterface();
    }


    private Button defaultFocusBtn(View oldFocus, View newFocus){
        if(!(newFocus instanceof Button)){
            return null;
        }
        return (Button) newFocus;

    }
    private String  oldBtnTag(View oldFocus){
        if(!(oldFocus instanceof Button)){
            return null;
        }
        Object tagObj=oldFocus.getTag();
        if(null==tagObj){return null;}
        return  tagObj.toString();
    }

    private void focusChange() {
        View view = binding.tvMenu;
        view.getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver.OnGlobalFocusChangeListener() {
            @Override
            public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                Log.d(TAG, "onGlobalFocusChanged: oldFocus=" + oldFocus);
                Log.d(TAG, "onGlobalFocusChanged: newFocus=" + newFocus);
                Button focusBtn=defaultFocusBtn(oldFocus,newFocus);
                if(null==focusBtn){return;}
                Object tagObj=focusBtn.getTag();
                if(null==tagObj){return;}
                String tag= tagObj.toString();
                Log.d(TAG, "onGlobalFocusChanged: newFocus=" + tag);
                if(tag.startsWith("menu_")){
                    tag=tag.substring(5);
                    binding.getMenu().setTab(tag);
                }
                String oldTag=null;
                switch (tag){
                    case "hzItem":
                        focusBtn.setNextFocusUpId(R.id.hzBtn);
                        oldTag=oldBtnTag(oldFocus);
                        if(null!=oldTag){
                            if(oldTag.equals("menu_hz")){
                                RecyclerView.ViewHolder viewHolder = binding.hzsView.findViewHolderForLayoutPosition(0);
                                if(null!=viewHolder){
                                    viewHolder.itemView.requestFocus();
                                }
                            }
                        }
                        break;
                    case "rateItem":
                        focusBtn.setNextFocusUpId(R.id.rateBtn);
                        oldTag=oldBtnTag(oldFocus);
                        if(null!=oldTag){
                            if(oldTag.equals("menu_rate")){
                                RecyclerView.ViewHolder viewHolder = binding.ratesView.findViewHolderForLayoutPosition(0);
                                if(null!=viewHolder){
                                    viewHolder.itemView.requestFocus();
                                }
                            }
                        }
                        break;
                    case "jdItem":
                        LinearLayout layoutJd = (LinearLayout) focusBtn.getParent();
                        RecyclerView.LayoutParams paramsJd = (RecyclerView.LayoutParams) layoutJd.getLayoutParams();
                        int itemPositionJd = paramsJd.getViewLayoutPosition();
                        if(itemPositionJd<6){
                            focusBtn.setNextFocusUpId(R.id.jdBtn);
                        }
                        oldTag=oldBtnTag(oldFocus);
                        if(null!=oldTag){
                            if(oldTag.equals("menu_jd")){
                                RecyclerView.ViewHolder viewHolder = binding.jdsView.findViewHolderForLayoutPosition(0);
                                if(null!=viewHolder){
                                    viewHolder.itemView.requestFocus();
                                }
                            }
                        }
                        break;
                    case "xjItem":
                        LinearLayout layout = (LinearLayout) focusBtn.getParent();
                        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) layout.getLayoutParams();
                        int itemPosition = params.getViewLayoutPosition();
                        if(itemPosition<6){
                            focusBtn.setNextFocusUpId(R.id.xjBtn);
                        }
                        Log.d(TAG, "xjItem: index=" + itemPosition);
                        oldTag=oldBtnTag(oldFocus);
                        if(null!=oldTag){
                            if(oldTag.equals("menu_xj")){
                                int id =  binding.xjsView.getLayoutManager().getItemCount();
                                LogUtil.i(TAG,"count "+ id+" "+ binding.xjsView.getChildCount()+" "+binding.xjsView.getAdapter().getItemCount());
                                int viewCount= binding.xjsView.getChildCount();
                                int num=binding.getMenu().getNow().getXj().getIndex();
                                if(num>viewCount){
                                    num=viewCount-1;
                                }
                                RecyclerView.ViewHolder viewHolder = binding.xjsView.findViewHolderForLayoutPosition(num);
                                if(null!=viewHolder){
                                    viewHolder.itemView.requestFocus();
                                }
                            }
                        }
                        break;
                    default:
                        LogUtil.i(TAG,"setTab"+tag);
                        break;
                }
            }
        });
    }

    private void xjBlind(List<XjItem> xjItems){
        BaseBindingAdapter xjAdapter = new BaseBindingAdapter<XjItem, ItemXjBinding>(xjItems,R.layout.item_xj) {
            @Override
            public void doBindViewHolder(BaseViewHolder<ItemXjBinding> holder, XjItem item) {
                holder.getBinding().setVariable(BR.item, item);
                holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
            }
        };
        xjAdapter.setItemPresenter(new XjBindPresenter());
        binding.xjsView
                .setLayoutManager(new GridLayoutManager(this, 6));
        binding.xjsView
                .setAdapter(xjAdapter);
    }
    private void jdBlind(List<JdItem> jdItems){
        BaseBindingAdapter jdAdapter = new BaseBindingAdapter<JdItem, ItemJdBinding>(jdItems,R.layout.item_jd) {
            @Override
            public void doBindViewHolder(BaseViewHolder<ItemJdBinding> holder, JdItem item) {
                holder.getBinding().setVariable(BR.item, item);
                holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
            }
        };
        jdAdapter.setItemPresenter(new JdBindPresenter());
        binding.jdsView
                .setLayoutManager(new GridLayoutManager(this, 6));
        binding.jdsView
                .setAdapter(jdAdapter);
    }
    private void hzBind(List<HzItem> hzItems){
        BaseBindingAdapter hzAdapter = new BaseBindingAdapter<HzItem, ItemHzBinding>(hzItems,R.layout.item_hz) {
            @Override
            public void doBindViewHolder(BaseViewHolder<ItemHzBinding> holder, HzItem item) {
                holder.getBinding().setVariable(BR.item, item);
                holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
            }
        };
        hzAdapter.setItemPresenter(new HzBindPresenter());
        binding.hzsView
                .setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        binding.hzsView
                .setAdapter(hzAdapter);
    }

    private void rateBind(List<RateItem> rateItems){
        BaseBindingAdapter rateAdapter = new BaseBindingAdapter<RateItem, ItemRateBinding>(rateItems,R.layout.item_rate) {
            @Override
            public void doBindViewHolder(BaseViewHolder<ItemRateBinding> holder, RateItem item) {
                holder.getBinding().setVariable(BR.item, item);
                holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
            }
        };
        rateAdapter.setItemPresenter(new RateBindPresenter());
        binding.ratesView
                .setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        binding.ratesView
                .setAdapter(rateAdapter);

    }

    public  class XjBindPresenter implements IBaseBindingPresenter {

        public void onClick(XjItem item) {
            LogUtil.i(TAG,item.getTitle());
            hideMenu();
            postMessage("click","xj-"+item.getId());

        }
    }
    public  class JdBindPresenter implements IBaseBindingPresenter {

        public void onClick(JdItem item) {
            LogUtil.i(TAG,item.getName());
            hideMenu();
            postMessage("click","jd-"+item.getId());

        }
    }

    public    class HzBindPresenter implements IBaseBindingPresenter {

        public void onClick(HzItem item) {
            LogUtil.i(TAG,item.getName());
            hideMenu();
            postMessage("click","hz-"+item.getId());

        }
    }
    public  class RateBindPresenter implements IBaseBindingPresenter {

        public void onClick(RateItem item) {
            LogUtil.i(TAG,item.getName());
            hideMenu();
            postMessage("click","rate-"+item.getId());

        }
    }

    public  class MenuTitleHandler {

        public void nextBtn() {
            hideMenu();
            postMessage("click","tv-next");
        }
        public void reloadBtn() {
            hideMenu();
            mWebView.reload();
        }
        public void btnClick(View view){
            LogUtil.i(TAG,"btnClick "+view);
             view.requestFocus();
        }
    }

    private Runnable hideLoadingRunnable = null;
    private AnimatorSet pulseSet = null;
    private Runnable loadingTickRunnable = null;
    /**
     * 连续会话的起算点：从点节目进入视频页那一刻起算，中途页面跳转不重置，
     * 所以遮罩上的秒数是「从点击到出画面」的完整耗时，而不是某一小段。
     */
    private long sessionStartAt = 0L;
    /** 会话进行中：期间任何页面加载完成都不收罩，只等视频真的出来（或会话超时）。 */
    private boolean sessionActive = false;

    /**
     * 遮罩收起时的防抖：显示了就别一闪而过，至少留住 MIN_SHOW。
     * 只有「收起」这一半需要防抖——显示时机由 onPageLoadStarted 的白名单直接决定，不设延迟。
     */
    private static final long OVERLAY_MIN_SHOW_MS = 420L;
    /**
     * 一次「点节目 → 出画面」的连续会话上限，内含多次页面跳转。
     * 会话内全程不主动收罩、计时也不归零，只有视频真出来才收；这里是兜底，
     * 防止取流失败时遮罩把用户永久挡在门外。
     */
    private static final long SESSION_MAX_MS = 18000L;
    private static final long FS_POLL_MAX_MS = 12000L;
    /** 300ms → 150ms：视频就绪后最多还要等一个轮询周期才收罩，缩短它。 */
    private static final long FS_POLL_INTERVAL_MS = 150L;
    /** 兜底收罩要晚于脱壳轮询，否则央视页还没脱完就被强制收掉。 */
    private static final long LOADING_FALLBACK_MS = 15000L;

    /**
     * 会话基线的合理上限：正常一次等待最长也就 SESSION_MAX_MS（遮罩到点必收）。
     * 若 sessionStartAt 比「现在」早了超过这个值，说明它是上一次等待遗留的脏基线
     * （典型场景：在栏目/列表页浏览太久，会话被放行保留却没有归零），
     * 必须重新起算——否则计时器一上来就显示 4000s+ 的脏数字（已实测复现 5010.4s）。
     */
    private static final long SESSION_STALE_MS = SESSION_MAX_MS + 5000L;

    /**
     * 连续这么多次探测不到 video 元素，就认定不是播放页（央视栏目/列表页），别再干等。
     *
     * 原为 5 次 × 300ms = 1.5s；v4.5.12 把轮询间隔缩到 150ms 时没同步改这个次数，
     * 判定窗口被顺带砍到 0.75s，实测正好卡在「专辑页 → 播放页」自动跳转的空档上——
     * 遮罩先收、跳转未发生，裸骨架露 1~2 秒。
     * 现按 24 次 × 150ms ≈ 3.6s 计：足以覆盖一次自动跳转，又不至于把真正的列表页挡死。
     */
    private static final int FS_NOVIDEO_MAX = 24;

    /**
     * 「已脱壳干净、只剩播放器」但主视频还没出画面时的宽限。
     *
     * makeFs() 只负责把播放器容器撑满 + 调一次 play()，此刻视频往往还在取流/缓冲；
     * 若这时立刻收罩，露出的正是「播放器黑屏 + 大片海报/播放键」——实测里第三次裸露。
     * 给它 3 秒出首帧，仍不出画面才放行（那时露的是播放器本体，不再是网页骨架，
     * 且页面上的「全屏 / 选集」按钮可用，不会把用户挡在门外）。
     */
    private static final long FS_CLEANED_GRACE_MS = 3000L;

    /**
     * 收罩判据是「视频真的出画面」，而不是「网页加载完」——onPageFinished 之后
     * 节目内容往往还要再等 3~4 秒才出画面，早收就会露出裸骨架。
     * 但不能无条件干等：tv.cctv.com 下还有栏目页/列表页，那里压根没有 video。
     * 分六种结果：
     *   playing    主视频已在播 → 收罩（唯一「真·就绪」信号）
     *   loading    播放器容器已就位、video 还没创建 → 继续等
     *   waiting    有 video 但未出画面 → 继续等
     *   clean_wait 页面已脱壳干净（__DXTV_FS_DONE__）但主视频仍未出画面 → 宽限数秒再放行
     *   cleaned    脱壳干净且页面里连 video 都没有 → 收罩
     *   novideo    压根不是播放页（栏目/列表）→ 累计到上限才放行，且不结束会话
     */
    private static final String FS_PROBE_JS =
            "(function(){try{"
            /* 是否已脱壳清理（pageFsBtn / cctvFullscreen 完成）。
               注意它只说明「网页骨架被清掉了」，不等于「画面出来了」——
               makeFs() 把容器撑满之后就立刻置位，此时视频常常还在缓冲，
               所以还有 video 时不能只凭它收罩（见 clean_wait）。 */
            + "var cleaned=!!window.__DXTV_FS_DONE__;"
            + "var vs=document.getElementsByTagName('video');"
            + "if(!vs||vs.length===0){"
            /* 播放器容器已就位、video 还没创建：正在初始化，得继续等，
               不能当成「这不是播放页」——否则遮罩会在播放器就绪前提前收起露骨架。 */
            + "if(document.getElementById('_video_player')||document.querySelector('.video-player')"
            + "||document.querySelector('.vjs-player')||document.querySelector('.video-js'))return 'loading';"
            /* 专辑页正准备自动跳播放页（cctvFullscreen 已按下「立即观看」）：
               这段跳转空档里本页确实没有 video，但绝不能按 novideo 放行。 */
            + "if(window.__DXTV_ENTERING_PLAYER__)return 'loading';"
            + "return cleaned?'cleaned':'novideo';}"
            + "var vw=window.innerWidth||1,vh=window.innerHeight||1;"
            + "for(var i=0;i<vs.length;i++){var v=vs[i];"
            + "if(!v||v.readyState<2)continue;"
            + "var r=v.getBoundingClientRect();"
            + "if(r.width<vw*0.35||r.height<vh*0.35)continue;"
            + "if(v.paused&&v.currentTime<=0)continue;"
            + "return 'playing';}"
            + "return cleaned?'clean_wait':'waiting';"
            + "}catch(e){return 'waiting';}})()";

    private static boolean isVideoPage(String url) {
        if (url == null) { return false; }
        return url.contains("tv.cctv.com") || url.contains("yangshipin.cn");
    }

    private Runnable pendingHideRunnable = null;
    private Runnable fsPollRunnable = null;
    private long overlayShownAt = 0L;
    private String loadingPrefix = "正在加载…";
    private int fsNoVideoStreak = 0;
    /** clean_wait 首次出现的时刻，用于「已脱壳但未出画面」的宽限计时。 */
    private long cleanWaitSince = 0L;
    /** 本次会话进入央视页的计数：1=专辑/详情页，2 及以上=播放页。用于区分阶段文案。 */
    private int cctvStageCount = 0;


    private void startPulseAnimation() {
        try {
            stopPulseAnimation();
            List<Animator> all = new ArrayList<>();
            View[] rings = new View[]{ binding.pulseRing1, binding.pulseRing2, binding.pulseRing3 };
            for (int i = 0; i < rings.length; i++) {
                View v = rings[i];
                if (v == null) { continue; }
                v.setScaleX(0.25f);
                v.setScaleY(0.25f);
                v.setAlpha(0f);
                ObjectAnimator sx = ObjectAnimator.ofFloat(v, View.SCALE_X, 0.25f, 1f);
                ObjectAnimator sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0.25f, 1f);
                ObjectAnimator al = ObjectAnimator.ofFloat(v, View.ALPHA, 0.95f, 0f);
                sx.setRepeatCount(ValueAnimator.INFINITE);
                sy.setRepeatCount(ValueAnimator.INFINITE);
                al.setRepeatCount(ValueAnimator.INFINITE);
                AnimatorSet one = new AnimatorSet();
                one.playTogether(sx, sy, al);
                one.setDuration(1600);
                one.setInterpolator(new AccelerateDecelerateInterpolator());
                one.setStartDelay(i * 530);
                all.add(one);
            }
            if (binding.loadingText != null) {
                binding.loadingText.setAlpha(1f);
                ObjectAnimator breath = ObjectAnimator.ofFloat(binding.loadingText, View.ALPHA, 1f, 0.55f, 1f);
                breath.setDuration(1800);
                breath.setRepeatCount(ValueAnimator.INFINITE);
                breath.setInterpolator(new AccelerateDecelerateInterpolator());
                all.add(breath);
            }
            pulseSet = new AnimatorSet();
            pulseSet.playTogether(all);
            pulseSet.start();
        } catch (Exception e) {
            LogUtil.e(TAG, "startPulseAnimation error: " + e.getMessage());
        }
    }


    private void stopPulseAnimation() {
        try {
            if (pulseSet != null) {
                pulseSet.cancel();
                pulseSet = null;
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "stopPulseAnimation error: " + e.getMessage());
        }
    }

    /**
     * 脱壳遮罩显示真实耗时，取代写死的「大约需要4秒」。
     * 有进度反馈的等待，体感明显短于无反馈的黑屏。
     *
     * 秒数按「整段会话」连续累加（点节目 → 打开节目页 → 进入播放页 → 加载视频），
     * 中途页面跳转不归零；只有文案随阶段切换。这样遮罩在整条链路上是一块、
     * 一次计时，中间不会出现「这一段完了先撤罩、下一段再弹罩」的骨架空档。
     */
    private void startLoadingTextTick() {
        try {
            stopLoadingTextTick();
            if (sessionStartAt <= 0L) { sessionStartAt = SystemClock.elapsedRealtime(); }
            loadingTickRunnable = new Runnable() {
                @Override
                public void run() {
                    if (binding == null || binding.loadingText == null) { return; }
                    if (binding.loadingOverlay.getVisibility() != View.VISIBLE) { return; }
                    long now = SystemClock.elapsedRealtime();
                    /* 兜底：基线缺失（<=0）或已过期（> SESSION_STALE_MS）就重新起算，
                       避免渲染出 sessionStartAt 遗留的脏数字（实测曾显示 5010.4s）。 */
                    if (sessionStartAt <= 0L || now - sessionStartAt > SESSION_STALE_MS) {
                        sessionStartAt = now;
                    }
                    float sec = (now - sessionStartAt) / 1000f;
                    if (sec < 0f) { sec = 0f; }
                    binding.loadingText.setText(
                            String.format(Locale.getDefault(), "%s %.1fs", loadingPrefix, sec));
                    binding.loadingOverlay.postDelayed(loadingTickRunnable, 200);
                }
            };
            binding.loadingOverlay.postDelayed(loadingTickRunnable, 200);
        } catch (Exception e) {
            LogUtil.e(TAG, "startLoadingTextTick error: " + e.getMessage());
        }
    }

    private void stopLoadingTextTick() {
        try {
            if (loadingTickRunnable != null && binding != null && binding.loadingOverlay != null) {
                binding.loadingOverlay.removeCallbacks(loadingTickRunnable);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "stopLoadingTextTick error: " + e.getMessage());
        }
        loadingTickRunnable = null;
    }


    private void showLoadingOverlay() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (binding == null || binding.loadingOverlay == null) { return; }
                    if (hideLoadingRunnable != null) {
                        binding.loadingOverlay.removeCallbacks(hideLoadingRunnable);
                        hideLoadingRunnable = null;
                    }
                    binding.loadingOverlay.animate().cancel();
                    binding.loadingOverlay.setAlpha(1f);
                    binding.loadingOverlay.setVisibility(View.VISIBLE);
                    overlayShownAt = SystemClock.elapsedRealtime();
                    startPulseAnimation();
                    startLoadingTextTick();
                    hideLoadingRunnable = new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingRunnable = null;
                            hideLoadingOverlay();
                        }
                    };
                    binding.loadingOverlay.postDelayed(hideLoadingRunnable, LOADING_FALLBACK_MS);
                }
            });
        } catch (Exception e) {
            LogUtil.e(TAG, "showLoadingOverlay error: " + e.getMessage());
        }
    }


    /**
     * 收起遮罩的对入口：保证遮罩一旦显示就至少停留 MIN_SHOW，避免一闪而过。
     */
    private void hideLoadingOverlay() {
        try {
            /* 收罩是终态：遮罩没有「延迟待显示」这一层，不存在被排队定时器重新弹出的情况。 */
            cancelPendingHide();
            if (overlayShownAt <= 0) {
                hideLoadingOverlayInternal(true);
                return;
            }
            long shownFor = SystemClock.elapsedRealtime() - overlayShownAt;
            if (shownFor >= OVERLAY_MIN_SHOW_MS) {
                hideLoadingOverlayInternal(true);
                return;
            }
            final long remain = OVERLAY_MIN_SHOW_MS - shownFor;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (binding == null || binding.loadingOverlay == null) { return; }
                    pendingHideRunnable = new Runnable() {
                        @Override
                        public void run() {
                            pendingHideRunnable = null;
                            hideLoadingOverlayInternal(true);
                        }
                    };
                    binding.loadingOverlay.postDelayed(pendingHideRunnable, remain);
                }
            });
        } catch (Exception e) {
            LogUtil.e(TAG, "hideLoadingOverlay error: " + e.getMessage());
        }
    }

    private void cancelPendingHide() {
        try {
            if (pendingHideRunnable != null && binding != null && binding.loadingOverlay != null) {
                binding.loadingOverlay.removeCallbacks(pendingHideRunnable);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "cancelPendingHide error: " + e.getMessage());
        }
        pendingHideRunnable = null;
    }

    private void hideLoadingOverlayInternal(boolean endSession) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (binding == null || binding.loadingOverlay == null) { return; }
                    if (hideLoadingRunnable != null) {
                        binding.loadingOverlay.removeCallbacks(hideLoadingRunnable);
                        hideLoadingRunnable = null;
                    }
                    stopFsPoll();
                    stopLoadingTextTick();
                    overlayShownAt = 0L;
                    if (endSession) {
                        /* 遮罩收起 = 会话结束，下次点节目重新起算。 */
                        sessionStartAt = 0L;
                        sessionActive = false;
                        cctvStageCount = 0;
                    }
                    if (binding.loadingOverlay.getVisibility() != View.VISIBLE) { return; }
                    stopPulseAnimation();
                    binding.loadingOverlay.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            binding.loadingOverlay.setVisibility(View.GONE);
                        }
                    }).start();
                }
            });
        } catch (Exception e) {
            LogUtil.e(TAG, "hideLoadingOverlayInternal error: " + e.getMessage());
        }
    }

    /**
     * 列表页放行：收遮罩，并**结束本次会话**（基线归零）。
     *
     * 判据是「连续 3.6 秒探测不到 video」——那多半是用户停在栏目/列表页浏览。
     * 关键：此时必须把会话一并结束掉。早期为了「随后点进播放页时计时能续上」而保留会话，
     * 正是计时器出现 4000s+ 脏数字的根因——用户在列表页浏览几分钟后，sessionStartAt 仍是
     * 几分钟前的旧值，再点节目时 onPageLoadStarted 见 sessionActive 仍为 true 就不重置基线，
     * 秒数便从旧基线一路算下来（实测 5010.4s）。
     * 「专辑页 → 播放页」那种真正连续的跳转（都在视频页之间）不会走到这里，因此不受影响。
     */
    private void releaseOverlayForBrowsing() {
        hideLoadingOverlayInternal(true);
    }

    /**
     * 计时器的唯一用途，是给「网页脱壳」等待期间遮一遮，别让裸骨架露出来。
     *
     * 因此这里是**白名单式**的：只有 isVideoPage(url)（tv.cctv.com / yangshipin.cn）
     * 才安排遮罩，其余页面一律不安排、什么也不做——本地页自身底色即 #0a1f3a，
     * 没有站点骨架可挡，给它盖遮罩只会平白闪一下。
     */
    @Override
    protected void onPageLoadStarted(String url) {
        if (isVideoPage(url)) {
            /* 会话只在第一次进视频页时起算：「专辑页 → 播放页」属于同一次点击，
               遮罩不撤、计时不归零，文案才随阶段切换。
               若会话虽未结束、但基线已是旧值（如列表页放行后隔了很久才点节目），同样重新起算，
               避免把上一次等待的旧基线当成这一次，导致计时器一上来就显示几千秒。 */
            if (!sessionActive || sessionStartAt <= 0L
                    || SystemClock.elapsedRealtime() - sessionStartAt > SESSION_STALE_MS) {
                sessionActive = true;
                sessionStartAt = SystemClock.elapsedRealtime();
                cctvStageCount = 0;
            }
            cctvStageCount++;
            loadingPrefix = (cctvStageCount <= 1) ? "正在打开节目页…" : "正在进入播放页…";
            /* 会话中的跳转必须「立刻」盖上：这几十到几百毫秒的跳转窗口，正是骨架最容易
               露出来的地方。央视页本身就要几百毫秒起步，不存在本地页那种「刚盖就加载完、
               遮罩一闪」的问题，直接盖。 */
            cancelPendingHide();
            stopFsPoll();
            showLoadingOverlay();
            return;
        }
        /* 非脱壳页：首页 / 央视片库 / 央视栏目 / 支持我 / 本地播放页 live.html。
           这里没有站点骨架可挡，不安排任何遮罩；会话状态同时复位。
           页面自身底色已是 #0a1f3a（见 css/my.css、css/rset.css），不会白闪。 */
        sessionActive = false;
        sessionStartAt = 0L;
        cctvStageCount = 0;
        loadingPrefix = "正在加载…";
        stopFsPoll();
    }

    /**
     * 视频页收罩要等「视频真的能播」，而不是网页加载完——后者会早收 3~4 秒露骨架。
     *
     * 超时按「会话起点」算而不是按本页加载完算：一次点击可能经历两次页面加载，
     * 各算各的会把窗口抻长，跨页共用一个窗口才不失控。
     */
    private void startFsPoll() {
        try {
            stopFsPoll();
            if (binding == null || binding.loadingOverlay == null || mWebView == null) { return; }
            loadingPrefix = "正在加载视频…";
            fsNoVideoStreak = 0;
            cleanWaitSince = 0L;
            final long base = (sessionStartAt > 0L) ? sessionStartAt : SystemClock.elapsedRealtime();
            final long deadline = Math.max(base + SESSION_MAX_MS,
                    SystemClock.elapsedRealtime() + FS_POLL_MAX_MS / 2);
            fsPollRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mWebView == null) { fsPollRunnable = null; return; }
                    if (SystemClock.elapsedRealtime() > deadline) {
                        fsPollRunnable = null;
                        hideLoadingOverlay();
                        return;
                    }
                    mWebView.evaluateJavascript(FS_PROBE_JS, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            if (s == null) { fsNoVideoStreak = 0; return; }
                            if (s.contains("playing")) {
                                stopFsPoll();
                                hideLoadingOverlay();
                                return;
                            }
                            if (s.contains("clean_wait")) {
                                /* 页面已脱壳干净（只剩播放器本体），但主视频还没出画面——
                                   此刻露出来的正是「黑屏 + 大播放键/海报」那张图。
                                   宽限 3 秒等首帧；仍不出画面才放行，那时露的是播放器本体
                                   而非网页骨架，页面里的「全屏 / 选集」按钮也能用。 */
                                if (cleanWaitSince == 0L) { cleanWaitSince = SystemClock.elapsedRealtime(); }
                                fsNoVideoStreak = 0;
                                if (SystemClock.elapsedRealtime() - cleanWaitSince >= FS_CLEANED_GRACE_MS) {
                                    stopFsPoll();
                                    hideLoadingOverlay();
                                }
                                return;
                            }
                            if (s.contains("cleaned")) {
                                /* 脱壳干净、且页面里连 video 都没有：只剩播放器容器，可以收罩。 */
                                stopFsPoll();
                                hideLoadingOverlay();
                                return;
                            }
                            if (s.contains("novideo")) {
                                fsNoVideoStreak++;
                                /* 只在「本次会话还停在第 1 页」时放行——那时用户可能只是在
                                   浏览栏目/列表；一旦已经跳到播放页，就绝不再提前收罩。
                                   放行只收遮罩、不结束会话：紧接着点进播放页时计时与文案要续上。 */
                                if (fsNoVideoStreak >= FS_NOVIDEO_MAX && cctvStageCount <= 1) {
                                    stopFsPoll();
                                    releaseOverlayForBrowsing();
                                }
                            } else {
                                cleanWaitSince = 0L;
                                fsNoVideoStreak = 0;
                            }
                        }
                    });
                    if (fsPollRunnable != null && binding != null && binding.loadingOverlay != null) {
                        binding.loadingOverlay.postDelayed(fsPollRunnable, FS_POLL_INTERVAL_MS);
                    }
                }
            };
            binding.loadingOverlay.postDelayed(fsPollRunnable, FS_POLL_INTERVAL_MS);
        } catch (Exception e) {
            LogUtil.e(TAG, "startFsPoll error: " + e.getMessage());
        }
    }

    private void stopFsPoll() {
        try {
            if (fsPollRunnable != null && binding != null && binding.loadingOverlay != null) {
                binding.loadingOverlay.removeCallbacks(fsPollRunnable);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "stopFsPoll error: " + e.getMessage());
        }
        fsPollRunnable = null;
    }

    /**
     * 页面框架加载完成就收起遮罩；视频页例外，要等视频真的能播（见 startFsPoll）。
     */
    @Override
    protected void onPageLoadFinished(String url) {
        if (sessionActive && isVideoPage(url)) {
            /* 会话进行中：专辑页也好、播放页也好，都不收罩。
               原先是「这一页加载完就收、下一段再弹」，中间的空档正是实测看到的裸骨架。 */
            if (overlayShownAt <= 0) { showLoadingOverlay(); }
            startFsPoll();
            return;
        }
        hideLoadingOverlay();
    }

    //menu mange
    protected     boolean isMenuShow(){
        int visible=  binding.tvMenu.getVisibility();
        if(visible== View.VISIBLE){
            return true;
        }
        return false;
    }
    protected void showMenu(String data){
        LogUtil.i(TAG,"data:: "+data);
        if(null==data||!data.startsWith("{")){
            return;
        }
        DetailMenu detailMenu = JsonUtil.fromJson(data, DetailMenu.class);
        binding.setMenu(detailMenu);
        xjBlind(detailMenu.getXjs());
        hzBind(detailMenu.getHzs());
        jdBlind(detailMenu.getJds());
        rateBind(detailMenu.getRates());
        binding.tvMenu.setVisibility(View.VISIBLE);
        binding.xjBtn.requestFocus();
    }
    protected void hideMenu(){
        binding.tvMenu.setVisibility(View.GONE);
    }
    public    void postMessage(String service, String data) {
        if(service.equals("click")){
            String click=Util.click(data);
            LogUtil.i(TAG,"clickCode: "+click);
            mWebView.evaluateJavascript(click,null);
        }
    }

    private void toLive(){
        Intent intent = new Intent(this, LiveActivity.class);
        startActivity(intent);
        finish();
    }
    private void toDouyin(){
        Intent intent = new Intent(this, DouyinActivity.class);
        startActivity(intent);
        finish();
    }
    protected void killAppProcess()
    {
        // 不再强杀进程（会丢状态、看起来像闪退），正常结束所有界面即可
        finishAffinity();
    }
    private  static  WebService webService=null;
    private void newWebService(){
        if(null==webService){
            webService=new WebService(10240);
        }
    }
    protected boolean openOkMenu(){
        return "1".equals(ValueUtil.getString(getApplicationContext(),"openOkMenu","0"));
    }
    //js
    public class JsInterface{

        @JavascriptInterface
        public void toast(String message){
            LogUtil.i(TAG,"message "+message);
            ToastUtils.show(MyApplication.getContext(),message, Toast.LENGTH_SHORT);
        }
        @JavascriptInterface
        public void message(String service,String data){
            LogUtil.i(TAG,"service "+service+" data "+data);
            if("activity".equals(service)){
                if(data.equals("live")){
                    toLive();
                }
                if(data.equals("douyin")){
                    toDouyin();
                }
                return;
            }
            if("menu".equals(service)){
                runOnUiThread(()->{
                    boolean isMenuShow=isMenuShow();
                    if(isMenuShow){
                        hideMenu();
                        return;
                    }
                    showMenu(data);
                });
                return;
            }
            if("openOkMenu".equals(service)){
                ValueUtil.putString(getApplicationContext(),"openOkMenu",data);
                if(data.equals("1")){
                    ToastUtils.show(thisContext, "开启OK键是菜单成功",Toast.LENGTH_SHORT);
                }else{
                    ToastUtils.show(thisContext, "关闭OK键是菜单成功",Toast.LENGTH_SHORT);
                }

                return;
            }
            if("closeApp".equals(service)){
                finishAffinity();
                return;
            }
            if("js".equals(service)){
                Util.evalOnUi(mWebView,data);
                return;
            }
            if("showLoading".equals(service)){
                showLoadingOverlay();
                return;
            }
            if("hideLoading".equals(service)){
                /* 会话进行中不理会页面来的收罩请求：脱壳链路上收罩时机统一由
                   「主视频真的在播」判定（见 startFsPoll / FS_PROBE_JS），
                   中途收罩正是实测里遮罩退去、裸骨架露出来的空档。 */
                if (!sessionActive) { hideLoadingOverlay(); }
                return;
            }
            if("key".equals(service)){
                keyCodeAllByCode(data);
                return;
            }
            if("keyNum".equals(service)){
                keyEventAll(Integer.parseInt(data));
                return;
            }
            if("clearCache".equals(service)){
                DataCleanManager.cleanInternalCache(thisContext);
                DataCleanManager.cleanExternalCache(thisContext);
                ToastUtils.show(thisContext, "清理缓存成功",Toast.LENGTH_SHORT);
                return;
            }
        }
        @JavascriptInterface
        public String queryByService(String service,String extPraram){
            LogUtil.i(TAG,"queryByService "+service+" extPraram "+extPraram);
            if("queryIp".equals(service)){
                newWebService();
                return Util.getLocalIPAddress(thisContext);
            }
            if("querySysInfo".equals(service)){
                // 去掉同步网络请求：原来 ConfigApi.getConfig() 运行在 JS 线程上，网络慢时最长会卡住 5 秒
                String oldJson= FileUtil.readExt(MyApplication.getAppContext(),"tv-web/update.json");
                ConfigDTO oldConfig = null;
                if(!oldJson.trim().isEmpty()){
                     oldConfig = JsonUtil.fromJson(oldJson,ConfigDTO.class);
                }
                SysInfo sysInfo = new SysInfo();
                boolean is64= Util.is64();
                sysInfo.setSys64(is64);
                sysInfo.setVersionCode(Build.VERSION.SDK_INT);
                sysInfo.setX5Ok(false);
                sysInfo.setX86(Util.isX86());
                sysInfo.setDeviceId(MyApplication.androidId);
                sysInfo.setOpenOkMenu(openOkMenu());
                sysInfo.setCacheSize(DataCleanManager.getCacheSize(thisContext));
                //Build.VERSION.SDK_INT
                sysInfo.setVersionName(AppVersionUtils.getVersionName());
                if(null!=oldConfig){
                    sysInfo.setResVersion(""+oldConfig.getRes().getVersion());
                }

                return JsonUtil.toJson(sysInfo);
            }
            return null;
        }
        @JavascriptInterface
        public String postJson(String url,String header, String requestBody){

            Map<String, String> headerMap= JsonUtil.fromJson(header,
                    new TypeToken<Map<String, String>>() {}.getType());
            if(!url.startsWith("http")){
                return FileUtil.readExt(MyApplication.getAppContext(),"tv-web/"+url);
            }
            LogUtil.i(TAG,headerMap.toString()+"url "+url+" "+requestBody);
            return HttpUtil.postJson(url,headerMap,requestBody);
        }
        @JavascriptInterface
        public String getJson(String url,String header){
            Map<String, String> headerMap= JsonUtil.fromJson(header,
                    new TypeToken<Map<String, String>>() {}.getType());
            if(!url.startsWith("http")){
                return FileUtil.readExt(MyApplication.getAppContext(),"tv-web/"+url);
            }
            LogUtil.i(TAG,headerMap.toString()+"url "+url);
            return HttpUtil.getJson(url,headerMap);
        }
        @JavascriptInterface
        public String getHtml(String url,String header){
            Map<String, String> headerMap= JsonUtil.fromJson(header,
                    new TypeToken<Map<String, String>>() {}.getType());
            LogUtil.i(TAG,headerMap.toString()+" getHtml "+url);
            return HttpUtil.getJson(url,headerMap);
        }

    }



}
