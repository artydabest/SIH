# Fix "cannot find symbol class DeviceDetailsActivity"

The project fails to build because `MainActivity.java` attempts to launch `DeviceDetailsActivity`, but the class and its registration are missing from the project.

## User Review Required

> [!NOTE]
> I will be creating a new Activity class and a corresponding layout file to resolve the build error. This will also involve registering the new activity in `AndroidManifest.xml`.

## Proposed Changes

### [app]

Summary: Create the missing `DeviceDetailsActivity`, its layout, and register it in the manifest.

#### [NEW] [DeviceDetailsActivity.java](file:///C:/Users/HP/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/DeviceDetailsActivity.java)
Create a new activity class that displays the device details passed from `MainActivity`.

#### [NEW] [activity_device_details.xml](file:///C:/Users/HP/AndroidStudioProjects/MyApplication4/app/src/main/res/layout/activity_device_details.xml)
Create a new layout file for `DeviceDetailsActivity` to display the information.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/HP/AndroidStudioProjects/MyApplication4/app/src/main/AndroidManifest.xml)
Register `DeviceDetailsActivity` in the manifest so it can be launched.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugJavaWithJavac` to verify the build error is resolved.

### Manual Verification
- Deploy the app to a device/emulator.
- Tap "SIMULATE NEARBY DEVICES".
- Tap "OPEN DEVICE" on one of the cards to verify `DeviceDetailsActivity` opens and displays the correct information.
