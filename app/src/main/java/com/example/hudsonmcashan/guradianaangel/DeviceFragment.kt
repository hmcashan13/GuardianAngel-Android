package com.example.hudsonmcashan.guradianaangel

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log.i
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_device.*

val TAG_DEVICE_FRAGMENT = "Device Fragment"
class DeviceFragment : Fragment() {
    val subject = BehaviorSubject.create<String>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subject.observeOn(AndroidSchedulers.mainThread()).subscribe ({
            i(TAG_DEVICE_FRAGMENT, "Event: $it")
            when(it) {
                "connected" -> {
                    this.hideTempSpinner()
                    this.hideWeightSpinner()
                }

                "connecting" -> {
                    this.showTempSpinner()
                    this.showWeightSpinner()
                }

                "notConnected" -> {
                    i(TAG_DEVICE_FRAGMENT, "TEST1")
                    this.temp_label.text = getString(R.string.notConnected)
//                    this.beacon_label.text = getString(R.string.notConnected)
                    this.weight_label.text = getString(R.string.No)
                    this.hideTempSpinner()
//                    this.hideBeaconSpinner()
                    this.hideWeightSpinner()
                }

                "beaconConnected" -> hideBeaconSpinner()

                "beaconConnecting" -> {
                    i(TAG_DEVICE_FRAGMENT, "TEST2")
                    showBeaconSpinner()
                }

                "beaconNotConnected" -> {
                    this.beacon_label.text = getString(R.string.notConnected)
                    hideBeaconSpinner()
                }
            }

        }, {
            i(TAG_DEVICE_FRAGMENT, "Error: $it")
        })
    }

    fun showTempSpinner() {
        temp_progressBar.visibility = View.VISIBLE
        temp_label.visibility = View.GONE
        Handler().postDelayed({
            temp_progressBar.visibility = View.GONE
            temp_label.visibility = View.VISIBLE
        }, spinnerTimeout)
    }

    fun showBeaconSpinner() {
        beacon_progressBar.visibility = View.VISIBLE
        beacon_label.visibility = View.GONE
        Handler().postDelayed({
            beacon_progressBar.visibility = View.GONE
            beacon_label.visibility = View.VISIBLE
        }, spinnerTimeout)
    }

    fun showWeightSpinner() {
        weight_progressBar.visibility = View.VISIBLE
        weight_label.visibility = View.GONE
        Handler().postDelayed({
            weight_label.visibility = View.VISIBLE
            weight_progressBar.visibility = View.GONE
        }, spinnerTimeout)
    }

    fun hideTempSpinner() {
        temp_progressBar.visibility = View.GONE
        temp_label.visibility = View.VISIBLE
    }

    fun hideBeaconSpinner() {
        beacon_progressBar.visibility = View.GONE
        beacon_label.visibility = View.VISIBLE
    }

    fun hideWeightSpinner() {
        weight_label.visibility = View.VISIBLE
        weight_progressBar.visibility = View.GONE
    }

    // Spinner functions
    fun showSpinners() {
        temp_progressBar.visibility = View.VISIBLE
        beacon_progressBar.visibility = View.VISIBLE
        temp_label.visibility = View.GONE
        beacon_label.visibility = View.GONE
        Handler().postDelayed({
            temp_label.visibility = View.VISIBLE
            beacon_label.visibility = View.VISIBLE
            weight_label.visibility = View.VISIBLE
            temp_progressBar.visibility = View.GONE
            beacon_progressBar.visibility = View.GONE
            weight_progressBar.visibility = View.GONE
        }, spinnerTimeout)
    }

    fun hideSpinners() {
        temp_label.visibility = View.VISIBLE
        beacon_label.visibility = View.VISIBLE
        weight_label.visibility = View.VISIBLE
        temp_progressBar.visibility = View.GONE
        beacon_progressBar.visibility = View.GONE
        weight_progressBar.visibility = View.GONE

    }

}
