package net.sensmaster;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.os.Handler;
import android.util.Log;

public class Data {
  private static final String TAG = "DATA";
  private byte[] data;
  private final Handler handler;

  public Data(Handler mHandler) {
    data = new byte[0];
    handler = mHandler;
  }

  private boolean process() {
    for (int i = 0; i <= data.length-4; i++) {
      int len = data[i+1];
      if(len>0 && len+i+2 <= data.length) {
        // enough characters in the buffer
//      Log.i(TAG, String.format("i=%d, len=%d, data.length=%d", i, len, data.length));

        long crc = Utility.checksum(Arrays.copyOfRange(data,i,i+len));
        ByteBuffer crc_bytes = ByteBuffer.allocate(8).putLong(crc);
//        for (int j = 0; j < crc_bytes.limit(); j++) {
//          Log.i(TAG,String.format("byte %d = %d", j, crc_bytes.get(j)));
//        }
        if(data[i+len+1] == crc_bytes.get(crc_bytes.limit()-1) )
          if(data[i+len] == crc_bytes.get(crc_bytes.limit()-2) ) {
            if(i>0) Log.i(TAG, String.format("Discarding %d bytes %s", i, new ByteArray(Arrays.copyOfRange(data, 0, i)).toString()));
//            Log.i(TAG, String.format("there is %d character msg starting at %d from %d", len, i, data.length));
//            Log.i(TAG, String.format("entire receiving %s", new ByteArray(Arrays.copyOfRange(data, i, i+len+2)).toString()));
            handler.obtainMessage(Monitor.MESSAGE_READ, len, -1, Arrays.copyOfRange(data, i, i+len)).sendToTarget();
            if(i+len+2 < data.length) {
              data = Arrays.copyOfRange(data, i+len+2, data.length);
            } else {
              data = new byte[0];
            }
            return true;
          }

      }
    }
    return false;
  }

  public void write(byte[] buf) {
    byte[] temp = new byte[data.length+buf.length];
    System.arraycopy(data, 0, temp, 0, data.length);
    System.arraycopy(buf, 0, temp, data.length, buf.length);
    data = temp;
    while(process()){};
  }
}
