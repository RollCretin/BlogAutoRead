package com.cretin.cretin.blogautoread

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.blankj.utilcode.util.BarUtils
import com.just.library.AgentWeb
import com.orhanobut.hawk.Hawk
import java.util.*

class ShowDetailActivity : AppCompatActivity() {
    private var mAgentWeb: AgentWeb? = null
    private var mFlag = false
    private var timer:Timer? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_detail)

        supportActionBar?.hide()

        var url = intent.getStringExtra("url")
        mFlag = intent.getBooleanExtra("flag", false)
        mAgentWeb = AgentWeb.with(this)//传入Activity
                .setAgentWebParent(findViewById<LinearLayout>(R.id.container), LinearLayout.LayoutParams(-1, -1))//传入AgentWeb 的父控件 ，如果父控件为 RelativeLayout ， 那么第二参数需要传入 RelativeLayout.LayoutParams ,第一个参数和第二个参数应该对应。
                .useDefaultIndicator()// 使用默认进度条
                .defaultProgressBarColor() // 使用默认进度条颜色
                .setReceivedTitleCallback({ view, title ->
                    findViewById<TextView>(R.id.title).text = title
                })
                .createAgentWeb()//
                .ready()
                .go(url)

        findViewById<ImageView>(R.id.close).setOnClickListener {
            if (!mAgentWeb?.back()!!) {
                finish()
            }
        }

        if (!mFlag) {
            //启动一个定时器 3秒后关闭
            var time: Long? = Hawk.get<Long>("d_time")
            if (time == null || time == 0L) {
                time = 3
            }
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    val intent = Intent()
                    setResult(100, intent)
                    finish()
                }
            }, time * 1000)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (mAgentWeb?.handleKeyEvent(keyCode, event)!!) {
            true
        } else super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        mAgentWeb?.webLifeCycle?.onPause()
        super.onPause()
    }

    override fun onResume() {
        mAgentWeb?.webLifeCycle?.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
