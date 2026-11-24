/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sudoplatform.emailexample.emailaddressblocklist.EmailAddressBlocklistScreen
import com.sudoplatform.emailexample.emailaddresses.EmailAddressesScreen
import com.sudoplatform.emailexample.emailaddresses.ProvisionEmailAddressScreen
import com.sudoplatform.emailexample.emailaliases.ProvisionEmailAliasScreen
import com.sudoplatform.emailexample.emailmessages.ComposeEmailMessageScreen
import com.sudoplatform.emailexample.emailmessages.ComposeMode
import com.sudoplatform.emailexample.emailmessages.EmailMessagesScreen
import com.sudoplatform.emailexample.emailmessages.ReadEmailMessageScreen
import com.sudoplatform.emailexample.entitlements.EntitlementsScreen
import com.sudoplatform.emailexample.keybackup.KeyBackupScreen
import com.sudoplatform.emailexample.notifications.MessageTarget
import com.sudoplatform.emailexample.register.RegisterScreen
import com.sudoplatform.emailexample.sudos.CreateSudoScreen
import com.sudoplatform.emailexample.sudos.EditSudoScreen
import com.sudoplatform.emailexample.sudos.EditSudoViewModel
import com.sudoplatform.emailexample.sudos.SudosScreen
import com.sudoplatform.emailexample.userdetails.UserDetailsScreen

/** Navigation arguments shared between routes and the view models that read them. */
object NavArgs {
    const val SUDO_ID = "sudoId"
    const val EMAIL_ADDRESS_ID = "emailAddressId"
    const val MESSAGE_ID = "messageId"
    const val EXTERNAL = "external"
    const val MODE = "mode"
}

object Routes {
    const val REGISTER = "register"
    const val SUDOS = "sudos"
    const val CREATE_SUDO = "sudos/create"
    const val KEY_BACKUP = "keys"
    const val ENTITLEMENTS = "entitlements"
    const val USER_DETAILS = "user"
    const val EDIT_SUDO = "sudos/{${EditSudoViewModel.SUDO_ID_ARG}}/edit"
    const val EMAIL_ADDRESSES = "sudos/{${NavArgs.SUDO_ID}}/addresses"
    const val PROVISION_EMAIL_ADDRESS = "sudos/{${NavArgs.SUDO_ID}}/addresses/provision"
    const val PROVISION_EMAIL_ALIAS = "sudos/{${NavArgs.SUDO_ID}}/aliases/provision?${NavArgs.EXTERNAL}={${NavArgs.EXTERNAL}}"
    const val EMAIL_MESSAGES = "addresses/{${NavArgs.EMAIL_ADDRESS_ID}}/messages"
    const val READ_EMAIL_MESSAGE = "addresses/{${NavArgs.EMAIL_ADDRESS_ID}}/messages/{${NavArgs.MESSAGE_ID}}"
    const val EMAIL_ADDRESS_BLOCKLIST = "addresses/{${NavArgs.EMAIL_ADDRESS_ID}}/blocklist"

    /** Optional arguments: [NavArgs.MODE], a [ComposeMode.routeValue], and [NavArgs.MESSAGE_ID]. */
    const val COMPOSE =
        "addresses/{${NavArgs.EMAIL_ADDRESS_ID}}/compose?${NavArgs.MODE}={${NavArgs.MODE}}&${NavArgs.MESSAGE_ID}={${NavArgs.MESSAGE_ID}}"

    fun editSudo(sudoId: String) = "sudos/$sudoId/edit"

    fun emailAddresses(sudoId: String) = "sudos/$sudoId/addresses"

    fun provisionEmailAddress(sudoId: String) = "sudos/$sudoId/addresses/provision"

    fun provisionEmailAlias(
        sudoId: String,
        external: Boolean,
    ) = "sudos/$sudoId/aliases/provision?${NavArgs.EXTERNAL}=$external"

    fun emailMessages(emailAddressId: String) = "addresses/$emailAddressId/messages"

    fun readEmailMessage(
        emailAddressId: String,
        messageId: String,
    ) = "addresses/$emailAddressId/messages/$messageId"

    fun emailAddressBlocklist(emailAddressId: String) = "addresses/$emailAddressId/blocklist"

    /**
     * @param messageId the message replied to or forwarded, or the draft edited; null for
     *  [ComposeMode.NEW].
     */
    fun compose(
        emailAddressId: String,
        mode: ComposeMode,
        messageId: String? = null,
    ): String {
        val route = "addresses/$emailAddressId/compose?${NavArgs.MODE}=${mode.routeValue}"
        return if (messageId == null) route else "$route&${NavArgs.MESSAGE_ID}=$messageId"
    }
}

