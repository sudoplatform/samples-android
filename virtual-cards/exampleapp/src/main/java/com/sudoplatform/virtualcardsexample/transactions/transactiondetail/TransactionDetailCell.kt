/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

/**
 * Custom [TransactionDetailCell] shown on a [TransactionDetailFragment]. Contains a [titleLabel],
 * [subtitleLabel] and [valueLabel].
 *
 * @property titleLabel Title label associated with the cell.
 * @property subtitleLabel Subtitle label associated with the cell.
 * @property valueLabel Value label associated with the cell.
 */
class TransactionDetailCell(
    val titleLabel: String,
    val subtitleLabel: String,
    val valueLabel: String
)
