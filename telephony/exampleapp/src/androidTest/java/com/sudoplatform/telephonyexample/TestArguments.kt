/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import androidx.test.platform.app.InstrumentationRegistry

/**
 * The value of an optional instrumentation argument, or [default] when the runner did not supply
 * one. Lets a test target an instance whose provider inventory differs, for example
 * `-Pandroid.testInstrumentationRunnerArguments.AREA_CODE=385`.
 *
 * @param argumentName [String] Name of the instrumentation argument.
 * @param default [String] Value to use when the argument is absent.
 * @return The argument value or [default].
 */
fun optionalArgument(
    argumentName: String,
    default: String,
): String =
    InstrumentationRegistry
        .getArguments()
        .getString(argumentName)
        ?.trim()
        ?.ifEmpty { null } ?: default
