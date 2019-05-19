package com.example.hudsonmcashan.guradianaangel

import android.annotation.TargetApi
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
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
import kotlin.collections.HashMap
import kotlin.concurrent.schedule
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
    private val TAG_BEACON = "BeaconDeviceActivity"
    lateinit var beaconManager: BeaconManager
    lateinit var my_region: Region

    // Bluetooth properties
    private val TAG_BLUETOOTH = "BluetoothDeviceActivity"
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bleScanner: BluetoothLeScanner
    lateinit var scanCallback: ScanCallback
    lateinit var uartGatt: BluetoothGatt
    var connectedDevice = ""

    var deviceMap: HashMap<String, BluetoothDevice> = HashMap()

    val UUID_SERVICE_UART = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    val UUID_CHARACT_TX   = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    val UUID_CHARACT_RX   = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";

    val DEFAULT_SCAN_TIMEOUT = 5000

    private var isInit           = false
    private var isScanning       = false
    private var isShowAllDevices = true

    val methodNameDeviceDiscovered = "bleUARTDeviceDiscovered"
    val methodNameScanningFinished = "bleUARTScanningFinished"
    val methodNameConnected        = "bleUARTConnected"
    val methodNameDisconnected     = "bleUARTDisconnected"
    val methodNameMessageReceived  = "bleUARTMessageReceived"

    var isBabyInSeat: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_connection)

            setupSettingsButton()
            setupNotificationManager()
            setupBeacon()
            //TODO: set UART up properly
            //setupUART()
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager?.unbind(this)
//        if (isScanning) stopScanning();
//        if (uartGatt.device.bondState >= BluetoothGatt.STATE_CONNECTED) disconnect();
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
        beaconManager?.getBeaconParsers()?.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        beaconManager?.bind(this)
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
        //        if (Build.VERSION.SDK_INT > 21) {
        //            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //            bluetoothAdapter = bluetoothManager.adapter
        //            bleScanner = bluetoothAdapter.bluetoothLeScanner
        //
        //            isInit = true
        //            Log.i(TAG_BLUETOOTH, "BleUART.init(): initialized")
        //
        ////            connectToDevice()
        //        }
    }

    // Notification
    private fun sendTemperatureNotification() {
        val intent = Intent(this, DeviceActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName, R.layout.notification_layout)
        contentView.setTextViewText(R.id.notification_title, title)
        contentView.setTextViewText(R.id.notification_content, tooHotNotificationDescription)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, tooHotNotificationDescription, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
                    .setContent(contentView)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    //.setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
                    .setAutoCancel(true)
        } else {
            builder = Notification.Builder(this)
                    .setContent(contentView)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
        }
        if (Build.VERSION.SDK_INT < 16) {
            notificationManager.notify(1, builder.getNotification())
        } else {
            notificationManager.notify(1, builder.build())
        }
    }

    private fun sendLeftRegionNotification() {
        val intent = Intent(this, DeviceActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName, R.layout.notification_layout)
        contentView.setTextViewText(R.id.notification_title, title)
        contentView.setTextViewText(R.id.notification_content, outOfRegionNotificationDescription)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, outOfRegionNotificationDescription, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
                    .setContent(contentView)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
        } else {
            builder = Notification.Builder(this)
                    .setContent(contentView)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
        }
        if (Build.VERSION.SDK_INT < 16) {
            notificationManager.notify(2, builder.getNotification())
        } else {
            notificationManager.notify(2, builder.build())
        }
    }

    private fun sendEnteredRegionNotification() {
        val intent = Intent(this, DeviceActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName, R.layout.notification_layout)
        contentView.setTextViewText(R.id.notification_title, title)
        contentView.setTextViewText(R.id.notification_content, inRegionNotificationDescription)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, inRegionNotificationDescription, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
                    .setContent(contentView)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
                    .setNumber(5)
        } else {
            builder = Notification.Builder(this)
                    .setContent(contentView)
                    .setSmallIcon(R.drawable.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.angel_wings_app_icon))
                    .setContentIntent(pendingIntent)
        }
        if (Build.VERSION.SDK_INT < 16) {
            notificationManager.notify(3, builder.getNotification())
        } else {
            notificationManager.notify(3, builder.build())
        }
    }

    // Settings
    private fun launchSettings() {
        // launch the settings activity
        val settings_button = findViewById<Button>(R.id.settings_button)
        settings_button.setOnClickListener {
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
    var x = 0

    // Beacon function
    override fun onBeaconServiceConnect() {
        beaconManager?.removeAllMonitorNotifiers()
        beaconManager?.removeAllRangeNotifiers()
        beaconManager?.addRangeNotifier { beacons, region ->
            if (beacons.isNotEmpty()) {
                val beacon = beacons.first()
                val distance = beacon.distance
                Log.i(TAG_BEACON, "beacon distance: $distance")

                if (distance < 0) {
                    beacon_label.text = "Not Connected"
                } else if (distance < 5) {
                    beacon_label.text = "Very Close"
                } else if (distance < 15) {
                    beacon_label.text = "Near"
                } else {
                    beacon_label.text = "Far"
                }
            }
        }

        try {
            beaconManager?.startRangingBeaconsInRegion(my_region)
        } catch (e: RemoteException) {
            Log.i(TAG_BEACON, "Ranging failed!!!")
            e.printStackTrace()
        }
        beaconManager?.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                if (region.equals(my_region)){
                    x = 1
                    Log.i(TAG_BEACON, "Hola")
                    //sendEnteredRegionNotification()

                }
            }

            override fun didExitRegion(region: Region) {
                if (region.equals(my_region)) {
                    x = 0
                    Log.i(TAG_BEACON, "Adios")
                    beacon_label.text = "Not Connected"
                    Timer().schedule(15000) {
                        Log.i(TAG_BEACON, "Timer complete")
                        if (x == 0) {
                            sendLeftRegionNotification()
                        }
                    }
                }
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                Log.i(TAG_BEACON, "I have just switched from seeing/not seeing beacons: $state")
            }
        })

        try {
            beaconManager?.startMonitoringBeaconsInRegion(my_region)
        } catch (e: RemoteException) {
            Log.i(TAG_BEACON, "Monitoring failed!!!")
            e.printStackTrace()
        }

    }
    val uuidString: String = "CC519F02-80F0-88F7-2B69-6FCFF084DF7E"
    val uuid:UUID = UUID.fromString(uuidString)

    // BLE extension
    fun stopScanning() {

    }

    fun disconnect() {
        if (uartGatt.device.bondState >= BluetoothGatt.STATE_CONNECTING) {
            Log.i(TAG_BLUETOOTH, "BleUARTGatt.disconnect(): disconnecting")
            uartGatt.disconnect()
        } else {
            Log.i(TAG_BLUETOOTH, "BleUARTGatt.disconnect(): already disconnected")
        }
    }

    fun startScanning() {
        startScanning(DEFAULT_SCAN_TIMEOUT);
    }

    fun startScanning(scanInterval: Int) {
        if (!isInit) {
            Log.i(TAG_BLUETOOTH, "BleUART.scan(): BleUART isn't initialized.")
            return;
        }

        deviceMap.clear();

        bleScanner.startScan(scanCallback);

        setTimerToStopScan(scanInterval);
        isScanning = true;
        Log.i(TAG_BLUETOOTH, "BleUART.startScan(): scanning started.")
    }

    fun setTimerToStopScan(scanInterval: Int) {
//        Timer().schedule(scanInterval) {
//            Log.i(TAG_BLUETOOTH, "BleUART.setTimerToStopScan(): timer triggered to stop scan")
//            stopScanning()
//        }
    }

    fun connectTo(deviceAddress: String) {
        if (uartGatt.device.bondState == BluetoothGatt.STATE_CONNECTED) {
            Log.i(TAG_BLUETOOTH, "BleUART.connectTo(): WARNING gatt is already connected")
            return;
        } else if (uartGatt.device.bondState >= BluetoothGatt.STATE_CONNECTING) {
            Log.i(TAG_BLUETOOTH, "BleUART.connectTo(): WARNING gatt is already connecting")
            return;
        }

        val device = deviceMap.get(deviceAddress)
        if (device == null) {
            Log.i(TAG_BLUETOOTH, "BleUART.connectTo(): device address <" + deviceAddress + "> not in map; starting scan")
            startScanning(DEFAULT_SCAN_TIMEOUT)
            connectedDevice = deviceAddress
            return;
        }

        Log.i(TAG_BLUETOOTH, "BleUART.connectTo(): connecting to <" + deviceAddress + ">")
//        uartGatt.init(device)
        return;
    }


}