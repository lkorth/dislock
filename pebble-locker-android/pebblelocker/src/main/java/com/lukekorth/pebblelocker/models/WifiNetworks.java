package com.lukekorth.pebblelocker.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

@Table(name = "WifiNetworks")
public class WifiNetworks extends Model {

    @Column(name = "ssid")
    public String ssid;

    @Column(name = "trusted")
    public boolean trusted;

    public WifiNetworks() {
        super();
    }

    public static void setNetworkTrusted(String ssid, boolean trusted) {
        WifiNetworks network = new Select().from(WifiNetworks.class).where("ssid = ?", ssid).executeSingle();
        if (network == null) {
            network = new WifiNetworks();
            network.ssid = ssid;
        }
        network.trusted = trusted;
        network.save();
    }

    public static int countTrustedDevices() {
        return new Select().from(WifiNetworks.class)
                .where("trusted = ?", true)
                .count();
    }

}
