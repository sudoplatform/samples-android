/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sudoplatform.telephonyexample.entitlements.EntitlementsScreen
import com.sudoplatform.telephonyexample.keybackup.KeyBackupScreen
import com.sudoplatform.telephonyexample.notifications.MessageTarget
import com.sudoplatform.telephonyexample.phonemessages.PhoneMessageScreen
import com.sudoplatform.telephonyexample.phonemessages.PhoneMessageViewModel
import com.sudoplatform.telephonyexample.phonemessages.PhoneMessagesScreen
import com.sudoplatform.telephonyexample.phonemessages.PhoneMessagesViewModel
import com.sudoplatform.telephonyexample.phonenumbers.PhoneNumbersScreen
import com.sudoplatform.telephonyexample.phonenumbers.PhoneNumbersViewModel
import com.sudoplatform.telephonyexample.phonenumbers.ProvisionPhoneNumberScreen
import com.sudoplatform.telephonyexample.phonenumbers.ProvisionPhoneNumberViewModel
import com.sudoplatform.telephonyexample.register.RegisterScreen
import com.sudoplatform.telephonyexample.sudos.CreateSudoScreen
import com.sudoplatform.telephonyexample.sudos.EditSudoScreen
import com.sudoplatform.telephonyexample.sudos.EditSudoViewModel
import com.sudoplatform.telephonyexample.sudos.SudosScreen
import com.sudoplatform.telephonyexample.userdetails.UserDetailsScreen

object Routes {
    const val REGISTER = "register"
    const val SUDOS = "sudos"
    const val CREATE_SUDO = "sudos/create"
    const val KEY_BACKUP = "keys"
    const val ENTITLEMENTS = "entitlements"
    const val USER_DETAILS = "user"
    const val EDIT_SUDO = "sudos/{${EditSudoViewModel.SUDO_ID_ARG}}/edit"
    const val PHONE_NUMBERS = "sudos/{${PhoneNumbersViewModel.SUDO_ID_ARG}}/numbers"
    const val PROVISION_PHONE_NUMBER = "sudos/{${ProvisionPhoneNumberViewModel.SUDO_ID_ARG}}/numbers/provision"
    const val PHONE_MESSAGES = "numbers/{${PhoneMessagesViewModel.PHONE_NUMBER_ID_ARG}}/messages"
    const val PHONE_MESSAGE = "messages/{${PhoneMessageViewModel.MESSAGE_ID_ARG}}"

    fun editSudo(sudoId: String) = "sudos/$sudoId/edit"

    fun phoneNumbers(sudoId: String) = "sudos/$sudoId/numbers"

    fun provisionPhoneNumber(sudoId: String) = "sudos/$sudoId/numbers/provision"

    fun phoneMessages(phoneNumberId: String) = "numbers/$phoneNumberId/messages"

    fun phoneMessage(messageId: String) = "messages/$messageId"
}

/**
 * @param pendingMessage a message from a tapped notification, opened once the user is past
 *  sign in, over its number's messages.
 */
@Composable
fun AppNavHost(
    federatedSignInUri: Uri?,
    onFederatedSignInUriConsumed: () -> Unit,
    pendingMessage: MessageTarget?,
    onPendingMessageConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route
    LaunchedEffect(pendingMessage, currentRoute) {
        if (pendingMessage == null || currentRoute == null || currentRoute == Routes.REGISTER) return@LaunchedEffect
        navController.navigate(Routes.phoneMessages(pendingMessage.phoneNumberId)) { popUpTo(Routes.SUDOS) }
        navController.navigate(Routes.phoneMessage(pendingMessage.messageId))
        onPendingMessageConsumed()
    }
    NavHost(navController = navController, startDestination = Routes.REGISTER) {
        composable(Routes.REGISTER) {
            RegisterScreen(
                federatedSignInUri = federatedSignInUri,
                onFederatedSignInUriConsumed = onFederatedSignInUriConsumed,
                onNavigateToSudos = {
                    navController.navigate(Routes.SUDOS) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SUDOS) {
            SudosScreen(
                onSignedOut = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.SUDOS) { inclusive = true }
                    }
                },
                onNavigateToPhoneNumbers = { sudoId -> navController.navigate(Routes.phoneNumbers(sudoId)) },
                onNavigateToCreateSudo = { navController.navigate(Routes.CREATE_SUDO) },
                onNavigateToEditSudo = { sudoId -> navController.navigate(Routes.editSudo(sudoId)) },
                onNavigateToEntitlements = { navController.navigate(Routes.ENTITLEMENTS) },
                onNavigateToUserDetails = { navController.navigate(Routes.USER_DETAILS) },
                onNavigateToKeyBackup = { navController.navigate(Routes.KEY_BACKUP) },
            )
        }
        composable(Routes.ENTITLEMENTS) {
            EntitlementsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.USER_DETAILS) {
            UserDetailsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.KEY_BACKUP) {
            KeyBackupScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.CREATE_SUDO) {
            CreateSudoScreen(
                onSudoCreated = { sudoId ->
                    navController.navigate(Routes.phoneNumbers(sudoId)) {
                        popUpTo(Routes.CREATE_SUDO) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.EDIT_SUDO,
            arguments = listOf(navArgument(EditSudoViewModel.SUDO_ID_ARG) { type = NavType.StringType }),
        ) {
            EditSudoScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Routes.PHONE_NUMBERS,
            arguments = listOf(navArgument(PhoneNumbersViewModel.SUDO_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            val sudoId = checkNotNull(entry.arguments?.getString(PhoneNumbersViewModel.SUDO_ID_ARG))
            PhoneNumbersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProvision = { navController.navigate(Routes.provisionPhoneNumber(sudoId)) },
                onNavigateToMessages = { phoneNumberId -> navController.navigate(Routes.phoneMessages(phoneNumberId)) },
            )
        }
        composable(
            Routes.PROVISION_PHONE_NUMBER,
            arguments = listOf(navArgument(ProvisionPhoneNumberViewModel.SUDO_ID_ARG) { type = NavType.StringType }),
        ) {
            ProvisionPhoneNumberScreen(
                onPhoneNumberProvisioned = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.PHONE_MESSAGES,
            arguments = listOf(navArgument(PhoneMessagesViewModel.PHONE_NUMBER_ID_ARG) { type = NavType.StringType }),
        ) {
            PhoneMessagesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMessage = { messageId -> navController.navigate(Routes.phoneMessage(messageId)) },
            )
        }
        composable(
            Routes.PHONE_MESSAGE,
            arguments = listOf(navArgument(PhoneMessageViewModel.MESSAGE_ID_ARG) { type = NavType.StringType }),
        ) {
            PhoneMessageScreen(onClose = { navController.popBackStack() })
        }
    }
}
