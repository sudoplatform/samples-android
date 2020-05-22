package com.sudoplatform.telephonyexample

import com.anonyome.keymanager.KeyManager
import com.anonyome.keymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.DefaultSudoProfilesClient
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.DefaultSudoUserClient
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudotelephony.DefaultSudoTelephonyClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import java.net.URI

class App : android.app.Application() {
    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoProfilesClient: SudoProfilesClient
    lateinit var sudoTelephonyClient: SudoTelephonyClient
    lateinit var keyManager: KeyManager

    override fun onCreate() {
        super.onCreate()

        val logger = Logger("telephonyExample", AndroidUtilsLogDriver(LogLevel.DEBUG))
        // create sudo user client (for registration and sign in)
        sudoUserClient = DefaultSudoUserClient(this, "sudo-test", logger)

        // create sudo profiles client (for creating, deleting and modifying sudos)
        val blobURI = URI(cacheDir.path)
        sudoProfilesClient = DefaultSudoProfilesClient(this, sudoUserClient, blobURI, logger)

        // create sudo telephony client (for searching, provisioning and using telephone numbers)
        sudoTelephonyClient = DefaultSudoTelephonyClient(this, sudoUserClient, sudoProfilesClient)

        // create key manager
        keyManager = KeyManagerFactory(this).createAndroidKeyManager() as KeyManager
    }
}