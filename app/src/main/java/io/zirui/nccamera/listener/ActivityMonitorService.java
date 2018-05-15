package io.zirui.nccamera.listener;

import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import io.zirui.nccamera.storage.LocalSPData;

public class ActivityMonitorService extends Service {
    private Handler durationTracker = new Handler();
    int millisBetweenUpdates = 1000;
    public static int trackingInterval = 180000;
    private static ActivityMonitorService serviceInstance;
    String lastApp = "";
    public View recordingIcon = null;

    private boolean isRecording = false;
    private Semaphore recordingMutex = new Semaphore(1);
    private MediaRecorder storedRecorder = null;
    private String storedFileName = null;
    private int storedRecordingID = -1;
    private String id;
    private static final String SP = "share_preference";
    private static final String spTimer = "timer";
    private Context context;
    private Location mLastLocation;
    private String audFileName;
    private String filePrefix = "Aud";


    public static ActivityMonitorService getServiceInstance() {
        return serviceInstance;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String currentApp = currentForegroundApp();
            if (lastApp == null) {
                if (currentApp != null && isTargetApp(currentApp)) {
                    if (!isRecording) {
                        serviceInstance.startAudioRecording();
                    }
                }
            } else if (currentApp == null) {
                if (lastApp != null && isTargetApp(lastApp)) {
                    if (isRecording) {
                        stopAudioRecording(storedRecorder, storedFileName, storedRecordingID);
                    }
                }
            } else if (!currentApp.equals(lastApp)) {
                if (isTargetApp(lastApp)) {
                    if (isRecording) {
                        stopAudioRecording(storedRecorder, storedFileName, storedRecordingID);
                    }
                } else if (isTargetApp(currentApp)) {
                    if (!isRecording) {
                        serviceInstance.startAudioRecording();
                    }
                }
            }

            lastApp = currentApp;

            SharedPreferences sp = getSharedPreference(getApplicationContext());
            long startTime = sp.getLong(spTimer, 0);
            if (startTime == 0 || System.currentTimeMillis() < (startTime + (1000 * 60 * 60 * 24 * 14))) {
                durationTracker.postDelayed(this, millisBetweenUpdates);
            }
        }
    };

    private static SharedPreferences getSharedPreference(@NonNull Context context){
        return context.getApplicationContext().getSharedPreferences(
                SP, Context.MODE_PRIVATE);
    }

    public ActivityMonitorService(Context context, Location mLastLocation) {
        this.context = context;
        this.mLastLocation = mLastLocation;
        id = LocalSPData.loadRandomID(context).substring(0, 7);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        SharedPreferences sp = getSharedPreference(context);
        if (sp.getLong(spTimer, 0) == 0) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(spTimer, System.currentTimeMillis());
            editor.commit();
        }

        super.onCreate();
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        ActivityMonitorService.serviceInstance = this;
        durationTracker.postDelayed(runnable, millisBetweenUpdates);
    }

    public boolean isTargetApp(String name) {
        if (name.equals("org.pbskids.cmc") ||
                name.equals("org.pbskids.dtigerexploreneighborhood")) {
            return true;
        }

        return false;
    }

    public String currentForegroundApp() {
        String currentApp = null;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }
        return currentApp;
    }

    public void startAudioRecording() {
        Log.e("Recording","Start Recording 1");
        try {
            Log.e("Recording","Start Recording 2");
            recordingMutex.acquire();
            if (isRecording) return;

            Random random = new Random();
            final int recordingID = random.nextInt(10000);
            storedRecordingID = recordingID;

            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/AppTracking");
            dir.mkdir();

            SharedPreferences sp = getSharedPreference(context);

            final String audioFilename = dir.getAbsolutePath() + "/" + id + "_" + System.currentTimeMillis() + ".3gpp";
            final MediaRecorder recorder = new MediaRecorder();

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFilename);

            try {
                Log.e("Recording","Attempting to prepare and start rec");
                recorder.prepare();
                recorder.start();
                isRecording = true;

                storedRecorder = recorder;
                storedFileName = audioFilename;

                Runnable stopAudioRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Recording","Attempt to Stop");
                        stopAudioRecording(recorder, audioFilename, recordingID);
                    }
                };

                Handler handler = new Handler();
                handler.postDelayed(stopAudioRunnable, 10000);
            } catch (Exception e) {
                Log.e("Recording Exception1", e.getMessage());
            }
        } catch (Exception e) {
            Log.e("Recording Exception2", e.getMessage());
        } finally {
            recordingMutex.release();
        }
    }


    public void stopAudioRecording(MediaRecorder recorder, String audioFilename, int recordingID) {
        Log.e("Recording","Stop Recording 1");
        if (!isRecording) {
            return;
        }

        try {
            Log.e("Recording","Stop Recording 2");
            recordingMutex.acquire();
            if (!isRecording) {
                return;
            }

            if (recordingID != storedRecordingID) {
                return;
            }

            recorder.stop();
            recorder.reset();
            recorder.release();

            isRecording = false;

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String locationStamp = mLastLocation != null ? mLastLocation.getLongitude() + "_" + mLastLocation.getLatitude(): "null";
            locationStamp = "[" + locationStamp + "]_";
            audFileName = filePrefix + "_" + timeStamp + "_" + locationStamp + "_" +id;

            Uri contentUri = Uri.fromFile(new File(audioFilename));
            String fileName = audFileName + ".3gpp";
            String path = id +"/audio/"+ fileName;
            // path to all images
            StorageReference all = FirebaseStorage.getInstance().getReference("all/audio/"+fileName);

            // path to user
            StorageReference ref = FirebaseStorage.getInstance().getReference(path);
            Log.e("Recording", "Start Upload");
            all.putFile(contentUri);
            ref.putFile(contentUri);
        } catch (Exception e) {

        } finally {
            storedFileName = null;
            storedRecorder = null;
            recordingMutex.release();
        }
    }
}