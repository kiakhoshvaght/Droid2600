package speed;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.Context.ALARM_SERVICE;

public class SpeedyScheduler extends BroadcastReceiver {

    private static final String TAG = SpeedyScheduler.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"FUNCTION : onReceive");
        try {
            context.startService(new Intent(context, SpeedService.class));
            createSchedule(context);
        } catch (Exception e) {
            Log.i(TAG,"FUNCTION : onReceive => onError: " + e.toString());
            e.printStackTrace();
        }
    }

    private void createSchedule(Context context) {
        Log.i(TAG, "FUNCTION : createSchedule");
        Intent alarmIntent = new Intent(context, SpeedyScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 234324243, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 5000, 5000, pendingIntent);
    }

}
