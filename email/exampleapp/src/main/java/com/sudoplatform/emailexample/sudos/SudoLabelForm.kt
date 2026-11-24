/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.whiteOutlinedTextFieldColors

/**
 * The label field and "Learn more" block shared by Create Sudo and Edit Sudo.
 *
 * @param showLabelRequired shows the field in its error state, asking for a label.
 * @param onSubmit called from the keyboard's Done action.
 */
@Composable
fun SudoLabelForm(
    label: String,
    onLabelChange: (String) -> Unit,
    enabled: Boolean,
    showLabelRequired: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val docUrl = stringResource(R.string.create_sudo_doc_url)

    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(R.string.sudo_label)) },
            isError = showLabelRequired,
            supportingText = {
                Text(stringResource(if (showLabelRequired) R.string.enter_sudo_name else R.string.choose_label_detail))
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            colors = whiteOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag(TestTags.SUDO_LABEL_FIELD),
        )
        Text(
            stringResource(R.string.create_sudo_learn_more),
            color = Color.Black,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 40.dp),
        )
        TextButton(
            onClick = { uriHandler.openUri(docUrl) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
        ) {
            Text(stringResource(R.string.learn_more).uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}
