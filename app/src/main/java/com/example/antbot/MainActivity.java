package com.example.antbot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;
    private final String DEVICE_ADDRESS="C8:C9:A3:C6:29:5E";//muss manuell für jeden ESP eingestellt werden
    private UUID uuid;
    private Handler handler;

    private BluetoothDevice device;

    int[] data = new int[15];
    private byte[] mBuffer;


    TextView mStatusBlueTv, mScanTv;
    ImageView mBlueIv;
    Button mON, mOFF, mDiscover, mSCAN, mLedOFF, mLedON;

    BluetoothAdapter mBA;
    BluetoothSocket mBS;
    InputStream mInStream;
    OutputStream mOutStream;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter2);

        mStatusBlueTv = findViewById(R.id.statusBluetoothTv);
        mScanTv = findViewById(R.id.scanTv);
        mBlueIv = findViewById(R.id.BluetoothIv);
        mON = findViewById(R.id.onButton);
        mOFF = findViewById(R.id.offButton);
        mLedOFF = findViewById(R.id.ledOffButton);
        mLedON = findViewById(R.id.ledOnButton);
        mDiscover = findViewById(R.id.disButton);
        mSCAN = findViewById(R.id.scanButton);

        mBA = BluetoothAdapter.getDefaultAdapter();
        if (mBA == null) {
            mStatusBlueTv.setText("Bluetooth is not available");
        } else {
            mStatusBlueTv.setText("Bluetooth is available");
        }
        if (!mBA.isEnabled()) {
            mBlueIv.setImageResource(R.drawable.ic_action_off);
        } else {
            mBlueIv.setImageResource(R.drawable.ic_action_on);
        }

        mON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBA.isEnabled()) {
                    showToast("Turning on Bluetooth!");

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.BLUETOOTH_ADMIN ) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                                REQUEST_ENABLE_BT);

                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        //  return;
                    }

                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                } else {
                    showToast("Bluetooth is already running!");
                }
            }
        });

        mOFF.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void onClick(View v) {
                if (mBA.isEnabled()) {

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.BLUETOOTH_ADMIN ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_ENABLE_BT);

                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        //  return;
                    }

                    mBA.disable();
                    showToast("Turning off Bluetooth!");
                    mBlueIv.setImageResource(R.drawable.ic_action_off);
                } else {
                    showToast("Bluetooth is already off!");
                }

            }
        });

        mDiscover.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (!mBA.isDiscovering()) {
                    showToast("Making your Device Visible");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVER_BT);
                }
            }
        });

        mSCAN.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void onClick(View v) {
                if (mBA.isEnabled()) {
                    mScanTv.setText("Paired Devices");

                    Context context = getApplicationContext();
                  /*  if (ActivityCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN},
                                REQUEST_ENABLE_BT);

                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                      //  return;
                    }*/

                    if (ActivityCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED) {//|| ActivityCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_ADMIN},//,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_DISCOVER_BT);

                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        //  return;
                    }

                    int scanmode= mBA.getScanMode();
                    if(!mBA.isEnabled()){
                        System.out.println(mBA.enable());
                    }
                    if(mBA.isDiscovering()){
                        System.out.println(mBA.cancelDiscovery());
                    }
                    int currentstate = mBA.getState();
                    if (currentstate == 12) {
                        showToast("\n Bluetooth is on...");
                    } else if (currentstate == 10) {
                        showToast("\n Bluetooth is not turned on, turn it on and restart the app.");
                        return;
                    } else {
                        showToast("\n Error occured, restart app after turning on bluetooth.");
                        return;
                    }
                    boolean started= mBA.startDiscovery();

                    showToast("started");



                    if (ActivityCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT ) != PackageManager.PERMISSION_GRANTED) {//|| ActivityCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_CONNECT},//,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_DISCOVER_BT);

                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        //  return;
                    }
                    Set<BluetoothDevice> devices = mBA.getBondedDevices();
                    String devNames= "";           //device.getname() kann nicht in .settext aufgerufen werden

                    for(BluetoothDevice device: devices){
                        devNames+="\nDevice: "+device.getName();//+"\nID: "+device.getUuids()[0].getUuid();
                        if(device.getName().toLowerCase().equals("esp32_02"))
                            uuid = device.getUuids()[0].getUuid();

                    }
                    if(devNames.equals(""))devNames += "no paired devices found";
                    mScanTv.setText(devNames);
                    if(devices.isEmpty()){
                        showToast("Unable to connect");
                    }else{
                        boolean found=false;
                        for (BluetoothDevice iterator : devices)
                        {
                            if(iterator.getAddress().equals(DEVICE_ADDRESS))
                            {
                                device=iterator;
                                found=true;
                                break;
                            }
                        }}
                }
                else{
                    showToast("turn on Bluetooth");
                }
                try {
                    BTconnect();
                    showToast("connected");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mLedOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread t = new Thread() {
                    public void run() {
                        try {
                            if(mBS.isConnected()) {
                                OutputStream os = mBS.getOutputStream();
                                String msg = "OFF\n";
                                os.write(msg.getBytes());
                            }
                        } catch (Exception ignored) {

                        }
                    }
                };
                t.start();
            }
        });

        mLedON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread t = new Thread() {
                    public void run() {
                        try {
                            if (mBS.isConnected()) {
                                OutputStream os = mBS.getOutputStream();
                                String msg = "ON\n";
                                os.write(msg.getBytes());
                            }
                        } catch (Exception e) {
                            showToast("check");
                        }
                    }
                };
                t.start();
            }
        });
        data = new int[]{10, 50, 100, 200, 50, 50, 30, 2, 50, 600, 70, 94, 52, 61, 30};
        DrawFOV(400,90, data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode==RESULT_OK) {
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                    showToast("Bluetooth is on");
                }
                else{
                    showToast("no Bluetooth for you");
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void BTconnect()
    {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.BLUETOOTH_CONNECT ) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);

            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //  return;
        }
        boolean connected=true;
        try {
            mBS = device.createRfcommSocketToServiceRecord(uuid);
            mBS.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                OutputStream outputStream = mBS.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                InputStream inputStream = mBS.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void DrawFOV(int sizeSqr, int fov, int[] FOV){

        int dataCount = FOV.length;
        Path drawPath = new Path();
        Paint drawPaint = new Paint();
        //drawPaint.setColor(Color.RED);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(1.0f);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.MITER);
        drawPaint.setStrokeCap(Paint.Cap.SQUARE);
        Paint canvasPaint = new Paint(Paint.DITHER_FLAG);
        Paint fillRed = new Paint(Paint.DITHER_FLAG);
        Paint fillGreen = new Paint(Paint.DITHER_FLAG);
        fillRed.setStyle(Paint.Style.FILL);
        fillRed.setColor(Color.RED);
        fillGreen.setColor(Color.GREEN);
        drawPaint.setColor(Color.GREEN);
        float halfFOV = (float) fov / 2;
        float fovData = (float) fov / dataCount;

        Bitmap canvasBitmap = Bitmap.createBitmap(sizeSqr, sizeSqr, Bitmap.Config.ARGB_8888);
        Canvas drawCanvas = new Canvas(canvasBitmap);
                    // red background
        drawPath.addArc(0,0,sizeSqr ,sizeSqr,180 + halfFOV,fov);
        drawPath.rLineTo(-141.4213562f,141.4213562f);
        drawPath.close();
        drawPath.setFillType(Path.FillType.WINDING);
        drawCanvas.drawPath(drawPath, fillRed);
        drawPath.reset();
        /*float a = 200 * ((float) Math.sin(Math.toRadians(fov-fovData * i)));
        drawPath.addArc(0,0,sizeSqr ,sizeSqr,180 + halfFOV,halfFOV);
        drawPath.rLineTo(0,a);
        drawPath.close();
        drawPath.setFillType(Path.FillType.WINDING);
        drawCanvas.drawPath(drawPath, fillGreen);
        drawPath.reset();*/
                    //analyze data and draw green arc
        for(int i = 0; i < dataCount; i++) {
            if (data[i] > 200) data[i] = 200;
            drawPath.addArc(200 - data[i], 200 - data[i], 200 + data[i], 200 + data[i],
                    270 - halfFOV + (i * fovData), fovData);
                drawPath.lineTo(200,200);
            drawPath.close();
            drawCanvas.drawPath(drawPath, fillGreen);
            drawPath.reset();
        }
        mBlueIv.setImageBitmap(canvasBitmap);
    }

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    public void ConnectedThread(BluetoothSocket socket) {
        mBS = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e("TAG", "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e("TAG", "Error occurred when creating output stream", e);
        }

        InputStream mInStream = tmpIn;
        OutputStream mOutStream = tmpOut;
    }

    public void run() {
        mBuffer = new byte[1024];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = mInStream.read(mBuffer);
                // Send the obtained bytes to the UI activity.
                Message readMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_READ, numBytes, -1,
                        mBuffer);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d("TAG", "Input stream was disconnected", e);
                break;
            }
        }
    }
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.

                if (ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.BLUETOOTH_ADMIN ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION , Manifest.permission.ACCESS_COARSE_LOCATION},
                            REQUEST_ENABLE_BT);

                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    //  return;
                }

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                mScanTv.append(deviceName);
            }
            else {
                mScanTv.append("not found");
            }
        }
    };

    private void showToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
}