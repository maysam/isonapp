package net.sensmaster;

import java.math.BigInteger;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MessageHandler extends Handler {
  private static final String TAG = "Message Handler";
  private final Monitor bv;
  private Reader reader;

  public MessageHandler(Monitor bluetoothViewer) {
    bv = bluetoothViewer;
  }

  @Override
  public void handleMessage(Message msg) {
    if (!Thread.currentThread().isInterrupted()) 
    switch (msg.what) {
      case BluetoothService.MSG_CONNECTED:
        bv.didConnect();
        bv.setStatus(formatStatusMessage(R.string.btstatus_connected_to_fmt, msg.obj));
        break;
      case BluetoothService.MSG_CONNECTING:
        bv.disconnected();
        bv.setStatus(formatStatusMessage(R.string.btstatus_connecting_to_fmt,msg.obj));
        break;
      case BluetoothService.MSG_NOT_CONNECTED:
        bv.disconnected();
        bv.setStatus(R.string.btstatus_not_connected);
        break;
      case BluetoothService.MSG_CONNECTION_FAILED:
        bv.disconnected();
        bv.setStatus(R.string.btstatus_not_connected);
        break;
      case BluetoothService.MSG_CONNECTION_LOST:
        bv.disconnected();
        bv.setStatus(R.string.btstatus_not_connected);
        break;
      case BluetoothService.MSG_BYTES_WRITTEN:
        String written = String.format("%x", new BigInteger(1,(byte[]) msg.obj));
        Log.i(TAG,"written = '"+ written+ "'");
        break;
      case Monitor.MESSAGE_READ:
        reader.handle((byte[])msg.obj);
        break;
    }
    super.handleMessage(msg);
  }

  private String formatStatusMessage(int formatResId, Object obj) {
    String deviceName = (String) obj;
    return bv.getString(formatResId, deviceName);
  }

  public SharedPreferences getSharedPreferences() {
    return bv.getPreferences(Context.MODE_PRIVATE);
  }

  public void setReader(Reader reader) {
    this.reader = reader;
  }

}
