package com.daoxuan.cctv;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.reflect.TypeToken;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.daoxuan.cctv.dao.HistoryDaoX;
import com.daoxuan.cctv.databinding.ActivityLiveBinding;
import com.daoxuan.cctv.databinding.ItemHzLiveBinding;
import com.daoxuan.cctv.domain.HzItem;
import com.daoxuan.cctv.databinding.DialogExitBinding;
import com.daoxuan.cctv.impl.BaseBindingAdapter;
import com.daoxuan.cctv.impl.BaseViewHolder;
import com.daoxuan.cctv.domain.live.Live;
import com.daoxuan.cctv.domain.live.Vod;
import com.daoxuan.cctv.impl.WebViewClientImpl;
import com.daoxuan.cctv.service.FavoriteService;
import com.daoxuan.cctv.service.UpdateService;
import com.daoxuan.cctv.util.FileUtil;
import com.daoxuan.cctv.util.HttpUtil;
import com.daoxuan.cctv.util.JsonUtil;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.util.Util;
import com.daoxuan.cctv.util.ValueUtil;
import com.daoxuan.cctv.utils.ToastUtils;

public class LiveActivity extends BaseActivity {
    protected String TAG = "LiveActivity";

    protected ActivityLiveBinding binding;
    private Context thisContext;
    private static Vod currentLive = null;
    private List<Live> provinces = new ArrayList<>();
    private int currentProvinceIndex = 0;
    private DialogExitBinding exitDialogBinding;
    private boolean isExitDialogShowing = false;
    
    private FavoriteService favoriteService;




    @Override
    protected void createInit() {
        bind();
        UpdateService.initBaseFolder(this);
        UpdateService.initTvData();
        thisContext=this;
        if(null==currentLive){
            currentLive = HistoryDaoX.currentChannel(this);
            //UpdateService.getByKey("0_0");
        }
        if(null==currentLive || !getIntent().hasExtra("liveKey")){
            currentLive = UpdateService.getByKey("0_0");
        }
        if(null==currentLive){
            ToastUtils.show(this,"获取数据错误 请重启",Toast.LENGTH_SHORT);
            finish();
            return;
        }
        initData();
        initWebView();
        try { mWebView.setBackgroundColor(Color.BLACK); } catch (Exception e) { LogUtil.e(TAG, "setBackgroundColor error: " + e.getMessage()); }
        mWebView.requestFocus();
        binding.webviewWrapper.requestFocus();
        //String liveUrl= "https://tv.cctv.com/live/cctv13/";
        loadLiveUrl(currentLive.getUrl());
        ToastUtils.show(this,"已支持遥控器上下左右可快速切台",Toast.LENGTH_SHORT);
    }

