/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.zeapo.pwdstore.crypto

import android.content.ClipData
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.zeapo.pwdstore.R
import com.zeapo.pwdstore.databinding.DecryptLayoutBinding

class DecryptActivity : BasePgpActivity() {
    private lateinit var binding: DecryptLayoutBinding

    private val relativeParentPath by lazy { getParentPath(fullPath, repoPath) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DecryptLayoutBinding.inflate(layoutInflater)
        with(binding) {
            setContentView(root)
            passwordCategory.text = relativeParentPath
            passwordFile.text = name
            passwordFile.setOnLongClickListener {
                val clipboard = clipboard ?: return@setOnLongClickListener false
                val clip = ClipData.newPlainText("pgp_handler_result_pm", name)
                clipboard.setPrimaryClip(clip)
                showSnackbar(resources.getString(R.string.clipboard_username_toast_text))
                true
            }
            try {
                passwordLastChanged.text =  resources.getString(R.string.last_changed, lastChangedString)
            } catch (e: RuntimeException) {
                passwordLastChanged.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pgp_handler, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
