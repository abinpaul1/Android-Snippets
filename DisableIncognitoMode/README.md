# Disable Incognito Mode on Chrome Android [No Root]

Disabling incognito mode on android.
<br><br>
## Without Rooting

Note: This method requires you to be the 'device-owner' of your Android device. This priviledge level can be assigned to only a single package(app) at a time.<br>
By default, if you have connected your google account, google would be device-owner. In that case you will have to remove your google account before proceeding.<br>
Remove each account from "Accounts" in settings.<br>
Data on the device should be safe but keep a backup just in case.<br>

Now connect device to your computer. Make sure you have `adb` installed on your pc.<br>
https://www.xda-developers.com/install-adb-windows-macos-linux/

For a detailed setup of enabling Device Owner, refer
https://documentation.meraki.com/SM/Device_Enrollment/Enabling_Device_Owner_Mode_using_Android_Debug_Bridge_(ADB)

To set our app as device-owner,<br>
`adb shell dpm set-device-owner com.example.disableincognitomode/.DevAdminReceiver`<br>
Also grant the app device administartor permission, either by going to settings->Device Administrators or through adb<br>
`adb shell dpm set-active-admin com.example.disableincognitomode/.DevAdminReceiver`<br>

Then run the app once and reopen chrome to see it disabled.<br>
You can then add your Google Account again.<br>


## Rooted Phones
``` Create device_owner.xml
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<device-owner package="your.owner.app.package.id" name="Your app name" />
```
In the sample app's case package="com.example.disableincognitomode", name="Disable Incognito Mode"
Copy this file to /data/system/ and give permissions
Do it from terminal using the following commands:
```
cp /pathto/device_owner.xml /data/system/
cd /data/system/
chown system:system device_owner.xml
```
Note: This will replace Google as device owner. You may have to add your Google account again first time after restarting.
Also grant device administrator permissions from settings.

Run the app once and see incognito mode grayed out thereafter.


### Reference
https://stackoverflow.com/questions/21183328/how-to-make-my-app-a-device-owner<br>
https://github.com/android/enterprise-samples
