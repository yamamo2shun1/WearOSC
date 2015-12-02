/**
 * Copylight (C) 2015, Shunichi Yamamoto, tkrworks.net
 *
 * This file is part of WearOSC.
 *
 * WearOSC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option ) any later version.
 *
 * WearOSC is distributed in the hope that it will be useful,
 * but WITHIOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WearOSC. if not, see <http:/www.gnu.org/licenses/>.
 *
 * WearOSC.java, v.0.5.0 2015/12/02
 */

package net.tkrworks.wearosc;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class WearOSC {
  private static final int MAX_BUF_SIZE    = 96;//64
  private static final int MAX_PACKET_SIZE = 1024;//192// 1024
  private static final int MAX_MESSAGE_LEN = 256;// 160
  private static final int MAX_ADDRESS_LEN = 64;
  private static final int MAX_ARGS_LEN    = 48;// 40

  public static final String SYSTEM_PREFIX   = "/sys";
  public static final String REMOTE_IP       = "/remote/ip";
  public static final String GET_REMOTE_IP   = "/sys/remote/ip/get";
  public static final String SET_REMOTE_IP   = "/sys/remote/ip/set";
  public static final String REMOTE_PORT     = "/remote/port";
  public static final String GET_REMOTE_PORT = "/sys/remote/port/get";
  public static final String SET_REMOTE_PORT = "/sys/remote/port/set";
  public static final String HOST_PORT       = "/remote/port";
  public static final String GET_HOST_PORT   = "/sys/host/port/get";
  public static final String SET_HOST_PORT   = "/sys/host/port/set";
  public static final String HOST_IP         = "/remote/port";
  public static final String GET_HOST_IP     = "/sys/host/ip/get";

  Context woscContext;

  private BluetoothAdapter mBluetoothAdapter;

  private DatagramSocket sndSocket;
  private DatagramSocket rcvSocket;
  private DatagramPacket sndPacket;
  private DatagramPacket rcvPacket;
  private boolean oscInitFlag = false;
  private String remoteIP = "192.168.1.255";
  private int remotePort = 8080;
  private String hostIP;
  private int hostPort = 8000;
  private boolean hasHostIP = false;
  private int oscTotalSize;
  private byte[] sndOSCData = new byte[MAX_MESSAGE_LEN];
  private byte[] rcvOSCData = new byte[MAX_PACKET_SIZE];
  private int indexA = 0;
  private int indexA0 = 0;
  private String rcvAddressStrings;
  private int rcvPrefixLength = 0;
  private int rcvAddressLength = 0;
  private int rcvTypesStartIndex = 0;
  private int rcvArgumentsLength = 0;
  private String rcvArgsTypeArray;
  private int[] rcvArgumentsStartIndex = new int[MAX_ARGS_LEN];
  private int[] rcvArgumentsIndexLength = new int[MAX_ARGS_LEN];

  public WearOSC(Context context) {
    woscContext = context;

    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Log.d("DEBUG", "bluetooth enabled = " + mBluetoothAdapter.isEnabled());
    mBluetoothAdapter.disable();
    Thread checkThread = new Thread() {
      public void run() {
        while(mBluetoothAdapter.isEnabled()) {
          try {
            Log.d("DEBUG", "waiting until bluetooth is disabled...");
            Thread.sleep(500);
          } catch(InterruptedException ie) {
            Log.e("EXCEPTION", "message", ie);
          }
        }
      }
    };
    checkThread.start();
    Log.d("DEBUG", "android wear's bluetooth is disabled!");

    hostIP = "0.0.0.0";

    Thread wifiCheckerThread = new Thread() {
      public void run() {
        int timeoutCount = 100;

        while(timeoutCount > 0 && hostIP.equals("0.0.0.0")) {
          try {
            Log.d("DEBUG", "waiting until ip address is gotten... " + timeoutCount);

            WifiManager mWifiManager = (WifiManager)woscContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            int ipAddress = mWifiInfo.getIpAddress();
            hostIP = ((ipAddress >> 0) & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." + ((ipAddress >> 24) & 0xFF);
            remoteIP = ((ipAddress >> 0) & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "." + ((ipAddress >> 16) & 0xFF) + ".255";

            Thread.sleep(1000);
            timeoutCount--;
          } catch (InterruptedException ie) {
            Log.e("EXCEPTION", "message", ie);
          }
        }
        Log.d("DEBUG", "HOST IP = " + hostIP + " remoteIP =  " + remoteIP);
        hasHostIP = true;
      }
    };
    wifiCheckerThread.start();
  }

  public void initOSCSocket() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Log.d("DEBUG", "init osc socket...");

          sndSocket = new DatagramSocket();
          rcvSocket = new DatagramSocket(hostPort);

          oscInitFlag = true;
        } catch(SocketException se) {
          Log.d("DEBUG", "failed socket...");
        }
      }
    }).start();
  }

  public boolean initializedOSCSocket() {
    return oscInitFlag;
  }

  public void closeOSCSocket() {
    if(sndSocket != null) {
      sndSocket.close();
      sndSocket = null;
    }

    if(rcvSocket != null) {
      rcvSocket.close();
      rcvSocket = null;
    }
  }

  public void releaseOSCPacket() {
    sndPacket = null;
    rcvPacket = null;
  }

  public void enableBluetoothAdapter() {
    mBluetoothAdapter.enable();
  }

  public void setOSCRemoteIP(String ip) {
    remoteIP = ip;
  }

  public String getOSCRemoteIP() {
    return remoteIP;
  }

  public void setOSCRemotePort(int port) {
    remotePort = port;
  }

  public int getOSCRemotePort() {
    return remotePort;
  }

  public String getOSCHostIP() {
    return hostIP;
  }

  public void setOSCHostPort(int port) {
    hostPort = port;
  }

  public int getOSCHostPort() {
    return hostPort;
  }

  public boolean isHasOSCHostIP() {
    return hasHostIP;
  }

  public void setOSCAddress(String prefix, String command) {
    int prefixSize = prefix.length();
    int commandSize = command.length();
    int addressSize = prefixSize + commandSize;
    int zeroSize = 0;

    Arrays.fill(sndOSCData, (byte) 0);
    oscTotalSize = 0;

    byte[] oscAddress = String.format("%s%s", prefix, command).getBytes();
    for(int i = 0; i < oscAddress.length; i++)
      sndOSCData[i] = oscAddress[i];

    zeroSize = (addressSize ^ ((addressSize >> 3) << 3)) == 0 ? 0 : 8 - (addressSize ^ ((addressSize >> 3) << 3));
    if(zeroSize == 0)
      zeroSize = 4;
    else if(zeroSize > 4 && zeroSize < 8)
      zeroSize -= 4;

    oscTotalSize = (addressSize + zeroSize);
  }

  public void setOSCTypeTag(String type) {
    int typeSize = type.length();
    int zeroSize = 0;

    byte[] oscTypeTag = String.format(",%s", type).getBytes();
    for(int i = 0; i < oscTypeTag.length; i++)
      sndOSCData[oscTotalSize + i] = oscTypeTag[i];

    typeSize++;
    zeroSize = (typeSize ^ ((typeSize >> 2) << 2)) == 0 ? 0 : 4 - (typeSize ^ ((typeSize >> 2) << 2));
    if(zeroSize == 0)
      zeroSize = 4;

    oscTotalSize += (typeSize + zeroSize);
  }

  public void addOSCIntArgument(int value) {
    sndOSCData[oscTotalSize++] = (byte)((value >> 24) & 0xFF);
    sndOSCData[oscTotalSize++] = (byte)((value >> 16) & 0xFF);
    sndOSCData[oscTotalSize++] = (byte)((value >> 8) & 0xFF);
    sndOSCData[oscTotalSize++] = (byte)((value >> 0) & 0xFF);
  }

  public void addOSCFloatArgument(float value) {
    int bits = Float.floatToIntBits(value);
    sndOSCData[oscTotalSize++] = (byte)((bits >> 24) & 0xFF);
    sndOSCData[oscTotalSize++] = (byte)((bits >> 16) & 0xFF);
    sndOSCData[oscTotalSize++] = (byte)((bits >> 8) & 0xFF);
    sndOSCData[oscTotalSize++] = (byte)(bits & 0xFF);
  }

  public void addOSCStringArgument(String str) {
    int oscStrArgumentLength = str.length();
    byte[] oscStrArgument = str.getBytes();
    for(int i = 0; i < oscStrArgument.length; i++)
      sndOSCData[oscTotalSize + i] = (byte)(oscStrArgument[i] & 0xFF);

    oscTotalSize += ((oscStrArgumentLength / 4) + 1) * 4;
  }

  public void clearOSCMessage() {
    Arrays.fill(sndOSCData, (byte)0);
    oscTotalSize = 0;
  }

  public void flushOSCMessage() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          //Log.d("DEBUG", "flush osc message...");

          InetAddress remoteAddr = InetAddress.getByName(remoteIP);

          sndPacket = new DatagramPacket(sndOSCData, oscTotalSize, remoteAddr, remotePort);
          sndSocket.send(sndPacket);
        } catch(UnknownHostException uhe) {
          Log.d("DEBUG", "failed sending... 0");
        } catch(IOException ioe) {
          Log.d("DEBUG", "failed sending... 1");
        }
      }
    }).start();
  }

  public void receiveOSCMessage() {
    rcvPacket = new DatagramPacket(rcvOSCData, rcvOSCData.length);

    try {
      rcvSocket.receive(rcvPacket);
    } catch(NullPointerException npe) {
      Log.d("DEBUG", "socket closed... 0");
    } catch(SocketException se) {
      //Log.d("EXCEPTION", "message", se);
      Log.d("DEBUG", "socket closed... 1");
    } catch(IOException ioe) {
      Log.e("EXCEPTION", "message", ioe);
    }
  }

  public boolean extractAddressFromOSCPacket() {
    indexA = 3;

    while(rcvOSCData[indexA] != 0) {
      indexA += 4;
      if(indexA >= MAX_PACKET_SIZE)
        return false;
    }

    indexA0 = indexA;
    indexA--;

    while(rcvOSCData[indexA] == 0) {
      indexA--;
      if(indexA < 0)
        return false;
    }
    indexA++;

    rcvAddressStrings = new String(rcvOSCData, 0, indexA);

    rcvAddressLength = indexA;

    return true;
  }

  public boolean extractTypeTagFromOSCPacket() {
    rcvTypesStartIndex = indexA0 + 1;
    indexA = indexA0 + 2;

    while(rcvOSCData[indexA] != 0) {
      indexA++;
      if(indexA >= MAX_PACKET_SIZE)
        return false;
    }
    Log.d("DEBUG", "index = " + rcvTypesStartIndex + " " + indexA);
    rcvArgsTypeArray = new String(rcvOSCData, rcvTypesStartIndex + 1, indexA - rcvTypesStartIndex - 1);

    return true;
  }

  public boolean extractArgumentsFromOSCPacket() {
    int u, length = 0;

    if(indexA == 0 || indexA >= MAX_PACKET_SIZE)
      return false;

    rcvArgumentsLength = indexA - rcvTypesStartIndex;
    int n = ((rcvArgumentsLength / 4) + 1) * 4;

    if(rcvArgumentsLength == 0)
      return false;

    for(int i = 0; i < rcvArgumentsLength - 1; i++) {
      rcvArgumentsStartIndex[i] = rcvTypesStartIndex + length + n;
      switch(rcvArgsTypeArray.substring(i, i + 1)) {
        case "i":
        case "f":
          length += 4;
          rcvArgumentsIndexLength[i] = 4;
          break;
        case "s":
          u = 0;
          while(rcvOSCData[rcvArgumentsStartIndex[i] + u] != 0) {
            u++;
            if((rcvArgumentsStartIndex[i] + u) >= MAX_PACKET_SIZE)
              return false;
          }

          rcvArgumentsIndexLength[i] = ((u / 4) + 1) * 4;

          length += rcvArgumentsIndexLength[i];
          break;
        default: // T, F,N,I and others
          break;
      }
    }
    return true;
  }

  public String getOSCAddress() {
    return rcvAddressStrings;
  }

  public int getArgumentsLength() {
    return rcvArgumentsLength - 1;
  }

  public int getIntArgumentAtIndex(int index) {
    int s = 0;
    int sign = 0, exponent = 0, mantissa = 0;
    int lvalue = 0;
    float fvalue = 0.0f;
    float sum = 0.0f;

    if(index >= rcvArgumentsLength - 1)
      return 0;

    switch(rcvArgsTypeArray.substring(index, index + 1)) {
      case "i":
        lvalue = ((rcvOSCData[rcvArgumentsStartIndex[index]] & 0xFF) << 24) |
            ((rcvOSCData[rcvArgumentsStartIndex[index] + 1] & 0xFF) << 16) |
            ((rcvOSCData[rcvArgumentsStartIndex[index] + 2] & 0xFF) << 8) |
            (rcvOSCData[rcvArgumentsStartIndex[index] + 3] & 0xFF);
        break;
      case "f":
        lvalue = ((rcvOSCData[rcvArgumentsStartIndex[index]] & 0xFF) << 24) |
            ((rcvOSCData[rcvArgumentsStartIndex[index] + 1] & 0xFF) << 16) |
            ((rcvOSCData[rcvArgumentsStartIndex[index] + 2] & 0xFF) << 8) |
            (rcvOSCData[rcvArgumentsStartIndex[index] + 3] & 0xFF);
        lvalue &= 0xffffffff;

        sign = (((lvalue >> 31) & 0x01) == 0x01) ? -1 : 1;
        exponent = ((lvalue >> 23) & 0xFF) - 127;
        mantissa = lvalue & 0x7FFFFF;

        for(s = 0; s < 23; s++) {
          int onebit = (mantissa >> (22 - s)) & 0x1;
          sum += (float)onebit * (1.0 / (float)(1 << (s + 1)));
        }
        sum += 1.0;

        if(exponent >= 0)
          fvalue = sign * sum * (1 << exponent);
        else
          fvalue = sign * sum * (1.0f / (float)(1 << Math.abs(exponent)));
        lvalue = (int)fvalue;
        break;
    }
    return lvalue;
  }

  public float getFloatArgumentAtIndex(int index) {
    int s = 0;
    int sign = 0, exponent = 0, mantissa = 0;
    int lvalue = 0;
    float fvalue = 0.0f;
    float sum = 0.0f;

    if(index >= rcvArgumentsLength - 1)
      return 0.0f;

    switch(rcvArgsTypeArray.substring(index, index + 1)) {
      case "i":
        lvalue = (rcvOSCData[rcvArgumentsStartIndex[index]] << 24) |
            (rcvOSCData[rcvArgumentsStartIndex[index] + 1] << 16) |
            (rcvOSCData[rcvArgumentsStartIndex[index] + 2] << 8) |
            rcvOSCData[rcvArgumentsStartIndex[index] + 3];
        fvalue = (float)lvalue;
        break;
      case "f":
        lvalue = ((rcvOSCData[rcvArgumentsStartIndex[index]] & 0xFF) << 24) |
            ((rcvOSCData[rcvArgumentsStartIndex[index] + 1] & 0xFF) << 16) |
            ((rcvOSCData[rcvArgumentsStartIndex[index] + 2] & 0xFF) << 8) |
            (rcvOSCData[rcvArgumentsStartIndex[index] + 3] & 0xFF);
        lvalue &= 0xffffffff;

        sign = (((lvalue >> 31) & 0x01) == 0x01) ? -1 : 1;
        exponent = ((lvalue >> 23) & 0xFF) - 127;
        mantissa = lvalue & 0x7FFFFF;

        for(s = 0; s < 23; s++) {
          int onebit = (mantissa >> (22 - s)) & 0x1;
          sum += (float)onebit * (1.0 / (float)(1 << (s + 1)));
        }
        sum += 1.0;

        if(exponent >= 0)
          fvalue = sign * sum * (1 << exponent);
        else
          fvalue = sign * sum * (1.0f / (float)(1 << Math.abs(exponent)));
        break;
    }
    return fvalue;
  }

  public String getStringArgumentAtIndex(int index) {
    if(index >= rcvArgumentsLength - 1)
      return "error";

    switch(rcvArgsTypeArray.substring(index, index + 1)) {
      case "i":
      case "f":
        return "error";
      case "s":
        int strLen = 0;
        while(rcvOSCData[rcvArgumentsStartIndex[index] + strLen] != 0)
          strLen++;

        return new String(rcvOSCData, rcvArgumentsStartIndex[index], strLen);
    }
    return "error";
  }

  public boolean getBooleanArgumentAtIndex(int index) {
    boolean flag = false;

    if(index >= rcvArgumentsLength - 1)
      return flag;

    switch(rcvArgsTypeArray.substring(index, index + 1)) {
      case "T":
        flag = true;
        break;
      case "F":
        flag = false;
        break;
    }
    return flag;
  }
}
