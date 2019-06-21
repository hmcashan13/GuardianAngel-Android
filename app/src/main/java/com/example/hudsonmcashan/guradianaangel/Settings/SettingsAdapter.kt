package com.example.hudsonmcashan.guradianaangel.Settings

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.hudsonmcashan.guradianaangel.R

class SettingsAdapter(private val context: Context,
                      private val dataSource: ArrayList<String>): BaseAdapter() {
    private val mContext: Context


    init {
        mContext = context
    }

    // responsible for how many rows in my list
    override fun getCount(): Int {
        return dataSource.size
    }

    // you can also ignore this
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // you can ignore this for now
    override fun getItem(position: Int): Any {
        return "TEST STRING"
    }

    // responsible for rendering out each row
    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val rowMain = layoutInflater.inflate(R.layout.row_main, viewGroup, false)

        val nameTextView = rowMain.findViewById<TextView>(R.id.name_textView)
        nameTextView.text = dataSource.get(position)

        nameTextView.setTextColor(Color.WHITE)
        //val positionTextView = rowMain.findViewById<TextView>(R.id.position_textview)
        //positionTextView.text = "Row number: $position"


        return rowMain
    }
}