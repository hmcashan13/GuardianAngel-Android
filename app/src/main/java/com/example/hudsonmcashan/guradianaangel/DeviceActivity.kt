package com.example.hudsonmcashan.guradianaangel

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.app.Notification.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import kotlinx.android.synthetic.main.activity_connection.*
import java.util.*
import org.altbeacon.beacon.*
import android.support.v7.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast
import org.jetbrains.anko.toast
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.*
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Log.*
import android.view.View
import com.example.hudsonmcashan.guradianaangel.Settings.Prefs
import com.example.hudsonmcashan.guradianaangel.Settings.SettingsActivity
import com.example.hudsonmcashan.guradianaangel.Settings.TAG_SETTINGS
import kotlin.math.roundToInt

// App info
const val appIdentifier = "com.example.hudsonmcashan.guradianaangel"
const val appTitle = "Guardian Angel"

// Tags
const val TAG_BEACON = "BeaconDeviceActivity"
const val TAG_BLUETOOTH = "BluetoothDeviceActivity"
const val TAG_NOTIFICATION = "NotificationDeviceActivity"

// State of connection
const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2
const val STATE_TEMP_SENSOR_OFF = 3
var mConnectionState = STATE_DISCONNECTED

@Suppress("DEPRECATION")
@TargetApi(23)
class DeviceActivity : AppCompatActivity(), BeaconConsumer {
    // Notification properties
    private val tooFarNotificationDescription = "You are far from the cushion"
    private val inRegionNotificationDescription = "Entered region"
    private val tooHotNotificationDescription = "It's too hot!"

    // Beacon properties
    lateinit var beaconManager: BeaconManager
    lateinit var myRegion: Region

    // Bluetooth properties
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bleScanner: BluetoothLeScanner
    private var mDeviceAddress: String? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    val ENABLE_BT_REQUEST_CODE = 1
    var isBabyInSeat: Boolean = false

