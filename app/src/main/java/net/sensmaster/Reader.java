package net.sensmaster;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Random;
import java.util.Stack;

import android.content.Context;
import android.util.Log;
import android.widget.ListView;

public class Reader {
  protected static final byte TagIndication = (byte) 0x01;
  protected static final byte ShowPage = (byte) 0x03;
  protected static final byte TagOperation = (byte) 0x05;
  protected static final byte ClearPassword = (byte) 0x11;
  protected static final byte StartSearch = (byte) 0xC0;
  protected static final byte StopSearch = (byte) 0xC1;
  protected static final byte GetInformationFlow = (byte) 0xc3;
  protected static final byte GetProductInformation = (byte) 0xC5;
  protected static final byte GetRFMode = (byte) 0xCC;
  private static final byte SetRFMode = (byte) 0xCD;
  protected static final byte SoftReset = (byte) 0xD0;

  static final byte RTF = 0x00;
  static final byte TTF = 0x01;
  private static final String TAG = "Reader";
  public static final byte SetRFConfiguration = 0x06;


  private final BluetoothService service;
  private boolean busy = false;
  private Stack<byte[]> command_stack = new Stack<>();
  private final TagAdapter tags;
  private Monitor bv;
  private static Hashtable<Byte, String> STATUS;
  private static Hashtable<Byte, String> PRODUCT;
  private static Hashtable<Byte, String> COMMAND;
  private static Hashtable<Byte, String> FLOW;
  private static Hashtable<Byte, String> TAGOP;
  private static Hashtable<Byte, String> RFMODE;
  private static Hashtable<Byte, String> PAGECODE;

  static {
    STATUS = new Hashtable<>();
    STATUS.put((byte) 0x01, "Command success");
    STATUS.put((byte) 0x02, "Command invalid parameter");
    STATUS.put((byte) 0x03, "Command failed");
    STATUS.put((byte) 0x04, "Command error : RF already running");
    STATUS.put((byte) 0x05, "Command failed : Out of memory");
    STATUS.put((byte) 0x06, "Command failed : Error writing to memory");
    STATUS.put((byte) 0x07, "Command failed : Request via wrong information flow");
    STATUS.put((byte) 0x08, "Command invalid : Checksum verification failed");
    STATUS.put((byte) 0x09, "Command failed : Log memory is empty");
    STATUS.put((byte) 0x0A, "Command failed : Not authorised, wrong password");
    STATUS.put((byte) 0x0B, "Command failed : No Tag detected");
    STATUS.put((byte) 0x0C, "Command failed : Not supported");
    STATUS.put((byte) 0x0D, "Command failed : Page does not exist");
    STATUS.put((byte) 0x0E, "Command timeout : Too many attempts with wrong password");
    STATUS.put((byte) 0x0F, "Command failed : Seclusion code locked");
    STATUS.put((byte) 0x10, "Command failed : Tag end of life");
    STATUS.put((byte) 0xB0, "Device is charging");
    STATUS.put((byte) 0xB1, "Device is not battery powered but does provide supply voltage information");
    STATUS.put((byte) 0xB2, "Device is low on battery");

    PRODUCT = new Hashtable<>();
    PRODUCT.put((byte) 0x00, "F2M07 / TM7G2 / FS700");
    PRODUCT.put((byte) 0x01, "TM701");
    PRODUCT.put((byte) 0x02, "TM702");
    PRODUCT.put((byte) 0x05, "TM705");
    PRODUCT.put((byte) 0x31, "TM901");
    PRODUCT.put((byte) 0x32, "TM902");
    PRODUCT.put((byte) 0x33, "TM903");

    COMMAND = new Hashtable<>();
    COMMAND.put(Reader.SoftReset, "Soft Reset");
    COMMAND.put(Reader.TagIndication, "Tag Indication");
    COMMAND.put(Reader.TagOperation, "Tag Operation");
    COMMAND.put(Reader.StartSearch, "Search for Tags");
    COMMAND.put(Reader.StopSearch, "Stop search for Tags");
    COMMAND.put(Reader.GetInformationFlow, "Get information flow");
    COMMAND.put(Reader.GetProductInformation, "Get product information");

    PAGECODE = new Hashtable<>();
    PAGECODE.put(PageCode.DeviceInformationPage, "Device Information Page");
    PAGECODE.put(PageCode.LogRequest, "Log Request");
    PAGECODE.put(PageCode.TemperatureAndHumidity, "Temperature & Humidity");

    RFMODE = new Hashtable<>();
    RFMODE.put((byte) 0x00, "Reader Talks First");
    RFMODE.put((byte) 0x01, "Tag Talks First");
    RFMODE.put((byte) 0x03, "Tag Talks First Fast11 or TTF Fast");


    FLOW = new Hashtable<>();
    FLOW.put((byte) 0x01,"RS232 / USB");
    FLOW.put((byte) 0x02,"Bluetooth");
    FLOW.put((byte) 0x03,"LAN");
    FLOW.put((byte) 0x04,"WLAN");
    FLOW.put((byte) 0x05,"GPRS");

    TAGOP = new Hashtable<>();
    TAGOP.put((byte) 0x00, "GetUserMemory");
    TAGOP.put((byte) 0x01, "SetUserMemory");
    TAGOP.put((byte) 0x05, "SetSystemTime");
    TAGOP.put((byte) 0x30, "SetTHSensor");
    TAGOP.put((byte) 0x32, "GetLog");
    TAGOP.put((byte) 0x33, "SetLog");
  }
  public Reader(BluetoothService service, Context _bv) {
    this.service = service;
    this.bv = (Monitor) _bv;
    tags = new TagAdapter(bv, R.layout.tag, R.id.status, this);
    ListView tagholder = (ListView) this.bv.findViewById(R.id.taglist);
    tagholder.setAdapter(tags);
  }
  void sendCommand(ByteArray command) {
    sendCommand(command.toBytes());
  }

