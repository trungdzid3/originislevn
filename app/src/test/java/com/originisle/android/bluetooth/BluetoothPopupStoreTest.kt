package com.originisle.android.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPopupStoreTest {

    @Test
    fun normalizeKey_macAddressAndDeviceName() {
        assertEquals("aa_bb_cc_11_22_33", BluetoothPopupStore.normalizeKey("AA:BB:CC:11:22:33"))
        assertEquals("airpods_pro_2nd_gen", BluetoothPopupStore.normalizeKey("  AirPods Pro (2nd Gen)! "))
        val fallback = BluetoothPopupStore.normalizeKey("---:::")
        assertTrue(fallback.matches(Regex("^[a-z0-9_]+$")))
    }

    @Test
    fun presetFromId_resolvesFiveHeadphonePresetsAndFallback() {
        assertEquals(5, BluetoothDevicePreset.entries.size)
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("tws"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.fromId("tws_white"))
        assertEquals(BluetoothDevicePreset.LENOVO_LE208, BluetoothDevicePreset.fromId("lenovo_le208"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.fromId("redmi_buds_6_play"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.fromId("over_ear"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("speaker"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("watch"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId(null))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("unknown_id"))
    }

    @Test
    fun inferFromName_detectsOverEar() {
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("Sony WH-1000XM5"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("AirPods Max"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("Bose QuietComfort 45"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("Tai nghe chụp tai Baseus"))
    }

    @Test
    fun inferFromName_detectsTwsDefault() {
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("AirPods Pro"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Sony WF-1000XM5"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Galaxy Buds2 Pro"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Unknown BT Device"))
    }

    @Test
    fun inferFromName_detectsTwsWhite() {
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Vivo TWS 4 White"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Tai nghe trắng"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Earbuds Pearl Edition"))
    }

    @Test
    fun inferFromName_detectsInteractive3DModels() {
        assertEquals(BluetoothDevicePreset.LENOVO_LE208, BluetoothDevicePreset.inferFromName("Lenovo LE208"))
        assertEquals(BluetoothDevicePreset.LENOVO_LE208, BluetoothDevicePreset.inferFromName("Lenovo LivePods TWS"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.inferFromName("Redmi Buds 6 Play"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.inferFromName("Redmi Buds6 Play"))
        assertTrue(BluetoothDevicePreset.LENOVO_LE208.isInteractive3D)
        assertTrue(BluetoothDevicePreset.REDMI_BUDS_6_PLAY.isInteractive3D)
        assertFalse(BluetoothDevicePreset.TWS_EARBUDS.isInteractive3D)
    }

    @Test
    fun isHeadphoneNameAllowed_blocksWatchesSpeakersAndOtherDevices() {
        // Watches & bands
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Galaxy Watch 6"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Mi Band 8"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Galaxy Fit 3"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Garmin Forerunner 265"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Đồng hồ thông minh"))

        // Speakers
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("JBL Flip 6"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Marshall Emberton"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Loa Bluetooth Mini"))

        // Laptops, cars, TVs, peripherals
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("MacBook Pro"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Laptop Dell XPS"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("CarKit BT"))
        assertFalse(BluetoothPopupStore.isHeadphoneNameAllowed("Sony Bravia TV"))
    }

    @Test
    fun isHeadphoneNameAllowed_allowsHeadphones() {
        assertTrue(BluetoothPopupStore.isHeadphoneNameAllowed("Lenovo LE208"))
        assertTrue(BluetoothPopupStore.isHeadphoneNameAllowed("Redmi Buds 6 Play"))
        assertTrue(BluetoothPopupStore.isHeadphoneNameAllowed("AirPods Pro"))
        assertTrue(BluetoothPopupStore.isHeadphoneNameAllowed("Sony WH-1000XM5"))
    }

    @Test
    fun popupThemeFromId_resolvesKnownAndFallback() {
        assertEquals(BluetoothPopupTheme.LIGHT, BluetoothPopupTheme.fromId("light"))
        assertEquals(BluetoothPopupTheme.SYSTEM, BluetoothPopupTheme.fromId("system"))
        assertEquals(BluetoothPopupTheme.DARK, BluetoothPopupTheme.fromId("dark"))
        assertEquals(BluetoothPopupTheme.SYSTEM, BluetoothPopupTheme.fromId(null))
        assertEquals(BluetoothPopupTheme.SYSTEM, BluetoothPopupTheme.fromId("unknown"))
    }
}
