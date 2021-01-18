/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

/**
 * Extensions of the Android Fragment.
 */

/**
 * Show an [AlertDialog] with a positive and negative button.
 */
fun Fragment.showAlertDialog(
    @StringRes titleResId: Int,
    @StringRes messageResId: Int,
    @StringRes positiveButtonResId: Int,
    onPositive: (() -> Unit) ? = null,
    @StringRes negativeButtonResId: Int,
    onNegative: (() -> Unit) ? = null
) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleResId)
        .setMessage(messageResId)
        .setPositiveButton(positiveButtonResId) { _, _ -> onPositive?.invoke() }
        .setNegativeButton(negativeButtonResId) { _, _ -> onNegative?.invoke() }
        .show()
}

/**
 * Show an [AlertDialog] with a positive and negative button.
 */
fun Fragment.showAlertDialog(
    @StringRes titleResId: Int,
    message: String,
    @StringRes positiveButtonResId: Int,
    onPositive: (() -> Unit) ? = null,
    @StringRes negativeButtonResId: Int,
    onNegative: (() -> Unit) ? = null
) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleResId)
        .setMessage(message)
        .setPositiveButton(positiveButtonResId) { _, _ -> onPositive?.invoke() }
        .setNegativeButton(negativeButtonResId) { _, _ -> onNegative?.invoke() }
        .show()
}

/**
 * Show an [AlertDialog] with only a positive button.
 */
fun Fragment.showAlertDialog(
    @StringRes titleResId: Int,
    @StringRes messageResId: Int,
    @StringRes positiveButtonResId: Int,
    onPositive: (() -> Unit) ? = null
) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleResId)
        .setMessage(messageResId)
        .setPositiveButton(positiveButtonResId) { _, _ -> onPositive?.invoke() }
        .show()
}

/**
 * Show an [AlertDialog] with only a negative button.
 */
fun Fragment.showAlertDialog(
    @StringRes titleResId: Int,
    message: String,
    @StringRes negativeButtonResId: Int,
    onNegative: (() -> Unit) ? = null
) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleResId)
        .setMessage(message)
        .setNegativeButton(negativeButtonResId) { _, _ -> onNegative?.invoke() }
        .show()
}

/**
 * Show an [AlertDialog] with only a title and positive button.
 */
fun Fragment.showAlertDialog(
    @StringRes titleResId: Int,
    @StringRes positiveButtonResId: Int,
    onPositive: (() -> Unit) ? = null
) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleResId)
        .setPositiveButton(positiveButtonResId) { _, _ -> onPositive?.invoke() }
        .show()
}

/**
 * Create an [AlertDialog] with a loading spinner and title.
 */
fun Fragment.createLoadingAlertDialog(@StringRes titleResId: Int): AlertDialog {
    return AlertDialog.Builder(requireContext())
        .setTitle(titleResId)
        .setView(layoutInflater.inflate(R.layout.layout_loading_dialog, null))
        .setCancelable(false)
        .create()
}
