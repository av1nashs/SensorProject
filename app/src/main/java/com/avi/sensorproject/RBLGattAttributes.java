package com.avi.sensorproject;

/**
 * Created by Avi on 6/18/2016.
 */
import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
public class RBLGattAttributes {
    private static HashMap<String, String> attributes = new HashMap<String, String>();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String BLE_SHIELD_TX = "713D0003-503E-4C75-BA94-3148F18D941E";
    public static String BLE_SHIELD_RX = "713D0002-503E-4C75-BA94-3148F18D941E";
    public static String BLE_SHIELD_SERVICE = "713D0000-503E-4C75-BA94-3148F18D941E";

    static {
        // RBL Services.
        attributes.put("713D0000-503E-4C75-BA94-3148F18D941E",
                "BLE Shield Service");
        // RBL Characteristics.
        attributes.put(BLE_SHIELD_TX, "BLE Shield TX");
        attributes.put(BLE_SHIELD_RX, "BLE Shield RX");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
