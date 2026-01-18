package com.awab.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var requestButton: Button
    private lateinit var accessibilityButton: Button

    private val permissions = mutableListOf<String>().apply {
        // أذونات الموقع
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // أذونات الكاميرا والميكروفون
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)

        // أذونات التخزين
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // أذونات جهات الاتصال
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.WRITE_CONTACTS)

        // أذونات التقويم
        add(Manifest.permission.READ_CALENDAR)
        add(Manifest.permission.WRITE_CALENDAR)

        // أذونات الهاتف
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.WRITE_CALL_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.READ_PHONE_NUMBERS)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }

        // أذونات الرسائل
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)

        // أذونات المستشعرات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            add(Manifest.permission.BODY_SENSORS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // أذونات الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // أذونات Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        // أذونات الشبكة القريبة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        // أذونات UWB
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                add(Manifest.permission.UWB_RANGING)
            } catch (e: Exception) {
                // قد لا تكون متاحة على جميع الأجهزة
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionStatus()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    private val notificationPolicyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        requestButton = findViewById(R.id.requestButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)

        createNotificationChannel()

        requestButton.setOnClickListener {
            requestAllPermissions()
        }

        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }

        updatePermissionStatus()
    }

    private fun requestAllPermissions() {
        // طلب الأذونات العادية أولاً
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // طلب الأذونات الخاصة
        requestSpecialPermissions()
    }

    private fun requestSpecialPermissions() {
        // إذن الظهور فوق التطبيقات الأخرى
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }

        // إذن تحسين البطارية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                batteryOptimizationLauncher.launch(intent)
            }
        }

        // إذن إدارة التخزين
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            }
        }

        // إذن سياسة الإشعارات (عدم الإزعاج)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                notificationPolicyLauncher.launch(intent)
            }
        }
    }

    private fun openAccessibilitySettings() {
        AlertDialog.Builder(this)
            .setTitle("خدمة إمكانية الوصول")
            .setMessage("لتفعيل جميع ميزات خدمة إمكانية الوصول، يرجى:\n\n" +
                    "1. الانتقال إلى الإعدادات\n" +
                    "2. اختيار 'MyAccessibilityService'\n" +
                    "3. تفعيل الخدمة\n" +
                    "4. السماح بجميع الأذونات المطلوبة")
            .setPositiveButton("فتح الإعدادات") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun updatePermissionStatus() {
        val status = StringBuilder()
        status.append("حالة الأذونات:\n\n")

        // الأذونات العادية
        var grantedCount = 0
        var deniedCount = 0

        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedCount++
            } else {
                deniedCount++
            }
        }

        status.append("الأذونات العادية:\n")
        status.append("✓ الممنوحة: $grantedCount\n")
        status.append("✗ المرفوضة: $deniedCount\n\n")

        // الأذونات الخاصة
        status.append("الأذونات الخاصة:\n")

        // إذن الظهور فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val overlayGranted = Settings.canDrawOverlays(this)
            status.append(if (overlayGranted) "✓" else "✗")
            status.append(" الظهور فوق التطبيقات\n")
        }

        // إذن تحسين البطارية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val batteryGranted = powerManager.isIgnoringBatteryOptimizations(packageName)
            status.append(if (batteryGranted) "✓" else "✗")
            status.append(" تجاهل تحسين البطارية\n")
        }

        // إذن إدارة التخزين
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageGranted = android.os.Environment.isExternalStorageManager()
            status.append(if (storageGranted) "✓" else "✗")
            status.append(" إدارة جميع الملفات\n")
        }

        // إذن سياسة الإشعارات
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationGranted = notificationManager.isNotificationPolicyAccessGranted
            status.append(if (notificationGranted) "✓" else "✗")
            status.append(" الوصول لسياسة الإشعارات\n")
        }

        // خدمة إمكانية الوصول
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        status.append(if (accessibilityEnabled) "✓" else "✗")
        status.append(" خدمة إمكانية الوصول\n")

        statusText.text = status.toString()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${MyAccessibilityService::class.java.name}"
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(service) == true
        }
        
        return false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "قناة افتراضية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "قناة الإشعارات الافتراضية للتطبيق"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
