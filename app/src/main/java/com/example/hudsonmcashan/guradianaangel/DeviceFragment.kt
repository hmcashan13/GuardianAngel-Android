package com.example.hudsonmcashan.guradianaangel

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_device.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DeviceFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [DeviceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DeviceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device, container, false)
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
