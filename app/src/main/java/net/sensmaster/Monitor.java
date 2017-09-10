/*
 * Copyright (C) 2010 Janos Gyerik
 *
 * This file is part of BluetoothViewer.
 *
 * BluetoothViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BluetoothViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BluetoothViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sensmaster;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import net.sensmaster.Tag.TagCallBack;

import java.util.Map;

public class Monitor extends Activity implements TabListener {

  private static final String TAG = Monitor.class.getSimpleName();

  // Intent request codes
  private static final int       REQUEST_CONNECT_DEVICE          = 1;
  private static final int       REQUEST_ENABLE_BT               = 2;

  private static final String    SAVED_PENDING_REQUEST_ENABLE_BT = "PENDING_REQUEST_ENABLE_BT";

  public static final int        MESSAGE_STATE_CHANGE            = 1;
  public static final int        MESSAGE_READ                    = 2;
  public static final int        MESSAGE_WRITE                   = 3;
  public static final int        MESSAGE_DEVICE_NAME             = 4;
  public static final int        MESSAGE_TOAST                   = 5;

  // Key names received from the BluetoothService Handler
  public static final String     DEVICE_NAME                     = "device_name";
  public static final String     TOAST                           = "toast";

  private static final String AUTHENTICATED = "AUTHENTICATED";

  private static final String CRITTERCISM_APP_ID = "5451b2c607229a6eb9000001";

  // Layout Views
  private TextView               mStatusView;
//  private View                   mSendTextContainer;

  // Toolbar
  private ImageButton            mToolbarConnectButton;
  private ImageButton            mToolbarDisconnectButton;
  private ImageButton            mToolbarPauseButton;
  private ImageButton            mToolbarPlayButton;

  private ArrayAdapter<String>   mConversationArrayAdapter = null;
  public void addText(String text) {
    Log.i(TAG, text);
    mConversationArrayAdapter.add(text);
  }
  private BluetoothAdapter       mBluetoothAdapter               = null;
  private BluetoothService mBluetoothService               = null;

  // State variables
  private boolean                paused                          = false;
  private boolean                connected                       = false;

  // do not resend request to enable Bluetooth
  // if there is a request already in progress
  // See: https://code.google.com/p/android/issues/detail?id=24931#c1
  private boolean                pendingRequestEnableBt          = false;

  ActionBar actionbar;
  boolean authenticated = true; //false;

//  private LocalyticsAmpSession localyticsSession;
  void authenticate(TagCallBack cb) {
    if(!authenticated) {
      Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
      startActivityForResult(intent, 2, new Bundle());

    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "++onCreate");
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      pendingRequestEnableBt = savedInstanceState.getBoolean(SAVED_PENDING_REQUEST_ENABLE_BT);
       authenticated = savedInstanceState.getBoolean(AUTHENTICATED);
    }
    if (android.os.Build.VERSION.SDK_INT > 9) {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
      System.out.println("*** My thread is now configured to allow connection");
    }

    actionbar = getActionBar();
    actionbar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ff7E6Bf1")));
    actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    Tab logTab = actionbar.newTab().setText("Log Panel").setTabListener(this);
    actionbar.addTab(logTab);
    logTab.setTag(this);
    setContentView(R.layout.main);
    try {
//    localyticsSession = new LocalyticsAmpSession(this.getApplicationContext());  // Context used to access device resources
    } catch (Exception e) {
      addText(e.getMessage());
    }
    // Register LocalyticsActivityLifecycleCallbacks
//     getApplication().registerActivityLifecycleCallbacks(new LocalyticsActivityLifecycleCallbacks(localyticsSession));

    mStatusView = (TextView) findViewById(R.id.btstatus);

//    mSendTextContainer = findViewById(R.id.send_text_container);

    mToolbarConnectButton = (ImageButton) findViewById(R.id.toolbar_btn_connect);
    mToolbarConnectButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        startDeviceListActivity();
      }
    });

    mToolbarDisconnectButton = (ImageButton) findViewById(R.id.toolbar_btn_disconnect);
    mToolbarDisconnectButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        disconnectDevices();
      }
    });

    mToolbarPauseButton = (ImageButton) findViewById(R.id.toolbar_btn_pause);
    mToolbarPauseButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        paused = true;
        onPausedStateChanged();
      }
    });

    mToolbarPlayButton = (ImageButton) findViewById(R.id.toolbar_btn_play);
    mToolbarPlayButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        paused = false;
        onPausedStateChanged();
      }
    });

    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  }
  
  public void onPause()
  {
//      this.localyticsSession.close(custom_dimensions);
//      this.localyticsSession.upload();
      super.onPause();
  }
  
  public void tagEvent(String message, Map<String,String> values) {
//    if(values != null)
//      localyticsSession.tagEvent(message, values);
//    else
//      localyticsSession.tagEvent(message);
  }

  private void startDeviceListActivity() {
    Intent serverIntent = new Intent(this, DeviceListActivity.class);
    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
  }

  @Override
  public void onStart() {
    Log.i(TAG, "++onStart");
    super.onStart();

    if (!mBluetoothAdapter.isEnabled() && !pendingRequestEnableBt) {
      pendingRequestEnableBt = true;
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    } 
    setupUserInterface();
  }

  protected Tag selected_tag = null;

  private Reader reader;

  private ListView mConversationView;

  private void setupUserInterface() {
    if(mConversationArrayAdapter != null)
      return;
    mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
    mConversationView = (ListView) findViewById(R.id.in);
    mConversationView.setAdapter(mConversationArrayAdapter);
    mConversationView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapter, View list, int pos, long arg3) {
        // TODO Auto-generated method stub
        String  selectedFromList = mConversationArrayAdapter.getItem(pos);
        ClipboardManager clipboarder = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied text", selectedFromList);
        clipboarder.setPrimaryClip(clip);
      }
    });

    mConversationArrayAdapter.add(getString(R.string.welcome_sensmaster));

    MessageHandler mHandler = new MessageHandler(this);
    findViewById(R.id.clear_log).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ArrayAdapter<?> arrayAdapter = (ArrayAdapter<?>)mConversationView.getAdapter();
        arrayAdapter.clear();
      }
    });

    mBluetoothService = new BluetoothService(mHandler);
    reader = new Reader(mBluetoothService, this);
    mHandler.setReader(reader);
    try {
      SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
      String address = settings.getString("address", "00:0B:CE:04:CE:62");
      BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
      mBluetoothService.connect(device);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage());
    }

    onBluetoothStateChanged();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mBluetoothService != null)
      mBluetoothService.stop();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i(TAG, "onActivityResult " + resultCode);
    switch (requestCode) {
      case REQUEST_CONNECT_DEVICE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
          String address = data
              .getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
          BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
          mBluetoothService.connect(device);
        }
        break;
      case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns
        pendingRequestEnableBt = false;
        if (resultCode != Activity.RESULT_OK) {
          Log.i(TAG, "BT not enabled");
          Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT)
          .show();
        }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
//      case R.id.menu_github:
//        openURL("getString(R.string.url_github)");
//        break;
//      case R.id.menu_rate:
//        openURL("getString(R.string.url_rate)");
//        break;
//      case R.id.menu_buy:
//        openURL("getString(R.string.url_full_app)");
//        break;
    }
    return false;
  }

//  private void openURL(String url) {
//    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//  }

  private void disconnectDevices() {
    if (mBluetoothService != null)
      mBluetoothService.stop();

    onBluetoothStateChanged();
  }

  private void onBluetoothStateChanged() {
    if (connected) {
      mToolbarConnectButton.setVisibility(View.GONE);
      mToolbarDisconnectButton.setVisibility(View.VISIBLE);
    } else {
      mToolbarConnectButton.setVisibility(View.VISIBLE);
      mToolbarDisconnectButton.setVisibility(View.GONE);
    }
    paused = false;
    onPausedStateChanged();
  }

  private void onPausedStateChanged() {
    if (connected) {
      if (paused) {
        mToolbarPlayButton.setVisibility(View.VISIBLE);
        mToolbarPauseButton.setVisibility(View.GONE);
      } else {
        mToolbarPlayButton.setVisibility(View.GONE);
        mToolbarPauseButton.setVisibility(View.VISIBLE);
      }
    } else {
      mToolbarPlayButton.setVisibility(View.GONE);
      mToolbarPauseButton.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.i(TAG, "++onSaveInstanceState");
    super.onSaveInstanceState(outState);
    outState
    .putBoolean(SAVED_PENDING_REQUEST_ENABLE_BT, pendingRequestEnableBt);
    outState.putBoolean(AUTHENTICATED, authenticated);
  }



  public void setStatus(String status) {
    mStatusView.setText(status);
    onBluetoothStateChanged();
  }
  public void setStatus(int status) {
    mStatusView.setText(status);
  }

  public void didConnect() {
    connected = true;
    onBluetoothStateChanged();
    reader.getProductInformation();
    reader.stopSearching();
    reader.getRFMode();
    reader.setRFMode(Reader.RTF);
    reader.getRFMode();
    reader.startSearching();
  }

  public void disconnected() {
    connected = false;
    onBluetoothStateChanged();
  }


  @Override
  public void onTabReselected(Tab tab, FragmentTransaction ft) {
    // TODO Auto-generated method stub
    onTabSelected(tab, ft);
  }

  @Override
  public void onTabSelected(Tab tab, FragmentTransaction ft) {
//    LinearLayout logListView = (LinearLayout) findViewById(R.id.loglist);
//    if(logListView == null)
//      return;
    Object obj = tab.getTag();
//    Log.i(TAG, "tab selected: " + obj.getClass().toString() + " and " + ft.toString());
//    if(selected_tag != null)
//      selected_tag.unselect();
    if(obj instanceof Monitor) {
      mConversationView.setAdapter(mConversationArrayAdapter);
    }
  }

  @Override
  public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    // TODO Auto-generated method stub

  }
}
