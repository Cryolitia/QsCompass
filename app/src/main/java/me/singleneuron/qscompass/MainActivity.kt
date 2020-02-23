package me.singleneuron.qscompass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaterialAlertDialogBuilder(this).setTitle(R.string.app_name).setMessage(R.string.message).setPositiveButton(R.string.ok){ _, _ -> this.finish() }.setOnCancelListener { this.finish() }.show()
    }

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

}
