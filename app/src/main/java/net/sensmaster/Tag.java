/**
 *
 */
package net.sensmaster;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * @author maysam
 */

public class Tag {
  private static final String TAG = "Tag";
  private static final byte SENSOR_SELECT_Dual = 0x03;
  protected static final byte GetUserMemory = 0x00;
  protected static final byte SetUserMemory = 0x01;
  static final byte GetDeviceInfo = 0x03;
  static final byte SetSystemTime = 0x05;
  private static final byte setDeviceInfo = 0x06;
  private static final byte SetTHSensor = 0x30;
  static final byte GetLog = 0x32;
  static final byte SetLog = 0x33;
  private static final Map<Byte, String> COMMAND;
  private static final Map<Byte, String> DSF;
  private static final byte STARTED = 0x02;
  private static final byte FINISHED = 0x04;
  private static final byte RTF = 0x00;
  private static final byte TTF = 0x01;
  private static final String URL = "http://isonweb.herokuapp.com";
  static final byte setRTFMode = 0x00;
  private static final byte setTTFMode = 0x01;
  private byte status;
  private final ByteArray uid;
  public int counter;
  private static int _count = 0;
  private int count = 0;

  // LinkedList<Double> temps, humidities;
  private final ArrayAdapter<String> dataAdapter;
  private final Reader reader;
  // private int total;
  private Calendar time;

  private int interval;
  private int log_interval;
  private int expecting = 0;
  private int tx_status = -1;
  private int countdown;
  private ArrayList<StoredData> stored_data = new ArrayList<StoredData>();
  private ByteArray storing_data = new ByteArray();
  private int offset;
  private boolean active = false;
  private double voltage;
  private byte rf_mode;
//  private int log_offset;
  private TagAdapter adapter;
  private ProgressDialog bar;
  private int _SIZE_TO_SEND;
  private int collected = 0;
  private boolean still_loading = false;

  static {
    COMMAND = new Hashtable<Byte, String>();
    COMMAND.put(GetLog, "Get Log");
    COMMAND.put(SetLog, "Set Log");
    COMMAND.put(GetDeviceInfo, "Get Device Info");
    COMMAND.put(SetSystemTime, "Set System Time");
    COMMAND.put(setDeviceInfo, "set RFMode to RTF");

    DSF = new Hashtable<Byte, String>();
    DSF.put((byte) 0x01, "Idle");
    DSF.put(STARTED, "Started");
    DSF.put((byte) 0x03, "Stopped");
    DSF.put(FINISHED, "Finished");
    DSF.put((byte) 0x05, "Alert");
    DSF.put((byte) 0x07, "Text");
    DSF.put((byte) 0x08, "Guard");
    DSF.put((byte) 0x09, "Show");
    DSF.put((byte) 0x0A, "Stationary");
    DSF.put((byte) 0x0B, "Moving");
  }

  private class HttpAsyncTask extends AsyncTask<HttpPost, Integer, String> {
    @Override
    protected String doInBackground(HttpPost... params) {
      HttpClient client = new DefaultHttpClient();
      HttpResponse httpResponse;
      InputStream inputStream = null;
      String result = "error";
      try {
        httpResponse = client.execute(params[0]);

        inputStream = httpResponse.getEntity().getContent();
        // convert inputstream to string
        if (inputStream != null) {
          result = convertInputStreamToString(inputStream);
          return "true";
        }
        else
          result = "Did not work!";
      } catch (ClientProtocolException e) {
        result = e.getMessage();
      } catch (IOException e) {
        result = e.getMessage();
      }
      return result;
    }
    @Override
    protected void onPostExecute(String result) {
      if(result.compareTo("true") == 0 || result.startsWith(".....")) {
        if(bar.isShowing()) {
          bar.incrementProgressBy(_SIZE_TO_SEND);
          if(bar.getProgress() == bar.getMax()) {
            bar.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
          }
        }
      }
      else
        addText(result);
    }
  }

  void pingServer() {
    doSendData(null, 0);
  }

