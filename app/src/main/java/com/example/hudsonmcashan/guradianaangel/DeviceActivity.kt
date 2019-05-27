package com.example.hudsonmcashan.guradianaangel

import android.annotation.TargetApi
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.os.Bundle
import android.os.RemoteException
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import kotlinx.android.synthetic.main.activity_connection.*
import java.util.*
import org.altbeacon.beacon.*
import android.support.v7.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import org.jetbrains.anko.toast
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.os.IBinder
import android.widget.ExpandableListView
import java.io.IOException
import java.io.InputStream

// Tags
val TAG_BEACON = "BeaconDeviceActivity"
val TAG_BLUETOOTH = "BluetoothDeviceActivity"

@TargetApi(21)
class DeviceActivity : AppCompatActivity(), BeaconConsumer {
    // Notification properties
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    private val channelId = "com.example.hudsonmcashan.guradianaangel"
    private val title = "Guardian Angel"
    private val outOfRegionNotificationDescription = "Your too far away from your baby"
    private val inRegionNotificationDescription = "Entered region"
    private val tooHotNotificationDescription = "Your car is too hot"

    // Beacon properties
    lateinit var beaconManager: BeaconManager
    lateinit var my_region: Region

    // Bluetooth properties
    lateinit var bluetoothDevice: BluetoothDevice
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bleScanner: BluetoothLeScanner
    lateinit var bleScanCallback: BluetoothAdapter.LeScanCallback
    private val  bluetoothServce = BluetoothLeService()

    private var mDeviceAddress: String = "C8:84:47:1E:73:4E"
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? = ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null

    val ENABLE_BT_REQUEST_CODE = 1

    var connectionState = STATE_DISCONNECTED

    var isBabyInSeat: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        setupSettingsButton()
        setupNotificationManager()
        setupBeacon()
        //TODO: set UART up properly


        setupUART()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }

