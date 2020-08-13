/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample

import androidx.fragment.app.Fragment

/**
 * Thrown when a [Fragment] was expecting an argument to be passed in the arguments
 * to the [Fragment] and the argument was missing.
 */
class MissingFragmentArgumentException(message: String) : RuntimeException(message)
