package com.charles.skypulse.app.ui.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks the [Context] wrapper chain to find the host [Activity] (needed to show full-screen ads). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
