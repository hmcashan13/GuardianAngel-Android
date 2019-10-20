package com.example.hudsonmcashan.guradianaangel

import android.util.Log.i
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

val TAG_TAB = "TabAdapter"

class TabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(p0: Int): Fragment {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        return when (position) {
//            0 -> { i(TAG_TAB,"") }
//            else -> {
//                return ThirdFragment()
//            }
//        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "First Tab"
            else -> {
                return "Second Tab"
            }
        }
    }
}