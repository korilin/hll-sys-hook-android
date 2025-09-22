package cn.huolala.syshooklib

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.huolala.threadshield.SuspendThreadSafeHelper
import com.huolala.syshooklib.databinding.ActivityMainBinding
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SuspendThreadSafeHelper.getInstance()
            .suspendThreadSafe(object : SuspendThreadSafeHelper.SuspendThreadCallback {
                override fun suspendThreadTimeout(waitTime: Double) {
                    // Handle timeout
                    Log.i("MainActivity", "suspendThreadTimeout: $waitTime")
                }

                override fun onError(errorMsg: String) {
                    // Handle error
                    Log.e("MainActivity", "onError: $errorMsg")
                }
            })

        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Start Mock Thread Suspend Timeout", Toast.LENGTH_SHORT).show()
            testThreadHook()
        }, 5000)
    }

    var myThread: Thread? = null

    private fun testThreadHook() {
        thread {
            for (i in 0..1000) {
                thread {
                    while (true) {

                    }
                }
            }
        }

        thread {
            myThread = thread(name = "EdisonLi-init-name") {
                // callThreadSuspendTimeout(myThread!!)
                while (true) {
                    // Log.d("EdisonLi",  this@SecondActivity.myThread?.name.toString())
                }
            }
            while (true) {
                //Log.d("EdisonLi", myThread?.name.toString())
                myThread?.name = "Thread-${Random.nextLong(1, 1000)}"
            }
        }

        // 测试 SuspendThreadByPeer 场景
        thread {
            while (true) {
                Thread.sleep(5L)
                Thread.getAllStackTraces()
            }
        }
        
        // 测试高频线程创建/销毁，可能触发 SuspendAll 
        thread {
            while (true) {
                Thread.sleep(5L)
                thread {
                    Thread.sleep(3L)
                }
            }
        }
        
        // Android 15 专门测试：高并发场景下的 GC 和线程操作
        testAndroid15SuspendAllScenario()
    }
    
    private fun testAndroid15SuspendAllScenario() {
        Log.i("MainActivity", "Starting Android 15 SuspendAll scenario test")
        
        // 创建大量短生命周期线程，增加 SuspendAll 被调用的概率
        thread {
            repeat(100) { index ->
                thread(name = "TestThread-$index") {
                    val data = ByteArray(1024 * 1024) // 分配内存触发 GC
                    Thread.sleep(10)
                    // 在线程结束时可能触发 SuspendAll
                }
                if (index % 10 == 0) {
                    Thread.sleep(1) // 给系统一些喘息时间
                }
            }
        }
        
        // 高频 GC 触发
        thread {
            repeat(50) {
                System.gc()
                Thread.sleep(50)
            }
        }
    }

    companion object {
        // Used to load the 'syshooklib' library on application startup.
        init {

        }
    }
}