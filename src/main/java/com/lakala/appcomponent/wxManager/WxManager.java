package com.lakala.appcomponent.wxManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.ByteArrayOutputStream;

/**
 * 微信分享
 *
 * @author dingqiqi
 */
public class WxManager {

    public static IWXAPI mIWxApi;

    //是否注册
    private boolean mIsRegister;

    public static String mAppId;

    public static String mAppSecret;

    private Context mContext;

    private static WxCallBack mWxCallBack;

    public WxManager(Context context, String appId, String appSecret) {
        mContext = context.getApplicationContext();
        mAppId = appId;
        mAppSecret = appSecret;
        mIWxApi = WXAPIFactory.createWXAPI(context, appId);

        mIsRegister = mIWxApi.registerApp(appId);
    }

    public static void sendSuccess(Object result) {
        if (mWxCallBack != null) {
            mWxCallBack.onSuccess(result);
        }
    }

    public static void sendFailed(String result) {
        if (mWxCallBack != null) {
            mWxCallBack.onFailed(result);
        }
    }

    public boolean isWXAppInstalled() {
        return mIWxApi.isWXAppInstalled();
    }

    /**
     * 第三方登录
     */
    public void wxAuthLogin(WxCallBack wxCallBack) {
        mWxCallBack = wxCallBack;

        if (!isWXAppInstalled()) {
            Toast.makeText(mContext, "微信客户端未安装!", Toast.LENGTH_SHORT).show();
            return;
        }

        //未注册
        if (!mIsRegister) {
            mIsRegister = mIWxApi.registerApp(mAppId);
        }

        //发起登录请求
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "wxAuthLogin";
        mIWxApi.sendReq(req);
    }

    /**
     * 微信分享
     *
     * @param type        friendCircle（朋友圈），friends（好友）
     * @param url         分享地址
     * @param title       标题
     * @param description 描述
     * @param filePath    图片文件地址
     */
    public void shareMessage(String type, String url, String title,
                             String description, String filePath, WxCallBack wxCallBack) {
        mWxCallBack = wxCallBack;

        if (!isWXAppInstalled()) {
            Toast.makeText(mContext, "微信客户端未安装!", Toast.LENGTH_SHORT).show();
            return;
        }

        //未注册
        if (!mIsRegister) {
            mIsRegister = mIWxApi.registerApp(mAppId);
        }

        WXWebpageObject webPage = new WXWebpageObject();
        webPage.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage(webPage);
        msg.title = title;
        msg.description = description;

        Bitmap bmp;

        if (TextUtils.isEmpty(filePath)) {
            bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.share_default_icon);
        } else {
            bmp = ImageUtil.FileToBitmap(filePath);
        }

        if (bmp != null) {
            //图片宽高
            int THUMB_SIZE = 150;

            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
            bmp.recycle();
            msg.thumbData = bmpToByteArray(thumbBmp);
        } else {
            msg.thumbData = null;
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;

        //发送给朋友圈关键标志
        if ("friendCircle".equals(type)) {
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
            boolean result = mIWxApi.sendReq(req);

            if (!result) {
                sendFailed("分享失败!");
            }
        } else if ("friends".equals(type)) {
            //会话
            req.scene = SendMessageToWX.Req.WXSceneSession;
            boolean result = mIWxApi.sendReq(req);

            if (!result) {
                sendFailed("分享失败!");
            }
        } else {
            Log.e("WxManager", "分享类型错误");
            sendFailed("分享类型错误!");
        }

    }

    private byte[] bmpToByteArray(final Bitmap bmp) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        bmp.recycle();

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public interface WxCallBack {
        void onSuccess(Object object);

        void onFailed(String msg);
    }
}
