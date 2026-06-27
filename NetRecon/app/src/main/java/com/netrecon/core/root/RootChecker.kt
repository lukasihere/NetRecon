package com.netrecon.core.root

import java.io.File

object RootChecker {

    fun isDeviceRooted(): Boolean {
        return checkSuBinary() || checkRootApps() || checkRWPaths()
    }

    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/su", "/system/bin/.ext/.su", "/system/usr/we-need-root/su-backup",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootApps(): Boolean {
        val rootApps = arrayOf(
            "com.noshufou.android.su", "com.noshufou.android.su.elite",
            "eu.chainfire.supersu", "com.koushikdutta.superuser",
            "com.thirdparty.superuser", "com.yellowes.su",
            "com.topjohnwu.magisk", "com.kingroot.kinguser",
            "com.kingo.root", "com.smedialink.oneclickroot"
        )
        return rootApps.any { app ->
            try {
                File("/data/data/$app").exists()
            } catch (e: Exception) { false }
        }
    }

    private fun checkRWPaths(): Boolean {
        val paths = arrayOf("/system", "/system/bin", "/system/sbin", "/system/xbin",
            "/vendor/bin", "/sys", "/sbin", "/etc")
        return paths.any { path ->
            try {
                val process = Runtime.getRuntime().exec("mount")
                val output = process.inputStream.bufferedReader().readText()
                output.contains("$path ") && (output.contains(" rw,") || output.contains(" rw "))
            } catch (e: Exception) { false }
        }
    }

    fun executeRootCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (output.isNotEmpty()) output else error
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun canExecuteRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }
}
