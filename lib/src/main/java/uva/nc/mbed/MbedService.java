package uva.nc.mbed;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import uva.nc.LocalServiceBinder;

public class MbedService extends Service {

    private static final String TAG = MbedService.class.getName();

    private final LocalServiceBinder<MbedService> binder = new LocalServiceBinder<MbedService>(this);
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                manager.close();
            }
        }
    };

    public MbedManager manager;

    @Override
    public void onCreate() {
        super.onCreate();

        manager = new MbedManager(this);

        Log.i(TAG, "mBed service created");

        // Register broadcast receiver.
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "mBed service destroyed");
        unregisterReceiver(receiver);
        manager.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