    // Preference properties
    var prefs: Prefs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        prefs = Prefs(this)
        setupActionBar()
        setupWriteSettings()
        setupInfoButton()
        setupSettingsButton()
        setupBeacon()
        setupUART()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mConnectionState == STATE_DISCONNECTED) { startScan() }
    }

    override fun onDestroy() {
        super.onDestroy()
        // turn off beacon
        beaconManager.unbind(this)
        // turn off UART
        unbindService(mServiceConnection)
        mBluetoothLeService = null
        // turn off receiver
        unregisterReceiver(mGattUpdateReceiver)
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.device_toolbar))
        setActionBarTitle(mConnectionState)
    }

    private fun setActionBarTitle(state: Int) {
        val actionBar = supportActionBar
        when(state) {
            0 -> actionBar!!.title = getString(R.string.notConnected)
            1 -> actionBar!!.title = getString(R.string.connecting)
            2 -> actionBar!!.title = getString(R.string.connected)
            3 -> actionBar!!.title = getString(R.string.tempSensorOff)
        }
    }

    private fun setupWriteSettings() {
        val canWrite = Settings.System.canWrite(this)
        if (!canWrite) {
            Settings.ACTION_MANAGE_WRITE_SETTINGS
        }
    }

    private fun setupInfoButton() {
        info_device_button.setOnClickListener {
            val infoButton = findViewById<Button>(R.id.info_device_button)
            infoButton.setOnClickListener {
                sendNotification(tooFarNotificationDescription)
//                val intent = Intent(this, InfoDeviceActivity::class.java)
//                startActivity(intent)
            }
        }
    }

    private fun setupSettingsButton() {
        settings_button.setOnClickListener {
            val settingsButton = findViewById<Button>(R.id.settings_button)
            settingsButton.setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }


    private fun setupBeacon() {
        // Setup UI
        runOnUiThread {
            beacon_label.visibility = View.GONE
            beacon_progressBar.visibility = View.VISIBLE
        }
        // Initialize Beacon properties
        beaconManager = BeaconManager.getInstanceForApplication(this)
        val uuid: UUID = UUID.fromString("fda50693-a4e2-4fb1-afcf-c6eb07647825")
        val id1: Identifier = Identifier.fromUuid(uuid)
        val id2: Identifier = Identifier.fromInt(10011)
        val id3: Identifier = Identifier.fromInt(10011)
        myRegion = Region("my_beacon_region", id1, id2, id3)
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        beaconManager.bind(this)
        //  Setup location permissions (API 23 and greater)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("This app needs location access")
                builder.setMessage("Please grant location access so this app can detect beacons")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1) }
                builder.show()
            }
        }
        //TODO: Error message that the app isn't compatible with the phone

    }

    private fun setupUART() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            this@DeviceActivity.startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_CODE)
            Toast.makeText(applicationContext, "Enabling Bluetooth!", Toast.LENGTH_LONG).show()
        } else if (mConnectionState != STATE_TEMP_SENSOR_OFF){
            // Bluetooth is enabled
            startScan()
            Intent(this@DeviceActivity, BluetoothLeService::class.java).also {
                bindService(it, mServiceConnection, Context.BIND_AUTO_CREATE)
            }
            i(TAG_BLUETOOTH, "UART setup")
        }
    }

    // Used to connect UART
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                e(TAG_BLUETOOTH, "Unable to initialize Bluetooth")
                finish()
            }
            i(TAG_BLUETOOTH, "callback from service connection")
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Used to enable Bluetooth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Check what request we’re responding to
        if (requestCode == ENABLE_BT_REQUEST_CODE) {
            // Request was successful
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(applicationContext, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
            }

            // Request was unsuccessful
            if(resultCode == RESULT_CANCELED){
                Toast.makeText(applicationContext, "An error occurred while attempting to enable Bluetooth",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Notification
    private fun sendNotification(description: String) {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        lateinit var notificationChannel: NotificationChannel
        lateinit var builder: Builder
        val intent = Intent(this, DeviceActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        notificationLayout.setTextViewText(R.id.notification_title, appTitle)
        notificationLayout.setTextViewText(R.id.notification_content, description)
        val smallIcon = R.mipmap.notification_icon_round
        val largeIcon = BitmapFactory.decodeResource(this.resources, R.mipmap.notification_icon_round)
        i(TAG_SETTINGS, "SDK VERSION: ${Build.VERSION.SDK_INT}")
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                notificationChannel = NotificationChannel(appIdentifier, description, NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.MAGENTA
                notificationChannel.enableVibration(true)
                notificationManager.createNotificationChannel(notificationChannel)

                builder = Builder(this, appIdentifier)
                        .setSmallIcon(R.mipmap.notification_icon)
//                        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomContentView(notificationLayout)
//                        .setCustomBigContentView(notificationLayoutExpanded)
                        .setContentIntent(pendingIntent)
                        .setChannelId(appIdentifier)
                        .setAutoCancel(true)
            }
            else -> builder = Builder(this)
                            .setSmallIcon(smallIcon)
                            .setContentIntent(pendingIntent)
        }

        notificationManager.notify(1, builder.build())
    }

    private fun isBabyInSeat(isInSeat: Boolean) {
        isBabyInSeat = isInSeat
        baby_in_seat_label.text = if (isBabyInSeat) "Yes" else "No"
    }

    // Beacon function
    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()
        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, _ ->
            if (beacons.isNotEmpty()) {

                val beacon = beacons.first()
                val distance = beacon.distance.toInt()
                i(TAG_BEACON, "beacon distance: $distance")

                if (mConnectionState == STATE_DISCONNECTED) {
                    // If we are disconnected from UART but ranging then we are close enough to connect
                    startScan()
                    // TODO: have the beacon be disconnected as well when UART is disconnected
                } else {
                    beacon_progressBar.visibility = View.GONE
                    beacon_label.visibility = View.VISIBLE
                    when (distance) {
                        in Int.MIN_VALUE until 0 -> beacon_label.text = getString(R.string.notConnected)
                        in 0..5 -> beacon_label.text = getString(R.string.veryClose)
                        in 5..10 -> beacon_label.text = getString(R.string.near)
                        else -> beacon_label.text = getString(R.string.far)
                    }
                }
            }
        }

        try {
            beaconManager.startRangingBeaconsInRegion(myRegion)
        } catch (e: RemoteException) {
            i(TAG_BEACON, "Ranging failed!!!")
            e.printStackTrace()
        }
        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                i(TAG_BEACON, "Hola")
                if (mConnectionState == STATE_DISCONNECTED) { startScan() }
            }

            override fun didExitRegion(region: Region) {
                i(TAG_BEACON, "Adios")
                beacon_label.text = getString(R.string.notConnected)
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                i(TAG_BEACON, "I have just switched from seeing/not seeing beacons: $state")
            }
        })

        try {
            beaconManager.startMonitoringBeaconsInRegion(myRegion)
        } catch (e: RemoteException) {
            i(TAG_BEACON, "Monitoring failed!!!")
            e.printStackTrace()
        }

    }

    // Initiates Bluetooth pairing process
    private fun startScan() {
        if (bluetoothAdapter.isEnabled) {
            runOnUiThread{
                temp_label.visibility = View.GONE
                temp_progressBar.visibility = View.VISIBLE
            }
            bleScanner = bluetoothAdapter.bluetoothLeScanner
            val scanFilter = ScanFilter.Builder().build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            bleScanner.startScan(Arrays.asList(scanFilter), settings, scanCallback)
        } else {
            toast("Oh no! Bluetooth is not enabled")
            i(TAG_BLUETOOTH, "bluetooth is not available")
        }
    }

    private fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val tempSensorOnOff = prefs?.tempSensorOnOff ?: true
            if(!tempSensorOnOff) {
                // Set State
                mConnectionState = STATE_TEMP_SENSOR_OFF
                // Setup UI
                setActionBarTitle(STATE_TEMP_SENSOR_OFF)
                return
            }
            if(getString(R.string.uartName) == result.device.name &&
                    (mConnectionState != STATE_CONNECTING || mConnectionState != STATE_CONNECTED)) {
                i(TAG_BLUETOOTH, "Scanning...")
                temp_progressBar.visibility = View.VISIBLE
                mConnectionState = STATE_CONNECTING
                setActionBarTitle(mConnectionState)
                mDeviceAddress = result.device.address
                stopScan()
                if (mBluetoothLeService != null) {
                    val connectionResult = mBluetoothLeService!!.connectUART(mDeviceAddress)
                    d(TAG_BLUETOOTH, "Connect request result=$connectionResult")
                } else {
                    d(TAG_BLUETOOTH, "Bluetooth service is null!!!")
                }
            }
            super.onScanResult(callbackType, result)
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    mConnectionState = STATE_CONNECTED
                    setActionBarTitle(mConnectionState)
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    sendNotification(tooFarNotificationDescription)
                    // Set State
                    mConnectionState = STATE_DISCONNECTED
                    // Setup UI
                    temp_progressBar.visibility = View.GONE
                    //beacon_progressBar.visibility = View.GONE
                    temp_label.visibility = View.VISIBLE
                    //beacon_label.visibility = View.VISIBLE
                    setActionBarTitle(mConnectionState)
                    temp_label.text = getString(R.string.notConnected)
                    beacon_label.text = getString(R.string.notConnected)
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                    dataCounter += 1
                    if(data != null && dataCounter % 2 == 0) {
                        parseAndDisplayData(data)
                    }
                }
            }
        }
    }
    // Delay time between showing updated data
    var dataCounter = 0

    private fun parseAndDisplayData(data: String) {
        val tempSensorOnOff = prefs?.tempSensorOnOff ?: true
        if (tempSensorOnOff) {
            val dataArray = data.split(" ")
            val rawTemp = dataArray[0]
            val rawWeight = dataArray[1]
            val celsiusTemp = parseTemp(rawTemp)
            val weight = parseWeight(rawWeight).toInt()
            val fahrenheitCelsius = prefs?.fahrenheitCelsius ?: true
            if (fahrenheitCelsius) {
                val fahrenheitTemp = (celsiusTemp.toDouble() * 9/5 + 32).roundToInt().toString()
                val fahrenheitTempWithDegree = "$fahrenheitTemp°F"
                temp_progressBar.visibility = View.GONE
                temp_label.visibility = View.VISIBLE
                temp_label.text = fahrenheitTempWithDegree
                i(TAG_BLUETOOTH, "temp: $fahrenheitTemp")
            } else {
                val celsiusTempWithDegree = "$celsiusTemp°C"
                temp_progressBar.visibility = View.GONE
                temp_label.visibility = View.VISIBLE
                temp_label.text = celsiusTempWithDegree
                i(TAG_BLUETOOTH, "temp: $celsiusTemp")
            }
            if (weight < 3000) {
                isBabyInSeat(true)
                // TODO: only send notifications if the baby is in the seat
            }
            i(TAG_BLUETOOTH, "weight: $weight")
        } else {
            // set state
            mConnectionState = STATE_TEMP_SENSOR_OFF
            // setup UI
            this.runOnUiThread {
                setActionBarTitle(STATE_TEMP_SENSOR_OFF)
                temp_progressBar.visibility = View.GONE
                temp_label.text = getString(R.string.notConnected)
            }
        }
    }

    private fun displayData() {}


    private fun parseTemp(temp: String): String {
        val regex1 = """(T=)""".toRegex()
        val temp2 = regex1.replace(temp, "")
        val regex2 = """(,)""".toRegex()
        return regex2.replace(temp2,".")
    }

    private fun parseWeight(weight: String): String {
        val regex1 = """(W=)""".toRegex()
        return regex1.replace(weight, "")
    }

    companion object {
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

}