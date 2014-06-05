package uva.nc.app;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.Serializable;

import uva.nc.ServiceActivity;
import uva.nc.bluetooth.BluetoothService;
import uva.nc.bluetooth.MasterManager;
import uva.nc.bluetooth.SlaveManager;
import uva.nc.mbed.MbedManager;
import uva.nc.mbed.MbedRequest;
import uva.nc.mbed.MbedResponse;
import uva.nc.mbed.MbedService;


public class MainActivity extends ServiceActivity {

    private static final String TAG = MainActivity.class.getName();

    // Receiver implemented in separate class, see bottom of file.
    private final MainActivityReceiver receiver = new MainActivityReceiver();

    // ID's for commands on mBed.
    private static final int COMMAND_SEND = 1;
    private static final int COMMAND_GET = 2;

    // ID's for commands on Bluetooth
    private static final float SLAVE_GET = 20.0f;

    // BT Controls.
    private TextView listenerStatusText;
    private TextView ownAddressText;
    private TextView deviceCountText;
    private Button listenerButton;
    private Button devicesButton;

    // mBed controls.
    private TextView mbedConnectedText;
    private Button mbedSendPosition;
    private Button mbedGetPosition;

    // Accessory to connect to when service is connected.
    private UsbAccessory toConnect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        attachControls();

