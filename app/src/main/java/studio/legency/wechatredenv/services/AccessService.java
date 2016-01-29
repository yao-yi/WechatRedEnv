package studio.legency.wechatredenv.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RelativeLayout;

import com.apkfuns.logutils.LogUtils;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;

import studio.legency.wechatredenv.configs.DingInfo;
import studio.legency.wechatredenv.configs.WechatInfo;
import studio.legency.wechatredenv.data.WechatRedEnvHis;
import studio.legency.wechatredenv.helpers.Common;
import studio.legency.wechatredenv.helpers.DingEventHelper;
import studio.legency.wechatredenv.helpers.NodeFinder;
import studio.legency.wechatredenv.helpers.WechatEventHelper;

/**
 * Created by Administrator on 2015/9/30.
 */
@EService
public class AccessService extends AccessibilityService {

    @Bean
    WechatEventHelper wechatEventHelper;

    @Bean
    DingEventHelper dingEventHelper;

    private PowerManager.WakeLock lock;

    private RelativeLayout relativeLayout;

    @Override
    protected void onServiceConnected() {
        LogUtils.d("微信服务已连接");
        keepScreenOn();
        cleanData();
        configService();
        super.onServiceConnected();
    }

    private void keepScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        lock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "LOCK");

        //请求常亮
        lock.acquire();
    }

    private void cleanData() {
        LogUtils.d("清空数据库");
        WechatRedEnvHis.deleteAll(WechatRedEnvHis.class);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    void configService() {
        AccessibilityServiceInfo accessibilityServiceInfo = getServiceInfo();
        if (accessibilityServiceInfo == null)
            accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
//                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED |
//                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_VIEW_LONG_CLICKED |
//                AccessibilityEvent.TYPE_VIEW_CLICKED;
        accessibilityServiceInfo.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        accessibilityServiceInfo.packageNames = new String[]{WechatInfo.package_name, DingInfo.package_name};
        accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        accessibilityServiceInfo.notificationTimeout = 10;
        setServiceInfo(accessibilityServiceInfo);
        // 4.0之后可通过xml进行配置,以下加入到Service里面
        /*
         * <meta-data android:name="android.accessibilityservice"
		 * android:resource="@xml/accessibility" />
		 */
    }

    @Bean
    NodeFinder nodeFinder;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        try {
            if (event == null) {
                return;
            }

            if (Common.is_view_test()) {
                int[] types = {AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_TOUCH_INTERACTION_START};
//                int[] types = {AccessibilityEvent.TYPE_TOUCH_INTERACTION_START};
                if (inType(types, event.getEventType())) {
                    AccessibilityNodeInfo nodeInfo = event.getSource();
                    if (nodeInfo == null) return;
                    nodeFinder.debugNode(nodeInfo);
                    showWindow(nodeInfo.getParent().getParent());
                }
                return;
            }

            if (WechatInfo.package_name.equals(event.getPackageName())) {
                wechatEventHelper.handleEvent(event);
            } else {
                dingEventHelper.handleEvent(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean inType(int[] types, int type) {
        for (int type1 : types) {
            if (type == type1) return true;
        }
        return false;
    }

    private void showWindow(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;
        int type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                        type = WindowManager.LayoutParams.TYPE_TOAST;
//                    } else {
//                        type = WindowManager.LayoutParams.TYPE_PHONE;
//                    }
//                    Log.d("ttttt", type + "asd");
        if (relativeLayout == null) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    type
                    ,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);
            params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            relativeLayout = new RelativeLayout(this);
            relativeLayout.setBackgroundColor(Color.argb(55, 200, 100, 100));
            wm.addView(relativeLayout, params);
        } else {
            relativeLayout.removeAllViews();
        }
        addViewToLayout(nodeInfo);
    }

    public void addViewToLayout(AccessibilityNodeInfo info) {
        if (info == null) return;
        addView(info, relativeLayout);
        if (info.getChildCount() != 0) {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    addViewToLayout(info.getChild(i));
                }
            }
        }
    }

    private void addView(AccessibilityNodeInfo info, RelativeLayout relativeLayout) {
        Rect rect = new Rect();
        info.getBoundsInScreen(rect);
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        View view = new View(this);
        relativeLayout.addView(view);
        RelativeLayout.LayoutParams l = (RelativeLayout.LayoutParams) view.getLayoutParams();
        l.leftMargin = rect.left;
        l.topMargin = rect.top;
        l.width = w;
        l.height = h;
        view.setBackgroundColor(Color.argb(55, 100, 100, 200));
    }

    @Override
    public void onInterrupt() {
        LogUtils.e("服务意外关闭");
//        lock.release();
    }
}