  public void softReset() {
    sendCommand(new byte[] { SoftReset, 0x02 });
  }

  public void handle(byte[] bytes) {
    int len = bytes[1];
    if(len != bytes.length) {
      Log.e(TAG, "Invalid msg: " + Utility.getHexString(bytes));
      return;
    }
    byte status = bytes[2];
    byte page_code;
    Log.i(TAG, String.format("received: %s", Utility.getHexString(bytes)));
    ByteArray uid;
    Tag tag;
    switch (bytes[0]) {
      case Reader.GetRFMode:
        String rf_mode = RFMODE.get(bytes[3]);
        bv.addText("Reader RF Mode: " + rf_mode);
        break;
      case Reader.TagOperation:
        // busy = true;
        uid = new ByteArray(Arrays.copyOfRange(bytes, 3, 3+5));
        tag = tags.get(uid);
        tag.handle(bytes, STATUS.get(status));
        break;
      case Reader.GetProductInformation:
        String device_name = PRODUCT.get(bytes[3]);
        bv.addText("Status: "  + STATUS.get(status));
        try {
          bv.addText(String.format("Device: "  + device_name + " Hardware: %x, Firmware: %x, Reader ID: %x", new BigInteger(1, Arrays.copyOfRange(bytes, 4, 4+5)), new BigInteger(1, Arrays.copyOfRange(bytes, 9, 9+3) ), new BigInteger(1, Arrays.copyOfRange(bytes, 12, 12+5))));
        } catch (Exception ex) {
          Log.e(TAG, "error = " + ex.getMessage());
        }
        break;
      case Reader.StartSearch:
        bv.addText("Status: "  + STATUS.get(status));
        break;
      case Reader.StopSearch:
        bv.addText("Status: "  + STATUS.get(status));
        break;
      case Reader.GetInformationFlow:
        bv.addText("Status: "  + STATUS.get(status));
        bv.addText("Flow: "  + FLOW.get(bytes[3]));
        break;
      case Reader.ShowPage:
        uid = new ByteArray(Arrays.copyOfRange(bytes, 3, 3+5));
        tag = tags.get(uid);
        if(status != 0x01) {
          Log.e(TAG, String.format("Tag: %s, Cannot show page\nStatus: %s", tag.toString(), STATUS.get(status)));
        } else {
          page_code = bytes[8];
          byte[] page = Arrays.copyOfRange(bytes, 9, 9+18);
          tag.parsePage(page_code, page);
        }
        break;
      case Reader.TagIndication:
        page_code = bytes[7];
        uid = new ByteArray(Arrays.copyOfRange(bytes, 2, 2+5));
        tag = tags.get(uid);
        if(bytes[1]==0x20)
        {
          byte[] page = Arrays.copyOfRange(bytes, 8, 8+18);
          tag.parsePage(page_code, page);
        } else {
          bv.addText(String.format(Locale.ENGLISH, "short? UID: %s, len: %d", tag.toString(), bytes[1]));
        }
        break;
    }
  }

  public boolean nextCommand() {
    if(command_stack.isEmpty()) {
      return false;
    } else {
      byte[] command = command_stack.peek();
      if(sendCommand(command)) {
        // went through
        command_stack.pop();
        return true;
      } else {
        return false;
      }
    }
  }

  public boolean sendCommand(byte[] command) {
    if(busy) {
      command_stack.push(command);
      return false;
    }
    command[1] = (byte) command.length;
    long crc = Utility.checksum(command);
    ByteBuffer crc_bytes = ByteBuffer.allocate(8).putLong(crc);
    byte[] bytes = new byte[command.length + 2];
    System.arraycopy(command, 0, bytes, 0, command.length);
    bytes[bytes.length-2] = crc_bytes.get(crc_bytes.limit()-2);
    bytes[bytes.length-1] = crc_bytes.get(crc_bytes.limit()-1);
    bv.addText(">> " + Utility.getHexString(bytes));
    service.write(bytes);
    return true;
  }
  public void getRFMode() {
    sendCommand(new byte[] {GetRFMode, 0x02});
  }
  public void setRFMode(byte mode) {
    sendCommand(new byte[] {SetRFMode, 0x03, mode});
  }
  public void startSearching() {
    Random rand = new Random();
    sendCommand(new byte[] { StartSearch, 0x04, (byte) rand.nextInt(), (byte) rand.nextInt() });
  }
  public void getProductInformation() {
    sendCommand(new byte[] { (byte) 0xc5, 0x02 });
  }
  public void stopSearching() {
    sendCommand(new byte[] {StopSearch, 0x02});
  }
}
