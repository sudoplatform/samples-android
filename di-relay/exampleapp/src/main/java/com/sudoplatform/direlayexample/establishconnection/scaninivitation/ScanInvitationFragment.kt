/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection.scaninivitation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.gson.Gson
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentScanInvitationBinding
import com.sudoplatform.direlayexample.establishconnection.PeerConnectionExchangeInformation
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.CoroutineContext

/**
 * This [ScanInvitationFragment] presents a QR code scanner that should be used to scan the QR code
 *  from a peer, who is running an instance of the sample app and is on the [InviteFragment],
 *  containing their [PeerConnectionExchangeInformation] so that a connection can be established.
 *
 *  This process is based off a highly simplified version of a DID Exchange:
 *      https://github.com/hyperledger/aries-rfcs/blob/master/features/0160-connection-protocol/README.md
 *
 * Links From:
 *  - [ConnectionOptionsFragment]: A user chooses "Scan Invitation" from the list of options displayed
 *   on the [ConnectionOptionsFragment].
 *
 * Links To:
 *  - [ConnectionFragment]: When a peer's QR code is scanned and their [PeerConnectionExchangeInformation]
 *   is successfully extracted, the [PeerConnectionExchangeInformation] of this user's chosen postbox
 *   will be sent to the peers postbox, therefore establishing a connection. When the connection is
 *   established the user will be automatically navigated to the [ConnectionFragment] where they can
 *   interact with the peer.
 */
class ScanInvitationFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** [Logger] used to log errors during registration. */
    private val errorLogger = Logger("DIRelaySample", AndroidUtilsLogDriver(LogLevel.ERROR))

    /** The [App] that holds references to the APIs this fragment needs. */
    lateinit var app: App

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentScanInvitationBinding>()
    private val binding by bindingDelegate

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ScanInvitationFragmentArgs by navArgs()

    /** The selected postbox/connection Identifier. */
    private lateinit var connectionId: String

    /** QR Code Scanner */
    private lateinit var codeScanner: CodeScanner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectionId = args.connectionId
        bindingDelegate.attach(FragmentScanInvitationBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.scan_invitation_toolbar_title)
            inflateMenu(R.menu.general_nav_menu)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        checkCameraPermissions()
        codeScanner = CodeScanner(app, binding.previewView)
        setupQRScanner()

        binding.connectButton.setOnClickListener {
            if (binding.enterInvitationEditText.text.isNotBlank()) {
                processPeerConnectionData(binding.enterInvitationEditText.text.toString())
            }
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    /**
     * Initializes the QR Scanner and define its callback function on successful scan.
     */
    private fun setupQRScanner() {
        with(codeScanner) {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false
            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    processPeerConnectionData(it.text)
                }
            }
            errorCallback = ErrorCallback.SUPPRESS
        }
    }

    /**
     * Checks whether the user has enabled camera permissions for this app. If not, they will be
     * prompted. On denying permission, an alert prompt will display.
     */
    private fun checkCameraPermissions() {
        val requestPermissionLauncher =
            registerForActivityResult(
                RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted.
                } else {
                    showAlertDialog(
                        titleResId = R.string.enable_camera_prompt_title,
                        message = resources.getString(R.string.enable_camera_prompt),
                        negativeButtonResId = android.R.string.ok
                    )
                }
            }
        if (ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    /**
     * attempt to decode the input [jsonData] as a [PeerConnectionExchangeInformation] object
     * and complete the invitation flow by responding to the peer's QR code invitation.
     *
     * @param jsonData the data scanned from the QR Scanner or input manually.
     */
    private fun processPeerConnectionData(jsonData: String) {

        launch {
            try {
                // convert to object
                val peerConnectionData =
                    Gson().fromJson(jsonData, PeerConnectionExchangeInformation::class.java)

                // store peers public key
                app.keyManagement.storePublicKeyOfPeer(
                    peerConnectionData.connectionId,
                    peerConnectionData.base64PublicKey
                )

                // create and retrieve keypair for this postbox
                app.keyManagement.createKeyPairForConnection(connectionId)
                val pubkey = app.keyManagement.getPublicKeyForConnection(connectionId)

                // post public key and postbox address to peers address
                val connectionData = PeerConnectionExchangeInformation(
                    connectionId,
                    pubkey
                )
                postPackedExchangeDataToPeer(peerConnectionData, connectionData)

                // store mapping from connectionId to peers connectionId
                app.connectionsStorage.storePeersConnectionId(
                    connectionId,
                    peerConnectionData.connectionId
                )

                // navigate to message thread
                navController.navigate(
                    ScanInvitationFragmentDirections.actionScanInvitationFragmentToConnectionFragment(
                        connectionId = connectionId
                    )
                )
            } catch (e: Exception) {
                errorLogger.error("error processing input data: \n\t${e.localizedMessage}\n\n\t$jsonData")
                showAlertDialog(
                    titleResId = R.string.invalid_invitation,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok,
                    onNegative = { if (!codeScanner.isPreviewActive) codeScanner.startPreview() }
                )
            }
        }
    }

    /**
     * Build a JSON serialized [EncryptedPayload] object containing the [connectionData] information,
     * encrypted with the peer's public key and sent to their endpoint. The public key and endpoint
     * of the peer are determined from the [peerConnection].
     *
     * @param peerConnection the peer's connection information, containing public key and connection ID
     * @param connectionData this postbox's connection information, containing public key and connection ID
     */
    private suspend fun postPackedExchangeDataToPeer(
        peerConnection: PeerConnectionExchangeInformation,
        connectionData: PeerConnectionExchangeInformation
    ) {
        val jsonData = Gson().toJson(connectionData)

        val encryptedJsonData = app.keyManagement.packEncryptedMessageForPeer(
            peerConnection.connectionId,
            jsonData
        )

        val peerPostboxURL = app.basePostboxEndpoint + peerConnection.connectionId
        val postRequest = Request.Builder()
            .url(peerPostboxURL)
            .post(encryptedJsonData.toRequestBody("text/plain".toMediaTypeOrNull()))
            .build()
        val client = OkHttpClient()
        val response = withContext(Dispatchers.IO) {
            client.newCall(postRequest).execute()
        }
        if (!response.isSuccessful) {
            throw java.lang.Exception("HTTP post to peer failed with code: ${response.code}")
        }
    }
}
