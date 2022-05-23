/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.shared

/**
 * Custom [InputFormCell] used for an input form. Contains a [label], [inputFieldText] and
 * [inputFieldHint].
 *
 * @property label Title label associated with the input in the cell.
 * @property inputFieldText Text input field for the cell.
 * @property inputFieldHint Hint within the input field for the cell.
 */
data class InputFormCell(
    val label: String,
    val inputFieldText: String,
    val inputFieldHint: String
)
