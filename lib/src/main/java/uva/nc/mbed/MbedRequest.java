package uva.nc.mbed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MbedRequest {

    // Fixed argument size for equal packets. mBed code seems to stick with the lowest readbuffer
    // size out of two.
    private static final int MAX_ARGS = 20;

    // Request ID is just an increment counter.
    private static int lastRequestId = 0;

    private float[] arguments;
    private int commandId;
    private int requestId;

    public MbedRequest(int commandId, float[] arguments) {
        this.requestId = lastRequestId++;
        setCommandId(commandId);
        setArguments(arguments);
    }

    public int getRequestId() {
        return requestId;
    }

    public int getCommandId() {
        return commandId;
    }

    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }

    public float[] getArguments() {
        return arguments;
    }

    public void setArguments(float[] arguments) {
        if (arguments != null && arguments.length > MAX_ARGS) {
            throw new IllegalArgumentException("Too many arguments! Adjust MAX_ARGS constant!");
        }
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return  "req=" + requestId +
                ", comm=" + commandId +
                ", args=" + Arrays.toString(arguments);
    }

    public byte[] getBytes() {
        int numArgs = (arguments == null) ? 0 : arguments.length;
        if (numArgs > MAX_ARGS) {
            throw new RuntimeException("Illegal number of arguments! Maximum is " +
                    String.valueOf(MAX_ARGS));
        }

        // Number of arguments defines size. Fixed size due to USBHost odditiy.
        int size = 4 + 4 + 4 + MAX_ARGS * 4;
        ByteBuffer b = ByteBuffer.allocate(size)
                                 .order(ByteOrder.LITTLE_ENDIAN)
                                 .putInt(requestId)
                                 .putInt(commandId)
                                 .putInt(numArgs);
        if (numArgs > 0) {
            for (float arg : arguments) {
                b.putFloat(arg);
            }
        }

        return b.array();
    }
}
