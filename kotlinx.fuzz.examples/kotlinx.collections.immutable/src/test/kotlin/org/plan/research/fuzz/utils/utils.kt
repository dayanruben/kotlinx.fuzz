/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider

fun FuzzedDataProvider.forceConsumeInts(size: Int): List<Int> {
    return List(size) { this.consumeInt() }
//    val ints = arrayListOf<Int>()
//    while (ints.size < size) {
//        System.err.println("force consume")
//        val next = consumeInts(size - ints.size)
//        ints.addAll(next.toTypedArray())
//    }
//    return ints
}


inline fun <T> tryOrNull(block: () -> T): T? = try {
    block()
} catch (e: Throwable) {
    null
}

inline fun <T> tryOr(default: T, block: () -> T): T = tryOrNull(block) ?: default

var validateInvariants: Boolean = false

var validateReverse: Boolean = false

var validateReplay: Boolean = false

var validateBuilder: Boolean = false

val initSize: Int =
    (System.getenv("INIT_SIZE")?.toIntOrNull() ?: 100).also { println("INIT_SIZE: $it") }

const val MAX_BUFFER_SIZE = 32
