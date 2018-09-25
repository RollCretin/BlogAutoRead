package com.cretin.cretin.blogautoread

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cretin.cretin.blogautoread.model.UrlData
import com.orhanobut.hawk.Hawk
import android.support.v7.widget.DividerItemDecoration
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.EditText
import cn.addapp.pickers.listeners.OnItemPickListener
import cn.addapp.pickers.picker.SinglePicker
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import java.util.*


class MainActivity : AppCompatActivity() {
    var list: MutableList<UrlData>? = null
    var listDoing: MutableList<UrlData>? = null
    var adapter: RecyclerAdapter? = null
    var recyclerView: RecyclerView? = null
    var taskTime: Int = 1
    var edTime: EditText? = null
    var btnStart: TextView? = null
    var btnStop: TextView? = null
    var taskState: Boolean = false
    var mTime: Int = 0
    var dTime: Int = 0
    var tvTips: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerview)
        btnStart = findViewById(R.id.tv_start)
        btnStop = findViewById(R.id.tv_finish)
        tvTips = findViewById(R.id.tv_tips)

        initData()

        findViewById<TextView>(R.id.tv_setting).setOnClickListener {
            var intent = Intent(this, AddLinkActivity::class.java)
            startActivity(intent)
        }

        //开始任务
        findViewById<TextView>(R.id.tv_start).setOnClickListener {
            startTask()
        }

        //设置执行次数
        findViewById<TextView>(R.id.tv_zx).setOnClickListener {
            var taskTimeStr = findViewById<TextView>(R.id.tv_zx)?.text?.toString()
            taskTime = taskTimeStr?.substring(5, taskTimeStr.length - 1)?.toInt()!!
            showSelect(0, taskTime - 1)
        }

        //设置M执行时间
        findViewById<TextView>(R.id.tv_mt).setOnClickListener {
            var mTimeStr = findViewById<TextView>(R.id.tv_mt)?.text?.toString()
            mTime = mTimeStr?.substring(4, mTimeStr.length - 1)?.toInt()!!
            showSelect(1, mTime - 1)
        }

        //设置D执行时间
        findViewById<TextView>(R.id.tv_dt).setOnClickListener {
            var dTimeStr = findViewById<TextView>(R.id.tv_dt)?.text?.toString()
            dTime = dTimeStr?.substring(4, dTimeStr.length - 1)?.toInt()!!
            showSelect(2, dTime - 1)
        }

        //清除日志
        findViewById<TextView>(R.id.tv_clear).setOnClickListener {
            list?.clear()
            adapter?.notifyDataSetChanged()
        }

        //开始任务
        findViewById<TextView>(R.id.tv_finish).setOnClickListener {
            //终止任务
            taskState = true
            timer?.cancel()
            timer = null
            btnStart?.isEnabled = true
            btnStop?.isEnabled = false
            ToastUtils.showLong("任务被终止")
        }
    }

    /**
     * 开始任务
     */
    private fun startTask() {
        //从缓存中拿数据
        var listDoings = Hawk.get<MutableList<UrlData>>("list")
        if (listDoings == null || listDoings?.isEmpty()!!) {
            //没有数据
            ToastUtils.showLong("本地没有可以处理的链接")
            return
        }
        listDoing = mutableListOf()
        listDoings.forEach {
            var d: UrlData = it
            if (d.state == UrlData.TYPE_OK) {
                listDoing?.add(d)
            }
        }
        if (listDoing?.isEmpty()!!) {
            ToastUtils.showLong("本地没有可以处理的链接")
            return
        }
        currPosition = 0
        //开始对listDoing循环进行操作
        var taskTimeStr = findViewById<TextView>(R.id.tv_zx)?.text?.toString()
        taskTime = taskTimeStr?.substring(5, taskTimeStr.length - 1)?.toInt()!!
        var mTimeStr = findViewById<TextView>(R.id.tv_mt)?.text?.toString()
        mTime = mTimeStr?.substring(4, mTimeStr.length - 1)?.toInt()!!
        var dTimeStr = findViewById<TextView>(R.id.tv_dt)?.text?.toString()
        dTime = dTimeStr?.substring(4, dTimeStr.length - 1)?.toInt()!!
        list?.clear()
        adapter?.notifyDataSetChanged()
        ToastUtils.showLong("任务已开始")
        btnStart?.isEnabled = false
        btnStop?.isEnabled = true
        taskState = false

        doit(listDoing?.get(currPosition % listDoing?.size!!)?.url!!)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun doit(url: String) {
        tvTips?.text = "执行日志输出：(总次数:" + taskTime * listDoing?.size!! + " 已完成次数:" + (currPosition + 1) + "次)"
        var time: Long? = Hawk.get<Long>("m_time")
        if (time == null || time == 0L) {
            time = 1
        }
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                var intent = Intent(this@MainActivity, ShowDetailActivity::class.java)
                intent.putExtra("url", url)
                startActivityForResult(intent, 100)
                timer?.cancel()
                timer = null
            }
        }, time * 1000L)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == 100) {
            if (currPosition % (listDoing?.size!!) == listDoing?.size!! - 1) {
                //说明一个轮回结束了 乱序一下当前列表
                Collections.shuffle(listDoing)
            }
            var d1 = listDoing!![currPosition % listDoing?.size!!]
            var d = d1.copy()
            d.time = TimeUtils.getNowString().toString()
            //任务被终止
            if (taskState) {
                list?.add(0, d)
                adapter?.notifyDataSetChanged()
                btnStart?.isEnabled = true
                btnStop?.isEnabled = false
                ToastUtils.showLong("任务已提前被终止")
                return
            }
            currPosition++
            if (currPosition >= taskTime * listDoing?.size!!) {
                list?.add(0, d)
                adapter?.notifyDataSetChanged()
                btnStart?.isEnabled = true
                btnStop?.isEnabled = false
                ToastUtils.showLong("任务已完成")
                return
            }
            doit(listDoing?.get(currPosition % listDoing?.size!!)?.url!!)
            list?.add(0, d)
            adapter?.notifyItemRemoved(0)
        }
    }

    var currPosition: Int = 0
    var timer: Timer? = null

    @SuppressLint("WrongViewCast")
    private fun initData() {
        //获取数据
        list = mutableListOf<UrlData>()
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter = RecyclerAdapter(R.layout.item_recycler, list)
        recyclerView?.adapter = adapter
        adapter?.setOnItemClickListener { adapter, view, position ->
            var url: String = list?.get(position)?.url!!
            var intent = Intent(this@MainActivity, ShowDetailActivity::class.java)
            intent.putExtra("url", url)
            intent.putExtra("flag", true)
            startActivityForResult(intent, 100)
        }

        //设置初始值
        var time1: Long? = Hawk.get<Long>("zx_time")
        if (time1 == null || time1 == 0L) {
            time1 = 1
        }
        var time2: Long? = Hawk.get<Long>("m_time")
        if (time2 == null || time2 == 0L) {
            time2 = 1
        }
        var time3: Long? = Hawk.get<Long>("d_time")
        if (time3 == null || time3 == 0L) {
            time3 = 1
        }
        findViewById<TextView>(R.id.tv_zx)?.text = "执行次数：" + time1 + "次"
        findViewById<TextView>(R.id.tv_mt).text = ("M时间：" + time2 + "秒")
        findViewById<TextView>(R.id.tv_dt).text = ("D时间：" + time3 + "秒")
    }

    class RecyclerAdapter(layout: Int, data: List<UrlData>?) : BaseQuickAdapter<UrlData, BaseViewHolder>(layout, data) {

        override fun convert(helper: BaseViewHolder?, item: UrlData?) {
            helper?.setText(R.id.tv_title, "网页标题：" + item?.title)
            helper?.setText(R.id.tv_link, "网页链接：" + item?.url)
            helper?.setText(R.id.tv_time, item?.time)
        }
    }

    @SuppressLint("WrongViewCast")
    fun showSelect(type: Int, position: Int) {
        val datas = arrayListOf<String>()
        for (i in 1..20) {
            if (type == 0) {
                datas.add("" + i + "次")
            } else if (type == 1 || type == 2) {
                datas.add("" + i + "秒")
            }
        }
        val picker = SinglePicker(this, datas)
        picker.setCanLoop(false)//不禁用循环
        picker.setLineVisible(true)
        picker.setSelectedIndex(position)
        picker.setOnItemPickListener(OnItemPickListener<String> { index, item ->
            if (type == 0) {
                Hawk.put("zx_time", (index + 1).toLong())
                findViewById<TextView>(R.id.tv_zx).text = ("执行次数：" + (index + 1) + "次")
            } else if (type == 1) {
                Hawk.put("m_time", (index + 1).toLong())
                findViewById<TextView>(R.id.tv_mt).text = ("M时间：" + (index + 1) + "秒")
            } else if (type == 2) {
                Hawk.put("d_time", (index + 1).toLong())
                findViewById<TextView>(R.id.tv_dt).text = ("D时间：" + (index + 1) + "秒")
            }
        })
        picker.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }
}