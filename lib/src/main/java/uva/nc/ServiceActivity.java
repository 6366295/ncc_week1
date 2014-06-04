package uva.nc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import uva.nc.bluetooth.BluetoothService;
import uva.nc.mbed.MbedService;


public abstract class ServiceActivity extends Activity {

    private static final String TAG = ServiceActivity.class.getName();

    private final ServiceConnection bluetoothConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "Bluetooth service connected!");
            bluetoothService = ((LocalServiceBinder<BluetoothService>)iBinder).getService();
            onBluetoothReady(bluetoothService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "Bluetooth connection interrupted!");
            bluetoothService = null;
        }
    };
    private final ServiceConnection mbedConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "mBed service connected!");
            mbedService = ((LocalServiceBinder<MbedService>)iBinder).getService();
            onMbedReady(mbedService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "mBed connection interrupted!");
            mbedService = null;
        }
    };

    private BluetoothService bluetoothService;
    private MbedService mbedService;
    private boolean boundToBluetooth;
    private boolean boundToMbed;


    @Override
    protected void onStart() {
        super.onStart();

        final Intent bluetoothIntent = new Intent(this, BluetoothService.class);
        final Intent mbedIntent = new Intent(this, MbedService.class);

        // Start services. This keeps them alive after the activity closes, at least for a while,
        // and at the very least until the end of the activity.
        startService(bluetoothIntent);
        startService(mbedIntent);

        // Bind to services.
        boundToBluetooth = bindService(bluetoothIntent, bluetoothConnection, 0);
        if (!boundToBluetooth) {
            Log.wtf(TAG, "Failed to bind to Bluetooth service!");
        }
        boundToMbed = bindService(mbedIntent, mbedConnection, 0);
        if (!boundToMbed) {
            Log.wtf(TAG, "Failed to bind to mBed service!");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (boundToBluetooth) {
            unbindService(bluetoothConnection);
        }
        if (boundToMbed) {
            unbindService(mbedConnection);
        }
    }


    // Called when the Bluetooth service is ready.
    protected void onBluetoothReady(BluetoothService bluetooth) {
    }

    // Called when the mBed service is ready.
    protected void onMbedReady(MbedService mbed) {
    }

    // Accessor for inheriting activities.
    protected final BluetoothService getBluetooth() {
        return bluetoothService;
    }

    protected final MbedService getMbed() {
        return mbedService;
    }

    // Random utilities.
    protected final void toastShort(final String text) {
        toast(text, Toast.LENGTH_SHORT);
    }

    protected final void toastLong(final String text) {
        toast(text, Toast.LENGTH_LONG);
    }

    private void toast(final String text, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ServiceActivity.this, text, duration).show();
            }
        });
    }
}
