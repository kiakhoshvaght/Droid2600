package Api;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import ModelClasses.BarangErrorResponse;
import retrofit2.adapter.rxjava.HttpException;

public abstract class BaseApi {

    private static final String TAG = BaseApi.class.getName();


    abstract public void init();

    public static BarangErrorResponse readError(Throwable throwable) {
        Log.i(TAG, "FUNCTION : readError");
        InputStream in = null;
        if (((HttpException) throwable).response().errorBody() != null) {
            in = ((HttpException) throwable).response().errorBody().byteStream();
        }
        BufferedReader reader;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            BarangErrorResponse error = new Gson().fromJson(sb.toString(), BarangErrorResponse.class);
            return error;
        } catch (Exception ignored) {
            Log.i(TAG, "FUNCTION : readError => Error reading error :D");
        }
        return null;
    }
}
