/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.register

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.components.DefaultButton
import com.sudoplatform.telephonyexample.ui.components.DropdownField
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.PrimaryButton
import com.sudoplatform.telephonyexample.ui.components.StatusBarIcons
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme
import com.sudoplatform.telephonyexample.ui.theme.WindowBackground

/**
 * Landing screen that lets the user register or sign in.
 *
 * Links From:
 *  - Sudos: after deregistering.
 *
 * Links To:
 *  - Sudos: once the user is signed in.
 *
 * @param federatedSignInUri an FSSO redirect delivered to the activity, if any.
 * @param onFederatedSignInUriConsumed called once [federatedSignInUri] has been handed to the view model.
 */
@Composable
fun RegisterScreen(
    federatedSignInUri: Uri?,
    onFederatedSignInUriConsumed: () -> Unit,
    onNavigateToSudos: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(federatedSignInUri) {
        viewModel.resumeSession(context as Activity, federatedSignInUri)
        if (federatedSignInUri != null) {
            onFederatedSignInUriConsumed()
        }
        onPauseOrDispose {}
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterEvent.NavigateToSudos -> onNavigateToSudos()
                is RegisterEvent.ShowMessage ->
                    Toast.makeText(context, context.getString(event.messageResId, event.detail), Toast.LENGTH_LONG).show()
            }
        }
    }

    RegisterContent(
        uiState = uiState,
        onRegistrationMethodSelected = viewModel::selectRegistrationMethod,
        onEnforceSingletonKeyPairChanged = viewModel::setEnforceSingletonKeyPair,
        onRegister = { viewModel.registerAndSignIn(context as Activity) },
        onSignOutOrReset = viewModel::signOutOrReset,
    )

    uiState.configurationError?.let { error ->
        MessageDialog(
            title = stringResource(R.string.error),
            message = stringResource(R.string.configuration_failure, error),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissConfigurationError,
            onDismissRequest = viewModel::dismissConfigurationError,
        )
    }
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onRegistrationMethodSelected: (RegistrationMethod) -> Unit,
    onEnforceSingletonKeyPairChanged: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onSignOutOrReset: () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(WindowBackground)
                .systemBarsPadding(),
    ) {
        DefaultButton(
            text = stringResource(if (uiState.usedFsso) R.string.sign_out else R.string.reset),
            onClick = onSignOutOrReset,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 15.dp, end = 15.dp)
                    .size(width = 100.dp, height = 40.dp)
                    .testTag(TestTags.SIGN_OUT_BUTTON),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 150.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.sudoplatform),
                contentDescription = null,
                modifier = Modifier.size(width = 276.dp, height = 93.dp),
            )
            Text(stringResource(R.string.telephony), color = Color.Black, fontSize = 30.sp)
            Text(
                stringResource(R.string.sample_app),
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 5.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        ) {
            DropdownField(
                label = stringResource(R.string.registration_method),
                options = RegistrationMethod.entries,
                selected = uiState.registrationMethod,
                optionText = { stringResource(it.labelResId()) },
                onSelected = onRegistrationMethodSelected,
                enabled = !uiState.isLoading,
                testTag = TestTags.REGISTRATION_METHOD,
                modifier = Modifier.width(240.dp),
            )
            Spacer(Modifier.height(16.dp))
            KeyModeToggle(
                enforceSingletonKeyPair = uiState.enforceSingletonKeyPair,
                onChanged = onEnforceSingletonKeyPairChanged,
                enabled = !uiState.isLoading,
                modifier = Modifier.width(240.dp),
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(R.string.register_login),
                onClick = onRegister,
                isLoading = uiState.isLoading,
                modifier = Modifier.size(width = 160.dp, height = 50.dp).testTag(TestTags.REGISTER_BUTTON),
            )
        }
    }
}

/**
 * Chooses whether every phone number shares one key pair. Chosen before signing in because the
 * telephony client is created with it at sign in.
 */
@Composable
private fun KeyModeToggle(
    enforceSingletonKeyPair: Boolean,
    onChanged: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = enforceSingletonKeyPair,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = onChanged,
                    ).testTag(TestTags.ENFORCE_SINGLETON_KEY_PAIR_TOGGLE),
        ) {
            Text(
                stringResource(R.string.single_key_for_all_numbers),
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enforceSingletonKeyPair, onCheckedChange = null, enabled = enabled)
        }
        Text(
            stringResource(
                if (enforceSingletonKeyPair) R.string.single_key_explanation else R.string.key_per_number_explanation,
            ),
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@StringRes
private fun RegistrationMethod.labelResId(): Int =
    when (this) {
        RegistrationMethod.TEST -> R.string.test_registration
        RegistrationMethod.FSSO -> R.string.federated_signin
    }

@Preview(showSystemUi = true)
@Composable
private fun RegisterContentPreview() {
    TelephonyExampleTheme {
        RegisterContent(
            uiState = RegisterUiState(),
            onRegistrationMethodSelected = {},
            onEnforceSingletonKeyPairChanged = {},
            onRegister = {},
            onSignOutOrReset = {},
        )
    }
}
