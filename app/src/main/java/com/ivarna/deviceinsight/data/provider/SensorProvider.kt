package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.ivarna.deviceinsight.domain.model.SensorDetail
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@javax.inject.Singleton
class SensorProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var cachedSensorDetails: List<SensorDetail>? = null
    @Volatile
    private var cachedSensorList: List<String>? = null
    @Volatile
    private var cachedSensorCount: Int? = null
    @Volatile
    private var cachedFingerprint: Boolean? = null

    fun getSensorCount(): Int {
        cachedSensorCount?.let { return it }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val count = sensorManager.getSensorList(Sensor.TYPE_ALL).size
        cachedSensorCount = count
        return count
    }

    fun getSensorList(): List<String> {
        cachedSensorList?.let { return it }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val list = sensorManager.getSensorList(Sensor.TYPE_ALL).map { it.name }
        cachedSensorList = list
        return list
    }

    fun getSensorDetails(): List<SensorDetail> {
        cachedSensorDetails?.let { return it }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val details = sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            SensorDetail(
                name = sensor.name,
                type = sensor.type,
                typeName = sensorTypeName(sensor.type),
                category = sensorCategory(sensor.type),
                vendor = sensor.vendor,
                version = sensor.version,
                resolution = sensor.resolution,
                maximumRange = sensor.maximumRange,
                power = sensor.power,
                minDelay = sensor.minDelay,
                isWakeUpSensor = sensor.isWakeUpSensor
            )
        }.sortedWith(compareBy({ it.category }, { it.name }))
        cachedSensorDetails = details
        return details
    }

    fun hasFingerprintSensor(): Boolean {
        cachedFingerprint?.let { return it }
        val hasSensor = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
        cachedFingerprint = hasSensor
        return hasSensor
    }

    private fun sensorTypeName(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector"
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic Rotation Vector"
        Sensor.TYPE_GRAVITY -> "Gravity"
        Sensor.TYPE_GYROSCOPE -> "Gyroscope"
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Gyroscope (Uncalibrated)"
        Sensor.TYPE_HEART_BEAT -> "Heart Beat"
        Sensor.TYPE_HEART_RATE -> "Heart Rate"
        Sensor.TYPE_LIGHT -> "Light"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "Off-Body Detect"
        Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Magnetic Field (Uncalibrated)"
        Sensor.TYPE_MOTION_DETECT -> "Motion Detect"
        Sensor.TYPE_ORIENTATION -> "Orientation"
        Sensor.TYPE_POSE_6DOF -> "Pose 6DoF"
        Sensor.TYPE_PRESSURE -> "Pressure"
        Sensor.TYPE_PROXIMITY -> "Proximity"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
        Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
        Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion"
        Sensor.TYPE_STATIONARY_DETECT -> "Stationary Detect"
        Sensor.TYPE_STEP_COUNTER -> "Step Counter"
        Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
        22 -> "Tilt Detector"
        23 -> "Wake Gesture"
        else -> "Other (Type $type)"
    }

    private fun sensorCategory(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
        Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_GAME_ROTATION_VECTOR,
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
        Sensor.TYPE_STEP_COUNTER,
        Sensor.TYPE_STEP_DETECTOR,
        Sensor.TYPE_SIGNIFICANT_MOTION,
        Sensor.TYPE_STATIONARY_DETECT,
        Sensor.TYPE_MOTION_DETECT,
        22,
        Sensor.TYPE_POSE_6DOF,
        Sensor.TYPE_ORIENTATION -> "Motion"

        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
        Sensor.TYPE_PROXIMITY -> "Position"

        Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_RELATIVE_HUMIDITY,
        Sensor.TYPE_PRESSURE,
        Sensor.TYPE_LIGHT -> "Environment"

        Sensor.TYPE_HEART_BEAT,
        Sensor.TYPE_HEART_RATE,
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT,
        23 -> "Biometric"

        else -> "Other"
    }
}