  interface NVOQService {
    @POST(URL + "/save")
    Call<String> save(@Query("username")String username);
  }
  private void doSendData(ByteArray pending2, int log_offset) {
    try {

      URI address = new URI(URL + "/save");
      HttpPost post = new HttpPost(address);

      List<NameValuePair> pairs = new ArrayList<NameValuePair>();
      pairs.add(new BasicNameValuePair("tag", uid.toString()));
      pairs.add(new BasicNameValuePair("status", DSF.get(status)));
      pairs.add(new BasicNameValuePair("interval", Integer.toString(interval)));
      pairs.add(new BasicNameValuePair("active", Boolean.toString(active)));
      if(time != null) {
        long date_i = time.getTimeInMillis();
        pairs.add(new BasicNameValuePair("datetime", Long.toString(date_i)));
      }
      if (pending2 != null) {
        String saveThis = pending2.toString();
        pairs.add(new BasicNameValuePair("data", saveThis));
        pairs.add(new BasicNameValuePair("offset", Integer.toString(log_offset)));
        pairs.add(new BasicNameValuePair("log_interval", Integer.toString(log_interval)));
      }

      post.setEntity(new UrlEncodedFormEntity(pairs));

      HttpAsyncTask task = new HttpAsyncTask();
      task.execute(post);

    } catch (UnsupportedEncodingException e) {
    } catch (URISyntaxException e) {
    }
  }

