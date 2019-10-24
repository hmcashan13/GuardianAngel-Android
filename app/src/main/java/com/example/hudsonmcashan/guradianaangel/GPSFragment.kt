package com.example.hudsonmcashan.guradianaangel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions



/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [GPSFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [GPSFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GPSFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_g, container, false)
        val mapFragment = this.childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment
        mapFragment.getMapAsync{
            it.mapType = GoogleMap.MAP_TYPE_NORMAL
            it.clear()
            val googlePlex = CameraPosition.builder()
                    .target(LatLng(37.4219999,-122.0862462))
                .zoom(10.toFloat())
                .bearing(0.toFloat())
                .tilt(45.toFloat())
                .build()
            it.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex), 10000, null)
            it.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex), 10000, null);
            it.addMarker(MarkerOptions()
                    .position(LatLng(37.4219999, -122.0862462))
                    .title("Spider Man"))

            it.addMarker(MarkerOptions()
                    .position(LatLng(37.4629101, -122.2449094))
                    .title("Iron Man")
                    .snippet("His Talent : Plenty of money"))

            it.addMarker(MarkerOptions()
                    .position(LatLng(37.3092293, -122.1136845))
                    .title("Captain America"))

        }
        return rootView
        //return inflater.inflate(R.layout.fragment_g, container, false)
    }
}
