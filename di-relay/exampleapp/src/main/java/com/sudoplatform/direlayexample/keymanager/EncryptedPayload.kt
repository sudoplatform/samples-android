/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.keymanager

import com.google.gson.annotations.SerializedName

/**
 * A data object of a packed encrypted payload this sample app structures its sent/receiving messages in.
 * Based off a super simplified version of DIDComm encrypted message structures:
 *      https://www.w3.org/TR/did-core/#encrypting
 *
 * @property cipherText url safe base64 representation of the AES encrypted payload
 * @property encryptedKey url safe base64 representation of the RSA encrypted AES key used to encrypt the ciphertext
 */
data class EncryptedPayload(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("encryptedKey") val encryptedKey: String
)