    /**
     * 换台防抖：遥控器长按会连续产生 ACTION_DOWN，必须合并成一次加载；
     * 但 1 秒太长（每次换台都白等 1 秒），250ms 足够合并连续按键，又几乎无感。
     */
    private static final long SWITCH_DEBOUNCE_MS = 250L;
    private long lastTime = 0;
    protected void initWebViewClient() {
        mWebView.setWebViewClient(new WebViewClientImpl(getBaseContext(),mWebView,1));
    }
   private Handler  handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String messageContent = (String) msg.obj;
                    if(currentLive!=null && currentLive.getKey().equals(messageContent)){
                        if (mWebView != null) {
                            /* 同一频道不重复整页重载：换了台才加载，避免白刷一次网页。 */
                            String target = currentLive.getUrl();
                            String current = null;
                            try { current = mWebView.getUrl(); } catch (Exception ignore) {}
                            if (target != null && !target.equals(current)) {
                                loadLiveUrl(target);
                            }
                        }
                    }
                    break;
                case 2:
                    binding.liveName.setText("");
                    break;
            }
        }
    };
    private boolean goNext(String nextType){
        if(null==currentLive){
            currentLive = UpdateService.getByKey("0_0");
        }
        String key= UpdateService.liveNext(currentLive.getTagIndex(),currentLive.getDetailIndex(),nextType);
        currentLive = UpdateService.getByKey(key);
        if(null!=currentLive){
            showToast(currentLive.getName(),this);
            String liveKey=currentLive.getKey();
            /* 先撤掉上一次未触发的换台，保证连续按键只落地最后一次。 */
            handler.removeMessages(1);
            handler.sendMessageDelayed (handler.obtainMessage(1, liveKey), SWITCH_DEBOUNCE_MS);

        }
        return true;
    }
    protected   void showToast(String text, Context context){
        binding.liveName.setText(text);
        //showToastOrg(text,context);
    }


    public boolean dispatchTouchEvent(MotionEvent event) {
        if(!isMenuShow()&&event.getAction() == MotionEvent.ACTION_DOWN){
            showMenu();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
    protected     boolean isMenuShow(){
        int visible=  binding.menuContainer.getVisibility();
        if(visible== View.VISIBLE){
            return true;
        }
        return false;
    }
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }
        int keyCode = event.getKeyCode();
        LogUtil.i("keyDown keyCode ", keyCode+" event" + event);
        
        if(isExitDialogShowing){
            if(keyCode==KeyEvent.KEYCODE_BACK){
                String currentStartPage = ValueUtil.getString(this, "startPage", "main");
                if ("main".equals(currentStartPage)) {
                    toHome();
                } else {
                    finish();
                }
                return true;
            }
            if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                return super.dispatchKeyEvent(event);
            }
            return super.dispatchKeyEvent(event);
        }

        boolean isMenuShow=isMenuShow();
        if(isMenuShow){
            if(keyCode==KeyEvent.KEYCODE_BACK||keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_TAB){
                hideMenu();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    currentProvinceIndex--;
                    if (currentProvinceIndex < 0) {
                        currentProvinceIndex = provinces.size() - 1;
                    }
                } else {
                    currentProvinceIndex++;
                    if (currentProvinceIndex >= provinces.size()) {
                        currentProvinceIndex = 0;
                    }
                }
                showCurrentProvince();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if(keyCode==KeyEvent.KEYCODE_MENU|| keyCode == KeyEvent.KEYCODE_TAB||keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){
            showMenu();
            return true;
        }

        if (isDigitKey(keyCode)) {
            digitInputHandler.removeCallbacks(commitDigitRunnable);
            digitBuffer.append(digitFromKeyCode(keyCode));
            try {
                ToastUtils.show(this, "输入: " + digitBuffer.toString(), Toast.LENGTH_SHORT);
            } catch (Throwable ignore) {}
            digitInputHandler.postDelayed(commitDigitRunnable, DIGIT_TIMEOUT_MS);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return goNext("right");
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return goNext("left");
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return goNext("down");
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            return goNext("up");
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPress();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void handleBackPress(){
        if (isExitDialogShowing) {
            String currentStartPage = ValueUtil.getString(this, "startPage", "main");
            if ("live".equals(currentStartPage)) {
                toHome();
            } else {
                finish();
            }
            return;
        }
        showExitDialog();
    }
    
    private void toHome(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showExitDialog() {
        if (exitDialogBinding == null) {
            initExitDialog();
        }
        isExitDialogShowing = true;
        exitDialogBinding.exitDialogContainer.setVisibility(View.VISIBLE);
        setupHzListInExit();
        
        exitDialogBinding.btnFavorite.setFocusable(true);
        exitDialogBinding.btnCancel.setFocusable(true);
        exitDialogBinding.btnStartToggle.setFocusable(true);
        
        updateFavoriteButtonInDialog();
        
        String currentStartPage = ValueUtil.getString(this, "startPage", "main");
        if ("main".equals(currentStartPage)) {
            exitDialogBinding.btnStartToggle.setText("启动即电视直播");
        } else {
            exitDialogBinding.btnStartToggle.setText("启动即视频点播");
        }
        



        try {
            exitDialogBinding.btnStartToggle.setNextFocusDownId(exitDialogBinding.hzListInExit.getId());
            exitDialogBinding.hzListInExit.post(() -> {
                try {
                    androidx.recyclerview.widget.RecyclerView.Adapter<?> adapter = exitDialogBinding.hzListInExit.getAdapter();
                    int count = adapter == null ? 0 : adapter.getItemCount();
                    if (count == 0) {
                        exitDialogBinding.btnStartToggle.setNextFocusDownId(exitDialogBinding.btnFavorite.getId());
                        exitDialogBinding.btnFavorite.setNextFocusUpId(exitDialogBinding.btnStartToggle.getId());
                    } else {
                        exitDialogBinding.btnFavorite.setNextFocusUpId(exitDialogBinding.hzListInExit.getId());
                    }
                } catch (Throwable ignore2) {}
            });
        } catch (Throwable ignore) {}

        
        exitDialogBinding.btnFavorite.post(() -> exitDialogBinding.btnFavorite.requestFocus());
    }
    
    private void updateFavoriteButtonInDialog() {
        if (currentLive != null && favoriteService != null) {
            if (favoriteService.isFavorite(currentLive.getUrl())) {
                exitDialogBinding.btnFavorite.setText("取消收藏");
            } else {
                exitDialogBinding.btnFavorite.setText("收藏当前频道");
            }
        }
    }
    
    private void hideExitDialog() {
        if (exitDialogBinding != null) {
            isExitDialogShowing = false;
            exitDialogBinding.exitDialogContainer.setVisibility(View.GONE);
        }
    }
    
    private void initExitDialog() {
        View dialogView = findViewById(R.id.exitDialog);
        exitDialogBinding = DataBindingUtil.bind(dialogView);
        
        if (exitDialogBinding == null) {
            return;
        }
        
        exitDialogBinding.btnFavorite.setOnClickListener(v -> {
            if (currentLive != null) {
                toggleFavorite(currentLive);
                updateFavoriteButtonInDialog();
                try {
                    exitDialogBinding.btnFavorite.post(() -> exitDialogBinding.btnFavorite.requestFocus());
                } catch (Throwable ignore) {}
            }
        });
        
        exitDialogBinding.btnCancel.setOnClickListener(v -> {
            hideExitDialog();
        });
        
        
        exitDialogBinding.btnStartToggle.setOnClickListener(v -> {
            String currentStartPage = ValueUtil.getString(this, "startPage", "main");
            if ("main".equals(currentStartPage)) {
                ValueUtil.putString(this, "startPage", "live");
                ToastUtils.show(this, "已设置启动首页为：电视直播", Toast.LENGTH_SHORT);
                exitDialogBinding.btnStartToggle.setText("启动即视频点播");
            } else {
                ValueUtil.putString(this, "startPage", "main");
                ToastUtils.show(this, "已设置启动首页为：视频点播", Toast.LENGTH_SHORT);
                exitDialogBinding.btnStartToggle.setText("启动即电视直播");
            }
        });



    }


    protected static String  videoQualityData=null;
    private void setupHzListInExit(){
        List<HzItem> hzItems=new ArrayList<>();
        if(null!=videoQualityData){
            hzItems=JsonUtil.fromJson(videoQualityData,new TypeToken<List<HzItem>>(){}.getType());
        }
        BaseBindingAdapter hzAdapter = new BaseBindingAdapter<HzItem, ItemHzLiveBinding>(hzItems,R.layout.item_hz_live) {
            @Override
            public void doBindViewHolder(BaseViewHolder<ItemHzLiveBinding> holder, HzItem item) {
                holder.getBinding().setVariable(BR.item, item);
                holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
            }
        };
        hzAdapter.setItemPresenter(new HzLiveBindPresenter());
        exitDialogBinding.hzListInExit.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,false));
        exitDialogBinding.hzListInExit.setAdapter(hzAdapter);
        exitDialogBinding.hzListInExit.addOnChildAttachStateChangeListener(new androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                View btn = view.findViewById(R.id.hzItem);
                if (btn != null) {
                    if (btn.getId() == View.NO_ID) {
                        btn.setId(View.generateViewId());
                    }
                    int upId = exitDialogBinding.btnStartToggle.getId();
                    btn.setNextFocusUpId(upId);
                    btn.setNextFocusDownId(exitDialogBinding.btnFavorite.getId());
                }
            }
            @Override
            public void onChildViewDetachedFromWindow(View view) { }
        });
        exitDialogBinding.hzListInExit.post(() -> {
            try {
                androidx.recyclerview.widget.RecyclerView.ViewHolder vh = exitDialogBinding.hzListInExit.findViewHolderForAdapterPosition(0);
                if (vh instanceof BaseViewHolder) {
                    ItemHzLiveBinding b = (ItemHzLiveBinding) ((BaseViewHolder<?>) vh).getBinding();
                    View first = b.hzItem;
                    if (first.getId() == View.NO_ID) {
                        first.setId(View.generateViewId());
                    }
            int upId = exitDialogBinding.btnStartToggle.getId();
            exitDialogBinding.btnFavorite.setNextFocusUpId(first.getId());
            first.setNextFocusUpId(upId);
                    first.setNextFocusDownId(exitDialogBinding.btnFavorite.getId());
                }
            } catch (Exception ignore) {}
        });
    }
    
    

    private void bind(){
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live);
        //binding.setMenuTitleHandler(new BaseWebViewActivity.MenuTitleHandler());
        ViewGroup container = binding.webviewWrapper;
        container.addView(mWebView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
       // lWebView=binding.webView;
        //focusChange();
    }


    protected void initWebChromeClient() {
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                isMenuShow=false;
                String url= view.getUrl();
                try {
                    url=  URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                LogUtil.i(TAG,"onProgressChangedX"+url);
                Vod vod = UpdateService.getByUrl(url);
                if(null!=vod){
                    currentLive=vod;
                    binding.liveName.setText(currentLive.getName()+" "+newProgress+"%");
                }
                if(newProgress==100){
                    HistoryDaoX.updateChannel(thisContext,url);
                    handler.sendMessageDelayed (handler.obtainMessage(2, "noText"),1000);
                }
                LogUtil.i("WebChromeClient", "onProgressChanged, newProgress:" + newProgress + ", view:" + view);
            }
            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                LogUtil.i("WebChromeClient","onShowCustomView");
                binding.fullscreen.addView(view);
                binding.fullscreen.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                LogUtil.i("WebChromeClient","onPermissionRequest "+request.getOrigin());
                LogUtil.i("WebChromeClient",request.getOrigin()+" "+ Arrays.toString(request.getResources()));
                request.deny();
            }
            @Override
            public void onHideCustomView() {
                LogUtil.i("WebChromeClient","onHideCustomView");
                binding.fullscreen.removeAllViews();
                binding.fullscreen.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected Object getJsInterface() {
        return new JsInterface();
    }

    private static boolean isMenuShow=false;
    public class JsInterface{

        @JavascriptInterface
        public void toast(String message){
            LogUtil.i(TAG,"message "+message);
            ToastUtils.show(MyApplication.getContext(),message, Toast.LENGTH_SHORT);
        }
        @JavascriptInterface
        public void message(String service,String data){
            LogUtil.i(TAG,"service "+service+" data "+data);
            if("menuShow".equals(service)){
                //Util.evalOnUi(lWebView,data);
                if(data.equals("1")){
                    isMenuShow =true;
                }else{
                    isMenuShow =false;
                }
                return;
            }
            if("js".equals(service)){
                Util.evalOnUi(mWebView,data);
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
            if("videoQuality".equals(service)){
                videoQualityData=data;
                return;
            }
            if("hideLoading".equals(service)){
                /* 会话进行中不理会页面来的收罩请求，避免中途撤罩露出骨架；
                   收罩统一由「主视频真的在播」判定，另有会话超时兜底。 */
                if (!sessionActive) { hideLoadingOverlay(); }
                return;
            }
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

    private Runnable hideLoadingRunnable = null;

    private AnimatorSet pulseSet = null;

    private Runnable loadingTickRunnable = null;
    /**
     * 连续会话的起算点：从切台那一刻起算，中途页面跳转不重置，
     * 遮罩上的秒数就是「从切台到出画面」的完整耗时。
     */
    private long sessionStartAt = 0L;
    /** 会话进行中：期间任何页面加载完成都不收罩，只等视频真的出来（或会话超时）。 */
    private boolean sessionActive = false;
    /** 会话内第几次进入视频页，用于阶段文案与「浏览页放行」判断。 */
    private int sessionPageCount = 0;

    /**
     * 央视页 onPageFinished 只代表网页框架到位，脱壳还没开始。
     * 黑遮罩已从 5 秒缩到 1.2 秒，若原生遮罩此时就收，1.2 秒后同样露出裸骨架，
     * 因此等「全屏/脱壳」标记立住再收。
     */
    private static final long FS_POLL_MAX_MS = 12000L;
    /** 300ms → 150ms：视频就绪后最多还要等一个轮询周期才收罩，缩短它。 */
    private static final long FS_POLL_INTERVAL_MS = 150L;
    private static final long LOADING_FALLBACK_MS = 15000L;
    /** 一次切台的连续会话上限：期间全程不收罩、计时不归零，超时兜底放行。 */
    private static final long SESSION_MAX_MS = 18000L;

    /**
     * 会话基线的合理上限：正常一次等待最长也就 SESSION_MAX_MS（遮罩到点必收）。
     * 若 sessionStartAt 比「现在」早了超过这个值，说明它是上一次等待遗留的脏基线，
     * 必须重新起算，否则计时器一上来就显示 4000s+ 的脏数字（与 BaseWebViewActivity 同源修复）。
     */
    private static final long SESSION_STALE_MS = SESSION_MAX_MS + 5000L;

    /**
     * 连续这么多次探测不到 video 元素，就认定不是播放页，别再干等。
     * 与 BaseWebViewActivity 保持一致：24 次 × 150ms ≈ 3.6s，覆盖自动跳转的空档，
     * 避免遮罩在页面跳转中途收起而露出裸骨架。
     */
    private static final int FS_NOVIDEO_MAX = 24;

    /**
     * 「已脱壳干净、只剩播放器」但主视频还没出画面时的宽限（与 BaseWebViewActivity 同源）。
     *
     * makeFs() 只负责把播放器容器撑满 + 调一次 play()，此刻视频往往还在取流/缓冲；
     * 若这时立刻收罩，露出的正是「播放器黑屏 + 大片海报/播放键」。
     * 给它 3 秒出首帧，仍不出画面才放行（那时露的是播放器本体，不再是网页骨架）。
     */
    private static final long FS_CLEANED_GRACE_MS = 3000L;

    /**
     * 收罩判据是「视频真的出画面」，不是「网页加载完」——onPageFinished 之后
     * 节目内容往往还要再等 3~4 秒才出画面，早收就会露出裸骨架。
     * 分六种结果：playing 收罩；loading（播放器容器已就位、video 还没创建）继续等；
     * waiting（有 video 但未出画面）继续等；clean_wait（已脱壳但未出画面）宽限后放行；
     * cleaned（脱壳干净且无 video）收罩；novideo 累计到上限才放行。
     */
    private static final String FS_PROBE_JS =
            "(function(){try{"
            /* 是否已脱壳清理：只说明网页骨架被清掉，不等于画面出来了（见 clean_wait）。 */
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

    /**
     * 主视频一出画面，就顺手把播放器自己的「全力加载中…」气泡清掉。
     *
     * 该气泡由央视直播播放器动态插进播放器容器内部，躲过了 cctvFullscreen 的 cleanPage()
     * （后者只清理容器之外的元素），于是它压在已经开播的画面上 1~2 秒才自己收起——
     * 实测只在「CCTV直播」入口出现，点播入口没有。cctvFullscreen.js 里另有自动守候；
     * 这里是更准的一击：原生判定 playing 的那一刻就清。
     */
    private static final String KILL_PAGE_LOADING_JS =
            "(function(){try{if(window.__DXTV_KILL_LOADING__){window.__DXTV_KILL_LOADING__();}}catch(e){}})()";

    private static boolean isVideoPage(String url) {
        if (url == null) { return false; }
        return url.contains("tv.cctv.com") || url.contains("yangshipin.cn");
    }

    private Runnable fsPollRunnable = null;
    private String loadingPrefix = "正在脱壳网页框架…";
    /** 当前正在加载的频道名：换台时贴在遮罩文案前，让用户一眼知道切到了哪个台。 */
    private String loadingChannelName = "";
    private int fsNoVideoStreak = 0;
    /** clean_wait 首次出现的时刻，用于「已脱壳但未出画面」的宽限计时。 */
    private long cleanWaitSince = 0L;

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
     * 秒数按「整段会话」连续累加，页面跳转不归零，遮罩全程不中断。
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
                       避免渲染出 sessionStartAt 遗留的脏数字。 */
                    if (sessionStartAt <= 0L || now - sessionStartAt > SESSION_STALE_MS) {
                        sessionStartAt = now;
                    }
                    float sec = (now - sessionStartAt) / 1000f;
                    if (sec < 0f) { sec = 0f; }
                    /* 换台时把频道名放在最前面：黑屏那几秒用户最想知道「切到哪个台了」。 */
                    if (loadingChannelName != null && !loadingChannelName.isEmpty()) {
                        binding.loadingText.setText(String.format(Locale.getDefault(),
                                "%s · %s %.1fs", loadingChannelName, loadingPrefix, sec));
                    } else {
                        binding.loadingText.setText(
                                String.format(Locale.getDefault(), "%s %.1fs", loadingPrefix, sec));
                    }
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

    private void hideLoadingOverlay() {
        hideLoadingOverlayInternal(true);
    }

    /**
     * 列表页放行：收遮罩，并**结束本次会话**（基线归零）。
     *
     * 判据是「连续 3.6 秒探测不到 video」——用户停在频道/列表页浏览。
     * 此时必须把会话一并结束掉，否则 sessionStartAt 会一直保留旧值，下次切台时
     * onPageLoadStarted 见 sessionActive 仍为 true 就不重置基线，计时器一上来就是几千秒
     * （与 BaseWebViewActivity 同源的 4000s+ 脏数字问题）。
     */
    private void releaseOverlayForBrowsing() {
        hideLoadingOverlayInternal(true);
    }

    /**
     * @param endSession 收罩是否同时结束「切台 → 出画面」的连续会话。
     *                   真：正常收罩（视频已出画面 / 会话超时）；假：列表页浏览放行。
     */
    private void hideLoadingOverlayInternal(final boolean endSession) {
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
                    if (endSession) {
                        /* 遮罩收起 = 会话结束，下次切台重新起算。 */
                        sessionStartAt = 0L;
                        sessionActive = false;
                        sessionPageCount = 0;
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
            LogUtil.e(TAG, "hideLoadingOverlay error: " + e.getMessage());
        }
    }

    /**
     * 会话进行中不收罩：一次切台可能经历多次页面加载，
     * 中途收罩正是实测里「遮罩退了、骨架还在」的那 1 秒。
     */
    @Override
    protected void onPageLoadFinished(String url) {
        if (sessionActive && isVideoPage(url)) {
            if (binding != null && binding.loadingOverlay != null
                    && binding.loadingOverlay.getVisibility() != View.VISIBLE) {
                showLoadingOverlay();
            }
            startFsPoll();
            return;
        }
        hideLoadingOverlay();
    }

    /**
     * 进入视频页即开启连续会话，回到列表页则结束会话。
     * 会话期间遮罩不撤、计时不归零，整条链路看起来是一次加载。
     */
    @Override
    protected void onPageLoadStarted(String url) {
        if (isVideoPage(url)) {
            /* 会话基线缺失或已过期（如列表页放行后隔了很久才点节目）就重新起算，
               避免把上一次等待的旧基线当成这一次，导致计时器一上来就显示几千秒。 */
            if (!sessionActive || sessionStartAt <= 0L
                    || SystemClock.elapsedRealtime() - sessionStartAt > SESSION_STALE_MS) {
                sessionActive = true;
                sessionStartAt = SystemClock.elapsedRealtime();
                sessionPageCount = 0;
            }
            sessionPageCount++;
        } else {
            sessionActive = false;
            sessionStartAt = 0L;
            sessionPageCount = 0;
        }
        if (!sessionActive) { return; }
        if (binding == null || binding.loadingOverlay == null) { return; }
        if (binding.loadingOverlay.getVisibility() != View.VISIBLE) {
            showLoadingOverlay();
        }
    }

    private void startFsPoll() {
        try {
            stopFsPoll();
            if (binding == null || binding.loadingOverlay == null || mWebView == null) { return; }
            loadingPrefix = "正在加载视频…";
            fsNoVideoStreak = 0;
            cleanWaitSince = 0L;
            /* 超时按会话起点算，跨页共用一个窗口，避免多次跳转各自抻长。 */
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
                                /* 画面已出，但播放器自己的「全力加载中…」气泡还压在它上面。
                                   那个气泡归播放器管、清掉还会被显示回来（实测遮罩收了它还赖
                                   1~2 秒），所以不能只打一枪——连打一串盖住这段窗口，
                                   之后由页面里的 pageToastKill.js 常驻值守接续。 */
                                killLoadingToastTail(8);
                                hideLoadingOverlay();
                                return;
                            }
                            if (s.contains("clean_wait")) {
                                /* 已脱壳干净但主视频还没出画面：宽限 3 秒等首帧，
                                   否则露出的就是「黑屏 + 大片播放键/海报」。 */
                                if (cleanWaitSince == 0L) { cleanWaitSince = SystemClock.elapsedRealtime(); }
                                fsNoVideoStreak = 0;
                                if (SystemClock.elapsedRealtime() - cleanWaitSince >= FS_CLEANED_GRACE_MS) {
                                    stopFsPoll();
                                    hideLoadingOverlay();
                                }
                                return;
                            }
                            if (s.contains("cleaned")) {
                                /* 脱壳干净、且页面里连 video 都没有：只剩播放器容器，收罩。 */
                                stopFsPoll();
                                hideLoadingOverlay();
                                return;
                            }
                            if (s.contains("novideo")) {
                                fsNoVideoStreak++;
                                /* 只在「会话还停在第 1 页」时放行——那时可能只是落在
                                   栏目/列表页；一旦已经进到播放页，就绝不提前收罩。
                                   放行只收遮罩、不结束会话，随后点进播放页时计时与文案续上。 */
                                if (fsNoVideoStreak >= FS_NOVIDEO_MAX && sessionPageCount <= 1) {
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
     * 连打若干次「清播放器气泡」。
     *
     * 为什么不只打一枪：那个「全力加载中…」归播放器自己控制，清掉后还会被它显示回来，
     * 实测遮罩都收了它还赖在画面上 1~2 秒。8 × 260ms ≈ 2 秒，正好盖住这段窗口；
     * 更长的值守交给页面里的 pageToastKill.js。
     */
    private void killLoadingToastTail(final int times) {
        if (times <= 0 || mWebView == null) { return; }
        try { mWebView.evaluateJavascript(KILL_PAGE_LOADING_JS, null); } catch (Exception e) {}
        if (binding == null || binding.loadingOverlay == null) { return; }
        binding.loadingOverlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWebView == null) { return; }
                killLoadingToastTail(times - 1);
            }
        }, 260L);
    }

    private void loadLiveUrl(String url) {
        loadingPrefix = "正在脱壳网页框架…";
        /* 所有调用点都先设置 currentLive 再加载，这里直接取名字即可；
           URL 对不上（例如首次进入）就不显示频道名，避免贴错台。 */
        if (currentLive != null && currentLive.getUrl() != null
                && currentLive.getUrl().equals(url)) {
            loadingChannelName = currentLive.getName() == null ? "" : currentLive.getName();
        } else {
            loadingChannelName = "";
        }
        /* 从这一刻起算连续会话：后续页面跳转不重置计时，也不撤遮罩。 */
        sessionActive = true;
        sessionStartAt = SystemClock.elapsedRealtime();
        sessionPageCount = 0;
        showLoadingOverlay();
        mWebView.loadUrl(url);
    }

    private void initData() {
        favoriteService = FavoriteService.getInstance(this);
        
        new Thread(() -> {
            List<Live> result = UpdateService.getByLivesWithFavorites(this);
            
            runOnUiThread(() -> {
                provinces = result;
                currentProvinceIndex = currentLive.getTagIndex();
                showCurrentProvince();
            });
        }).start();
    }

    private Vod createVod(String name, String key, String url) {
        Vod vod = new Vod();
        vod.setName(name);
        vod.setKey(key);
        vod.setUrl(url);
        return vod;
    }

    private void showCurrentProvince() {
        if (provinces == null || provinces.isEmpty()) {
            binding.provinceName.setText("无数据");
            setupChannelList(new ArrayList<>());
            return;
        }
        
        if (currentProvinceIndex < 0) {
            currentProvinceIndex = 0;
        } else if (currentProvinceIndex >= provinces.size()) {
            currentProvinceIndex = provinces.size() - 1;
        }
        
        Live currentProvince = provinces.get(currentProvinceIndex);
        int count = currentProvince.getVods() == null ? 0 : currentProvince.getVods().size();
        binding.provinceName.setText(currentProvince.getName() + "(" + count + ")");
        setupChannelList(currentProvince.getVods());
        try {
            if (currentLive != null && currentLive.getTagIndex() == currentProvinceIndex) {
                int idx = Math.max(0, currentLive.getDetailIndex());
                if (binding.channelList.getAdapter() != null && binding.channelList.getCount() > 0) {
                    if (idx >= binding.channelList.getCount()) { idx = binding.channelList.getCount() - 1; }
                    final int finalIdx = idx;
                    binding.channelList.post(() -> {
                        if (!isExitDialogShowing) {
                            binding.channelList.setSelection(finalIdx);
                            binding.channelList.requestFocus();
                        }
                    });
                }
            }
        } catch (Throwable ignore) {}
    }

    private void setupChannelList(List<Vod> channels) {
        ArrayAdapter<Vod> adapter = new ArrayAdapter<Vod>(this, android.R.layout.simple_list_item_1, channels) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                Button btn;
                if (convertView == null) {
                    btn = new Button(getContext());
                    btn.setLayoutParams(new AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    btn.setTextColor(Color.WHITE);
                    btn.setTextSize(16);
                    btn.setPadding(24, 16, 24, 16);
                    btn.setBackgroundResource(R.drawable.menu_button_background);
                    btn.setClickable(false);
                    btn.setFocusable(false);
                } else {
                    btn = (Button) convertView;
                    
                    if (!(btn.getLayoutParams() instanceof AbsListView.LayoutParams)) {
                        btn.setLayoutParams(new AbsListView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                }
                
                Vod channel = getItem(position);
                if (channel != null) {
                    btn.setText(channel.getName());
                }
                
                return btn;
            }
        };
        binding.channelList.setAdapter(adapter);
        binding.channelList.setOnItemClickListener((parent, view, position, id) -> {
            try {
                Vod channel = channels.get(position);
                if (channel.getUrl() != null) {
                    currentLive = channel;
                    runOnUiThread(() -> {
                        try {
                            LogUtil.i(TAG, "Loading URL in WebView: " + channel.getUrl());
                            loadLiveUrl(channel.getUrl());
                            LogUtil.i(TAG, "URL loaded successfully");
                        } catch (Exception e) {
                            LogUtil.e(TAG, "Error loading URL in WebView: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    
                    HistoryDaoX.updateChannel(thisContext, channel.getUrl());
                    
                    showToast(channel.getName(), this);
                    
                    hideMenu();
                } else {
                    LogUtil.e(TAG, "Channel or URL is null");
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "Error handling channel click: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showMenu() {
        /* 打开菜单说明用户要浏览频道，撤销尚未落地的换台，避免选台瞬间又跳走。 */
        handler.removeMessages(1);
        binding.menuContainer.setVisibility(View.VISIBLE);
        isMenuShow = true;
        try {
            if (currentLive != null) {
                currentProvinceIndex = currentLive.getTagIndex();
            }
        } catch (Throwable ignore) {}
        showCurrentProvince();
        setupProvinceButtons();
        try {
            int sel = 0;
            if (currentLive != null) { sel = Math.max(0, currentLive.getDetailIndex()); }
            if (binding.channelList.getAdapter() != null && binding.channelList.getCount() > 0) {
                if (sel >= binding.channelList.getCount()) { sel = binding.channelList.getCount() - 1; }
                binding.channelList.setSelection(sel);
                binding.channelList.requestFocus();
            }
        } catch (Throwable ignore) {}
        
        binding.menuContainer.setOnClickListener(v -> hideMenu());
    }

    private void setupProvinceButtons() {
        binding.prevProvinceArea.setOnClickListener(v -> {
            currentProvinceIndex--;
            if (currentProvinceIndex < 0) {
                currentProvinceIndex = provinces.size() - 1;
            }
            showCurrentProvince();
        });

        binding.nextProvinceArea.setOnClickListener(v -> {
            currentProvinceIndex++;
            if (currentProvinceIndex >= provinces.size()) {
                currentProvinceIndex = 0;
            }
            showCurrentProvince();
        });
    }

    private void hideMenu() {
        binding.menuContainer.setVisibility(View.GONE);
        isMenuShow = false;
        binding.menuContainer.setOnClickListener(null);
    }


    public class HzLiveBindPresenter implements com.daoxuan.cctv.impl.IBaseBindingPresenter {
        public void onClick(HzItem item){
            if(item.getAction()!=null&&item.getAction().trim().length()>0){
                Util.evalOnUi(mWebView,item.getAction());
            }else if(item.getId()!=null){
                String js = "$$(\\\"#"+item.getId()+"\\\").click()";
                Util.evalOnUi(mWebView,js);
            }
        }
    }
    
    private void toggleFavorite(Vod vod) {
        if (favoriteService.isFavorite(vod.getUrl())) {
            favoriteService.removeFavorite(vod.getUrl());
            ToastUtils.show(this, "已取消收藏: " + vod.getName(), Toast.LENGTH_SHORT);
        } else {
            favoriteService.addFavorite(vod);
            ToastUtils.show(this, "已收藏: " + vod.getName(), Toast.LENGTH_SHORT);
        }
        
        initData();
    }


    private final StringBuilder digitBuffer = new StringBuilder();
    private final Handler digitInputHandler = new Handler(Looper.getMainLooper());
    private static final int DIGIT_TIMEOUT_MS = 1000;
    private final Runnable commitDigitRunnable = new Runnable() {
        @Override
        public void run() {
            String s = digitBuffer.toString();
            digitBuffer.setLength(0);
            if (s.isEmpty()) return;
            try {
                int num = Integer.parseInt(s);
                jumpToFavoriteByNumber(num);
            } catch (NumberFormatException ignore) {}
        }
    };
    private boolean isDigitKey(int keyCode) {
        return keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9;
    }
    private char digitFromKeyCode(int keyCode) {
        return (char) ('0' + (keyCode - KeyEvent.KEYCODE_0));
    }

    private void jumpToFavoriteByNumber(int num) {
        Live favoriteLive = null;
        for (Live l : provinces) {
            if ("favorite".equals(l.getTag())) {
                favoriteLive = l;
                break;
            }
        }
        if (favoriteLive == null || favoriteLive.getVods() == null) {
            return;
        }
        int idx = num - 1; // 1-based -> 0-based
        if (idx < 0 || idx >= favoriteLive.getVods().size()) {
            return;
        }
        Vod channel = favoriteLive.getVods().get(idx);
        currentLive = channel;

        runOnUiThread(() -> {
            try {
                loadLiveUrl(channel.getUrl());
            } catch (Exception e) {
                LogUtil.e(TAG, "Error loading URL: " + e.getMessage());
            }
        });

        try {
            HistoryDaoX.updateChannel(thisContext, channel.getUrl());
        } catch (Throwable ignore) {}
        showToast(channel.getName(), this);
        hideMenu();
    }
}