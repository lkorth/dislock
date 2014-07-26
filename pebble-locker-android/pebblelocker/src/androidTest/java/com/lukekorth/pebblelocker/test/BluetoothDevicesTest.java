package com.lukekorth.pebblelocker.test;

import com.lukekorth.pebblelocker.models.BluetoothDevices;

import java.util.List;

public class BluetoothDevicesTest extends BaseApplicationTestCase {

    public void testIsTrustedDeviceConnectedReturnsFalseWhenNoTrustedDeviceConnected() {
        assertFalse(BluetoothDevices.isTrustedDeviceConnected());
    }

    public void testIsTrustedDeviceConnectedReturnsTrueWhenTrustedDeviceIsConnected() {
        createBluetoothDevice("test", "1", true, true);

        assertTrue(BluetoothDevices.isTrustedDeviceConnected());
    }

    public void testGetConnectedDevicesReturnsAListOfConnectedDevices() {
        BluetoothDevices first = createBluetoothDevice("test1", "1", true, false);
        createBluetoothDevice("test2", "2", false, true);
        BluetoothDevices second = createBluetoothDevice("test3", "3", true, false);

        List<BluetoothDevices> devices = BluetoothDevices.getConnectedDevices();

        assertEquals(2, devices.size());
        assertTrue(devices.contains(first));
        assertTrue(devices.contains(second));
    }

    public void testGetConnectionStatusReturnsCorrectStatus() {
        createBluetoothDevice("device1", "1", true, true);
        createBluetoothDevice("device2", "2", true, true);
        createBluetoothDevice("device3", "3", false, false);

        String result = BluetoothDevices.getConnectionStatus();

        assertEquals("device1 connected\ndevice2 connected", result);
    }

}
