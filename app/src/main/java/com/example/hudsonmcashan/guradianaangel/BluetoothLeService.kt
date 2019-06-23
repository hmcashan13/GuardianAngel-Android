package com.example.hudsonmcashan.guradianaangel

import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.Log
import java.util.*
import android.os.IBinder
import android.bluetooth.BluetoothGattDescriptor

@TargetApi(23)
class BluetoothLeService : Service() {

    private var connectionState = STATE_DISCONNECTED

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = BluetoothAdapter.STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    val services = mBluetoothGatt?.discoverServices()
                    Log.i(TAG_BLUETOOTH, "Connected to GATT server.")
                    Log.i(TAG_BLUETOOTH, "Attempting to start service discovery: $services")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = BluetoothAdapter.STATE_DISCONNECTED
                    Log.i(TAG_BLUETOOTH, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG_BLUETOOTH, "Services discovered")
            val service = mBluetoothGatt!!.getService(UUID_SERVICE_UART)
            val characteristicTx = service.getCharacteristic(UUID_CHARACT_TX)

            setCharacteristicNotification(characteristicTx,true)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            val value = characteristic?.value
            if (value != null) {
                val data = String(value, Charsets.UTF_8)
                broadcastUpdate(ACTION_DATA_AVAILABLE, data)
            }
        }

    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, data: String) {
        val intent = Intent(action)

        intent.putExtra(EXTRA_DATA, "$data")
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: BluetoothLeService
            get(): BluetoothLeService = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    private val mBinder = LocalBinder()

    /**
     * Initializes a reference to the local Bluetooth adapter.
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        Log.d(TAG_BLUETOOTH, "Initializing bluetooth")
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG_BLUETOOTH, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG_BLUETOOTH, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * @param address The device address of the destination device.
     * *
     * *
     * @return Return true if the connection is initiated successfully. The connection result
     * *         is reported asynchronously through the
     * *         `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * *         callback.
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG_BLUETOOTH, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress
                && mBluetoothGatt != null) {
            Log.d(TAG_BLUETOOTH, "Trying to use an existing mBluetoothGatt for connection.")
            if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }

        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG_BLUETOOTH, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this@BluetoothLeService, false, mGattCallback)
        Log.d(TAG_BLUETOOTH, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG_BLUETOOTH, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    fun close() {
        if (mBluetoothGatt == null) {
            return
        } else {
            mBluetoothGatt?.close()
            mBluetoothGatt = null
        }
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG_BLUETOOTH, "BluetoothAdapter not initialized")
            return false
        }
        return mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     * @param characteristic Characteristic to act on.
     * *
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
                                      enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG_BLUETOOTH, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        if (UUID_CHARACT_TX == characteristic.uuid) {
            val descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() {
            if (mBluetoothGatt == null) return null

            return mBluetoothGatt!!.services
        }

    companion object {
        val UART_UUID = UUID.fromString("8519BF04-6C36-4B4A-4182-A2764CE2E05A")
        val UUID_SERVICE_UART = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val UUID_CHARACT_RX   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val UUID_CHARACT_TX   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val ACTION_GATT_CONNECTED = "com.example.hudsonmcashan.guardianangel.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.hudsonmcashan.guardianangel.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED =
                "com.example.hudsonmcashan.guardianangel.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.hudsonmcashan.guardianangel.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.example.hudsonmcashan.guardianangel.EXTRA_DATA"
    }
}