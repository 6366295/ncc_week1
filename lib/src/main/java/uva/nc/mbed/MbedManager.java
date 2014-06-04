package uva.nc.mbed;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MbedManager {

    private static final String TAG = MbedManager.class.getName();
    private static final int WAIT_WRITE = 50; //ms
    private static final int WAIT_CLOSE = 50; //ms

    public static final String DEVICE_ATTACHED = "uva.nc.mbed.DeviceAttached";
    public static final String DEVICE_DETACHED = "uva.nc.mbed.DeviceDetached";
    public static final String DATA_READ = "uva.nc.mbed.DataRead";
    public static final String EXTRA_DATA = "uva.nc.mbed.Data";

    private final Context context;

    private UsbManager usbManager;
    private String accessoryName;

    private ConnectedThread commThread;
    private ParcelFileDescriptor pfd;


    public MbedManager(Context context) {
        this.context = context;
        this.usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
    }

    public void attach(UsbAccessory accessory) {
        if (accessory == null || areChannelsOpen()) {
            return;
        }

        close();

        // Try to open the accessory. This can fail if the device is rotated after connecting to
        // the mbed. -- Might be fixed now.
        Log.d(TAG, "Opening accessory " + accessoryName);
        try {
            pfd = usbManager.openAccessory(accessory);
            if (pfd == null) {
                Log.e(TAG, "Failed to get accessory file descriptor!");
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        }

        accessoryName = accessory.getManufacturer() + " (" + accessory.getSerial() + ")";

        // Setup comm channel.
        FileDescriptor fd = pfd.getFileDescriptor();
        FileInputStream input = new FileInputStream(fd);
        FileOutputStream output = new FileOutputStream(fd);

        commThread = new ConnectedThread(input, output);
        commThread.start();

        Log.i(TAG, "Attached " + accessoryName);
        context.sendBroadcast(new Intent(DEVICE_ATTACHED));
    }

    public void write(MbedRequest request) {
        if (areChannelsOpen()) {
            Log.i(TAG, "Sending " + request);
            byte[] requestBytes = request.getBytes();
            commThread.write(requestBytes);
        } else {
            Log.e(TAG, "No comm channel!");
        }
    }

    public void close() {
        // Called when underlying stream died or when comm channels need to be closed.
        if (commThread != null) {
            commThread.close();
            commThread = null;
        }
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException e) {
            }
            pfd = null;
            context.sendBroadcast(new Intent(DEVICE_DETACHED));
        }
    }

    public boolean areChannelsOpen() {
        return commThread != null && pfd != null;
    }


    private class ConnectedThread extends Thread {

        private final FileInputStream input;
        private final FileOutputStream output;
        public boolean ioError = false;

        public ConnectedThread(FileInputStream input, FileOutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
            Log.i(TAG, "Established USB comm with " + accessoryName);
            byte[] buffer = new byte[16384];
            while (!ioError) {
                try {
                    int read = input.read(buffer);
                    if (read > 0) {
                        try {
                            MbedResponse response = new MbedResponse(buffer);
                            Log.i(TAG, "Received " + response);

                            Intent readIntent = new Intent(DATA_READ);
                            readIntent.putExtra(EXTRA_DATA, response);
                            context.sendBroadcast(readIntent);
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Failed to read data!");
                            break;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IO error on read", e);
                    ioError = true;
                    break;
                }
            }

            MbedManager.this.close();
        }

        public void write(byte[] bytes) {
            try {
                Thread.sleep(WAIT_WRITE);
                output.write(bytes);
                output.flush();
            } catch (Exception e) {
                Log.e(TAG, "IO error on write", e);
                ioError = true;
                MbedManager.this.close();
            }
        }

        public void close() {
            Log.v(TAG, "Closing comm streams with " + accessoryName);
            try {
                Thread.sleep(WAIT_CLOSE);
                input.close();
            } catch (Exception e) { }
            try {
                output.close();
            } catch (IOException e) {
            }
        }
    }
}
