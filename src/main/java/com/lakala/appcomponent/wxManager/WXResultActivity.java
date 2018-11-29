package com.lakala.appcomponent.wxManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

/**
 * 微信分享Activity 只能在packageName.wxapi包名下,只能是WXEntryActivity这个名称
 */
public class WXResultActivity extends Activity implements IWXAPIEventHandler {

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
                if (resp instanceof SendAuth.Resp) {
                    WxManager.sendResult((SendAuth.Resp) resp);
                    finish();
                    return;
                } else {
                    result = getString(R.string.errcode_success);
                }
                break;
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

        WxManager.sendResult(result);
        finish();
    }
}
