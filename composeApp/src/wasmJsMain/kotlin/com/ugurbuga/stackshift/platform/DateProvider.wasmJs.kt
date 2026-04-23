package com.ugurbuga.stackshift.platform

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("Date")
private external class JsDate : JsAny {
    constructor()
    fun getFullYear(): Int
    fun getMonth(): Int
    fun getDate(): Int
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun getCurrentDate(): CurrentDate {
    val date = JsDate()
    return CurrentDate(
        year = date.getFullYear(),
        month = date.getMonth() + 1,
        day = date.getDate()
    )
}
