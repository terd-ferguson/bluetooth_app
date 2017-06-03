package com.ferguson.turd.shwifty_20;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ////////////////////BT vars etc///////////////////////////////////////////////////////////////////
    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> foundDevices = new HashSet<>();
    //ConnectThread myConnectThread;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "MainActivity";
    public static final String uniqueNAME = "THING1";
    public String ADDRESS;
    public boolean deviceFound = false;
    private final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    private UUID myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
    CountDownTimer pairing_timer, bonding_timer;
    boolean unpair = false;
    //////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////Discover//////////////////////////////////////////////////////////////////////////////////
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //if new device discovered...
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //add device to set
                foundDevices.add(device);
                //store name and address
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                //if name == thing1
                if (uniqueNAME.equals(device.getName())) {
                    //display device name
                    Toast.makeText(getApplicationContext(), deviceName + " found", Toast.LENGTH_SHORT).show();
                    //unregister receiver
                    unregisterReceiver(mReceiver);
                    Toast.makeText(getApplicationContext(), "BroadcastReceiver unregistered", Toast.LENGTH_SHORT).show();
                    //cancel discovery
                    cancel_discovery();
                    //cancel pairing timer
                    pairing_timer.cancel();
                    Toast.makeText(getApplicationContext(), "pairing_timer cancelled", Toast.LENGTH_SHORT).show();
                    //attempt to pair devices
                    try_to_pair(device);
                } else {
                    Toast.makeText(getApplicationContext(), "Device name: " + deviceName + "\r\n" +
                            "Device address: " + deviceHardwareAddress + "\r\n", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////File Path////////////////////////////////////////////////////
    public String path2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shwifty_1.0";
    public String filename1 = "/csvfile.csv";
    public String filename2 = "/dataLogging.txt";
    File file1 = new File(path2 + filename1);
    File file2 = new File(path2 + filename2);
    //////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //declare path location for file
    public String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shwifty_1.0";
    //declare name of file
    public String addrFile = "/address.txt";
    public String dimensionFile = "/tankInfo.txt";
    //declare file
    File f_addr = new File(path + addrFile);
    File f_dim = new File(path + dimensionFile);

    // initialize strings used for verifyStoragePermissions function
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //declare function that verifies storage permissions
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    Button viewData;

    //import database
    com.ferguson.turd.shwifty_10.DatabaseHelper myDb;

    //initialize button variables
    Button changeSettings, eraseSettings, changeMac, eraseMac, deleteSettings, deleteMac, connect, disconnect, sendData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //link button ids with variables
        changeSettings = (Button) findViewById(R.id.btn1);
        changeMac = (Button) findViewById(R.id.btn2);
        eraseSettings = (Button) findViewById(R.id.btn3);
        eraseMac = (Button) findViewById(R.id.btn4);
        connect = (Button) findViewById(R.id.btn5);
        deleteSettings = (Button) findViewById(R.id.btn6);
        deleteMac = (Button) findViewById(R.id.btn7);
        disconnect = (Button) findViewById(R.id.btn8);
        sendData = (Button) findViewById(R.id.btn9);

        //upon start of activity, attempt reading the address file.
        //if it does not exist, prompt user to enter address.
        try {
            //read address file
            readAddressFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //notify user that file does not exist
            Toast.makeText(this, "Address file does not exist", Toast.LENGTH_SHORT).show();
            //address dialog box pop up
            addressDialog();
            //while(true) {}
        }

        //upon start of activity, attempt reading the settings file.
        //if it does not exist, redirect user to settings page.
        try {
            //read dimensions file
            readDimensionsFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //notify user that file does not exist
            Toast.makeText(this, "Dimensions file does not exist", Toast.LENGTH_SHORT).show();

            //goto settings page
            Intent intent = new Intent(this, com.ferguson.turd.shwifty_10.SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            //while(true) {}
        }

        //erase file
        //eraseFile();
        //delete file
        deleteFile(file2);
        //delete database
        deleteDatabase("log.db");

        //enable file reading/writing permissions
        verifyStoragePermissions(this);
        //initialize project folder
        File dir2 = new File(path2);
        //make project folder
        dir2.mkdirs();

        //link DatabaseHelper class to database
        myDb = new DatabaseHelper(this);

        //change settings on file
        changeSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //goto settings page
                Intent intent = new Intent(MainActivity.this, com.ferguson.turd.shwifty_10.SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        //erase settings file
        eraseSettings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                eraseFile(f_dim);
            }
        });

        //change mac address on file
        changeMac.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addressDialog();
            }
        });

        //erase mac address file
        eraseMac.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                eraseFile(f_addr);
            }
        });

        //connect to BT device
        connect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                bluetooth_setup();
                paired_devices_check();
            }
        });

        //disconnect from BT device
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do nothing
                //working out bugs
            }
        });

        //delete setings file
        deleteSettings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                deleteFile(f_dim);
            }
        });

        //delete mac address file
        deleteMac.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                deleteFile(f_addr);
            }
        });

        //send data in settings file to device
        sendData.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //no need to send address to device...maybe change to device name in future?
                /*if(myThreadConnected!=null){
                    //byte[] bytesToSend = inputField.getText().toString().getBytes();
                    try {
                        myThreadConnected.write(readAddressFile().getBytes());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }*/

                //if connected to device, read the dimensions and send them in string: s(shape)xxx.xxyyy.yyzzz.zzaaaabbbbs
                if(myThreadConnected!=null){
                    //byte[] bytesToSend = inputField.getText().toString().getBytes();
                    try {
                        myThreadConnected.write(readDimensionsFile().getBytes());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

    }

    //write address to file function
    public void writeAddress(String data) {

        /////////////////////////////////////////////////////
        verifyStoragePermissions(MainActivity.this);
        //File dir = new File(path);
        //dir.mkdirs();
        /////////////////////////////////////////////////////

        /////////////////////////////////////////////////////////////////////////////////////
        try {

            Writer output;
            output = new BufferedWriter(new FileWriter(f_addr, false)); //false=overwrite, true=append
            output.write(data);
            output.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /////////////////////////////////////////////////////////////////////////////////////
    }

    //read address file function
    public String readAddressFile() throws FileNotFoundException {

        String finalString = "";
        //declare scanner for reading file at specified path
        Scanner scanner = new Scanner(new File(path + addrFile));

        //use ',' delimiter to parse data
        scanner.useDelimiter(","); //this is not being used

        //parse file
        while (scanner.hasNext()) {
            //get next data
            String nextData = scanner.next();
            Toast.makeText(this, nextData, Toast.LENGTH_SHORT).show();
            //append data to output string
            finalString = finalString + nextData;
        }

        //close scanner
        scanner.close();
        //return file in the form of a string
        return finalString;
    }

    //read dimensions file
    public String readDimensionsFile() throws FileNotFoundException {

        String finalString = "";
        //declare scanner for reading file at specified path
        Scanner scanner = new Scanner(new File(path + dimensionFile));

        //use ',' delimiter to parse data
        //scanner.useDelimiter(",");

        //parse file
        //parse file
        while (scanner.hasNext()) {
            //get next data
            String nextData = scanner.next();
            Toast.makeText(this, "Dimensions: " + nextData, Toast.LENGTH_SHORT).show();
            //append data to output string
            finalString = finalString + nextData;
        }

        //close scanner
        scanner.close();
        //return file contents in the form of a string
        return finalString;
    }

    //alert dialog box function for entering mac address
    public void addressDialog() {

        /*final EditText et1, et2, et3, et4, et5, et6;
        et1 = (EditText) findViewById(R.id.box1);
        et2 = (EditText) findViewById(R.id.box2);
        et3 = (EditText) findViewById(R.id.box3);
        et4 = (EditText) findViewById(R.id.box4);
        et5 = (EditText) findViewById(R.id.box5);
        et6 = (EditText) findViewById(R.id.box6);
        final int size = 2;*/

        // "00:11:22:AA:BB:CC"
        //String address = "00:11:22:AA:BB:CC";

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Dialog box won't disappear if screen is touched
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_layout, null));

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.dialog_message)
                .setTitle(R.string.dialog_title);

        //create button for data submission
        builder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //create a var for dialog box
                Dialog d = (Dialog) dialog;
                //link button variable to dialog using button id
                EditText et1 = (EditText) d.findViewById(R.id.box1);
                //store value to string
                final String address = et1.getText().toString();
                //write value to address file
                writeAddress(address);
            }
        });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        //while the user has not entered the address
       /* while(dialogBox == 1) {
            dialog.show();
        }*/

        //show the dialog box
        dialog.show();

        //mBuilder.setView(mView);
    }

    //function for erasing a file
    public void eraseFile(File file) {

        try {

            //create empty string
            String blankText = "";

            //overwrite file with empty string
            Writer output;
            output = new BufferedWriter(new FileWriter(file, false));
            output.write(blankText);
            output.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //function for deleting a file
    public void deleteFile(File file) {
        //delete file
        file.delete();
        //if successful, generate message for user
        Toast.makeText(getApplicationContext(), "File Deleted", Toast.LENGTH_SHORT).show();

    }

    //onStop function for when activity stops
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called");
        super.onStop();
        finish();
    }

    //onDestroy function for when activity is being destroyed
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();

        /*if(myThreadConnected!=null){
            myThreadConnected.cancel();
        }*/
        /*if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }*/
        /*if(mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "trying to disable adapter", Toast.LENGTH_LONG).show();
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "adapter disabled", Toast.LENGTH_LONG).show();
        }*/
        //exit app
        finish();
    }



    //unpair from a device
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //transfer a file to database
    public void TransferFile() {
        //transfer file to database
        try {
            //scan the csv file and transfer to SQLite db
            FileScanner();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //if successful, generate message for user
        Toast.makeText(getApplicationContext(), "File Transferred", Toast.LENGTH_SHORT).show();
    }

    //scan a file
    public void FileScanner () throws FileNotFoundException {
        //create index for each variable on a single line
        int index = 0;
        //create temp strings for allocating data to SQLite db
        String tmp1 = null,
                tmp2 = null,
                tmp3 = null,
                tmp4 = null,
                tmp5 = null,
                tmp6 = null,
                tmp7;

        //create new scanner for file path
        Scanner scanner = new Scanner(new File(path2 + filename1));
        //scan csv file by comma
        scanner.useDelimiter(",");
        //while the next csv value is there
        while(scanner.hasNext()){
            //get first value and increment index
            if(index == 0){
                tmp1 = scanner.next();
                index++;
            }
            //get second value and increment index
            else if(index == 1){
                tmp2 = scanner.next();
                index++;
            }
            //get third value and increment index
            else if(index == 2){
                tmp3 = scanner.next();
                index++;
            }
            //get fourth value and increment index
            else if(index == 3){
                tmp4 = scanner.next();
                index++;
            }
            //get fifth value and increment index
            else if(index == 4){
                tmp5 = scanner.next();
                index++;
            }
            //get sixth value and increment index
            else if(index == 5){
                tmp6 = scanner.next();
                index++;
            }
            //get seventh value and increment index
            else{
                tmp7 = scanner.next();
                //once all values on the line are obtained, insert data into SQLite db
                myDb.insertData(tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7);
                //reset index for new line
                index = 0;
            }
        }
        //when the end of the file is reached, close the scanner
        scanner.close();
    }

    //set up bluetooth
    public void bluetooth_setup() {
        //1. Get the BluetoothAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
        }

        //2. Enable Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //wait for bluetooth to be enabled
            //TAKE THIS FUNCTION OUT AND ADD A CONDITION TO THE NEXT FUNCTION AFTER SETUP
            while(!mBluetoothAdapter.isEnabled()){}
            Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
        }
    }

    //check for paired devices
    public void paired_devices_check() {
        //Toast.makeText(getApplicationContext(), "test", Toast.LENGTH_SHORT).show();

        //create a set to store found paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Toast.makeText(getApplicationContext(), pairedDevices.toString(), Toast.LENGTH_SHORT).show();
        //if paired devices are found...
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name, address  and UUID of each paired device.
            for (BluetoothDevice device : pairedDevices) {

                ParcelUuid[] uuids = device.getUuids();
                for (ParcelUuid ep : uuids) {
                    Log.e("UUID records : ", ep.toString());
                    Toast.makeText(getApplicationContext(), "UUID: " + ep.toString() + "\r\n" +
                            Integer.toString(pairedDevices.size()) + "\r\n" +
                            pairedDevices.toString() + "\r\n" +
                            device.getName() + "\r\n" +
                            device.getAddress() + "\r\n" +
                            device.getUuids() + "\r\n" +
                            device.getBluetoothClass() + "\r\n" +
                            device.getBondState() + "\r\n" +
                            device.getClass() + "\r\n", Toast.LENGTH_LONG).show();

                    //if name of device == thing1 connect
                    if (uniqueNAME.equals(device.getName())) {
                        myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                        myThreadConnectBTdevice.start();
                    }
                }

                //discover_devices();

                //if device matches device on file, make connection and get new data.
                //match address, uuid, name...or maybe just address??? all three might be overkill but better be safe than sorry
            }
        }
        //if there are no paired devices...
        else {
            Toast.makeText(getApplicationContext(), "No paired devices found", Toast.LENGTH_SHORT).show();

            //search for new device
            discover_devices();
        }
    }

    //discover devices
    public void discover_devices(){
        //if already discovering...
        if(mBluetoothAdapter.isDiscovering()){
            //cancel discovery
            mBluetoothAdapter.cancelDiscovery();

            //check BT permissions in manifest
            checkBTPermissions();

            //start new discovery
            mBluetoothAdapter.startDiscovery();
            //start intent filter for discover method
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, discoverDevicesIntent);

            //start a timer for discovery mode
            pairing_timer = new CountDownTimer(15000, 1000) {
                public void onTick(long millisUntilFinished) {
                    Toast.makeText(getApplicationContext(), "Searching for device: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
                }

                //once the timer is finished...
                public void onFinish() {
                    Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                    //cancel discovery
                    cancel_discovery();
                    Toast.makeText(getApplicationContext(), "Discovery cancelled", Toast.LENGTH_SHORT).show();
                }
            }.start();//start timer method
        }
        //if not discovering devices...
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            //start discovering devices
            mBluetoothAdapter.startDiscovery();
            //start intent filter for discover method
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, discoverDevicesIntent);

            //start a timer for discovery mode
            pairing_timer = new CountDownTimer(15000, 1000) {
                public void onTick(long millisUntilFinished) {
                    Toast.makeText(getApplicationContext(), "Searching for device: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
                }

                public void onFinish() {
                    Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                    //cancel discovery
                    cancel_discovery();
                    Toast.makeText(getApplicationContext(), "Discovery cancelled", Toast.LENGTH_SHORT).show();
                }
            }.start();//start timer method
        }
    }

    //cancel discovery
    public void cancel_discovery(){
        //if discovering already, cancel discovery
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Discover: Cancelling discovery.");
            Toast.makeText(getApplicationContext(), "Discovery cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    //attempt to pair with given device
    public void try_to_pair(BluetoothDevice device) {
        //get name of device
        String deviceName = device.getName();

        //if pairing is allowed for device...
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "trying to pair with " + deviceName);
            Toast.makeText(getApplicationContext(), "Trying to pair with " + deviceName, Toast.LENGTH_SHORT).show();
            //start timer for pairing
            bonding_timer();
            Toast.makeText(getApplicationContext(), "Bonding timer started", Toast.LENGTH_SHORT).show();
            //pair
            device.createBond();
            //wait until bonded...this could be bad???
            while(device.getBondState() != device.BOND_BONDED) {}
            //once paired, cancel timer
            bonding_timer.cancel();
            Toast.makeText(getApplicationContext(), "Bonding timer cancelled", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Paired with: " + deviceName, Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Connecting with: " + deviceName, Toast.LENGTH_SHORT).show();

            //start connecting thread
            myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
            myThreadConnectBTdevice.start();
        }
        //if pairing is not allowed...
        else {
            Toast.makeText(getApplicationContext(), "Phone unable to pair", Toast.LENGTH_SHORT).show();
            //exit app
            finish();
        }
    }

    //check the BT permissions in manifest
    private void checkBTPermissions(){
        //if API level is greater than LOLLIPOP, check permissions
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            //check to see if permissions have been added to AndroidManifest.xml
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                //if they have been added
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
            //otherwise no permission check needed
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    //create a timer method for pairing
    public void bonding_timer() {
        bonding_timer = new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) {
                Toast.makeText(getApplicationContext(), "Trying to pair: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
            }

            //once timer is done
            public void onFinish() {
                Toast.makeText(getApplicationContext(), "Pairing failed", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "Exiting App", Toast.LENGTH_SHORT).show();
                //exit app
                finish();
            }
        }.start();//start timer
    }

    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                Toast.makeText(getApplicationContext(), "bluetoothSocket: \n" + bluetoothSocket, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "something wrong bluetoothSocket.connect(): \n" + eMessage, Toast.LENGTH_SHORT).show();
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), msgconnected, Toast.LENGTH_SHORT).show();
                    }});

                startThreadConnected(bluetoothSocket);
            }else{
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(), "close bluetoothSocket", Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
                Toast.makeText(getApplicationContext(), "bluetoothSocket closed", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        private boolean isWritable() {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    String strReceived = new String(buffer, 0, bytes);

                    final String msgReceived = String.valueOf(bytes) +
                            " bytes received:\n"
                            + strReceived;

                    /////////////////////////////////////////////////////////////////////////////////////
                    if (isWritable()) {
                        try {
                            //FileOutputStream fos = new FileOutputStream(f);
                            /*String data = "Data has been written to this test file.\r\n" +
                                    "This test was successful.\r\n\r\n";*/
                            String data = strReceived;

                            Writer output;
                            output = new BufferedWriter(new FileWriter(file2, true));
                            output.write(data);
                            output.close();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    /////////////////////////////////////////////////////////////////////////////////////

                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            //Toast.makeText(getApplicationContext(), msgReceived, Toast.LENGTH_SHORT).show();
                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), msgConnectionLost, Toast.LENGTH_SHORT).show();
                        }});
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                Toast.makeText(getApplicationContext(), "close connectedbluetoothSocket", Toast.LENGTH_LONG).show();
                connectedBluetoothSocket.close();
                Toast.makeText(getApplicationContext(), "connectedbluetoothSocket closed", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
