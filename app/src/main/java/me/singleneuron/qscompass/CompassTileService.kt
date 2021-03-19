package me.singleneuron.qscompass

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.ContextCompat

class CompassTileService : TileService(), SensorEventListener, ServiceConnection {

    private lateinit var sensorManager : SensorManager
    private lateinit var aSensor : Sensor
    private lateinit var mSensor : Sensor
    private var accelerometerValues = FloatArray(3)
    private var magneticFieldValues = FloatArray(3)
    private var lastSensorValue : BooleanArray = BooleanArray(2)
    private var radianFloat : Float = 0F
    private var degreeFloat : Float = 0F
    private var lastDegree : Int = 0
    private var isBackgroundServiceConnected = false
    private lateinit var mService: BackgroundService

    override fun onClick() {
        super.onClick()
        Log.d("statue","OnClick")
        if (!isBackgroundServiceConnected) {
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
            return
        } else {
            if (qsTile.state == Tile.STATE_UNAVAILABLE) {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
            }
        }
        if (qsTile.state!=Tile.STATE_ACTIVE) {
            qsTile.state = Tile.STATE_ACTIVE
            if (!this::sensorManager.isInitialized) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            }
            sensorManager.registerListener(this,aSensor,SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_UI)
        }
        else {
            qsTile.state =  Tile.STATE_INACTIVE
            qsTile.label = this.getString(R.string.compass)
            if (this::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        }
        qsTile.updateTile()
    }

    private fun calculateOrientation() {
        Log.d("statue","calculateOrientation")
        if (qsTile==null||qsTile.state!=Tile.STATE_ACTIVE) return
        val values = FloatArray(3)
        val R1 = FloatArray(9)
        SensorManager.getRotationMatrix(R1,null,accelerometerValues,magneticFieldValues)
        SensorManager.getOrientation(R1,values)
        //Log.d("calculate:",values.joinToString())
        //Log.d("calculate:",R.joinToString())
        if (values[0]==radianFloat) return
        radianFloat = values[0]
        degreeFloat  = Math.toDegrees(values[0].toDouble()).toFloat()
        var degree : Int = degreeFloat.toInt()
        if (degree==lastDegree) return
        lastDegree = degree
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
        canvas.rotate(360F-degreeFloat,(bitmap.width/2.0).toFloat(),(bitmap.height/2.0).toFloat())
        canvas.drawBitmap(bitmap,0.0.toFloat(),0.0.toFloat(),null)
        qsTile.icon = Icon.createWithBitmap(bmResult)

        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d("statue","OnStartListening")
        val intent = Intent(this, BackgroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, this, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)
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
        sensorManager.unregisterListener(this)
        mService.stopListening()
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel("channelID", "通知", NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.apply {
                enableLights(false)
                enableVibration(false)
                vibrationPattern = longArrayOf(0)
                setSound(null,null)
            }
            val notificationManager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            if (this::sensorManager.isInitialized) sensorManager.unregisterListener(this)
            unbindService(this)
            if (isBackgroundServiceConnected) mService.stopBackgroundService()
        } catch (e: Exception) {
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!=null&&event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values
            lastSensorValue[0] = true
            //Log.d("sensorUpdate:MAGNETIC",magneticFieldValues.joinToString())
        }
        if (event!=null&&event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values
            lastSensorValue[1] = true
            //Log.d("sensorUpdate:ACCELEROMETER",accelerometerValues.joinToString())
        }
        if (! (lastSensorValue[0] && lastSensorValue[1])) return
        lastSensorValue[0] = false
        lastSensorValue[1] = false
        calculateOrientation()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d("statue","OnServiceDisconnected")
        isBackgroundServiceConnected = false
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d("statue","OnServiceConnected")
        val binder = service as BackgroundService.LocalBinder
        mService = binder.getService()
        isBackgroundServiceConnected = true
    }

}
