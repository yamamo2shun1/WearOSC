# WearOSC
Open Sound Control App for Android Wear  
This app can send the following touch gestures via the Open Sonud Control.

1. One finger tap `/wosc/touch [x(float)] [y(float)] [state:1(int)]`
1. One finger slide `/wosc/touch [x(float)] [y(float)] [state:-1(int)]`
1. One finger double-tap `/wosc/touch [x(float)] [y(float)] [state:3(int)]`
1. Two finger tap `/wosc/touch [x(float)] [y(float)] [state:2(int)]`
1. Two finger slide `/wosc/touch [x(float)] [y(float)] [state:-2(int)]`

## Network Configuration
The default settings are as follows.

* Host Port = 8000
* Remote IP = Broadcast Address in your connected Wi-Fi network.
* Remote Port = 8080

These values can be changed and confirmed by the following OSC messages.

1. `/sys/host/ip/get`
1. `/sys/host/port/set [port(int)]`
1. `/sys/host/port/get`
1. `/sys/remote/ip/set [ip(String)]`
1. `/sys/remote/ip/get`
1. `/sys/remote/port/set [port(int)]`
1. `/sys/remote/port/get`