/**
 * @param pendingMessage a message from a tapped notification, opened once the user is past
 *  sign in, over its address's messages.
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
        navController.navigate(Routes.emailMessages(pendingMessage.emailAddressId)) { popUpTo(Routes.SUDOS) }
        navController.navigate(Routes.readEmailMessage(pendingMessage.emailAddressId, pendingMessage.messageId))
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
                onNavigateToEmailAddresses = { sudoId -> navController.navigate(Routes.emailAddresses(sudoId)) },
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
                    navController.navigate(Routes.emailAddresses(sudoId)) {
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
            Routes.EMAIL_ADDRESSES,
            arguments = listOf(navArgument(NavArgs.SUDO_ID) { type = NavType.StringType }),
        ) { entry ->
            val sudoId = checkNotNull(entry.arguments?.getString(NavArgs.SUDO_ID))
            EmailAddressesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProvisionAddress = { navController.navigate(Routes.provisionEmailAddress(sudoId)) },
                onNavigateToProvisionAlias = { external -> navController.navigate(Routes.provisionEmailAlias(sudoId, external)) },
                onNavigateToMessages = { emailAddressId -> navController.navigate(Routes.emailMessages(emailAddressId)) },
                onNavigateToKeyBackup = { navController.navigate(Routes.KEY_BACKUP) },
            )
        }
        composable(
            Routes.PROVISION_EMAIL_ADDRESS,
            arguments = listOf(navArgument(NavArgs.SUDO_ID) { type = NavType.StringType }),
        ) {
            ProvisionEmailAddressScreen(
                onEmailAddressProvisioned = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.PROVISION_EMAIL_ALIAS,
            arguments =
                listOf(
                    navArgument(NavArgs.SUDO_ID) { type = NavType.StringType },
                    navArgument(NavArgs.EXTERNAL) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
        ) {
            ProvisionEmailAliasScreen(
                onEmailAliasProvisioned = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.EMAIL_MESSAGES,
            arguments = listOf(navArgument(NavArgs.EMAIL_ADDRESS_ID) { type = NavType.StringType }),
        ) { entry ->
            val emailAddressId = checkNotNull(entry.arguments?.getString(NavArgs.EMAIL_ADDRESS_ID))
            EmailMessagesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMessage = { messageId -> navController.navigate(Routes.readEmailMessage(emailAddressId, messageId)) },
                onNavigateToCompose = { navController.navigate(Routes.compose(emailAddressId, ComposeMode.NEW)) },
                onNavigateToDraft = { draftId -> navController.navigate(Routes.compose(emailAddressId, ComposeMode.DRAFT, draftId)) },
                onNavigateToBlocklist = { navController.navigate(Routes.emailAddressBlocklist(emailAddressId)) },
                onNavigateToKeyBackup = { navController.navigate(Routes.KEY_BACKUP) },
            )
        }
        composable(
            Routes.READ_EMAIL_MESSAGE,
            arguments =
                listOf(
                    navArgument(NavArgs.EMAIL_ADDRESS_ID) { type = NavType.StringType },
                    navArgument(NavArgs.MESSAGE_ID) { type = NavType.StringType },
                ),
        ) { entry ->
            val emailAddressId = checkNotNull(entry.arguments?.getString(NavArgs.EMAIL_ADDRESS_ID))
            ReadEmailMessageScreen(
                onNavigateBack = { navController.popBackStack() },
                onReply = { messageId -> navController.navigate(Routes.compose(emailAddressId, ComposeMode.REPLY, messageId)) },
                onForward = { messageId -> navController.navigate(Routes.compose(emailAddressId, ComposeMode.FORWARD, messageId)) },
            )
        }
        composable(
            Routes.COMPOSE,
            arguments =
                listOf(
                    navArgument(NavArgs.EMAIL_ADDRESS_ID) { type = NavType.StringType },
                    navArgument(NavArgs.MODE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(NavArgs.MESSAGE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) {
            ComposeEmailMessageScreen(
                onNavigateBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            Routes.EMAIL_ADDRESS_BLOCKLIST,
            arguments = listOf(navArgument(NavArgs.EMAIL_ADDRESS_ID) { type = NavType.StringType }),
        ) {
            EmailAddressBlocklistScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
