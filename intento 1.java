import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class Seminario {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    Button btnEnableDisable;
    Button btnDiscover;
    Button btnSend;
    TextView txtArduino;

    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    BluetoothDevice bluetoothDevice;
    UUID uuid;
    BluetoothSocket bluetoothSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEnableDisable = (Button) findViewById(R.id.btnEnableDisable);
        btnDiscover = (Button) findViewById(R.id.btnDiscover);
        btnSend = (Button) findViewById(R.id.btnSend);
        txtArduino = (TextView) findViewById(R.id.txtArduino);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        btnEnableDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                } else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intent);
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothAdapter.startDiscovery();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothSocket != null) {
                    try {
                        OutputStream outputStream = bluetoothSocket.getOutputStream();
                        String msg = "Hello Arduino!";
                        outputStream.write(msg.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No Bluetooth Connection Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            }
        }
    }

    private void listPairedDevices() {
        pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "Paired Devices: " + device.getName() + " " + device.getAddress());
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        bluetoothDevice = device;

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            Log.d(TAG, "Connected to Bluetooth Device");

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream inputStream = bluetoothSocket.getInputStream();
                        byte[] buffer = new byte[256];
                        int bytesRead = inputStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytesRead);
                        Log.d(TAG, "Incoming Message: " + incomingMessage);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtArduino.setText("Received: " + incomingMessage);
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 2000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void discoverDevices() {
        bluetoothAdapter.startDiscovery();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Discovered Device: " + device.getName() + " " + device.getAddress());

                if (device.getName() != null && device.getName().equals("HC-05")) {
                    connectToDevice(device);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
}
}