  public Tag(ByteArray _uid, final Monitor context, Reader _reader, TagAdapter tagAdapter) {
    _count ++;
    count = _count;
    uid = _uid;
    reader = _reader;
    adapter = tagAdapter;
    dataAdapter = new ArrayAdapter<String>(context.getApplicationContext(), R.layout.message);
    addText(uid.toString() + " Log View");
    bar = new ProgressDialog(context);
    bar.setCancelable(false);
    bar.setCanceledOnTouchOutside(false);
    bar.setTitle("Connecting to " + uid.toString());
    bar.setProgressPercentFormat(NumberFormat.getPercentInstance());
    bar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    bar.setButton(ProgressDialog.BUTTON_POSITIVE, "Close", new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        bar.dismiss();
        bar.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
      }
    });
  }

  void CommandTag(byte command) {
    CommandTag(command, 0);
  }

  // convert inputstream to String
  private static String convertInputStreamToString(InputStream inputStream) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    String line = "";
    String result = "";
    while ((line = bufferedReader.readLine()) != null)
      result += line;
    inputStream.close();
    return result;

  }

  public String GET(String url, byte[] stored_data) {
    InputStream inputStream = null;
    String result = "";
    try {

      // create HttpClient
      HttpClient httpclient = new DefaultHttpClient();
      // make GET request to the given URL
      httpclient.getParams().setParameter("data", stored_data);
      HttpResponse httpResponse = httpclient.execute(new HttpPost(url));
      // HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

      // receive response as inputStream
      inputStream = httpResponse.getEntity().getContent();

      // convert inputstream to string
      if (inputStream != null)
        result = convertInputStreamToString(inputStream);
      else
        result = "Did not work!";
    } catch (Exception e) {
    }
    return result;
  }

  interface TagCallBack {
    void sendData();
  }

  public ByteArray toByteArray() {
    return uid;
  }

  public boolean isEqual(Tag tag) {
    return uid.isEqual(tag.toByteArray());
  }

  public void addText(String text) {
    dataAdapter.add(text);
    Log.v(TAG, uid.toString() + ": " + text);
  }

  private void CommandTag(byte operation, byte[] params) {
    byte[] command = new byte[8 + params.length];
    command[0] = Reader.TagOperation;
    System.arraycopy(uid.toBytes(), 0, command, 2, 5);
    command[7] = operation;
    System.arraycopy(params, 0, command, 8, params.length);
    reader.sendCommand(command);
  }

  void CommandTag(byte command, int param1) {
    dataAdapter.add(String.format("sending command \"%s\"", COMMAND.get(command)));
    byte[] _command;
    switch (command) {
      case setRTFMode:
      case setTTFMode:
        byte[] rfconfig_params = new byte[21];
        rfconfig_params[0] = 0x01;
        rfconfig_params[1] = command; // either RTF or TTF
        //        rfconfig_params[2] = rf_power;
        //
        //        rfconfig_params[3] = 0x00;
        //        rfconfig_params[4] = 0x00;
        //        rfconfig_params[5] = 0x00;
        //        rfconfig_params[6] = 0x00;
        //        rfconfig_params[7] = 0x00;
        //        rfconfig_params[8] = 0x00;

        _command = new byte[7 + rfconfig_params.length];
        _command[0] = Reader.SetRFConfiguration;
        System.arraycopy(uid.toBytes(), 0, _command, 2, 5);
        System.arraycopy(rfconfig_params, 0, _command, 7, rfconfig_params.length);
        addText(Utility.getHexString(_command));
        reader.sendCommand(_command);
        break;
      case Tag.GetDeviceInfo:
        byte[] device_info_params = { 0x00, 0x08, (byte) 0x1a, (byte) 0xff,
            (byte) 0xff, (byte) 0xff };
        _command = new byte[7 + device_info_params.length];
        _command[0] = Reader.ShowPage;
        System.arraycopy(uid.toBytes(), 0, _command, 2, 5);
        System.arraycopy(device_info_params, 0, _command, 7,
            device_info_params.length);
        reader.sendCommand(_command);
        break;
      case Tag.SetSystemTime:
        byte[] time_params = new byte[0x11];
        byte[] time = Utility.getTime();
        System.arraycopy(time, 0, time_params, 12, 5);
        CommandTag(SetSystemTime, time_params);
        break;
      case Tag.SetTHSensor:
        byte[] sensor_params = new byte[0x15];
        sensor_params[5] = SENSOR_SELECT_Dual;
        CommandTag(SetTHSensor, sensor_params);
        break;
      case Tag.SetLog:
        byte[] set_log_params = new byte[0x15];
        set_log_params[13 - 8] = SENSOR_SELECT_Dual;
        set_log_params[15 - 8] = (byte) 0x78;
        // 15 second intervals
        CommandTag(SetLog, set_log_params);
        break;
      case Tag.GetLog:
        byte[] get_log_params = new byte[0x15];
        int start = 0;
        int end = 8192;
        if(expecting>0) {
          if(countdown>0) {
            //            got_bytes = expecting*4 - countdown;
            start = expecting - countdown/4;
            start = storing_data.length()/4;
            int mod = storing_data.length() % 4;
            storing_data.removeFromEnd(mod);
          }
          //          end = start + 100;
          end = expecting;
        }
        addText(String.format("Starting from %d to %d, expecting %d with countdown %d", start, end, expecting, countdown));

        get_log_params[12 - 8] = (byte) ((start >> 16) & 0xff);
        get_log_params[13 - 8] = (byte) ((start >> 8) & 0xff);
        get_log_params[14 - 8] = (byte) (start & 0xff);
        get_log_params[15 - 8] = (byte) ((end >> 16) & 0xff);
        get_log_params[16 - 8] = (byte) ((end >> 8) & 0xff);
        get_log_params[17 - 8] = (byte) (end & 0xff);
        CommandTag(GetLog, get_log_params);
        if(!bar.isShowing()) {
          bar.setMax(collected);
          bar.setProgress(0);
          bar.setMessage("Loading data from sensor");
          bar.show();
          bar.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
          still_loading = true;
        }
        break;
    }
  }

  public void setStatus(byte b) {
    status = b;
    update_status_line();
  }

  private void update_status_line() {
    adapter.notifyDataSetChanged();
  }

  public void getAgain(int length) {
    CommandTag(GetLog);
  }

  public void parsePage(byte page_code, byte[] bytes) {
    int in_interval = 0;
    Log.w(TAG, String.format("parsing page %x for %s", page_code, uid.toString()));
    if (tx_status != -1) {
      if ((bytes[0] != Reader.TagOperation) && (offset > 0)) {
        // let's end it now
        addText(Utility.getHexString(bytes));
        addText(String.format("ParsePage: wrong: END pending.len: %d, offset: %d",
            storing_data.length(), offset));
        // if(offset < pending.length)
        //        if (parseData(pending, offset))
        getAgain(expecting - countdown/4);
        offset = 0;
      }
      tx_status = -1;
    }
    switch (page_code) {
      case PageCode.LogRequest:
        int _collected = (((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF)) & 0xFFFFFF;
        if(collected != _collected) {
          collected = _collected;
          adapter.update(this);
          double temp = Utility.calcTemperature(bytes[15 - 8], bytes[16 - 8]);
          double rh = Utility.calcHumidity(bytes[17 - 8], bytes[18 - 8]);
          addText(String.format(
              "Tag %s: %d rows logged so far, latest: Temp: %f, Humidity: %f",
              uid.toString(), collected,
              temp, rh));
        }
        if(bar.isShowing() && still_loading) {
          CommandTag(Tag.GetLog);
        }
        break;
      case PageCode.DeviceInformationPage:
        setStatus(bytes[0]);
        rf_mode = bytes[1];
        byte[] rf_interval = Arrays.copyOfRange(bytes, 3, 3 + 2);
        if ((rf_mode == RTF) | (rf_mode == TTF)) {
          if ((rf_interval[0] | rf_interval[1]) == 0x00) {
            in_interval = 60;
          } else {
            in_interval = ((rf_interval[0] << 8) | rf_interval[1] & 0xffff);
          }
          in_interval *= 1000; // convert to milliseconds
        } else {
          if ((rf_interval[0] | rf_interval[1]) == 0x00) {
            in_interval = 500;
          } else {
            in_interval = ((rf_interval[0] << 8) | rf_interval[1]) & 0xffff;
          }
        }
        interval = in_interval;

        //        byte[] batt_age = Arrays.copyOfRange(bytes, 18-8, 22-8);

        byte batt_v = bytes[25-8];
        voltage = (batt_v & 0xff)*0.02;
        update_status_line();
        break;
    }
  }

  @Override
  public String toString() {
    String datetimestr = "no time!";
    if (time != null) {
      DateFormat format = DateFormat.getDateTimeInstance();
      datetimestr = format.format(time.getTime());
    }
    String rf_mode_str = "undetermined";
    if(rf_mode == TTF) {
      rf_mode_str = "TTF";
    }
    if(rf_mode == RTF){
      rf_mode_str = "RTF";
    }
    return String.format(Locale.ENGLISH,
        "%d: %s: %d/%d data collected, datetime: %s, interval: %d, log_interval: %d, status: %s, voltage: %.1f V, RFMODE: %s",
        count, uid.toString(), collected, getCollected(), datetimestr, interval, log_interval, DSF.get(status), voltage, rf_mode_str);
  }

  public void handle(byte[] bytes, String status_str) {
    int len = bytes.length;
    byte status = bytes[2];
    byte tag_op = 0;
    if(len >= 9){
      tag_op = bytes[8];
      if (tag_op != Tag.GetLog)
        addText(String.format("Command \"%s\" returned Status: %s", COMMAND.get(tag_op), status_str ));
    } else {
      addText(String.format("returned Status: %s", status_str ));
    }
    if (status == 0x01) {
      if(len > 9){

        if (tag_op == Tag.GetLog) {
          tx_status = (bytes[9] & 0xC0) >> 6;
            int _countdown = ((bytes[9] & 0x3f) << 16)
                + ((bytes[10] & 0xff) << 8) + (bytes[11] & 0xff);
            if (countdown == _countdown) {
              // dataAdapter.add("Discarding repeating line 05...");
              return;
            }
            countdown = _countdown;
            String op_status;
            int datalen = bytes.length - 12;
            int copylen = len - 12; // (int) (Math.floor((len-12)/4)*4);
            switch (tx_status) {
              case 0:
                op_status = "Transmitting header";
                //                if (len == 0x17)
                {
                  // looking at the header
                  byte[] datetime = Arrays.copyOfRange(bytes,
                      12, 5 + 12);
                  Calendar cal = Utility.toTime(datetime);
                  //                  ByteArray loginterval = new ByteArray(Arrays.copyOfRange(bytes, 17, 2 + 17));
                  byte[] total = Arrays.copyOfRange(bytes, 19, 3 + 19);
                  byte logselect = bytes[22];
                  // tag.setTotal(((total[0] & 0xff) << 16) + ((total[1] & 0xff)
                  // << 8) + (total[2] & 0xff));
                  time = cal;

                  int _interval = ((bytes[17] & 0xff) << 8) | (bytes[18] & 0xff);

                  if (_interval != log_interval) {
                    log_interval = _interval;
                    update_status_line();
                  }
                  int total_log = ((total[0] & 0xff) << 16) | ((total[1] & 0xff) << 8) | (total[2] & 0xff);
                  if(expecting == 0) {
                    expecting = total_log;
                    storing_data = new ByteArray();
                    if(bar != null) {
                      bar.setMax(expecting);
                    }
                  }
                  String str = String.format(Locale.ENGLISH,
                      "datetime=%s, countdown=%d, total_log=%d, total=%s, logselect=%x, time=%s, expecting=%d",
                      datetime.toString(), countdown, total_log, Utility.getHexString(total), logselect, cal.getTime().toString(), expecting);
                  addText(str);

                  offset = 0;
                }
                break;
              case 1:
                op_status = "Transmitting log data";
                storing_data.append(bytes, 12, copylen);
                if(bar != null) {
                  bar.setProgress(storing_data.length()/4);
                }
                offset += datalen;
                break;
              case 2:
                storing_data.append(bytes, 12, copylen);
                if(bar != null) {
                  bar.setProgress(storing_data.length()/4);
                  still_loading = false;
                }
                offset += datalen;
                
                int index = -1;
                for (int i = 0; i < stored_data.size(); i++) {
                  StoredData temp = stored_data.get(i);
                  if(temp.isEqual(storing_data, time.getTimeInMillis(), log_interval)) {
                    index = i;
                  }
                }
                if(index == -1) {
                  stored_data.add(new StoredData(storing_data, time.getTimeInMillis(), log_interval));
                }
                adapter.update(this);
                bar.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                sendDataToServer();
                dataAdapter.add(String.format("Finished loading data from sensor tag.\ndata length: %d, len: %d, offset: %d, expecting = %d, countdown=%d",
                    storing_data.length(), len, offset, expecting, countdown));
                op_status = "End of log data transmission";
                offset = 0;
                _countdown = -1;
                expecting = 0;
                break;
              default:
                op_status = "";
                break;
            }
            if(offset>0)
              addText(String.format(
                  "%s: remaining %d bytes or %d data points", op_status,
                  countdown, countdown/4 ));
            if (len == 0x1D) {
              //              ByteArray start = new ByteArray(Arrays.copyOfRange(bytes, 13,
              //                  3 + 13));
              //              ByteArray end = new ByteArray(Arrays.copyOfRange(bytes, 16,
              //                  3 + 16));
              //              String str = String.format("start=%s, end=%s", start.toString(),
              //                  end.toString());
              //              addText(str);
            }
        }
      }
    }
  }

  public void showLog(ListView list) {
    list.setAdapter(dataAdapter);
  }

  public void sendDataToServer() {
    _SIZE_TO_SEND = 2048;
    bar.setMessage("Uploading data ...");
    bar.setProgress(0);
    for (int i = 0; i < storing_data.length(); i+=_SIZE_TO_SEND ) {
      int end = i + _SIZE_TO_SEND;
      if(end > storing_data.length())
        end = storing_data.length();
      byte[] tosend = Arrays.copyOfRange(storing_data.toBytes(), i, end);
      doSendData(new ByteArray(tosend), i);
    }
  }

  public String getUid() {
    return uid.toString();
  }

  public int getCollected() {
    return collected;
  }

  public void setCollected(int _collected) {
    collected = _collected;
  }

  public Integer getRFInterval() {
    return interval;
  }

  public ByteArray hasData() {
    return storing_data;
  }

  public long getDatetime() {
    if(time != null) {
      long date_i = time.getTimeInMillis();
      return date_i;
    }
    return 0;
  }

  public int getLogInterval() {
    return log_interval;
  }
}
