package me.singleneuron.qscompass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class BackgroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moveTaskToBack(true)
    }

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

}
