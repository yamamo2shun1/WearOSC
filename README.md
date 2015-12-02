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
* Remote IP = Broadcast Address
* Remote Port = 8080

These values are changeable by 
