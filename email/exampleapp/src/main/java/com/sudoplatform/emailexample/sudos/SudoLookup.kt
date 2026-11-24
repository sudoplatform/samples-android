/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException

/**
 * Find the Sudo with [sudoId]. SudoProfilesClient caches the Sudos it has listed, so the cache is
 * checked before the service.
 *
 * @throws SudoProfileException.SudoNotFoundException if the user has no Sudo with [sudoId].
 */
@Throws(SudoProfileException::class)
suspend fun SudoProfilesClient.findSudo(sudoId: String): Sudo =
    listSudos(ListOption.CACHE_ONLY).find { it.id == sudoId }
        ?: listSudos(ListOption.REMOTE_ONLY).find { it.id == sudoId }
        ?: throw SudoProfileException.SudoNotFoundException("Sudo $sudoId not found")

/** The label a user gave the Sudo, or its ID for a Sudo created without one by another client. */
val Sudo.displayLabel: String get() = label ?: id.orEmpty()
