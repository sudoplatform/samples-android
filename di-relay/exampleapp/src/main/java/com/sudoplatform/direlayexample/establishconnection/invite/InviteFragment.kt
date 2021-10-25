/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection.invite

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentInviteBinding
import com.sudoplatform.direlayexample.establishconnection.PeerConnectionExchangeInformation
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.subscription.DIRelayEventSubscriber
import com.sudoplatform.sudodirelay.types.PostboxDeletionResult
import com.sudoplatform.sudodirelay.types.RelayMessage
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap
import kotlin.coroutines.CoroutineContext

/**
 * This [InviteFragment] displays a QR code containing the [PeerConnectionExchangeInformation] of the
 *  chosen postbox. This QR code is intended to be scanned by a peer, who has an instance of this sample
 *  app running, from their [ScanInvitationFragment] screen. After producing the QR code invitation, the
 *  [InviteFragment] will subscribe to the chosen postbox waiting a peer wishing to connect sends a relay
 *  message containing their own encrypted [PeerConnectionExchangeInformation] - therefore establishing a
 *  connection.
 *
 *  This process is based off a highly simplified version of a DID Exchange:
 *      https://github.com/hyperledger/aries-rfcs/blob/master/features/0160-connection-protocol/README.md
 *
 * Links From:
 *  - [ConnectionOptionsFragment]: A user chooses "Create Invitation" from the list of options displayed
 *   on the [ConnectionOptionsFragment].
 *
 * Links To:
 *  - [ConnectionFragment]: When a relay message is receiving on the chosen postbox, if the relay message
 *   content is packed in a JSON [EncryptedPayload] format and contains a [PeerConnectionExchangeInformation]
 *   JSON object of the peer's information, then the connection will be established and the user will
 *   automatically navigate forward to the [ConnectionFragment] where they can interact with the peer.
 */
class InviteFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** [Logger] used to log errors during registration. */
    private val errorLogger = Logger("DIRelaySample", AndroidUtilsLogDriver(LogLevel.ERROR))

    /** The [App] that holds references to the APIs this fragment needs. */
    lateinit var app: App

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentInviteBinding>()
    private val binding by bindingDelegate

    /** Values for constructing QR Code */
    private val qrCodeWriter = QRCodeWriter()
    internal val barcodeFormatQRCode = BarcodeFormat.QR_CODE

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: InviteFragmentArgs by navArgs()

    /** The selected postbox/connection Identifier. */
    private lateinit var connectionId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectionId = args.connectionId
        bindingDelegate.attach(FragmentInviteBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.invite_toolbar_title)
            inflateMenu(R.menu.general_nav_menu)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        initializeInvitationFlow()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        unsubscribeFromRelayEvents()
    }

    /**
     * Initializes the invitation process for this sample app: A key pair is created and stored
     * for this connectionId, and a QR code is generated and displayed containing this information,
     * then this fragment listens for a response to the invitation from another user of the sample app.
     */
    private fun initializeInvitationFlow() {
        launch {
            app.keyManagement.createKeyPairForConnection(connectionId)
            val publicKey = app.keyManagement.getPublicKeyForConnection(connectionId)
            val exchangeData = PeerConnectionExchangeInformation(connectionId, publicKey)
            val connectionDataJSON = Gson().toJson(exchangeData)
            displayQRCode(connectionDataJSON)
            binding.copyInvitationEditText.setText(connectionDataJSON)
            listenForIncomingInvitationResponse()
        }
    }

    /**
     * Generates and displays a QR code containing the JSON serialized [PeerConnectionExchangeInformation].
     *
     * @param connectionDataJSON the connection information about this client's postbox.
     */
    private suspend fun displayQRCode(
        connectionDataJSON: String
    ) {
        val width = 300
        val height = 300
        val imageBitmap = createBitmap(width, height)

        val qrCodeBitmap: Bitmap? = withContext(Dispatchers.IO) {
            try {
                val hintMap: MutableMap<EncodeHintType, Any> =
                    EnumMap(EncodeHintType::class.java)
                hintMap[EncodeHintType.MARGIN] = 0

                val bitmapMatrix =
                    qrCodeWriter.encode(
                        connectionDataJSON,
                        barcodeFormatQRCode,
                        width,
                        height,
                        hintMap
                    )

                for (i in 0 until width) {
                    for (j in 0 until height) {
                        imageBitmap.setPixel(
                            i,
                            j,
                            if (bitmapMatrix.get(i, j)) Color.BLACK else Color.WHITE
                        )
                    }
                }
                imageBitmap
            } catch (e: Exception) {
                errorLogger.error("error generating QR Code: ${e.localizedMessage}")
                null
            }
        }
        qrCodeBitmap?.let { bm ->
            binding.qrCode.setImageBitmap(bm)
        }
    }

    /**
     * Use the relay client to subscribe to incoming messages on this postbox. On receiving message
     * process the message's cipherText.
     */
    private fun listenForIncomingInvitationResponse() {
        launch {
            app.diRelayClient.subscribeToRelayEvents(
                connectionId,
                object : DIRelayEventSubscriber {
                    override fun connectionStatusChanged(state: DIRelayEventSubscriber.ConnectionState) {}

                    override fun messageIncoming(message: RelayMessage) {
                        processReceivedResponse(message.cipherText)
                    }

                    override fun postBoxDeleted(update: PostboxDeletionResult) {}
                }
            )
        }
    }

    /**
     * Use the relay client to unsubscribe from live RelayEvent updates.
     */
    private fun unsubscribeFromRelayEvents() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.diRelayClient.unsubscribeToRelayEvents(connectionId)
                }
            } catch (e: SudoDIRelayClient.DIRelayException) {
                app.logger.error("Failed to unsubscribe: $e")
            }
        }
    }

    /**
     * attempt to unpack/decrypt the [jsonData] and interpret the contents as a serialized
     * [PeerConnectionExchangeInformation]. If valid, store their connection information against
     * this connection, establishing the peer connection, and navigate to the connection.
     *
     * @param jsonData the data to process.
     */
    private fun processReceivedResponse(jsonData: String) {
        launch {
            try {
                val decryptedJsonData =
                    app.keyManagement.unpackEncryptedMessageForConnection(connectionId, jsonData)

                val peerConnectionData =
                    Gson().fromJson(
                        decryptedJsonData,
                        PeerConnectionExchangeInformation::class.java
                    )

                app.keyManagement.storePublicKeyOfPeer(
                    peerConnectionId = peerConnectionData.connectionId,
                    base64PublicKey = peerConnectionData.base64PublicKey
                )

                app.connectionsStorage.storePeersConnectionId(
                    connectionId,
                    peerConnectionData.connectionId
                )
                navController.navigate(
                    InviteFragmentDirections.actionInviteFragmentToConnectionFragment(
                        connectionId
                    )
                )
            } catch (e: Exception) {
                errorLogger.error(e.stackTraceToString())
                errorLogger.error("received invalid postbox data: $jsonData")
                showAlertDialog(
                    titleResId = R.string.invalid_response,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok
                )
            }
        }
    }
}