//    override fun onPause() {
//        super.onPause()
//        unregisterReceiver(mGattUpdateReceiver)
//    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    private fun setupSettingsButton() {
        settings_button.setOnClickListener {
            launchSettings()
        }
    }

    private fun setupNotificationManager() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun setupBeacon() {
        beaconManager = BeaconManager.getInstanceForApplication(this)
        val uuid: UUID = UUID.fromString("fda50693-a4e2-4fb1-afcf-c6eb07647825")
        val id1: Identifier = Identifier.fromUuid(uuid)
        val id2: Identifier = Identifier.fromInt(10011)
        val id3: Identifier = Identifier.fromInt(10011)
        my_region = Region("my_beacon_region", id1, id2, id3)
        beaconManager.getBeaconParsers().add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        beaconManager.bind(this)
        //  Setup location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("This app needs location access")
                builder.setMessage("Please grant location access so this app can detect beacons")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener(DialogInterface.OnDismissListener { requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1) })
                builder.show()
            }
        }
    }

    // Code to manage Service lifecycle.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.d(TAG_BLUETOOTH, "Just making sure this thing works ya know")
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG_BLUETOOTH, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG_BLUETOOTH, "If I put enough print statements, surely something will be printed")
            mBluetoothLeService = null
        }
    }

    private fun setupUART() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            // Bluetooth is not supported
            Toast.makeText(applicationContext,"This device doesn’t support Bluetooth",Toast.LENGTH_SHORT).show()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                // Bluetooth is not enabled
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                this@DeviceActivity.startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_CODE)
                Toast.makeText(applicationContext, "Enabling Bluetooth!", Toast.LENGTH_LONG).show()
            } else {
                // Bluetooth is enabled
                startScan()
                Intent(this@DeviceActivity, BluetoothLeService::class.java).also {
                    bindService(it, mServiceConnection, Context.BIND_AUTO_CREATE)
                }
                Log.i(TAG_BLUETOOTH, "UART setup")
            }

        }
    }

    // Used to enable Bluetooth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Check what request we’re responding to//
        if (requestCode == ENABLE_BT_REQUEST_CODE) {
            //If the request was successful…//
            if (resultCode == Activity.RESULT_OK) {
                //...then display the following toast.//
                Toast.makeText(applicationContext, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
            }

            //If the request was unsuccessful...//
            if(resultCode == RESULT_CANCELED){
                //...then display this alternative toast.//
                Toast.makeText(getApplicationContext(), "An error occurred while attempting to enable Bluetooth",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Notification
    private fun sendNotification(description: String) {
        val intent = Intent(this, DeviceActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName, R.layout.notification_layout)
        contentView.setTextViewText(R.id.notification_title, title)
        contentView.setTextViewText(R.id.notification_content, description)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
                    .setAutoCancel(true)
        } else {
            builder = Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
        }
        if (Build.VERSION.SDK_INT < 16) {
            notificationManager.notify(1, builder.notification)
        } else {
            notificationManager.notify(1, builder.build())
        }
    }

    // Settings
    private fun launchSettings() {
        // launch the settings activity
        val settingsButton = findViewById<Button>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun isBabyInSeat() {
        isBabyInSeat = !isBabyInSeat
        if (isBabyInSeat) {
            baby_in_seat_label.text = "Yes"
        } else {
            baby_in_seat_label.text = "No"
        }
    }
    //var x = 0

    // Beacon function
    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()
        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, _ ->
            if (beacons.isNotEmpty()) {
                val beacon = beacons.first()
                val distance = beacon.distance.toInt()
                Log.i(TAG_BEACON, "beacon distance: $distance")

                when(distance){
                    in Int.MIN_VALUE..0 -> beacon_label.text = "Not Connected"
                    in 0..5 -> beacon_label.text = "Very Close"
                    in 5..10 -> beacon_label.text = "Near"
                    else -> beacon_label.text = "Far"
                }
            }
        }

        try {
            beaconManager.startRangingBeaconsInRegion(my_region)
        } catch (e: RemoteException) {
            Log.i(TAG_BEACON, "Ranging failed!!!")
            e.printStackTrace()
        }
        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                Log.i(TAG_BEACON, "Hola")
            }

            override fun didExitRegion(region: Region) {
                Log.i(TAG_BEACON, "Adios")
                beacon_label.text = "Not Connected"
                //sendNotification(outOfRegionNotificationDescription)
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                Log.i(TAG_BEACON, "I have just switched from seeing/not seeing beacons: $state")
            }
        })

        try {
            beaconManager.startMonitoringBeaconsInRegion(my_region)
        } catch (e: RemoteException) {
            Log.i(TAG_BEACON, "Monitoring failed!!!")
            e.printStackTrace()
        }

    }

    // UART functions
    private fun checkBondedDevices() {
        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                Log.i(TAG_BLUETOOTH, "paired device name: $device.name address: ${device.address}")
            }
        }
    }
    private fun startScan() {
        if (bluetoothAdapter.isEnabled) {
            bleScanner = bluetoothAdapter.bluetoothLeScanner
            val scanFilter = ScanFilter.Builder().build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            bleScanner.startScan(Arrays.asList(scanFilter), settings, scanCallback)
        } else {
            toast("Bluetooth is not enabled")
            Log.i(TAG_BLUETOOTH, "bluetooth is not available")
        }
    }
    private fun stopScan() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        } else {
            bluetoothAdapter.stopLeScan(bleScanCallback)
        }
    }

    val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if("NORDIC_USART" == result.device.name) {
                mDeviceAddress = result.device.address
                Log.i(TAG_BLUETOOTH, "Connecting to Guardian Angel: $mDeviceAddress")
                stopScan()
                if (mBluetoothLeService != null) {
                    val result = mBluetoothLeService!!.connect(mDeviceAddress)
                    Log.d(TAG_BLUETOOTH, "Connect request result=" + result)
                }
            }
            super.onScanResult(callbackType, result)
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                //updateConnectionState(R.string.connected)
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                //updateConnectionState(R.string.disconnected)
                //clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService!!.supportedGattServices)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }


    private fun displayData(data: String?) {
        if (data != null) {
            val dataArray = data.split(" ")
            val rawTemp1 = dataArray[0]
            val regex1 = """(T=)""".toRegex()
            val rawTemp2 = regex1.replace(rawTemp1, "")
            val regex2 = """(,)""".toRegex()
            val celsiusTemp = regex2.replace(rawTemp2,".")
            val farenheitTemp = (celsiusTemp.toDouble() * 9/5 + 32).toString()
            val farenheitTempWithDegree = "$farenheitTemp°F"
            temp_label!!.text = farenheitTempWithDegree
            Log.i(TAG_BLUETOOTH, "temp: $farenheitTemp")
        }
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private val servicesListClickListner = ExpandableListView.OnChildClickListener { parent, v, groupPosition, childPosition, id ->
        if (mGattCharacteristics != null) {
            val characteristic = mGattCharacteristics!![groupPosition][childPosition]
            val charaProp = characteristic.properties
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService!!.setCharacteristicNotification(
                            mNotifyCharacteristic!!, false)
                    mNotifyCharacteristic = null
                }
                mBluetoothLeService!!.readCharacteristic(characteristic)
            }
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                mNotifyCharacteristic = characteristic
                mBluetoothLeService!!.setCharacteristicNotification(
                        characteristic, true)
            }
            return@OnChildClickListener true
        }
        false
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val gattServiceData = ArrayList<HashMap<String, String>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String>>>()
        mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
        for (gattService in gattServices) {

        }
    }

    companion object {
        @JvmField
        var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        @JvmField
        var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

}