package com.example.hudsonmcashan.guradianaangel

import android.annotation.TargetApi
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import kotlinx.android.synthetic.main.activity_connection.*
import java.util.*
import org.altbeacon.beacon.*
import android.support.v7.app.AlertDialog;
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import org.jetbrains.anko.toast
import kotlin.collections.HashMap
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask

@TargetApi(21)
class DeviceActivity : AppCompatActivity(), BeaconConsumer {
    // Tags
    val TAG_BEACON = "BeaconDeviceActivity"
    val TAG_BLUETOOTH = "BluetoothDeviceActivity"

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
    private lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bleScanner: BluetoothLeScanner
    lateinit var scanCallback: ScanCallback
    lateinit var bleGattCallback: BluetoothGattCallback
    lateinit var uartGatt: BluetoothGatt

    var deviceMap: HashMap<String, BluetoothDevice> = HashMap()
    val UART_UUID = UUID.fromString("8519BF04-6C36-4B4A-4182-A2764CE2E05A")
    val UUID_SERVICE_UART = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val UUID_CHARACT_TX   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val UUID_CHARACT_RX   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    val DEFAULT_SCAN_TIMEOUT = 5000L

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

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
//        if (isScanning) stopScanning()
//        if (uartGatt.device.bondState >= BluetoothGatt.STATE_CONNECTED) disconnect()
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

    private fun setupUART() {
        // Setup UART
        if (Build.VERSION.SDK_INT > 21) {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            setupCallBacks()
            startScan()

            Log.i(TAG_BLUETOOTH, "UART setup")
            //connectTo(UART_UUID)
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
                if (region.equals(my_region)){
                    //x = 1
                    Log.i(TAG_BEACON, "Hola")
                    //sendEnteredRegionNotification()

                }
            }

            override fun didExitRegion(region: Region) {
                if (region.equals(my_region)) {
                   // x = 0
                    Log.i(TAG_BEACON, "Adios")
                    beacon_label.text = "Not Connected"
                    //Timer().schedule(15000) {
                        Log.i(TAG_BEACON, "Timer complete")
                        //if (x == 0) {
                            sendNotification(outOfRegionNotificationDescription)
                        //}
                    //}
                }
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

    private fun setupCallBacks() {
        scanCallback = object: ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                Log.i(TAG_BLUETOOTH, "Scanned devices: " + result.device.name)
                if("NORDIC_USART".equals(result.device.name)) {
                    Toast.makeText(this@DeviceActivity,"hello",Toast.LENGTH_SHORT).show()
                    bleScanner.stopScan(scanCallback)
                    uartGatt = result.device.connectGatt(
                            applicationContext, false, bleGattCallback)
                }
                super.onScanResult(callbackType, result)
            }
        }

        bleGattCallback = object: BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                gatt?.discoverServices()
                super.onConnectionStateChange(gatt, status, newState)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                val uartService = gatt?.getService(UUID_SERVICE_UART)
                if (uartService != null) {
                    val uartCharacteristic = uartService.getCharacteristic(UUID_CHARACT_TX)
                    Log.i(TAG_BLUETOOTH, "TX Characterstic found")
                    gatt.readCharacteristic(uartCharacteristic)
                } else {
                    Log.i(TAG_BLUETOOTH, "uart service was not found")
                }
                super.onServicesDiscovered(gatt, status)
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                if (characteristic != null) {
                    val value: String = characteristic.getStringValue(0)
                    Log.i(TAG_BLUETOOTH, "uart string: $value")
                } else {
                    Log.i(TAG_BLUETOOTH, "uart service was not found")
                }


                super.onCharacteristicRead(gatt, characteristic, status)
            }
        }
    }
}