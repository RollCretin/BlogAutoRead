package com.cretin.cretin.blogautoread.model

import android.app.Activity

/**
 * @date: on 2018/9/4
 * @author: cretin
 * @email: mxnzp_life@163.com
 * @desc: 添加描述
 */

data class UrlData(val url: String, val title: String, var time: String?, var state: Int) {
    companion object {
        val TYPE_OK: Int = 0
        val TYPE_HIDE: Int = 1
    }

    override fun equals(other: Any?): Boolean {
        val o = other as UrlData
        return o.url == (url)
    }
}