package com.ai.assistance.operit.util.stream

import com.ai.assistance.operit.util.AppLogger
/** 将字符串转换为字符流 用于在MarkdownTextComposable中将普通字符串转换为所需的字符流 */
fun String.stream(): Stream<Char> {
    val self = this
    return stream {
        for (c in self) {
            emit(c)
        }
    }
}
