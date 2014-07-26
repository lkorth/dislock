package com.lukekorth.pebblelocker.test;

import com.activeandroid.query.Select;
import com.google.android.gms.wearable.Node;
import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

import java.util.List;

public class AndroidWearHelperTest extends BaseApplicationTestCase {

    private AndroidWearHelper mAndroidWearHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAndroidWearHelper = new AndroidWearHelper(getContext(), new Logger(getContext(), "TEST"));
    }

    public void testAddDeviceCreatesNewDevice() {
        Node node = new TestNode("id1", "name1");

        mAndroidWearHelper.addDevice(node);

        AndroidWearDevices device = getDevice("id1");
        assertEquals("id1", device.deviceId);
        assertEquals("name1", device.name);
    }

    public void testSetDeviceTrustedCorrectlySavesDevice() {
        mAndroidWearHelper.setDeviceTrusted("testName", "testId", true);

        assertTrue(getDevice("testId").trusted);

        mAndroidWearHelper.setDeviceTrusted("testName", "testId", false);

        assertFalse(getDevice("testId").trusted);
    }

    public void testIsTrustedDeviceConnectedReturnsFalseWhenNoTrustedDeviceIsConnected() {
        mAndroidWearHelper.setDeviceTrusted("testDevice1", "testDevice1", true);
    }

    public void testGetEncountedDevicesReturnsAllDevices() {
        AndroidWearDevices device1 = createAndroidWearDevice("device1", "device1", true);
        AndroidWearDevices device2 = createAndroidWearDevice("device2", "device2", false);

        List<AndroidWearDevices> result = mAndroidWearHelper.getEncounteredDevices();

        assertEquals(2, result.size());
        assertTrue(result.contains(device1));
        assertTrue(result.contains(device2));
    }

    private AndroidWearDevices getDevice(String id) {
        return new Select()
                .from(AndroidWearDevices.class)
                .where("deviceId = ?", id)
                .executeSingle();
    }

    private static class TestNode implements Node {

        private String mId;
        private String mName;

        public TestNode(String id, String name) {
            mId = id;
            mName = name;
        }

        @Override
        public String getId() {
            return mId;
        }

        @Override
        public String getDisplayName() {
            return mName;
        }
    }
}
