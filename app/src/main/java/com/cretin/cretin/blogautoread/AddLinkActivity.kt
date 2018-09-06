package com.cretin.cretin.blogautoread

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.Color
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cretin.cretin.blogautoread.model.UrlData
import com.orhanobut.hawk.Hawk
import org.jsoup.Jsoup
import java.net.URL
import com.blankj.utilcode.util.FragmentUtils.setBackgroundColor
import android.support.v4.view.ViewCompat.setAlpha
import cn.addapp.pickers.widget.WheelView
import cn.addapp.pickers.common.LineConfig
import java.util.*
import cn.addapp.pickers.listeners.OnItemPickListener
import cn.addapp.pickers.listeners.OnSingleWheelListener
import cn.addapp.pickers.picker.SinglePicker


class AddLinkActivity : AppCompatActivity() {
    var list: MutableList<UrlData>? = null
    var adapter: RecyclerAdapter? = null
    var edTitle: EditText? = null
    var edLink: EditText? = null
    var recyclerView: RecyclerView? = null
    var dialog: ProgressDialog? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_link)

        recyclerView = findViewById(R.id.recyclerview)

        initData()

        //解析链接
        findViewById<TextView>(R.id.tv_check).setOnClickListener {
            val url: String = findViewById<EditText>(R.id.ed_link).text.toString().trim()
            if (TextUtils.isEmpty(url)) {
                ToastUtils.showLong("请先输入链接")
            } else {
                checkLink(url)
            }
        }

        //解析链接
        findViewById<TextView>(R.id.tv_add).setOnClickListener {
            val url: String = findViewById<EditText>(R.id.ed_link).text.toString().trim()
            val title: String = findViewById<EditText>(R.id.ed_title).text.toString().trim()
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(title)) {
                ToastUtils.showLong("请先输入完整内容")
            } else {
                addUrl(url, title)
            }
        }

        //重置
        findViewById<TextView>(R.id.tv_reset).setOnClickListener {
            edTitle?.setText("")
            edLink?.setText("")
        }

        edTitle = findViewById(R.id.ed_title)
        edLink = findViewById(R.id.ed_link)
    }

    private fun addUrl(url: String, title: String) {
        val data = UrlData(url, title, "", UrlData.TYPE_OK)
        if (list?.contains(data)!!) {
            ToastUtils.showLong("请不要重复添加数据")
            return
        }
        list?.add(0, data)
        adapter?.notifyItemInserted(0)
        //存储起来
        Hawk.put("list", list)
        ToastUtils.showLong("添加成功")
        edTitle?.setText("")
        edLink?.setText("")
    }

    private fun initData() {
        list = mutableListOf()
        val temp = Hawk.get<MutableList<UrlData>>("list")
        if (temp != null) {
            list?.addAll(temp)
        }
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter = RecyclerAdapter(R.layout.item_recycler, list)
        recyclerView?.adapter = adapter

        adapter?.setOnItemClickListener { adapter, view, position ->
            showConfirmDialog(position)
        }
    }

    /**
     * 显示确认对话框
     */
    private fun showConfirmDialog(position: Int) {
        var data: UrlData? = list?.get(position)
        val datas = arrayListOf<String>()
        datas.add("设置为正常")
        datas.add("设置为隐藏")
        datas.add("删除此链接")
        val picker = SinglePicker(this, datas)
        picker.setCanLoop(false)//不禁用循环
        picker.setLineVisible(true)
        picker.setSelectedIndex(data?.state!!)
        picker.setOnItemPickListener(OnItemPickListener<String> { index, item ->
            if (index > 1) {
                list?.removeAt(position)
                adapter?.notifyItemRemoved(position)
            } else {
                data?.state = index
                list?.set(position, data)
                adapter?.notifyItemChanged(position)
            }
            Hawk.put("list", list)
        })
        picker.show()
    }

    private fun checkLink(url: String) {
        if (dialog == null) {
            dialog = ProgressDialog.show(this, "", "正在解析，请稍后...")
        }
        dialog?.show()
        Task().execute(url)
    }

    inner class Task : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val document = Jsoup.parse(URL(params[0]), 5000)
            return document.head().getElementsByTag("title").text()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            dialog?.dismiss()
            edTitle?.setText(result)
        }
    }

    class RecyclerAdapter(layout: Int, data: List<UrlData>?) : BaseQuickAdapter<UrlData, BaseViewHolder>(layout, data) {

        override fun convert(helper: BaseViewHolder?, item: UrlData?) {
            helper?.setVisible(R.id.tv_state, true)
            helper?.setText(R.id.tv_title, "网页标题：" + item?.title)
            helper?.setText(R.id.tv_state, "状态：" + (if (item?.state == UrlData.TYPE_OK) "正常" else "隐藏"))
            helper?.setText(R.id.tv_link, "网页链接：" + item?.url)
            helper?.setVisible(R.id.tv_time, false)
        }
    }
}