        // If this intent was started with an accessory, store it temporarily and clear once connected.
        UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            this.toConnect = accessory;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, receiver.getIntentFilter());
        refreshBluetoothControls();
        refreshMbedControls();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    @Override
    protected void onBluetoothReady(BluetoothService bluetooth) {
        refreshBluetoothControls();
    }

    @Override
    protected void onMbedReady(MbedService mbed) {
        if (toConnect != null) {
            mbed.manager.attach(toConnect);
            toConnect = null;
        }
        refreshMbedControls();
    }


    private void attachControls() {
        // Bluetooth controls.
        ownAddressText = (TextView)findViewById(R.id.own_address);
        listenerStatusText = (TextView)findViewById(R.id.listener_status);
        listenerButton = (Button)findViewById(R.id.listener);
        deviceCountText = (TextView)findViewById(R.id.device_count);
        devicesButton = (Button)findViewById(R.id.devices);
        devicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent launch = new Intent(MainActivity.this, DevicesActivity.class);
                startActivity(launch);
            }
        });
        mbedConnectedText = (TextView)findViewById(R.id.mbed_connected);

        // mBed controls.
        mbedSendPosition = (Button)findViewById(R.id.send_position);
        mbedSendPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText mbedPositionText = (EditText)findViewById(R.id.position);

                String temp = mbedPositionText.getText().toString();
                float[] args = new float[1];

                // Check if input is empty
                if(temp.matches("")) {
                    args[0] = 0.0f;
                } else {
                    args[0] = Float.valueOf(mbedPositionText.getText().toString());
                }

                // Limit input position to 10
                if(args[0] > 10.0f)
                    args[0] = 10.0f;

                // Do this if bluetooth is on
                BluetoothService bluetooth = getBluetooth();
                if (bluetooth != null) {
                    // If master send position to all connected slaves
                    if (bluetooth.master.countConnected() > 0) {
                        mbedPositionText.setText("");

                        bluetooth.master.sendToAll(args[0]);
                    // Else do the same thing as when bluetooth is off
                    } else {
                        toastShort("Sent position\n");
                        mbedPositionText.setText("");

                        getMbed().manager.write(new MbedRequest(COMMAND_SEND, args));
                    }
                } else {
                    toastShort("Sent position\n");
                    mbedPositionText.setText("");

                    getMbed().manager.write(new MbedRequest(COMMAND_SEND, args));
                }
            }

        });
        mbedGetPosition = (Button)findViewById(R.id.get_position);
        mbedGetPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Do this if bluetooth is on
                BluetoothService bluetooth = getBluetooth();
                if (bluetooth != null) {
                    // If master send position to all connected slaves
                    if (bluetooth.master.countConnected() > 0) {
                        bluetooth.master.sendToAll(20);
                    // Else do the same thing as when bluetooth is off
                    } else {
                        float[] args = new float[1];
                        args[0] = 0.0f;

                        toastShort("Get position");

                        getMbed().manager.write(new MbedRequest(COMMAND_GET, args));
                    }
                } else {
                    float[] args = new float[1];
                    args[0] = 0.0f;

                    toastShort("Get position");

                    getMbed().manager.write(new MbedRequest(COMMAND_GET, args));
                }
            }

        });
    }

    private void refreshBluetoothControls() {
        String slaveStatus = "Status not available";
        String slaveButton = "Start listening";
        String ownAddress = "Not available";
        String connected = "0";
        boolean slaveButtonEnabled = false;
        boolean devicesButtonEnabled = false;

        // Well it's not pretty, but it (barely) avoids duplicate logic.
        final BluetoothService bluetooth = getBluetooth();
        if (bluetooth != null) {
            slaveButtonEnabled = true;
            devicesButtonEnabled = true;
            ownAddress = bluetooth.utility.getOwnAddress();

            int devConnected = bluetooth.master.countConnected();
            if (bluetooth.master.countConnected() > 0) {
                connected = String.valueOf(devConnected);
            }

            if (bluetooth.slave.isConnected()) {
                slaveStatus = "Connected to " + bluetooth.slave.getRemoteDevice();
                slaveButton = "Disconnect";
                listenerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bluetooth.slave.disconnect();
                    }
                });
            } else if (bluetooth.slave.isListening()) {
                slaveStatus = "Waiting for connection";
                slaveButton = "Stop listening";
                listenerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bluetooth.slave.stopAcceptOne();
                    }
                });
            } else {
                slaveStatus = "Not listening";
                slaveButton = "Start listening";
                listenerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!bluetooth.utility.isDiscoverable()) {
                            bluetooth.utility.setDiscoverable();
                        }
                        bluetooth.slave.startAcceptOne();
                    }
                });
            }
        }

        listenerStatusText.setText(slaveStatus);
        listenerButton.setText(slaveButton);
        listenerButton.setEnabled(slaveButtonEnabled);
        ownAddressText.setText(ownAddress);
        deviceCountText.setText(connected);
        devicesButton.setEnabled(devicesButtonEnabled);
    }

    private void refreshMbedControls() {
        String connText = getString(R.string.not_connected); // if you want to localize
        boolean enableButtons = false;

        // Enable mBed controls when master node is connected to at least one slave node
        final BluetoothService bluetooth = getBluetooth();
        if(bluetooth != null) {
            if(bluetooth.master.countConnected() > 0) {
                connText = "Connected to slave apps";
                enableButtons = true;
            }
        }

        MbedService mbed = getMbed();
        if (mbed != null && mbed.manager.areChannelsOpen()) {
            connText = getString(R.string.connected);
            enableButtons = true;
        }

        mbedConnectedText.setText(connText);
        mbedSendPosition.setEnabled(enableButtons);
        mbedGetPosition.setEnabled(enableButtons);
    }



    // Broadcast receiver which handles incoming events. If it were smaller, inline it.
    private class MainActivityReceiver extends BroadcastReceiver {

        // Refresh BT controls on these events.
        private final String BLUETOOTH_REFRESH_ON[] = { MasterManager.DEVICE_ADDED,
                                                        MasterManager.DEVICE_REMOVED,
                                                        MasterManager.DEVICE_STATE_CHANGED,
                                                        SlaveManager.LISTENER_CONNECTED,
                                                        SlaveManager.LISTENER_DISCONNECTED,
                                                        SlaveManager.STARTED_LISTENING,
                                                        SlaveManager.STOPPED_LISTENING };

        private final String MBED_REFRESH_ON[] = {      MbedManager.DEVICE_ATTACHED,
                                                        MbedManager.DEVICE_DETACHED };


        // Returns intents this receiver responds to.
        protected IntentFilter getIntentFilter() {
            IntentFilter filter = new IntentFilter();

            // Notification updates.
            for (String action : BLUETOOTH_REFRESH_ON) {
                filter.addAction(action);
            }
            for (String action : MBED_REFRESH_ON) {
                filter.addAction(action);
            }

            // Data received events.
            filter.addAction(MbedManager.DATA_READ);
            filter.addAction(MasterManager.DEVICE_RECEIVED);
            filter.addAction(SlaveManager.LISTENER_RECEIVED);

            return filter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Refresh on most Bluetooth or mBed events.
            for (String update : BLUETOOTH_REFRESH_ON) {
                if (action.equals(update)) {
                    refreshBluetoothControls();
                    break;
                }
            }
            for (String update : MBED_REFRESH_ON) {
                if (action.equals(update)) {
                    refreshMbedControls();
                    break;
                }
            }

            // Process received data.
            if (action.equals(SlaveManager.LISTENER_RECEIVED)) {

                // Slave received data from master.
                Serializable obj = intent.getSerializableExtra(SlaveManager.EXTRA_OBJECT);
                if (obj != null) {
                    // If slave receive get position from slave command from master
                    if (Float.valueOf(String.valueOf(obj)) == SLAVE_GET) {
                        float[] args = new float[1];
                        args[0] = 0.0f;

                        toastShort("Get position\n");

                        getMbed().manager.write(new MbedRequest(COMMAND_GET, args));
                    } else {
                        toastShort("Requested position from master:\n" + String.valueOf(obj));
                        float[] args = new float[1];

                        args[0] = Float.valueOf(String.valueOf(obj));
                        getMbed().manager.write(new MbedRequest(COMMAND_SEND, args));
                    }
                } else {
                    toastShort("From master:\nnull");
                }
            } else if (action.equals(MasterManager.DEVICE_RECEIVED)) {

                // Master received data from slave.
                Serializable obj = intent.getSerializableExtra(MasterManager.EXTRA_OBJECT);
                BluetoothDevice device = intent.getParcelableExtra(MasterManager.EXTRA_DEVICE);
                if (obj != null) {
                    toastShort("Current position from " + device + ":\n" + String.valueOf(obj));
                } else {
                    toastShort("From " + device + "\nnull!");
                }
            } else if (action.equals(MbedManager.DATA_READ)) {

                // mBed data received.
                MbedResponse response = intent.getParcelableExtra(MbedManager.EXTRA_DATA);
                if (response != null) {
                    // Errors handled as separate case, but this is just sample code.
                    if (response.hasError()) {
                        toastLong("Error! " + response);
                        return;
                    }

                    float[] values = response.getValues();

                    if (response.getCommandId() == COMMAND_SEND) {
                        if (values == null || values.length != 1) {
                            toastShort("Error!");
                        } else {
                            toastShort("Moved to chosen position:\n" + String.valueOf(values[0]));
                        }
                    } else if (response.getCommandId() == COMMAND_GET) {
                        if (values == null || values.length != 1) {
                            toastShort("Error!");
                        } else {
                            // If slave is connected to master
                            final BluetoothService bluetooth = getBluetooth();
                            if(bluetooth != null) {
                                if (bluetooth.slave.isConnected()) {
                                    bluetooth.slave.sendToMaster(values[0]);
                                }
                            }
                            toastShort("Current position of Servo system:\n" + String.valueOf(values[0]));
                        }
                    }
                }
            }
        }
    }
}