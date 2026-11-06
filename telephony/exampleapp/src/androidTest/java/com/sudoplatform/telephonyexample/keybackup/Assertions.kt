/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.keybackup

/**
 * Asserts that [block] throws a [T], and returns it. The JUnit on the test classpath predates
 * `Assert.assertThrows`. Being inline, [block] can call suspend functions from a coroutine.
 */
inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) {
            return e
        }
        throw AssertionError("Expected ${T::class.java.simpleName} but ${e::class.java.simpleName} was thrown", e)
    }
    throw AssertionError("Expected ${T::class.java.simpleName} but nothing was thrown")
}
