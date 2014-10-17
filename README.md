Dislock
=============

[![Travis Build Status](https://travis-ci.org/lkorth/pebble-locker.svg?branch=master)](https://travis-ci.org/lkorth/pebble-locker)

Dislock makes use of the [Device Administration API](http://developer.android.com/guide/topics/admin/device-admin.html) to control Android's lock screen.
Enjoy the use of your Android without a lock screen when your Pebble watch, selected Bluetooth devices or Wifi networks are connected and the security 
of a lock screen when you walk away causing your Pebble, other Bluetooth devices or Wifi networks to disconnect.

Available on the [Play Store](https://play.google.com/store/apps/details?id=com.lukekorth.pebblelocker)

## Device Support

Dislock supports:
* Pebble watches
* Bluetooth devices that maintain a constant connection and show up in your list of paired devices
* Android Wear
* Wifi networks (by SSID, multiple APs with the same SSID will be detected as a single network)
* Android devices using the built in VPN client

Dislock *does not* support:
* Bluetooth 4 (LE) devices like Fitbits
* Android devices that are encrypted or have other apps that require a pin or password to be set
* Pattern lock

## Uninstalling

To uninstall you must deactivate Dislock as a device admin either in Dislock or in your device's menu. You may or may not need to reboot.

## FAQ

*Q:* Help, I'm locked out, what do I do?

*A:* In the event that you become locked out you can visit the [Android Device Manager](https://www.google.com/android/devicemanager) and change the pin or password and unlock it using your new pin or password.

## Additional Info

Read more and comment on the [blog post](http://lukekorth.com/blog/pebble-locker/)

## License

Dislock is open source and available under the MIT license. See the [LICENSE](LICENSE) file for more info.
