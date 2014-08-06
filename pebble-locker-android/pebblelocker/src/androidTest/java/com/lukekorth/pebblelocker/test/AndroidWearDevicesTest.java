package com.lukekorth.pebblelocker.test;

import com.google.android.gms.wearable.Node;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

import java.util.List;

public class AndroidWearDevicesTest extends BaseApplicationTestCase {

    public void testGetDevicesReturnsEmptyListIfNoDevices() {
        assertEquals(0, AndroidWearDevices.getDevices().size());
    }

    public void testGetDevicesReturnsAllDevices() {
        AndroidWearDevices device = createAndroidWearDevice("test", "1", false, false);

        List<AndroidWearDevices> devices = AndroidWearDevices.getDevices();

        assertEquals(1, devices.size());
        assertEquals(device, devices.get(0));
    }

    public void testIsTrustedDeviceConnectedReturnsFalseWhenNoTrustedDeviceConnected() {
        assertFalse(AndroidWearDevices.isTrustedDeviceConnected());
    }

    public void testIsTrustedDeviceConnectedReturnsTrueWhenTrustedDeviceIsConnected() {
        createAndroidWearDevice("test", "1", true, true);

        assertTrue(AndroidWearDevices.isTrustedDeviceConnected());
    }

    public void testSetDeviceConnectedCreatesDeviceIfItDoesNotExist() {
        assertEquals(0, AndroidWearDevices.getDevices().size());
        Node node = new TestNode("test-3443", "3443");

        AndroidWearDevices.setDeviceConnected(node, true);

        List<AndroidWearDevices> devices = AndroidWearDevices.getDevices();
        assertEquals(1, devices.size());
        assertEquals("test-3443", devices.get(0).name);
        assertEquals("3443", devices.get(0).deviceId);
        assertEquals(true, devices.get(0).connected);
        assertEquals(false, devices.get(0).trusted);
    }

    public void testSetDeviceConnectedSetsConnectedCorrectly() {
        createAndroidWearDevice("test", "id", false, false);
        Node node = new TestNode("test", "id");

        AndroidWearDevices.setDeviceConnected(node, true);

        List<AndroidWearDevices> devices = AndroidWearDevices.getDevices();
        assertEquals(1, devices.size());
        assertEquals("test", devices.get(0).name);
        assertEquals("id", devices.get(0).deviceId);
        assertEquals(true, devices.get(0).connected);
        assertEquals(false, devices.get(0).trusted);
    }

    public void testGetConnectionStatusReturnsCorrectStatus() {
        String result = AndroidWearDevices.getConnectionStatus();
        assertNull(result);

        createAndroidWearDevice("device1", "1", true, true);
        result = AndroidWearDevices.getConnectionStatus();
        assertEquals("Android Wear Connected", result);
    }

    private static class TestNode implements Node {
        private String mName;
        private String mId;

        public TestNode(String name, String id) {
            mName = name;
            mId = id;
        }
        @Override
        public String getDisplayName() {
            return mName;
        }
        @Override
        public String getId() {
            return mId;
        }
    }

}
