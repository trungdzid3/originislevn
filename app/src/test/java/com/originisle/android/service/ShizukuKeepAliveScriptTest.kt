package com.originisle.android.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuKeepAliveScriptTest {

    private val script = ShizukuKeepAliveHelper.buildSafeKeepAliveScript(
        pkg = "com.autonavi.minimap",
        uid = 10234,
        userId = 0,
    )

    @Test
    fun containsSafeKeepAliveCommands() {
        assertTrue(script.contains("deviceidle whitelist"))
        assertTrue(script.contains("set-standby-bucket"))
        assertTrue(script.contains("RUN_ANY_IN_BACKGROUND"))
        assertTrue(script.contains("allow_listener"))
        assertTrue(script.contains("OK|BUCKET="))
    }

    @Test
    fun forbidsSensitivePermissionGrants() {
        val forbidden = listOf(
            "pm grant",
            "RECEIVE_SMS",
            "RECORD_AUDIO",
            "ACCESS_FINE_LOCATION",
            "ACCESS_COARSE_LOCATION",
            "RECEIVE_SENSITIVE_NOTIFICATIONS",
            "BLUETOOTH_CONNECT",
            "BLUETOOTH_SCAN",
        )
        forbidden.forEach { token ->
            assertFalse("Script must not contain $token", script.contains(token))
        }
    }
}
