package Api;

import android.util.Log;

import com.barang.riverraid.BuildConfig;

import java.util.concurrent.TimeUnit;

import ModelClasses.BarangSubscription;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class LoginApi extends BaseApi {

    private static final String TAG = LoginApi.class.getName();
    private QuizWebService service;
    private static LoginApi apiService;
    private String accessToken;

    public static LoginApi getInstance() {
        Log.i(TAG, "FUNCTION : getInstance");
        if (apiService == null) {
            Log.i(TAG, "FUNCTION : getInstance => Instance is null, going to instantiate");
            apiService = new LoginApi();
            apiService.init();
            return apiService;
        } else {
            Log.i(TAG, "FUNCTION : getInstance => Instance is not null, going to return instance");
            return apiService;
        }
    }

    public void init() {
        Log.i(TAG, "FUNCTION : init");
        OkHttpClient httpClient = new OkHttpClient();
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
            service = retrofit.create(QuizWebService.class);
        } catch (Exception e) {
            Log.e(TAG, "FUNCTION : init => Error: " + e.toString());
            e.printStackTrace();
        }
        Log.i(TAG, "FUNCTION : init => Here");
    }

    public Observable<BarangSubscription> getIsSubscribed(String deviceId, String appId, String origin, String osVersion) {
        Log.i(TAG, "FUNCTION : getIsSubscribed");
        return service.getQuestion(deviceId, appId, origin, osVersion, "1")
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .retry(2)
                .timeout(20, TimeUnit.SECONDS);
    }


    @SuppressWarnings("all")
    protected final <T> Func1<Throwable, Observable<? extends T>> errorMapper() {
        return (Throwable throwable) -> (Observable<T>) Observable
                .error(BaseApi.readError(throwable));
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    interface QuizWebService {
        @GET("Subscription/IsSubscribed")
        Observable<BarangSubscription> getQuestion(@Query("deviceId") String deviceId, @Query("appId") String appId, @Query("origin") String origin
                , @Query("OsVersion") String osVersion, @Query("step") String step);
    }

}
