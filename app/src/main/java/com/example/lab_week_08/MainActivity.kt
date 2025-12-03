package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // 1. Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // 2. Worker constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // 3. Build workers
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(constraints)
            .setInputData(input("001"))
            .build()

        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(constraints)
            .setInputData(input("001"))
            .build()

        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(constraints)
            .setInputData(input("001"))
            .build()

        // 4. Chain first → second
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // 5. First observer
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) {
                if (it?.state?.isFinished == true) {
                    show("First worker done")
                }
            }

        // 6. Second observer → start NotificationService
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) {
                if (it?.state?.isFinished == true) {
                    show("Second worker done")
                    launchNotificationService()
                }
            }

        // 7. After NotificationService is done → run ThirdWorker
        NotificationService.trackingCompletion.observe(this) {
            if (it != null) {
                show("NotificationService done — starting ThirdWorker")
                workManager.enqueue(thirdRequest)
            }
        }

        // 8. Third observer → start SecondNotificationService
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) {
                if (it?.state?.isFinished == true) {
                    show("Third worker done")
                    launchSecondNotificationService()
                }
            }

        // 9. After SecondNotificationService done
        SecondNotificationService.completionLiveData.observe(this) {
            if (it == true) {
                show("SecondNotificationService done!")
            }
        }
    }

    private fun input(value: String) =
        Data.Builder().putString("Id", value).build()

    private fun show(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun launchNotificationService() {
        val intent = Intent(this, NotificationService::class.java)
        intent.putExtra("Id", "001")
        ContextCompat.startForegroundService(this, intent)
    }

    private fun launchSecondNotificationService() {
        val intent = Intent(this, SecondNotificationService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
