package test.com.batteryweartest;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * MainActivity for BatteryWearTest:
 * The program tracks battery usage of the wear watch (though isn't really optical).
 * It logs battery consumption results through Logcat.
 */
public class MainActivity extends Activity {
    int batteryLevel;
    TextView textView;
    TextView textView2;
    Intent intent;
    Button bn1;
    static BatteryMonitorData bmData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                bmData = new BatteryMonitorData();
                textView = (TextView) stub.findViewById(R.id.text);
                textView2 = (TextView) stub.findViewById(R.id.text2);
                textView2.setText("App is starting");
                textView2.setText("");
                bn1 = (Button) stub.findViewById(R.id.btnaddnewtext1);
                bn1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bmData != null) {
                            Intent monitorIntent = new Intent(v.getContext(), BatteryService.class);
                            /** Give monitorintent the intent used for BatteryMonitor so the values update in foreground service */
                            v.getContext().startService(monitorIntent);
                        }
                    }
                });


            }});
    }

    /**
     * Get current battery level
     * @return Integer of current battery level
     */
    public int getBatteryLevel() {
        return this.batteryLevel;
    }

    /** Set current battery level. */
    public void setBatteryLevel(int i) {
        this.batteryLevel = i;
    }



    /**
     * BroadcastReceiver which listens to changes in the battery state;
     * updates the batteryLevel on MainActivity whenever battery level changes.
     * Also updates the TextView with battery info on MainActivity.
    */
    private static BroadcastReceiver BatteryMonitor;

    {
        BatteryMonitor = new BroadcastReceiver() {
            Integer scale = -1;
            Integer level = -1;
            int voltage = -1;

            @Override
            public void onReceive(Context context, Intent intent) {

                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                /** Set batteryLevel for MainActivity every time it changes: */
                setBatteryLevel(level);
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                Integer status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                /** Whenever battery is changed, this clause is called: */
                if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                    Integer percent = (level * 100) / scale;

                    if (textView != null) {
                        if (isCharging) {
                            textView.setText("Battery is charging: " + percent + "%");
                        } else if (!isCharging) {
                            if (percent < 20) {
                                textView.setText("Battery is getting low: " + percent + "%");
                            } else {
                                textView.setText("Battery charge is " + percent + "%");
                            }
                        }
                    }
                }
            }
        };


}
    /**
     * A class to track battery usage every x seconds;
     * Includes a scheduled event which print current battery state every minute in the Logcat log.
     */

    public class BatteryMonitorData {
        int level;
        ArrayList<Long> times;
        ArrayList<Integer> levels;

        final ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);

        /** Saves battery timestamps and battery level to two different ArrayLists and prints the current data. */
        public void getBatteryDataEveryMinute() {
            Log.d("BatteryMonitorData", "Starting logging");
            times = new ArrayList<>();
            levels = new ArrayList<>();
            final long start = System.currentTimeMillis() / 1000L;
            level = getBatteryLevel();
            final Runnable logger = new Runnable() {
                public void run() {
                    long time = System.currentTimeMillis() / 1000L;
                    level = getBatteryLevel();
                    Log.i("BatteryMonitorData", "Time: " + (time - start) + " Level: " + level);
                    times.add(time);
                    levels.add(level);
                }
            };

            /** Starts logging items after 10 seconds, and repeats task every x seconds */
            final ScheduledFuture loggerHandle = scheduler.scheduleAtFixedRate(logger, 10, 60, SECONDS);

            /** Schedule runs for 60*x seconds (= x minutes) */
            scheduler.schedule(new Runnable() {
                public void run() {
                    loggerHandle.cancel(true);
                    if (loggerHandle.isDone()) {
                        Log.d("BatteryMonitorData", "Beeperhandler is done ");
                        scheduler.shutdown();
                        printData(times, levels);
                    }
                }
            }, 60 * 60, SECONDS);
        }

        /** Handling recorded data:
         * After the recording, this method prints stats for the data which has been collected.
         * Prints total time and battery level dropped.
         * **/
        public void printData(ArrayList<Long> times, ArrayList<Integer> levels) {
            Log.d("BatteryMonitorData", "Handling data: ");
            if (times != null && levels != null) {
                if (times.size() > 0 && levels.size() > 0) {
                    Long first = times.get(0);
                    int lastInt = times.size()-1;
                    Long last = times.get(lastInt);

                    int max = levels.get(0);
                    lastInt = levels.size()-1;
                    int min = levels.get(lastInt);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);

                    Log.i("BatteryMonitorData2: ", "Battery usage stats: \n Recorded data for " + (last - first) + " seconds. \n Battery level dropped " + ((max - min) * 100 / scale) + "%");

                    if (textView2 != null) {
                        final int help = (max - min) * 100 / scale;
                        final long help2 = last - first;

                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView2.append("Battery usage stats: \n Recorded data for " + help2 + " seconds. \n Battery level dropped " + help + "%");
                                }
                            });
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                }

            }
    }

    /** Service which starts battery monitoring as a foreground service */
    public static class BatteryService extends Service {
        public BatteryService() {
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("","start command");
            if (intent != null) {
                Log.d("BatteryService", "Starting foreground service");
                intent = this.registerReceiver(BatteryMonitor, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                bmData.getBatteryDataEveryMinute();
            }
            if (intent == null) {
                Log.d("BatteryService", "Intent not found");
            }
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        /** Unregister the receiver on destroy */
        public void onDestroy() {
            Log.d("BatteryService", "Terminating batteryService");
            super.onDestroy();
            this.unregisterReceiver(BatteryMonitor);
        }
    }

}
