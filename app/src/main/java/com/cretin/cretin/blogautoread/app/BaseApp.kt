package com.cretin.cretin.blogautoread.app

import android.app.Application
import com.orhanobut.hawk.NoEncryption
import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.LogInterceptor


/**
 * @date: on 2018/9/4
 * @author: cretin
 * @email: mxnzp_life@163.com
 * @desc: 添加描述
 */
class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //初始化Hawk
        Hawk.init(this)
                .build()
    }
}