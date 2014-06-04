package uva.nc.mbed;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MbedResponse implements Parcelable {
    final int requestId;
    final int commandId;
    final int errorCode;
    final float[] values;

    public int getRequestId() {
        return requestId;
    }

    public int getCommandId() {
        return commandId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean hasError() {
        return errorCode != 0;
    }

    public String getErrorMessage() {
        switch(errorCode) {
            case 0:
                return "No error";
            case 1:
                return "Command not found";
            default:
                return "Unknown error";
        }
    }

    public float[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return  "req=" + requestId +
                ", comm=" + commandId +
                ", err=" + errorCode +
                ", vals=" + Arrays.toString(values);
    }

    public MbedResponse(byte[] buffer) {
        ByteBuffer b = ByteBuffer.wrap(buffer)
                                 .order(ByteOrder.LITTLE_ENDIAN);
        this.requestId = b.getInt(0);
        this.commandId = b.getInt(4);
        this.errorCode = b.getInt(8);

        int count = b.getInt(12);
        if (count > 0) {
            this.values = new float[count];
            for (int i = 0; i < count; i++) {
                values[i] = b.getFloat(16 + (i * 4));
            }
        } else {
            this.values = null;
        }
    }

    public MbedResponse(Parcel p) {
        this.requestId = p.readInt();
        this.commandId = p.readInt();
        this.errorCode = p.readInt();

        int arrLen = p.readInt();
        if (arrLen > 0) {
            values = new float[arrLen];
            p.readFloatArray(values);
        } else {
            values = null;
        }
    }



    /* Parcelable implementation. Parcelable. Parcel-able. Can be parcelled. To parcel or not to
     * parcel.
     *
     * To learn how to pronounce it, please visit
     *  https://www.youtube.com/watch?v=4wFCmTlHeM8
     *  https://www.youtube.com/watch?v=36JWyHDGkRU
     *
     *  */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(requestId);
        parcel.writeInt(commandId);
        parcel.writeInt(errorCode);
        parcel.writeInt(values == null ? 0 : values.length);
        if (values != null && values.length > 0) {
            parcel.writeFloatArray(values);
        }
    }

    public static final Parcelable.Creator<MbedResponse> CREATOR = new Parcelable.Creator<MbedResponse>() {

        @Override
        public MbedResponse createFromParcel(Parcel parcel) {
            return new MbedResponse(parcel);
        }

        @Override
        public MbedResponse[] newArray(int i) {
            return new MbedResponse[i];
        }
    };
}