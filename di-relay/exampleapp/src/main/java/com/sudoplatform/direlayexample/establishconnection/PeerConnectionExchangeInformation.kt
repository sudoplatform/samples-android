/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * A data object containing connection information for a postbox that is required to establish an
 *  encrypted connection. This data object is based off a highly simplified version of a DID Document
 *  or invitation message created during a DID Exchange:
 *      https://github.com/hyperledger/aries-rfcs/blob/master/features/0160-connection-protocol/README.md
 *
 * @property connectionId the unique identifier of the postbox this instance refers to.
 * @property base64PublicKey the url safe base64 representation of the RSA public key associated
 *  to the postbox this instance refers to.
 */
@Keep
data class PeerConnectionExchangeInformation(
    @SerializedName("connectionId") val connectionId: String,
    @SerializedName("publicKey") val base64PublicKey: String
)
