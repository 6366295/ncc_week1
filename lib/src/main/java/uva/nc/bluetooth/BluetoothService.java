package uva.nc.bluetooth;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.UUID;

import uva.nc.LocalServiceBinder;

public class BluetoothService extends Service {

    private static final String TAG = BluetoothService.class.getName();

    public static final int SERVICE_PORT = 15; // [1-30]

    public MasterManager master;
    public SlaveManager slave;
    public BluetoothUtility utility;

    private final LocalServiceBinder<BluetoothService> binder = new LocalServiceBinder<BluetoothService>(this);
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                utility.acceptPairingRequest(intent);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                master.clearInactiveDevices();
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                master.addDiscoveredDevice(device);
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        master = new MasterManager(this, SERVICE_PORT);
        slave = new SlaveManager(this, SERVICE_PORT);
        utility = new BluetoothUtility(this);

        Log.i(TAG, "Bluetooth service created");

        // Register broadcast receiver.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Bluetooth service destroyed");
        unregisterReceiver(receiver);
        master.disconnectAll();
        slave.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public ArrayAdapter<BluetoothDevice> getDevicesAdapter(Activity activity, int itemTemplate) {
        return new BluetoothDeviceListAdapter(activity, itemTemplate, master);
    }
}
