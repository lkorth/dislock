package com.lukekorth.pebblelocker.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "AndroidWearDevices")
public class AndroidWearDevices extends Model {

    @Column(name = "name")
    public String name;

    @Column(name = "deviceId")
    public String deviceId;

    @Column(name = "trusted")
    public boolean trusted;

    public AndroidWearDevices() {
        super();
    }
}
