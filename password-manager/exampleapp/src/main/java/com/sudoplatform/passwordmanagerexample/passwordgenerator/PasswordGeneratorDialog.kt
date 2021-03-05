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
import com.sudoplatform.passwordmanagerexample.databinding.FragmentPasswordGeneratorDialogBinding
import com.sudoplatform.passwordmanagerexample.logins.CreateLoginFragment
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.PasswordStrength
import com.sudoplatform.sudopasswordmanager.calculateStrengthOfPassword
import com.sudoplatform.sudopasswordmanager.generatePassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

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

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentPasswordGeneratorDialogBinding>()
    private val binding by bindingDelegate

    private var length = DEFAULT_PASSWORD_LENGTH
    private var shouldMonitorLengthTextChange = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentPasswordGeneratorDialogBinding.inflate(inflater, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generatePassword()

        shouldMonitorLengthTextChange = false

        binding.editTextLength.setText(length.toString())

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, b: Boolean) {
                length = value
                generatePassword()
                shouldMonitorLengthTextChange = false
                binding.editTextLength.setText(length.toString())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                // Don't care
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // Don't care
            }
        })

        binding.editTextGeneratedPassword.addTextChangedListener(
            AfterTextChangedWatcher { text ->
                if (shouldMonitorLengthTextChange) {
                    calculateStrength(text.toString())
                }
                shouldMonitorLengthTextChange = true
            }
        )

        binding.editTextLength.addTextChangedListener(
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
                binding.seekBar.progress = length
                generatePassword()
            }
        )

        binding.editTextLength.onFocusChangeListener = View.OnFocusChangeListener { v, _ ->
            v?.let {
                if (!it.hasFocus()) {
                    binding.editTextLength.setText("$length")
                }
            }
        }

        binding.switchLowercase.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        binding.switchUppercase.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        binding.switchNumbers.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        binding.switchSymbols.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonOk.setOnClickListener {
            // Send the generated password back to the fragment that invoked this dialogue
            setFragmentResult(
                GENERATED_PASSWORD,
                bundleOf(
                    GENERATED_PASSWORD to binding.editTextGeneratedPassword.text.toString()
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
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun generatePassword() {
        val password = generatePassword(
            length,
            binding.switchUppercase.isChecked,
            binding.switchLowercase.isChecked,
            binding.switchNumbers.isChecked,
            binding.switchSymbols.isChecked
        )
        calculateStrength(password)
        binding.editTextGeneratedPassword.setText(password)
    }

    private fun calculateStrength(password: String) {
        when (calculateStrengthOfPassword(password)) {
            PasswordStrength.VeryWeak -> {
                binding.editTextStrength.text = requireContext().getString(R.string.very_weak)
                binding.editTextStrength.setTextColor(Color.RED)
            }
            PasswordStrength.Weak -> {
                binding.editTextStrength.text = requireContext().getString(R.string.weak)
                binding.editTextStrength.setTextColor(Color.RED)
            }
            PasswordStrength.Moderate -> {
                binding.editTextStrength.text = requireContext().getString(R.string.moderate)
                binding.editTextStrength.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOrange))
            }
            PasswordStrength.Strong -> {
                binding.editTextStrength.text = requireContext().getString(R.string.strong)
                binding.editTextStrength.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen))
            }
            PasswordStrength.VeryStrong -> {
                binding.editTextStrength.text = requireContext().getString(R.string.very_strong)
                binding.editTextStrength.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen))
            }
        }
    }
}
