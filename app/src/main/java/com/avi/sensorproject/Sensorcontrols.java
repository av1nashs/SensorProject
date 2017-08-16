package com.avi.sensorproject;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


import android.location.Location;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;


//Google Sheets Update
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
/////


public class Sensorcontrols extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    private final static String TAG = Sensorcontrols.class.getSimpleName();

    private Button connectBtn = null;
    private TextView rssiValue = null;
    private TextView COValue = null;
    private TextView NH3Value = null;
    private TextView NO2Value = null;
    private ToggleButton NO2Btn, NH3Btn, COBtn;
    private int co, no2, nh3;
    //private SeekBar servoSeekBar, PWMSeekBar;

    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;
    private boolean upload ;

    private byte[] data = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    //GPS Part
    //private Button b;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private FloatingActionButton b;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private FloatingActionButton fab3;

    private boolean FAB_Status = false;

    Vibrator mVibrator;
    ////////
    //Animations
    Animation show_fab_1;
    Animation hide_fab_1;
    Animation show_fab_2;
    Animation hide_fab_2;
    Animation show_fab_3;
    Animation hide_fab_3;


    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sensorcontrols, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

                readValues(data);
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensorcontrols);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLatitudeTextView = (TextView) findViewById((R.id.lat));
        mLongitudeTextView = (TextView) findViewById((R.id.longv));
        //b = (FloatingActionButton) findViewById(R.id.fab);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //Floating Action Buttons
        b = (FloatingActionButton) findViewById(R.id.fab);
        fab1 = (FloatingActionButton) findViewById(R.id.fab_1);
        fab2 = (FloatingActionButton) findViewById(R.id.fab_2);
        fab3 = (FloatingActionButton) findViewById(R.id.fab_3);

        //Animations
        show_fab_1 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab1_show);
        hide_fab_1 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab1_hide);
        show_fab_2 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab2_show);
        hide_fab_2 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab2_hide);
        show_fab_3 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab3_show);
        hide_fab_3 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab3_hide);

        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);


        //spreadsheet


        rssiValue = (TextView) findViewById(R.id.rssiValue);

        COValue = (TextView) findViewById(R.id.COText);

        NO2Value = (TextView) findViewById(R.id.NO2Text);

        NH3Value = (TextView) findViewById(R.id.NH3Text);

        connectBtn = (Button) findViewById(R.id.connect);
        connectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (scanFlag == false) {
                    scanLeDevice();

                    Timer mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mDevice != null) {
                                mDeviceAddress = mDevice.getAddress();
                                mBluetoothLeService.connect(mDeviceAddress);
                                scanFlag = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast toast = Toast
                                                .makeText(
                                                        Sensorcontrols.this,
                                                        "Couldn't search Ble Shield device!",
                                                        Toast.LENGTH_SHORT);
                                        toast.setGravity(0, 0, Gravity.CENTER);
                                        toast.show();
                                    }
                                });
                            }
                        }
                    }, SCAN_PERIOD);
                }

                System.out.println(connState);
                if (connState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }
            }
        });


        COBtn = (ToggleButton) findViewById(R.id.COBtn);
        COBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                byte[] buf = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00};

                if (isChecked)
                    buf[1] = 0x01;
                else
                    buf[1] = 0x00;

                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        });

        NO2Btn = (ToggleButton) findViewById(R.id.NO2Btn);
        NO2Btn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                byte[] buf = new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00};

                if (isChecked == true)
                    buf[1] = 0x01;
                else
                    buf[1] = 0x00;

                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        });

        NH3Btn = (ToggleButton) findViewById(R.id.NH3Btn);
        NH3Btn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                byte[] buf = new byte[]{(byte) 0x02, (byte) 0x00, (byte) 0x00};

                if (isChecked == true)
                    buf[1] = 0x01;
                else
                    buf[1] = 0x00;

                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        });



        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(Sensorcontrols.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        upload = true;
    }

    private void displayData(String data) {
        if (data != null) {
            rssiValue.setText(data);
        }
    }

    private void readValues(byte[] data) {

        for (int i = 0; i < data.length; i += 3) {
            /*if (data[i] == 0x0A) {
                if (data[i + 1] == 0x01)
                    digitalInBtn.setChecked(false);
                else
                    digitalInBtn.setChecked(true);
            } else if */
            if (data[i] == 0x0B) {
                float coValue;
                int analogCOVal;


                analogCOVal = ((data[i + 1] << 8) & 0x0000ff00)
                        | (data[i + 2] & 0x000000ff);

                coValue = calculateCOConcentration(analogCOVal);

                //co = coValue;

                COValue.setText(coValue + "ppm");
                //colorvibrate();
            }
            else if (data[i] == 0x0C) {
                float no2Value;
                int analogno2Value;


                analogno2Value = ((data[i + 1] << 8) & 0x0000ff00)
                        | (data[i + 2] & 0x000000ff);

                no2Value = calculateNO2Concentration(analogno2Value);

                //no2 = no2Value;

                NO2Value.setText(no2Value + "ppm");
            }

            else if (data[i] == 0x0D) {
                float nh3Value;
                int analognh3Value;

                analognh3Value = ((data[i + 1] << 8) & 0x0000ff00)
                        | (data[i + 2] & 0x000000ff);

                nh3Value = calculateNH3Concentration(analognh3Value);

                //nh3 = nh3Value;

                NH3Value.setText(nh3Value + "ppm");
            }
        }
    }

    private final Callback<Void> callCallback = new Callback<Void>() {
        @Override
        public void onResponse(Response<Void> response) {
            Log.d("XXX", "Submitted. " + response);
        }

        @Override
        public void onFailure(Throwable t) {
            Log.e("XXX", "Failed", t);
        }
    };

    private void setButtonEnable() {
        flag = true;
        connState = true;

        //digitalOutBtn.setEnabled(flag);
        COBtn.setEnabled(flag);
        NO2Btn.setEnabled(flag);
        NH3Btn.setEnabled(flag);
        // servoSeekBar.setEnabled(flag);
        //PWMSeekBar.setEnabled(flag);
        connectBtn.setText("Disconnect");
    }

    private void setButtonDisable() {
        flag = false;
        connState = false;

        // digitalOutBtn.setEnabled(flag);
        COBtn.setEnabled(flag);
        NO2Btn.setEnabled(flag);
        NH3Btn.setEnabled(flag);
        // servoSeekBar.setEnabled(flag);
        // PWMSeekBar.setEnabled(flag);
        connectBtn.setText("Connect");
    }

    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 26, j = 0; i >= 11; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = bytesToHex(serviceUuidBytes);
                    if (stringToUuidString(serviceUuid).equals(RBLGattAttributes.BLE_SHIELD_SERVICE.toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    private Handler myHandler;
    //spreadsheet
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://docs.google.com/forms/d/")
            .build();
    final SensorSpreadsheetWebService spreadsheetWebService = retrofit.create(SensorSpreadsheetWebService.class);
    int delay = 1000;
    private Runnable myRunnable = new Runnable() {
        @Override
        public void run(){
            String LatitudeInput = mLatitudeTextView.getText().toString();
            String LongitudeInput = mLongitudeTextView.getText().toString();
            String COInput = COValue.getText().toString();
            String NO2Input = NO2Value.getText().toString();
            String NH3Input = NH3Value.getText().toString();
            Call<Void> completeQuestionnaireCall = spreadsheetWebService.completeQuestionnaire(LatitudeInput, LongitudeInput, COInput, NO2Input, NH3Input);
            completeQuestionnaireCall.enqueue(callCallback);
            //do something
            myHandler.postDelayed(this, delay);
        }
    };

    //GPS Part
    @Override
    protected void onStart() {
        super.onStart();
        upload = true ;
        myHandler = new Handler();

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (FAB_Status == false) {
                    //Display FAB menu
                    expandFAB();
                    FAB_Status = true;
                } else {
                    //Close FAB menu
                    hideFAB();
                    FAB_Status = false;
                }
            }
        });



        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplication(), "Location and Upload Services Stopped", Toast.LENGTH_SHORT).show();
                myHandler.removeCallbacks(myRunnable);
                //Snackbar.make(view, "Upload Stopped", Snackbar.LENGTH_LONG)
                        //.setAction("Action", null).show();
                hideFAB();
                FAB_Status = false;

                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }

                mLatitudeTextView.setText("");
                mLongitudeTextView.setText("");
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getApplication(), "Floating Action Button 2", Toast.LENGTH_SHORT).show();
                mGoogleApiClient.connect();
                Snackbar.make(view, "GPS Tracking Started", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getApplication(), "Floating Action Button 3", Toast.LENGTH_SHORT).show();
                mGoogleApiClient.connect();
                Snackbar.make(view, "Upload Started", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                myHandler.post(myRunnable);
            }
        });
    }



    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        mLatitudeTextView.setText(String.valueOf(location.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(location.getLongitude()));


        Toast.makeText(this, "Updated: " + mLastUpdateTime, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());

    }
    //////////////////////////////////////////////////
    @Override
    protected void onStop() {
        super.onStop();
        //upload = false;
        flag = false;
        myHandler.removeCallbacks(myRunnable);

        unregisterReceiver(mGattUpdateReceiver);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //upload = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void expandFAB() {

        //Floating Action Button 1
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) fab1.getLayoutParams();
        layoutParams.rightMargin += (int) (fab1.getWidth() * 1.7);
        layoutParams.bottomMargin += (int) (fab1.getHeight() * 0.25);
        fab1.setLayoutParams(layoutParams);
        fab1.startAnimation(show_fab_1);
        fab1.setClickable(true);

        //Floating Action Button 2
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) fab2.getLayoutParams();
        layoutParams2.rightMargin += (int) (fab2.getWidth() * 1.5);
        layoutParams2.bottomMargin += (int) (fab2.getHeight() * 1.5);
        fab2.setLayoutParams(layoutParams2);
        fab2.startAnimation(show_fab_2);
        fab2.setClickable(true);

        //Floating Action Button 3
        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) fab3.getLayoutParams();
        layoutParams3.rightMargin += (int) (fab3.getWidth() * 0.25);
        layoutParams3.bottomMargin += (int) (fab3.getHeight() * 1.7);
        fab3.setLayoutParams(layoutParams3);
        fab3.startAnimation(show_fab_3);
        fab3.setClickable(true);
    }


    private void hideFAB() {

        //Floating Action Button 1
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) fab1.getLayoutParams();
        layoutParams.rightMargin -= (int) (fab1.getWidth() * 1.7);
        layoutParams.bottomMargin -= (int) (fab1.getHeight() * 0.25);
        fab1.setLayoutParams(layoutParams);
        fab1.startAnimation(hide_fab_1);
        fab1.setClickable(false);

        //Floating Action Button 2
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) fab2.getLayoutParams();
        layoutParams2.rightMargin -= (int) (fab2.getWidth() * 1.5);
        layoutParams2.bottomMargin -= (int) (fab2.getHeight() * 1.5);
        fab2.setLayoutParams(layoutParams2);
        fab2.startAnimation(hide_fab_2);
        fab2.setClickable(false);

        //Floating Action Button 3
        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) fab3.getLayoutParams();
        layoutParams3.rightMargin -= (int) (fab3.getWidth() * 0.25);
        layoutParams3.bottomMargin -= (int) (fab3.getHeight() * 1.7);
        fab3.setLayoutParams(layoutParams3);
        fab3.startAnimation(hide_fab_3);
        fab3.setClickable(false);
    }


    private float calculateCOConcentration(int coAnalogValue){
        float refResForCo = 133;  //After finding this value we use this for measuring Rs/R0
        float measuredResInAirForCo;
        float ratioOfResForCO;
        float finalCOValue;

        measuredResInAirForCo = 56 * coAnalogValue / (1020-coAnalogValue);
        ratioOfResForCO = measuredResInAirForCo / refResForCo;

        finalCOValue = (float) (Math.pow(ratioOfResForCO, -1.179)*4.385);



        return finalCOValue; //For finding resistance let's return this later should be changed to finalCOValue
    }

    private float calculateNO2Concentration(int NO2AnalogValue){
        float refResForNO2 = 3 ;  //After finding this value we use this for measuring Rs/R0
        float measuredResInAirForNO2;
        float ratioOfResForNO2;
        float finalNO2Value;

        measuredResInAirForNO2 = 56 * NO2AnalogValue / (1020-NO2AnalogValue);
        ratioOfResForNO2 = measuredResInAirForNO2 / refResForNO2;

        finalNO2Value = (float) (Math.pow(ratioOfResForNO2, 1.007)/6.855);


        return finalNO2Value; //For finding resistance let's return this later should be changed to finalNO2Value
    }

    private float calculateNH3Concentration(int NH3AnalogValue){
        float refResForNH3 = 37;  //After finding this value we use this for measuring Rs/R0
        float measuredResInAirForNH3;
        float ratioOfResForNH3;
        float finalNH3Value;

        measuredResInAirForNH3 = 56 * NH3AnalogValue / (1020-NH3AnalogValue);
        ratioOfResForNH3 = measuredResInAirForNH3 / refResForNH3;

        finalNH3Value = (float) (Math.pow(ratioOfResForNH3, -1.67)/1.47);



        return finalNH3Value; //For finding resistance let's return this later should be changed to finalNH3Value
    }


    private void colorvibrate() {
        if (co < 9) {
            //green color
        } else if (co > 9 && co < 155) {
            long pattern[] = {0, 100, 100, 100, 100, 100};

            // yellow
            mVibrator.vibrate(pattern, 0);
        } else if (co > 155) {
            long pattern[] = {0, 300, 50, 300, 50, 300};

            // red
            mVibrator.vibrate(pattern, 2);
        }

    }

    /* Formula References
        case NO2:
        {
            c = pow(ratio2, 1.007)/6.855;  //mod by jack
            break;
        }
        case NH3:
        {
            c = pow(ratio0, -1.67)/1.47;  //modi by jack
            break;

         */

}
