package me.singleneuron.qscompass.wear

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.wear.ambient.AmbientLifecycleObserver
import me.singleneuron.qscompass.wear.databinding.ActivityMainBinding

private const val BURN_IN_OFFSET_PX = 10

class MainActivity : AppCompatActivity(), SensorEventListener,
    AmbientLifecycleObserver.AmbientLifecycleCallback {
    private var sensorManager: SensorManager? = null
    private var aSensor: Sensor? = null
    private var mSensor: Sensor? = null
    private var accelerometerValues: FloatArray? = FloatArray(3)
    private var magneticFieldValues: FloatArray? = FloatArray(3)
    private val lastSensorValue = BooleanArray(2)
    private var hasFeature = true
    private var mDoBurnInProtection = false
    private var binding: ActivityMainBinding? = null

    private val ambientObserver = AmbientLifecycleObserver(this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        val packageManager = getPackageManager()
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)) {
            Toast.makeText(this, "Compass Not Found", Toast.LENGTH_SHORT).show()
            hasFeature = false
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            Toast.makeText(this, "Accelerometer Not Found", Toast.LENGTH_SHORT).show()
            hasFeature = false
        }
        if (!hasFeature) return

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        aSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (aSensor == null) {
            Toast.makeText(this, "Accelerometer Not Found", Toast.LENGTH_SHORT).show()
            hasFeature = false
        }

        if (mSensor == null) {
            Toast.makeText(this, "Magnetic Not Found", Toast.LENGTH_SHORT).show()
            hasFeature = false
        }
        if (!hasFeature) return

        lifecycle.addObserver(ambientObserver)
    }

    override fun onResume() {
        super.onResume()
        resume()
    }

    override fun onStop() {
        super.onStop()
        stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(ambientObserver)
    }

    private fun resume() {
        if (hasFeature) {
            sensorManager!!.registerListener(
                this,
                aSensor,
                if (BuildConfig.DEBUG) SensorManager.SENSOR_DELAY_NORMAL else SensorManager.SENSOR_DELAY_UI
            )
            sensorManager!!.registerListener(
                this,
                mSensor,
                if (BuildConfig.DEBUG) SensorManager.SENSOR_DELAY_NORMAL else SensorManager.SENSOR_DELAY_UI
            )
        } else {
            binding!!.constraintLayout.visibility = View.GONE
            binding!!.tips.text = getString(R.string.notSupport)
            binding!!.constraintLayout.visibility = View.VISIBLE
        }
    }

    private fun stop() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values
            Log.d("Magnetic update", event.values.contentToString())
            lastSensorValue[0] = true
            //Log.d("sensorUpdate:MAGNETIC",magneticFieldValues.joinToString())
        }
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values
            Log.d("Accelerometer update", event.values.contentToString())
            lastSensorValue[1] = true
            //Log.d("sensorUpdate:ACCELEROMETER",accelerometerValues.joinToString())
        }
        if (!(lastSensorValue[0] && lastSensorValue[1])) return
        lastSensorValue[0] = false
        lastSensorValue[1] = false
        calculateOrientation()
    }

    @SuppressLint("SetTextI18n")
    private fun calculateOrientation() {
        //Log.d("statue","calculateOrientation");
        val values = FloatArray(3)
        val R1 = FloatArray(9)
        SensorManager.getRotationMatrix(R1, null, accelerometerValues, magneticFieldValues)
        SensorManager.getOrientation(R1, values)
        //if (values[0]==radianFloat) return;
        values[0]
        val degreeFloat = Math.toDegrees(values[0].toDouble()).toFloat()
        var degree = degreeFloat.toInt()
        //if (degree==lastDegree) return;
        if (degree < 0) degree += 360
        var s = ""
        when (degree) {
            in 355..<360, in 0..<5 -> s =
                getString(R.string.N)

            in 5..<85 -> s = getString(R.string.EN)
            in 85..<95 -> s = getString(R.string.E)
            in 95..<175 -> s = getString(R.string.ES)
            in 175..<185 -> s = getString(R.string.S)
            in 185..<265 -> s = getString(R.string.WS)
            in 265..<275 -> s = getString(R.string.W)
            in 275..<355 -> s = getString(R.string.WN)
        }

        binding!!.textView.text = "$s $degree°"

        val bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.navigation)
        val bmResult =
            createBitmap(bitmap.getWidth(), bitmap.getHeight())
        val canvas = Canvas(bmResult)
        canvas.rotate(
            360f - degreeFloat,
            (bitmap.getWidth() / 2.0).toFloat(),
            (bitmap.getHeight() / 2.0).toFloat()
        )
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        binding!!.imageView.setImageBitmap(bmResult)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
        super.onEnterAmbient(ambientDetails)
        stop()
        mDoBurnInProtection = ambientDetails.burnInProtectionRequired
        binding!!.constraintLayout.visibility = View.GONE
        binding!!.tips.setText(R.string.ambientMode)
        binding!!.tips.visibility = View.VISIBLE
        binding!!.contentView.setBackgroundColor(getColor(R.color.oled_dark))
    }

    override fun onUpdateAmbient() {
        super.onUpdateAmbient()
        if (mDoBurnInProtection) {
            val x = (Math.random() * 2 * BURN_IN_OFFSET_PX - BURN_IN_OFFSET_PX).toInt()
            val y = (Math.random() * 2 * BURN_IN_OFFSET_PX - BURN_IN_OFFSET_PX).toInt()
            binding!!.getRoot().setPadding(x, y, 0, 0)
        }
    }

    override fun onExitAmbient() {
        super.onExitAmbient()
        resume()
        if (mDoBurnInProtection) {
            binding!!.getRoot().setPadding(0, 0, 0, 0)
        }
        binding!!.constraintLayout.visibility = View.VISIBLE
        binding!!.tips.visibility = View.GONE
        binding!!.contentView.setBackgroundColor(getColor(R.color.dark_grey))
    }
}
