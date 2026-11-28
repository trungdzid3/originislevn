package com.originisle.android.bluetooth

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun presetFromId_resolvesAllTwentyHeadphonePresetsAndFallback() {
        assertEquals(20, BluetoothDevicePreset.entries.size)
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("tws"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.fromId("tws_white"))
        assertEquals(BluetoothDevicePreset.LENOVO_LE208, BluetoothDevicePreset.fromId("lenovo_le208"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.fromId("redmi_buds_6_play"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.fromId("over_ear"))
        assertEquals(BluetoothDevicePreset.AIRPODS_CLASSIC, BluetoothDevicePreset.fromId("airpods_1_2"))
        assertEquals(BluetoothDevicePreset.AIRPODS_3_4, BluetoothDevicePreset.fromId("airpods_3_4"))
        assertEquals(BluetoothDevicePreset.AIRPODS_PRO, BluetoothDevicePreset.fromId("airpods_pro"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_PLUS, BluetoothDevicePreset.fromId("galaxy_buds_plus"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_LIVE, BluetoothDevicePreset.fromId("galaxy_buds_live"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_2_PRO, BluetoothDevicePreset.fromId("galaxy_buds_2_pro"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_3, BluetoothDevicePreset.fromId("galaxy_buds_3"))
        assertEquals(BluetoothDevicePreset.VIVO_TWS_CLASSIC, BluetoothDevicePreset.fromId("vivo_tws_2_3"))
        assertEquals(BluetoothDevicePreset.VIVO_TWS_4, BluetoothDevicePreset.fromId("vivo_tws_4"))
        assertEquals(BluetoothDevicePreset.IQOO_TWS, BluetoothDevicePreset.fromId("iqoo_tws"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_AIR, BluetoothDevicePreset.fromId("oppo_enco_air"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_BUDS, BluetoothDevicePreset.fromId("oppo_enco_buds"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_X, BluetoothDevicePreset.fromId("oppo_enco_x"))
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_3, BluetoothDevicePreset.fromId("xiaomi_buds_3"))
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_4_5, BluetoothDevicePreset.fromId("xiaomi_buds_4_5"))
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
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Sony WF-1000XM5"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Unknown BT Device"))
    }

    @Test
    fun inferFromName_detectsAirPods() {
        assertEquals(BluetoothDevicePreset.AIRPODS_PRO, BluetoothDevicePreset.inferFromName("AirPods Pro"))
        assertEquals(BluetoothDevicePreset.AIRPODS_PRO, BluetoothDevicePreset.inferFromName("AirPod Pro 2"))
        assertEquals(BluetoothDevicePreset.AIRPODS_3_4, BluetoothDevicePreset.inferFromName("AirPods 3"))
        assertEquals(BluetoothDevicePreset.AIRPODS_3_4, BluetoothDevicePreset.inferFromName("AirPod 3rd Gen"))
        assertEquals(BluetoothDevicePreset.AIRPODS_3_4, BluetoothDevicePreset.inferFromName("AirPods 4 (ANC)"))
        assertEquals(BluetoothDevicePreset.AIRPODS_CLASSIC, BluetoothDevicePreset.inferFromName("Air Pods"))
        assertEquals(BluetoothDevicePreset.AIRPODS_CLASSIC, BluetoothDevicePreset.inferFromName("AirPods 2"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("AirPods Max"))
    }

    @Test
    fun inferFromName_detectsSamsungBuds() {
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_PLUS, BluetoothDevicePreset.inferFromName("Galaxy Buds+"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_PLUS, BluetoothDevicePreset.inferFromName("Samsung Buds Plus"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_PLUS, BluetoothDevicePreset.inferFromName("SM-R175"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_LIVE, BluetoothDevicePreset.inferFromName("Galaxy Buds Live"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_LIVE, BluetoothDevicePreset.inferFromName("SM-R180"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_2_PRO, BluetoothDevicePreset.inferFromName("Galaxy Buds2 Pro"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_2_PRO, BluetoothDevicePreset.inferFromName("Samsung Buds FE"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_2_PRO, BluetoothDevicePreset.inferFromName("SM-R510"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_3, BluetoothDevicePreset.inferFromName("Buds3 Pro"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_3, BluetoothDevicePreset.inferFromName("Galaxy Buds 3"))
        assertEquals(BluetoothDevicePreset.GALAXY_BUDS_3, BluetoothDevicePreset.inferFromName("SM-R630"))
    }

    @Test
    fun inferFromName_detectsVivoAndIqooTws() {
        assertEquals(BluetoothDevicePreset.VIVO_TWS_CLASSIC, BluetoothDevicePreset.inferFromName("Vivo TWS 2"))
        assertEquals(BluetoothDevicePreset.VIVO_TWS_CLASSIC, BluetoothDevicePreset.inferFromName("Vivo TWS 3 Pro"))
        assertEquals(BluetoothDevicePreset.VIVO_TWS_4, BluetoothDevicePreset.inferFromName("Vivo TWS 4"))
        assertEquals(BluetoothDevicePreset.VIVO_TWS_4, BluetoothDevicePreset.inferFromName("Vivo TWS Air 2"))
        assertEquals(BluetoothDevicePreset.VIVO_TWS_4, BluetoothDevicePreset.inferFromName("Vivo Earbuds 2"))
        assertEquals(BluetoothDevicePreset.IQOO_TWS, BluetoothDevicePreset.inferFromName("iQOO TWS 1e"))
        assertEquals(BluetoothDevicePreset.IQOO_TWS, BluetoothDevicePreset.inferFromName("iQOO Buds Dark"))
    }

    @Test
    fun inferFromName_detectsOppoEnco() {
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_AIR, BluetoothDevicePreset.inferFromName("Oppo Enco Air 3"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_BUDS, BluetoothDevicePreset.inferFromName("Enco Buds 2"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_BUDS, BluetoothDevicePreset.inferFromName("Enco W31 Đen"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_X, BluetoothDevicePreset.inferFromName("Enco Free3"))
        assertEquals(BluetoothDevicePreset.OPPO_ENCO_X, BluetoothDevicePreset.inferFromName("Oppo Enco X2 Black"))
    }

    @Test
    fun inferFromName_detectsXiaomiAndRedmiBuds() {
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_4_5, BluetoothDevicePreset.inferFromName("Xiaomi Buds 4 Pro"))
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_4_5, BluetoothDevicePreset.inferFromName("Xiaomi Buds 5"))
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_3, BluetoothDevicePreset.inferFromName("Xiaomi Buds 3T Pro"))
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_3, BluetoothDevicePreset.inferFromName("Mi True Wireless Silver"))
        assertEquals(BluetoothDevicePreset.XIAOMI_BUDS_3, BluetoothDevicePreset.inferFromName("Mi Air 2S"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Redmi Buds 5"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Redmi AirDots 3"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Redmi Buds 6 Pro"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Redmi Buds 6 Lite White"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Redmi Buds 5 Trắng"))
    }

    @Test
    fun inferFromName_detectsTwsWhite() {
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Tai nghe trắng"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Earbuds Pearl Edition"))
    }

    @Test
    fun inferFromName_detectsInteractive3DModels() {
        assertEquals(BluetoothDevicePreset.LENOVO_LE208, BluetoothDevicePreset.inferFromName("Lenovo LE208"))
        assertEquals(BluetoothDevicePreset.LENOVO_LE208, BluetoothDevicePreset.inferFromName("Lenovo LivePods TWS"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.inferFromName("Redmi Buds 6 Play"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.inferFromName("Redmi Buds6 Play"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.inferFromName("Redmi Buds 6"))
        assertEquals(BluetoothDevicePreset.REDMI_BUDS_6_PLAY, BluetoothDevicePreset.inferFromName("M2401E1"))
        assertTrue(BluetoothDevicePreset.LENOVO_LE208.isInteractive3D)
        assertTrue(BluetoothDevicePreset.REDMI_BUDS_6_PLAY.isInteractive3D)
        assertFalse(BluetoothDevicePreset.TWS_EARBUDS.isInteractive3D)
        assertFalse(BluetoothDevicePreset.AIRPODS_PRO.isInteractive3D)
    }

    @Test
    fun getDeviceConfig_usesAutoDetectByDefaultAndRespectsManualPreset() {
        val context = createInMemoryPrefsContext()
        val address = "AA:BB:CC:11:22:33"

        val autoConfig = BluetoothPopupStore.getDeviceConfig(context, address, "AirPods Pro")
        assertEquals(BluetoothDevicePreset.AIRPODS_PRO, autoConfig.preset)
        assertNull(autoConfig.customImagePath)
        assertNull(autoConfig.customName)
        assertTrue(autoConfig.enabled)

        BluetoothPopupStore.setDevicePreset(context, address, BluetoothDevicePreset.OVER_EAR)
        val manualConfig = BluetoothPopupStore.getDeviceConfig(context, address, "AirPods Pro")
        assertEquals(BluetoothDevicePreset.OVER_EAR, manualConfig.preset)
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

    @Test
    fun redmiCaseColorFromId_resolvesAllFourColorsAndFallback() {
        assertEquals(4, RedmiCaseColor.entries.size)
        assertEquals(RedmiCaseColor.ICE_BLUE, RedmiCaseColor.fromId("ice_blue"))
        assertEquals(RedmiCaseColor.WHITE, RedmiCaseColor.fromId("white"))
        assertEquals(RedmiCaseColor.BLACK, RedmiCaseColor.fromId("black"))
        assertEquals(RedmiCaseColor.PINK, RedmiCaseColor.fromId("pink"))
        assertEquals(RedmiCaseColor.ICE_BLUE, RedmiCaseColor.fromId(null))
        assertEquals(RedmiCaseColor.ICE_BLUE, RedmiCaseColor.fromId("unknown"))
    }

    @Test
    fun redmiCaseColor_getAndSetPersistThroughSharedPreferences() {
        val context = createInMemoryPrefsContext()
        assertEquals(RedmiCaseColor.ICE_BLUE, BluetoothPopupStore.getRedmiCaseColor(context))

        RedmiCaseColor.entries.forEach { expected ->
            BluetoothPopupStore.setRedmiCaseColor(context, expected)
            assertEquals(expected, BluetoothPopupStore.getRedmiCaseColor(context))
        }
    }

    private fun createInMemoryPrefsContext(): Context {
        val backing = mutableMapOf<String, MutableMap<String, Any?>>()
        return object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val store = backing.getOrPut(name) { mutableMapOf() }
                return object : SharedPreferences {
                    override fun getAll(): MutableMap<String, *> = store.toMutableMap()
                    override fun getString(key: String, defValue: String?): String? =
                        store[key] as? String ?: defValue
                    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
                        @Suppress("UNCHECKED_CAST")
                        (store[key] as? MutableSet<String>) ?: defValues
                    override fun getInt(key: String, defValue: Int): Int = store[key] as? Int ?: defValue
                    override fun getLong(key: String, defValue: Long): Long = store[key] as? Long ?: defValue
                    override fun getFloat(key: String, defValue: Float): Float = store[key] as? Float ?: defValue
                    override fun getBoolean(key: String, defValue: Boolean): Boolean =
                        store[key] as? Boolean ?: defValue
                    override fun contains(key: String): Boolean = store.containsKey(key)
                    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                        private val pending = mutableMapOf<String, Any?>()
                        private var clearAll = false

                        override fun putString(key: String, value: String?) = apply { pending[key] = value }
                        override fun putStringSet(key: String, values: MutableSet<String>?) = apply {
                            pending[key] = values?.toMutableSet()
                        }
                        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
                        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
                        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
                        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
                        override fun remove(key: String) = apply { pending[key] = null }
                        override fun clear() = apply { clearAll = true }
                        override fun commit(): Boolean {
                            apply()
                            return true
                        }
                        override fun apply() {
                            if (clearAll) store.clear()
                            pending.forEach { (k, v) ->
                                if (v == null) store.remove(k) else store[k] = v
                            }
                        }
                    }

                    override fun registerOnSharedPreferenceChangeListener(
                        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
                    ) = Unit

                    override fun unregisterOnSharedPreferenceChangeListener(
                        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
                    ) = Unit
                }
            }
        }
    }
}
