/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun viewEntitlements(func: ViewEntitlementsRobot.() -> Unit) = ViewEntitlementsRobot().apply { func() }

/**
 * Testing robot that manages the view entitlements screen.
 *
 * @since 2020-11-10
 */
class ViewEntitlementsRobot : BaseRobot() {

    private val entitlementsRecyclerView = withId(R.id.entitlementsRecyclerView)
    private val loadingDialog = withId(R.id.progressBar)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(entitlementsRecyclerView, 5_000L)
    }
}
