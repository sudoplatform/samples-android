/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.passwordgenerator

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.logins.CreateLoginFragment
import com.sudoplatform.sudopasswordmanager.PasswordStrength
import com.sudoplatform.sudopasswordmanager.calculateStrengthOfPassword
import com.sudoplatform.sudopasswordmanager.generatePassword
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_password_generator_dialog.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren

internal const val DEFAULT_PASSWORD_LENGTH = 20
internal const val MIN_PASSWORD_LENGTH = 6
internal const val MAX_PASSWORD_LENGTH = 50

/**
 * This [PasswordGeneratorDialog] presents a password generator and strength checker in a dialogue form.
 *
 * - Links From:
 *  - [CreateLoginFragment]: If a user taps on the "Password Generator" button they will be shown this view. When
 *  invoked from this view it expects two arguments: vault and vaultLogin.
 *  - [SettingsFragment]: If a user taps on the "Password Generator" button they will be shown this view. When
 *  invoked from this view not arguments are expected.
 */
class PasswordGeneratorDialog : DialogFragment(), CoroutineScope {

    companion object {
        internal const val GENERATED_PASSWORD = "generatedPassword"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private var length = DEFAULT_PASSWORD_LENGTH
    private var shouldMonitorLengthTextChange = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_generator_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generatePassword()

        shouldMonitorLengthTextChange = false
        view.editText_length.setText(length.toString())

        view.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, b: Boolean) {
                length = value
                generatePassword()
                shouldMonitorLengthTextChange = false
                view.editText_length.setText(length.toString())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                // Don't care
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // Don't care
            }
        })

        view.editText_generated_password.addTextChangedListener(
            AfterTextChangedWatcher { text ->
                if (shouldMonitorLengthTextChange) {
                    calculateStrength(text.toString())
                }
                shouldMonitorLengthTextChange = true
            }
        )

        view.editText_length.addTextChangedListener(
            AfterTextChangedWatcher { text ->
                text?.let { lengthText ->
                    val lengthTextAsInt = if (lengthText.isNotEmpty()) lengthText.toString().toInt() else 0
                    if (lengthTextAsInt < MIN_PASSWORD_LENGTH) {
                        length = MIN_PASSWORD_LENGTH
                    } else if (lengthTextAsInt > MAX_PASSWORD_LENGTH) {
                        length = MAX_PASSWORD_LENGTH
                    } else {
                        length = lengthTextAsInt
                    }
                }
                view.seekBar.progress = length
                generatePassword()
            }
        )

        view.editText_length.onFocusChangeListener = View.OnFocusChangeListener { v, _ ->
            v?.let { editText ->
                {
                    if (!editText.hasFocus()) {
                        view.editText_length.setText("$length")
                    }
                }
            }
        }

        view.switch_lowercase.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        view.switch_uppercase.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        view.switch_numbers.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        view.switch_symbols.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        view.button_cancel.setOnClickListener {
            findNavController().popBackStack()
        }

        view.button_ok.setOnClickListener {
            // Send the generated password back to the fragment that invoked this dialogue
            setFragmentResult(
                GENERATED_PASSWORD,
                bundleOf(
                    GENERATED_PASSWORD to view.editText_generated_password.text.toString()
                )
            )
            findNavController().popBackStack()
        }
    }

    private class AfterTextChangedWatcher(private val onAfterTextChanged: (text: Editable?) -> Unit) : TextWatcher {
        override fun afterTextChanged(text: Editable?) {
            onAfterTextChanged.invoke(text)
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            // Don't care
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            // Don't care
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun generatePassword() {
        val view = this.view ?: return
        val password = generatePassword(
            length,
            view.switch_uppercase.isChecked,
            view.switch_lowercase.isChecked,
            view.switch_numbers.isChecked,
            view.switch_symbols.isChecked
        )
        calculateStrength(password)
        view.editText_generated_password.setText(password)
    }

    private fun calculateStrength(password: String) {
        val view = this.view ?: return
        when (calculateStrengthOfPassword(password)) {
            PasswordStrength.VeryWeak -> {
                view.editText_strength.text = view.context.getString(R.string.very_weak)
                view.editText_strength.setTextColor(Color.RED)
            }
            PasswordStrength.Weak -> {
                view.editText_strength.text = view.context.getString(R.string.weak)
                view.editText_strength.setTextColor(Color.RED)
            }
            PasswordStrength.Moderate -> {
                view.editText_strength.text = view.context.getString(R.string.moderate)
                view.editText_strength.setTextColor(ContextCompat.getColor(view.context, R.color.colorOrange))
            }
            PasswordStrength.Strong -> {
                view.editText_strength.text = view.context.getString(R.string.strong)
                view.editText_strength.setTextColor(ContextCompat.getColor(view.context, R.color.colorGreen))
            }
            PasswordStrength.VeryStrong -> {
                view.editText_strength.text = view.context.getString(R.string.very_strong)
                view.editText_strength.setTextColor(ContextCompat.getColor(view.context, R.color.colorGreen))
            }
        }
    }
}
