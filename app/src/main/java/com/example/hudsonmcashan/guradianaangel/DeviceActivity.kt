package com.example.hudsonmcashan.guradianaangel

import android.annotation.TargetApi
import android.app.*
import android.app.Notification.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.widget.Button
import kotlinx.android.synthetic.main.activity_connection.*
import java.util.*
import org.altbeacon.beacon.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.*
import android.provider.Settings
import android.util.Log.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.hudsonmcashan.guradianaangel.Settings.Prefs
import com.example.hudsonmcashan.guradianaangel.Settings.SettingsActivity
import com.example.hudsonmcashan.guradianaangel.Settings.TAG_SETTINGS
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_device.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt

// Timeouts
const val scannerTimeout = 10000L
const val spinnerTimeout = 10000L

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

const val ENABLE_BT_REQUEST_CODE = 1

@Suppress("DEPRECATION")
@TargetApi(26)
class DeviceActivity : AppCompatActivity(), BeaconConsumer {
    // Notification properties
    private val tooFarNotificationDescription = "You are far from the cushion"
    private val inRegionNotificationDescription = "Entered region"
    private val tooHotNotificationDescription = "It's too hot!"

    // Beacon properties
    lateinit var beaconManager: BeaconManager
    lateinit var myRegion: Region

    // Bluetooth properties
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bleScanner: BluetoothLeScanner
    private var mBluetoothLeService: BluetoothLeService? = null
    private var isScanning: Boolean = false

    var isWeightDetected: Boolean = false

