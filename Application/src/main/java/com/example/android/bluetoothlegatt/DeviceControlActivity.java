package com.example.android.bluetoothlegatt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.erz.joysticklibrary.JoyStick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceControlActivity extends Activity {
    private String fire = "-1";

    public String mType = "Joystick";

    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    public BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;

    private int INNER_MAX_TIME = 6000;

    private TextView actionString;
    private TextView timeHolding;
    private TextView Inner_text;

    private boolean shooted;
    private boolean retry;

    private String superString = "0;0;-1";

    final Handler handler1 = new Handler();

    private TextView ltxt, rtxt, rrtxt, rltxt;
    private double rr = 1.0d, rl = 1.0d;
    private int desviationl = 100, desviationr = 100;
    private JoyStick ljoyStick, rjoyStick;
    private double r = 0, l = 0;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                transmit();
                mConnected = true;
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            }
        }

    };

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        AudioManager volumeConfig = (AudioManager) getSystemService(AUDIO_SERVICE);
        volumeConfig.setStreamVolume(AudioManager.STREAM_MUSIC, 6, 0);

        actionString = (TextView) findViewById(R.id.stateShoot);
        timeHolding = (TextView) findViewById(R.id.timeHolding);
        Inner_text = (TextView) findViewById(R.id.Inner_max_time);
        Inner_text.setText(""+(INNER_MAX_TIME/1000)+"seconds");
        timeHolding.setText("0");

        //Joystick configuration

        rjoyStick = (JoyStick) findViewById(R.id.right);
        assert rjoyStick != null;
        rjoyStick.setType(JoyStick.TYPE_8_AXIS);
        rjoyStick.enableStayPut(false);
        //Set JoyStickListener
        rjoyStick.setListener(new JoyStick.JoyStickListener() {

            @Override
            public void onMove(JoyStick joyStick, double v, double v1, int i) {
                Log.i("right", "," + v + "," + v1 + "," + i);
                double zxasd = Math.abs(v1);
                l = (Math.pow((zxasd - 48) * 0.1, 3) + 111) * Math.signum(v);

                sendMove();
//                rtxt.setText(String.valueOf(l));
            }

            @Override
            public void onTap() {

            }

            @Override
            public void onDoubleTap() {

            }
        });

        ljoyStick = (JoyStick) findViewById(R.id.left);
        assert ljoyStick != null;
        ljoyStick.setHapticFeedbackEnabled(true);
        ljoyStick.setType(JoyStick.TYPE_2_AXIS_UP_DOWN);
        ljoyStick.enableStayPut(false);

        ljoyStick.setListener(new JoyStick.JoyStickListener() {

            @Override
            public void onMove(JoyStick joyStick, double v, double v1, int i) {
                Log.i("left", "," + v + "," + v1 + "," + i);
                double zxasd = Math.abs(v1);
                r = (Math.pow((zxasd - 48) * 0.1, 3) + 111) * Math.signum(v);

                sendMove();
                // ltxt.setText(String.valueOf(r));
                // \left(\left(x-48\right)\cdot0.1\right)^3+111
            }

            @Override
            public void onTap() {
            }

            @Override
            public void onDoubleTap() {
            }
        });


        final int delay = 25;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);

        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        String LIST_NAME = "NAME";
        String LIST_UUID = "UUID";
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                    bluetoothGattCharacteristicHM_10 = gattService.getCharacteristic(BluetoothLeService.UUID_HM_10);


            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                event.startTracking();
                actionString.setText(R.string.shooting);
                fire = "1";
                sendMove();
                return true;
            } else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                event.startTracking();
                actionString.setText(R.string.recovery);
                fire = "0";
                sendMove();
                return true;
            }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                setNormal();
                shooted = true;
                return true;
            } else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                shooted = false;
                fire = "-1";
                retry = true;
                maxHolder();
                return true;
            }
        return super.onKeyUp(keyCode, event);
    }

    public void setNormal() {
        final Handler handler = new Handler();
        int delay = 2000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timeHolding.setText("0");
                fire = "-1";
                sendMove();
                actionString.setText(R.string.no_action);
            }
        }, delay);
    }

    private void maxHolder(){
        final int[] t = {0};
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!shooted) {
                    t[0]++;
                    timeHolding.setText(getString(
                    R.string.timeHolding, t[0]));
                    if ((t[0] == (INNER_MAX_TIME / 1000)) && !retry) {
                        actionString.setText(R.string.shooting);
                        fire = "1";
                        shooted = true;
                        sendMove();
                        setNormal();
                    } else if (t[0] < INNER_MAX_TIME / 1000 && !retry) {
                        handler1.postDelayed(this, 1000);
                    }else if(retry){
                        t[0] = 0;
                        retry = false;
                        maxHolder();
                    }
                }
            }
        }, 1000);
    }

    private void sendMove() {

        int ll = (int) (l * 2.55 * ((double) desviationl / 100));
        int rr = (int) (r * 2.55 * ((double) desviationr / 100));

        superString = rr + ";" + ll + ";" + fire + "\n";
        Log.d("BtConnet", superString);
    }

    private void transmit(){
        final Handler mHandler = new Handler();

        Toast.makeText(this, "Wait for the connection to stablish",Toast.LENGTH_LONG).show();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                //Log.d("BtSending", "run: Sending..");


                mBluetoothLeService.writeCharacteristic(superString,bluetoothGattCharacteristicHM_10);

                mHandler.postDelayed(this,250);

            }
        },1000);

    }
}