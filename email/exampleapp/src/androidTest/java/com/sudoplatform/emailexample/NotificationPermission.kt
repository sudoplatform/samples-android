/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/**
 * Grants the notification permission [MainActivity] asks for at launch before applying
 * [composeRule], so the permission dialog doesn't cover the app.
 */
fun withNotificationPermission(composeRule: TestRule): RuleChain {
    val permission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            RuleChain.emptyRuleChain()
        }
    return RuleChain.outerRule(permission).around(composeRule)
}
