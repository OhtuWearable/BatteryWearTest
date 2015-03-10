package test.com.batteryweartest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView textView;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                textView = (TextView) stub.findViewById(R.id.text);
                textView.setText("");
            }
        });
        intent = this.registerReceiver(BatteryMonitor, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    //Unregister the receiver on destroy;
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.BatteryMonitor);
    }



    /**
     * BroadcastReceiver which listens to changes in the battery state
     */
    private BroadcastReceiver BatteryMonitor;

    {
        BatteryMonitor = new BroadcastReceiver() {
            Integer scale = -1;
            Integer level = -1;
            int voltage = -1;


            @Override
            public void onReceive(Context context, Intent intent) {

                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                Integer status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                //when battery is changed, this clause is called:
                if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                    long time = System.currentTimeMillis() / 1000L;
                    //System.out.println(time + " and percent:" + level);

                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                    Integer percent = (level * 100) / scale;

                    if (textView != null) {
                        if (isCharging) {
                            //do nothing if the battery is charging
                            //or start services again if recovering after shutting things down
                            textView.setText("Battery is charging: " + percent + "%");
                        } else if (!isCharging) {
                            if (percent < 20) {
                                //if battery is low, shut down things
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
}
