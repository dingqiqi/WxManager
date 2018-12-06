package com.lakala.appcomponent.wxManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 微信分享Activity 只能在packageName.wxapi包名下,只能是WXEntryActivity这个名称
 */
public class WXResultActivity extends Activity implements IWXAPIEventHandler {

    private Executor mExecutor = Executors.newFixedThreadPool(1);

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            WxManager.mIWxApi.handleIntent(getIntent(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        WxManager.mIWxApi.handleIntent(intent, this);
    }

    // 微信发送请求到第三方应用时，会回调到该方法
    @Override
    public void onReq(BaseReq req) {

    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    @Override
    public void onResp(BaseResp resp) {
        String result;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                //登陆成功
                if (resp instanceof SendAuth.Resp) {
                    getToken((SendAuth.Resp) resp);
                    return;
                } else {
                    result = getString(R.string.errcode_success);
                    WxManager.sendSuccess(result);
                    finish();
                    return;
                }
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = getString(R.string.errcode_cancel);
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = getString(R.string.errcode_deny);
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                result = getString(R.string.errcode_unsupported);
                break;
            default:
                result = getString(R.string.errcode_unknown);
                break;
        }

        WxManager.sendFailed(result);
        finish();
    }

    /**
     * 获取token
     *
     * @param resp
     */
    private void getToken(SendAuth.Resp resp) {
        final Uri.Builder builder = Uri.parse("https://api.weixin.qq.com/sns/oauth2/access_token").buildUpon();
        builder.appendQueryParameter("appid", WxManager.mAppId);
        builder.appendQueryParameter("secret", WxManager.mAppSecret);
        builder.appendQueryParameter("code", resp.openId);
        builder.appendQueryParameter("grant_type", "authorization_code");

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = null;
                try {
                    URL url = new URL(builder.toString());
                    //打开连接
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.connect();

                    if (200 == urlConnection.getResponseCode()) {
                        //得到输入流
                        inputStream = urlConnection.getInputStream();
                        outputStream = new ByteArrayOutputStream();

                        byte[] buffer = new byte[1024];
                        int len;
                        while (-1 != (len = inputStream.read(buffer))) {
                            outputStream.write(buffer, 0, len);
                        }

                        outputStream.flush();

                        mResult = outputStream.toString("utf-8");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (TextUtils.isEmpty(mResult)) {
                        WxManager.sendFailed("获取token失败");
                        finish();
                    } else {
                        getUserInfo(mResult);
                    }
                }
            }
        });

    }

    private String mResult;

    private void getUserInfo(String result) {

        String token = null, openId = null;
        try {
            JSONObject jsonObject = new JSONObject(result);

            token = jsonObject.optString("access_token");
            openId = jsonObject.optString("openid");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //获取参数失败
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(openId)) {
            WxManager.sendFailed("token或者openId为空");
            finish();
            return;
        }

        final Uri.Builder builder = Uri.parse("https://api.weixin.qq.com/sns/userinfo").buildUpon();
        builder.appendQueryParameter("access_token", token);
        builder.appendQueryParameter("openid", openId);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = null;
                try {
                    URL url = new URL(builder.toString());
                    //打开连接
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.connect();

                    if (200 == urlConnection.getResponseCode()) {
                        //得到输入流
                        inputStream = urlConnection.getInputStream();
                        outputStream = new ByteArrayOutputStream();

                        byte[] buffer = new byte[1024];
                        int len;
                        while (-1 != (len = inputStream.read(buffer))) {
                            outputStream.write(buffer, 0, len);
                        }

                        outputStream.flush();

                        mResult = outputStream.toString("utf-8");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(mResult)) {
                                WxManager.sendFailed("用户信息获取失败");
                            } else {
                                WxManager.sendSuccess(mResult);
                            }

                            finish();
                        }
                    });
                }
            }
        });
    }
}
