/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.zeapo.pwdstore.crypto

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.github.ajalt.timberkt.Timber
import com.github.ajalt.timberkt.Timber.tag
import com.google.android.material.snackbar.Snackbar
import com.zeapo.pwdstore.R
import com.zeapo.pwdstore.UserPreference
import me.msfjarvis.openpgpktx.util.OpenPgpApi
import me.msfjarvis.openpgpktx.util.OpenPgpServiceConnection
import org.apache.commons.io.FilenameUtils
import org.openintents.openpgp.IOpenPgpService2
import org.openintents.openpgp.OpenPgpError

@Suppress("Registered")
open class BasePgpActivity : AppCompatActivity(), OpenPgpServiceConnection.OnBound {

    val settings: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private var _keyIDs = emptySet<String>()
    val keyIDs get() = _keyIDs

    private var serviceConnection: OpenPgpServiceConnection? = null
    var api: OpenPgpApi? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        tag(TAG)

        _keyIDs = settings.getStringSet("openpgp_key_ids_set", null) ?: emptySet()
        val providerPackageName = settings.getString("openpgp_provider_list", "")

        if (providerPackageName.isNullOrEmpty()) {
            Toast.makeText(this, resources.getString(R.string.provider_toast_text), Toast.LENGTH_LONG).show()
            val intent = Intent(this, UserPreference::class.java)
            startActivityForResult(intent, PgpActivity.OPEN_PGP_BOUND)
        } else {
            serviceConnection = OpenPgpServiceConnection(this, providerPackageName, this)
            serviceConnection?.bindToService()

        }
    }

    override fun onBound(service: IOpenPgpService2) {
        initOpenPgpApi()
    }

    override fun onError(e: Exception) {
        TODO("Not yet implemented")
    }

    private fun initOpenPgpApi() {
        api = api ?: OpenPgpApi(this, serviceConnection!!.service!!)
    }

    /**
     * Shows a [Snackbar] with the provided [message] and [length]
     */
    fun showSnackbar(message: String, length: Int = Snackbar.LENGTH_SHORT) {
        runOnUiThread { Snackbar.make(findViewById(android.R.id.content), message, length).show() }
    }

    /**
     * Base handling of OpenKeychain errors based on the error contained in [result]
     */
    fun handleError(result: Intent) {
        // TODO show what kind of error it is
        /* For example:
         * No suitable key found -> no key in OpenKeyChain
         *
         * Check in open-pgp-lib how their definitions and error code
         */
        val error: OpenPgpError? = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR)
        if (error != null) {
            when (error.errorId) {
                OpenPgpError.OPPORTUNISTIC_MISSING_KEYS -> {
                    showSnackbar("")
                }
            }
            showSnackbar("Error from OpenKeyChain : " + error.message)
            Timber.e { "onError getErrorId: ${error.errorId}" }
            Timber.e { "onError getMessage: ${error.message}" }
        }
    }

    companion object {
        private const val TAG = "APS/BasePgpActivity"

        /**
         * Gets the relative path to the repository
         */
        fun getRelativePath(fullPath: String, repositoryPath: String): String =
            fullPath.replace(repositoryPath, "").replace("/+".toRegex(), "/")

        /**
         * Gets the Parent path, relative to the repository
         */
        fun getParentPath(fullPath: String, repositoryPath: String): String {
            val relativePath = getRelativePath(fullPath, repositoryPath)
            val index = relativePath.lastIndexOf("/")
            return "/${relativePath.substring(startIndex = 0, endIndex = index + 1)}/".replace("/+".toRegex(), "/")
        }

        /**
         * Gets the name of the password (excluding .gpg)
         */
        fun getName(fullPath: String): String {
            return FilenameUtils.getBaseName(fullPath)
        }

        /**
         * /path/to/store/social/facebook.gpg -> social/facebook
         */
        @JvmStatic
        fun getLongName(fullPath: String, repositoryPath: String, basename: String): String {
            var relativePath = getRelativePath(fullPath, repositoryPath)
            return if (relativePath.isNotEmpty() && relativePath != "/") {
                // remove preceding '/'
                relativePath = relativePath.substring(1)
                if (relativePath.endsWith('/')) {
                    relativePath + basename
                } else {
                    "$relativePath/$basename"
                }
            } else {
                basename
            }
        }
    }
}
