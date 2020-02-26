package me.singleneuron.qscompass

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.hardware.*
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class CompassTileService : TileService(), SensorEventListener {

    private lateinit var sensorManager : SensorManager
    private lateinit var aSensor : Sensor
    private lateinit var mSensor : Sensor
    private var accelerometerValues = FloatArray(3)
    private var magneticFieldValues = FloatArray(3)

    override fun onClick() {
        super.onClick()
        Log.d("statue","OnClick")
        if (qsTile.state!=Tile.STATE_ACTIVE) {
            qsTile.state = Tile.STATE_ACTIVE
            val intent = Intent(this,BackgroundActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            if (!this::sensorManager.isInitialized) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            }
            sensorManager.registerListener(this,aSensor,SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL)
        }
        else {
            qsTile.state =  Tile.STATE_INACTIVE
            qsTile.label = this.getString(R.string.compass)
            sensorManager.unregisterListener(this)
        }
        qsTile.updateTile()
    }

    private fun calculateOrientation() {
        if (qsTile==null||qsTile.state!=Tile.STATE_ACTIVE) return
        val values = FloatArray(3)
        val R1 = FloatArray(9)
        SensorManager.getRotationMatrix(R1,null,accelerometerValues,magneticFieldValues)
        SensorManager.getOrientation(R1,values)
        //Log.d("calculate:",values.joinToString())
        //Log.d("calculate:",R.joinToString())
        var degree : Int = Math.toDegrees(values[0].toDouble()).toInt()
        if (degree<0) degree += 360
        val s : String = when(degree) {
            in 355..360, in 0..5 -> getString(R.string.N)
            in 5..85 -> getString(R.string.EN)
            in 85..95 -> getString(R.string.E)
            in 95..175 -> getString(R.string.ES)
            in 175..185 -> getString(R.string.S)
            in 185..265 -> getString(R.string.WS)
            in 265..275 -> getString(R.string.W)
            in 275..355 -> getString(R.string.WN)
            else ->""
        }
        qsTile.label = "$s $degree°"

        val bitmap = BitmapFactory.decodeResource(this.resources,R.drawable.navigation)
        val bmResult = Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmResult)
        canvas.rotate(degree.toFloat(),(bitmap.width/2.0).toFloat(),(bitmap.height/2.0).toFloat())
        canvas.drawBitmap(bitmap,0.0.toFloat(),0.0.toFloat(),null)
        qsTile.icon = Icon.createWithBitmap(bmResult)

        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d("statue","OnStartListening")
        //qsTile.state = Tile.STATE_INACTIVE
        //qsTile.updateTile()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d("statue","OnStopListening")
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.label = getString(R.string.compass)

        val bitmap = BitmapFactory.decodeResource(this.resources,R.drawable.navigation)
        val bmResult = Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmResult)
        canvas.drawBitmap(bitmap,0.0.toFloat(),0.0.toFloat(),null)
        qsTile.icon = Icon.createWithBitmap(bmResult)

        qsTile.updateTile()
        //sensorManager.unregisterListener(myListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!=null&&event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values
            //Log.d("sensorUpdate:MAGNETIC",magneticFieldValues.joinToString())
        }
        if (event!=null&&event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values
            //Log.d("sensorUpdate:ACCELEROMETER",accelerometerValues.joinToString())
        }
        calculateOrientation()
    }

}
