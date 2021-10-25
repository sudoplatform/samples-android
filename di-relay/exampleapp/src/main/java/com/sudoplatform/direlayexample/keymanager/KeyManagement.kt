/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.keymanager

import android.content.Context
import android.util.Base64
import com.google.gson.GsonBuilder
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudokeymanager.KeyManagerInterface
import com.sudoplatform.sudokeymanager.StoreException
import java.util.UUID

/**
 * Class for managing key pairs and cryptography relating to postbox connections with peers.
 * Interfaces with [KeyManagerInterface].
 *
 * @param context the application context
 */
class KeyManagement(context: Context) {

    /** instance of SudoKeyManager */
    private val keyManager: KeyManagerInterface =
        KeyManagerFactory(context).createAndroidKeyManager()

    /** GSON instance without html escaping characters */
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    /**
     * Attempts to create and store a KeyPair for the given [connectionId]
     * Replaces the keypair for the connection if it already exists
     *
     * @param connectionId Postbox identifier
     */
    fun createKeyPairForConnection(connectionId: String) {
        try {
            removeKeysForConnection(connectionId)
            keyManager.generateKeyPair(connectionId)
        } catch (e: StoreException) {
            // key already exists
        }
    }

    /**
     * Get the public key that is stored for a given [connectionId]
     *
     * @param connectionId Postbox identifier
     * @return The public key stored for the connection, represented in url safe base64
     */
    fun getPublicKeyForConnection(connectionId: String): String {
        val rawPubKey = keyManager.getPublicKeyData(connectionId)
        return Base64.encodeToString(rawPubKey, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Attempts to remove all stored keys (public/private) mapped to a [connectionId]
     *
     * @param connectionId connectionId which public/private keys are mapped to
     */
    fun removeKeysForConnection(connectionId: String) {
        try {
            keyManager.deleteKeyPair(connectionId)
        } catch (e: Exception) {
            // key not stored
        }
    }

    /**
     * Given an packed [message] message, with JSON format of [EncryptedPayload],
     * from a peer who is connected on a given [connectionId], unpack and decrypt the [message]
     * and return the string contents
     *
     * @param connectionId Postbox identifier
     * @param message the packed message assumed to have JSON format of serialized [EncryptedPayload]
     * @return the unpacked decrypted message string
     */
    fun unpackEncryptedMessageForConnection(connectionId: String, message: String): String {
        val encryptedPayload = gson.fromJson(message, EncryptedPayload::class.java)

        val rawEncryptedKey =
            Base64.decode(encryptedPayload.encryptedKey, Base64.URL_SAFE or Base64.NO_WRAP)

        val symmetricKey = keyManager.decryptWithPrivateKey(
            connectionId,
            rawEncryptedKey
        )

        val rawEncryptedCiphertext =
            Base64.decode(encryptedPayload.cipherText, Base64.URL_SAFE or Base64.NO_WRAP)

        val rawDecryptedCiphertext =
            keyManager.decryptWithSymmetricKey(symmetricKey, rawEncryptedCiphertext)

        return rawDecryptedCiphertext.toString(Charsets.UTF_8)
    }

    /**
     * Stores the [base64PublicKey] of the peer with the identifier of [peerConnectionId]
     *
     * @param peerConnectionId Postbox identifier of the peer
     * @param base64PublicKey The public key of the peer, represented in url safe base64
     */
    fun storePublicKeyOfPeer(peerConnectionId: String, base64PublicKey: String) {
        try {
            val rawPubKey = Base64.decode(base64PublicKey, Base64.URL_SAFE or Base64.NO_WRAP)
            keyManager.addPublicKey(rawPubKey, peerConnectionId)
        } catch (e: StoreException) {
            // key already exists
        }
    }

    /**
     * Encrypt and pack a [message] using the public key of [peerConnectionId]
     *
     * @param peerConnectionId the connectionId of the peer who's public key is stored
     * @param message the string message to pack
     * @return the packed encrypted message in JSON format of serialized [EncryptedPayload]
     */
    fun packEncryptedMessageForPeer(peerConnectionId: String, message: String): String {
        val temporaryKeyIdentifier = UUID.randomUUID().toString()
        keyManager.generateSymmetricKey(temporaryKeyIdentifier)

        val rawCiphertext = keyManager.encryptWithSymmetricKey(
            temporaryKeyIdentifier,
            message.toByteArray(Charsets.UTF_8)
        )

        val rawAESKey = keyManager.getSymmetricKeyData(temporaryKeyIdentifier)

        val rawEncryptedKey = keyManager.encryptWithPublicKey(
            peerConnectionId,
            rawAESKey
        )

        val encryptedPayload = EncryptedPayload(
            cipherText = Base64.encodeToString(rawCiphertext, Base64.URL_SAFE or Base64.NO_WRAP),
            encryptedKey = Base64.encodeToString(rawEncryptedKey, Base64.URL_SAFE or Base64.NO_WRAP)
        )

        return gson.toJson(encryptedPayload)
    }
}
