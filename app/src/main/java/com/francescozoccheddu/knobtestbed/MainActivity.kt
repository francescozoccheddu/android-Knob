package com.francescozoccheddu.knobtestbed

import android.app.Activity
import android.os.Bundle
import com.francescozoccheddu.knob.KnobView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val knob = findViewById<KnobView>(R.id.knob)
        knob.isFocusableInTouchMode = true
    }

}
