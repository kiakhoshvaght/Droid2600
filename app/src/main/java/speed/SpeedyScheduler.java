package speed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SpeedyScheduler extends BroadcastReceiver {

    private static final String TAG = SpeedyScheduler.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"FUNCTION : onReceive");
        try {
            context.startService(new Intent(context, SpeedService.class));
        } catch (Exception e) {
            Log.i(TAG,"FUNCTION : onReceive => onError: " + e.toString());
            e.printStackTrace();
        }
    }
}