    // Preference property
    var prefs: Prefs? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_connection)

        prefs = Prefs(this)
        setupTabBar()
        setupActionBar()
        setupWriteSettings()
        setupInfoButton()
        setupSettingsButton()

        setupUART()
        setupBeacon()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mConnectionState == STATE_DISCONNECTED && bluetoothManager.adapter != null) { startScan() }
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

    private val gpsFragment = GPSFragment()
    private val deviceFragment = DeviceFragment()
    private val fragmentManager = supportFragmentManager
    private var activeFragment: Fragment = deviceFragment

    private fun setupTabBar() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.navigationView)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        fragmentManager.beginTransaction().add(R.id.main_container, gpsFragment,"2").hide(gpsFragment).commit()
        fragmentManager.beginTransaction().add(R.id.main_container, deviceFragment, "1").commit()

    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_device -> {
                fragmentManager.beginTransaction().hide(activeFragment).show(deviceFragment).commit()
                activeFragment = deviceFragment
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_gps -> {
                fragmentManager.beginTransaction().hide(activeFragment).show(gpsFragment).commit()
                activeFragment = gpsFragment
                return@OnNavigationItemSelectedListener true
            }
        }
        false
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
        } else {
            // TODO: handle not being able to (show error?)
        }
    }

    private fun setupInfoButton() {
        info_device_button.setOnClickListener {
            val infoButton = findViewById<Button>(R.id.info_device_button)
            infoButton.setOnClickListener {
                val intent = Intent(this, InfoDeviceActivity::class.java)
                startActivity(intent)
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
        //runOnUiThread {
            deviceFragment.subject.onNext("beaconConnecting")
        //}
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
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter != null) {
            bluetoothAdapter = bluetoothManager.adapter
            if (!bluetoothAdapter.isEnabled) {
                // Bluetooth is not enabled
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                this@DeviceActivity.startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_CODE)
                Toast.makeText(applicationContext, "Enabling Bluetooth!", Toast.LENGTH_LONG).show()
            } else if (mConnectionState != STATE_TEMP_SENSOR_OFF) {
                // Bluetooth is enable
                startScan()
//            Handler().postDelayed({
//                if (mConnectionState == STATE_CONNECTED || mConnectionState == STATE_CONNECTING) {
//                    // It has to be either connecting or connected
//                    d(TAG_BLUETOOTH,"it is either connected or connecting")
//                    if (isScanning) {
//                        stopScan()
//                    }
//                } else {
//                    // TODO: display error with ability to retry or cancel
//                }
//            }, scannerTimeout)

                Intent(this@DeviceActivity, BluetoothLeService::class.java).also {
                    bindService(it, mServiceConnection, Context.BIND_AUTO_CREATE)
                }
                i(TAG_BLUETOOTH, "UART setup")
            }
        } else {
            // TODO: have the format of the AlertDialog look nicer
            // Initialize a new instance of
            val builder = AlertDialog.Builder(this@DeviceActivity)

            // Set the alert dialog title
            builder.setTitle("Error!")

            // Display a message on alert dialog
            builder.setMessage("Bluetooth is not available on this device")

            // Display a neutral button on alert dialog
            builder.setNeutralButton("Ok"){_,_ -> }

            // TODO: give more information for the user to do fix the problem (email support)
            // TODO: this could simply be because the bluetooth is turned off

            // Finally, make the alert dialog using builder
            val dialog: AlertDialog = builder.create()

            // Display the alert dialog on app interface
            dialog.show()
            i(TAG_BLUETOOTH, "Bluetooth is not available on this device")

            // Bluetooth is not available on device
            Timer().schedule(500) {
                deviceFragment.subject.onNext("notConnected")
            }

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

    private fun setWeightStatus(isWeightDetected: Boolean) {
        // TODO: only send notifications if weight is detected
        this.isWeightDetected = isWeightDetected
        weight_label.text = if (isWeightDetected) "Yes" else "No"
    }

    // Manages Beacon connection
    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()
        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, _ ->
            if (beacons.isNotEmpty()) {
                val beacon = beacons.first()
                val distance = beacon.distance.toInt()
                i(TAG_BEACON, "beacon distance: $distance")

                if (mConnectionState == STATE_DISCONNECTED && bluetoothAdapter.isEnabled) {
                    // If we are disconnected from UART but ranging then we are close enough to connect
                    //startScan()

                    // TODO: have the beacon be disconnected as well when UART is disconnected
                } else {
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
                deviceFragment.subject.onNext("beaconNotConnected")
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
        i(TAG_BLUETOOTH, "starting scan")
        if (bluetoothAdapter.isEnabled && !isScanning) {
            i(TAG_BLUETOOTH, "Scanning...")
            // Set State
            isScanning = true
            Handler().postDelayed({
                if (mConnectionState == STATE_CONNECTED || mConnectionState == STATE_CONNECTING) {
                    // It has to be either connecting or connected
                    d(TAG_BLUETOOTH,"it is either connected or connecting")
                    if (isScanning) {
                        stopScan()
                    }
                } else {
                    // TODO: display error with ability to retry or cancel
                }
            }, scannerTimeout)
            // Setup UI
            //runOnUiThread{
                deviceFragment.subject.onNext("connecting")
            //}
            bleScanner = bluetoothAdapter.bluetoothLeScanner
            val scanFilter = ScanFilter.Builder().build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            bleScanner.startScan(Arrays.asList(scanFilter), settings, scanCallback)

        }

    }

    private fun stopScan() {
        isScanning = false
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
            if(getString(R.string.uartName) == result.device.name && mConnectionState == STATE_DISCONNECTED) {
                // Set State
                mConnectionState = STATE_CONNECTING
                // Setup UI
                //runOnUiThread {
                    setActionBarTitle(mConnectionState)
                //}
                val mDeviceAddress = result.device.address
                stopScan()
                if (mBluetoothLeService != null) {
                    val connectionResult = mBluetoothLeService!!.connectUART(mDeviceAddress)
                    d(TAG_BLUETOOTH, "Connect request result=$connectionResult")
                } else {
                    d(TAG_BLUETOOTH, "Bluetooth service is null!!!")
                    // TODO: display error with ability to retry
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
                    // Set State
                    mConnectionState = STATE_CONNECTED
                    // Setup UI
                    //runOnUiThread {
                        setActionBarTitle(mConnectionState)
                        deviceFragment.hideTempSpinner()
                        deviceFragment.hideWeightSpinner()
                    //}
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    sendNotification(tooFarNotificationDescription)
                    // Set State
                    mConnectionState = STATE_DISCONNECTED
                    if (isScanning) {
                        stopScan()
                    }
                    // Setup UI
                    //runOnUiThread {
                        //hideTempSpinner()
                        deviceFragment.subject.onNext("notConnected")
                    //}

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
        if (tempSensorOnOff) { // Temp Sensor is On
            val dataArray = data.split(" ")
            val rawTemp = dataArray[0]
            val rawWeight = dataArray[1]
            val celsiusTemp = parseTemp(rawTemp)
            val weight = parseWeight(rawWeight).toInt()
            val fahrenheitCelsius = prefs?.fahrenheitCelsius ?: true
            if (fahrenheitCelsius) {
                val fahrenheitTemp = (celsiusTemp.toDouble() * 9/5 + 32).roundToInt().toString()
                val fahrenheitTempWithDegree = "$fahrenheitTemp°F"
                i(TAG_BLUETOOTH, "temp: $fahrenheitTemp")
                // Setup UI
                //runOnUiThread {
                    //temp_progressBar.visibility = View.GONE
                    //temp_label.visibility = View.VISIBLE
                    //temp_label.text = fahrenheitTempWithDegree
                //}

            } else {
                val celsiusTempWithDegree = "$celsiusTemp°C"
                i(TAG_BLUETOOTH, "temp: $celsiusTemp")
                // Setup UI
                //runOnUiThread {
                    //temp_progressBar.visibility = View.GONE
                    //temp_label.visibility = View.VISIBLE
                    //temp_label.text = celsiusTempWithDegree
                //}
            }
            if (weight < 3000) {
                setWeightStatus(true)
            }
            i(TAG_BLUETOOTH, "weight: $weight")
        } else { // Temp Sensor is Off
            // Set State
            mConnectionState = STATE_TEMP_SENSOR_OFF
            // Setup UI
            //runOnUiThread {
                setActionBarTitle(STATE_TEMP_SENSOR_OFF)
                temp_progressBar.visibility = View.GONE
                deviceFragment.temp_label.text = getString(R.string.notConnected)
            //}
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