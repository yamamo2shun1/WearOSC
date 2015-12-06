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
 * MainActivity.java, v.0.6.1 2015/12/06
 */

package net.tkrworks.wearosc;

import android.os.*;
import android.os.Process;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends WearableActivity {
  private static final int DEVICE_ID = 0;

  private static final int TOUCH_NONE             = 0;
  private static final int ONE_FINGER_SINGLE_TAP  = 1;
  private static final int TWO_FINGERS_SINGLE_TAP = 2;
  private static final int ONE_FINGER_DOUBLE_TAP  = 3;
  private static final int ONE_FINGER_SLIDE       = -1;
  private static final int TWO_FINGERS_SLIDE      = -2;

  private Handler mHandler;
  private RelativeLayout mContainerView;
  private ImageView mBackgroudView;
  private TextView mStateView;

  private WearOSC wosc;

  private int multiTouchNum = 0;
  private int prevMultiTouchNum = 0;
  private int repeatTapNum = 0;
  private int pointerMoveCount = 0;
  private boolean bStartFadeout = false;
  private float touchStateTextAlpha = 0.0f;
  private float prevTouchX = 0.0f;
  private float prevTouchY = 0.0f;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setAmbientEnabled();

    mHandler = new Handler();

    mContainerView = (RelativeLayout)findViewById(R.id.container);
    mBackgroudView = (ImageView)findViewById(R.id.background);
    mStateView = (TextView)findViewById(R.id.state);

    wosc = new WearOSC(this);
    wosc.initOSCSocket();

    Thread rcvOSCThread = new Thread() {
      public void run() {
        while(true) {
          if(wosc.initializedOSCSocket()) {
            wosc.receiveOSCMessage();
            if(!wosc.extractAddressFromOSCPacket())
              continue;
            if(!wosc.extractTypeTagFromOSCPacket())
              continue;
            if(!wosc.extractArgumentsFromOSCPacket())
              continue;

            if(wosc.getOSCAddress().equals(WearOSC.GET_REMOTE_IP)) {
              Log.d("DEBUG", "current remote IP = " + wosc.getOSCRemoteIP());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.REMOTE_IP);
              wosc.setOSCTypeTag("is");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCStringArgument(wosc.getOSCRemoteIP());
              wosc.flushOSCMessage();
            }
            else if(wosc.getOSCAddress().equals(WearOSC.SET_REMOTE_IP)) {
              String prevIP = wosc.getOSCRemoteIP();
              Log.d("DEBUG", "current remote IP = " + wosc.getOSCRemoteIP());
              wosc.setOSCRemoteIP(wosc.getStringArgumentAtIndex(0));
              Log.d("DEBUG", "new remote IP = " + wosc.getOSCRemoteIP());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.REMOTE_IP);
              wosc.setOSCTypeTag("is");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCStringArgument("CHANGED:" + prevIP + "=>" + wosc.getOSCRemoteIP());
              wosc.flushOSCMessage();
            }
            else if(wosc.getOSCAddress().equals(WearOSC.GET_REMOTE_PORT)) {
              Log.d("DEBUG", "current remote port = " + wosc.getOSCRemotePort());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.REMOTE_PORT);
              wosc.setOSCTypeTag("ii");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCIntArgument(wosc.getOSCRemotePort());
              wosc.flushOSCMessage();
            }
            else if(wosc.getOSCAddress().equals(WearOSC.SET_REMOTE_PORT)) {
              int prevPort = wosc.getOSCRemotePort();
              Log.d("DEBUG", "current remote port = " + wosc.getOSCRemotePort());
              wosc.setOSCRemotePort(wosc.getIntArgumentAtIndex(0));
              Log.d("DEBUG", "new remote port = " + wosc.getOSCRemotePort());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.REMOTE_PORT);
              wosc.setOSCTypeTag("is");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCStringArgument("CHANGED:" + prevPort + "=>" + wosc.getOSCRemotePort());
              wosc.flushOSCMessage();
            }
            else if(wosc.getOSCAddress().equals(WearOSC.GET_HOST_PORT)) {
              Log.d("DEBUG", "current host port = " + wosc.getOSCHostPort());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.HOST_PORT);
              wosc.setOSCTypeTag("ii");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCIntArgument(wosc.getOSCHostPort());
              wosc.flushOSCMessage();
            }
            else if(wosc.getOSCAddress().equals(WearOSC.SET_HOST_PORT)) {
              int prevPort = wosc.getOSCHostPort();
              Log.d("DEBUG", "current host port = " + wosc.getOSCHostPort());
              wosc.setOSCHostPort(wosc.getIntArgumentAtIndex(0));
              Log.d("DEBUG", "new host port = " + wosc.getOSCHostPort());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.HOST_PORT);
              wosc.setOSCTypeTag("is");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCStringArgument("CHANGED:" + prevPort + "=>" + wosc.getOSCHostPort());
              wosc.flushOSCMessage();
            }
            else if(wosc.getOSCAddress().equals(WearOSC.GET_HOST_IP)) {
              Log.d("DEBUG", "current host port = " + wosc.getOSCHostIP());

              wosc.setOSCAddress(WearOSC.SYSTEM_PREFIX, WearOSC.HOST_IP);
              wosc.setOSCTypeTag("is");
              wosc.addOSCIntArgument(DEVICE_ID);
              wosc.addOSCStringArgument(wosc.getOSCHostIP());
              wosc.flushOSCMessage();
            }

            //Log.d("DEBUg", "rcv data = " + rcvAddressStrings + " " + rcvArgsTypeArray + " " + getIntArgumentAtIndex(0) + " " + getStringArgumentAtIndex(1) + " " + getFloatArgumentAtIndex(2));
          }
        }
      }
    };
    rcvOSCThread.start();

    Thread fadeoutText = new Thread() {
      public void run() {
        int count = 0;
        while(true) {
          if(bStartFadeout) {
            mHandler.post(new Runnable() {
              @Override
              public void run() {
                //Log.d("DEBUG", "alpha = " + touchStateTextAlpha);
                touchStateTextAlpha -= 0.05f;
                mStateView.setAlpha(touchStateTextAlpha);

                if (touchStateTextAlpha <= 0.0f)
                  bStartFadeout = false;
              }
            });

            try {
              Thread.sleep(50);
            } catch (InterruptedException ie) {
              Log.e("EXCEPTION", "message", ie);
            }
          }
          if(!wosc.isHasOSCHostIP()) {
            switch(count) {
              case 0:
                mHandler.post(new Runnable() {
                  @Override
                  public void run() {
                    mStateView.setText("Getting IP.");
                  }
                });
                count++;
                break;
              case 1:
                mHandler.post(new Runnable() {
                  @Override
                  public void run() {
                    mStateView.setText("Getting IP..");
                  }
                });
                count++;
                break;
              case 2:
                mHandler.post(new Runnable() {
                  @Override
                  public void run() {
                    mStateView.setText("Getting IP...");
                  }
                });
                count = 0;
                break;
            }
          }
          else if(wosc.isHasOSCHostIP() && count < 3) {
            mHandler.post(new Runnable() {
              @Override
              public void run() {
                mStateView.setText("WearOSC");
              }
            });
            count = 100;
          }
        }
      }
    };
    fadeoutText.start();
  }

  @Override
  protected void onPause() {
    super.onPause();

    Log.d("DEBUG", "pause...");
  }

  @Override
  protected void onResume() {
    super.onResume();

    Log.d("DEBUG", "resume...");
  }

  @Override
  protected void onStop() {
    super.onStop();

    wosc.closeOSCSocket();
    wosc.releaseOSCPacket();
    wosc.enableBluetoothAdapter();

    Log.d("DEBUG", "stop...");

    android.os.Process.killProcess(Process.myPid());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    Log.d("DEBUG", "destroy...");
  }

  @Override
  public void onEnterAmbient(Bundle ambientDetails) {
    super.onEnterAmbient(ambientDetails);
    updateDisplay();
  }

  @Override
  public void onUpdateAmbient() {
    super.onUpdateAmbient();
    updateDisplay();
  }

  @Override
  public void onExitAmbient() {
    updateDisplay();
    super.onExitAmbient();
  }

  private void updateDisplay() {
    if (isAmbient()) {
      mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
    } else {
      mContainerView.setBackground(null);
    }
  }

  private void setTouchState(final int num) {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        switch(num) {
          case TWO_FINGERS_SLIDE:
            mStateView.setText("2 Fingers Slide");
            break;
          case ONE_FINGER_SLIDE:
            mStateView.setText("1 Finger Slide");
            break;
          case TOUCH_NONE:
            switch(prevMultiTouchNum) {
              case ONE_FINGER_SINGLE_TAP:
                mStateView.setText("1 Finger Tap");
                break;
              case TWO_FINGERS_SINGLE_TAP:
                mStateView.setText("2 Fingers Tap");
                break;
              case ONE_FINGER_DOUBLE_TAP:
                mStateView.setText("1 Finger Double Tap");
                break;
            }
            break;
        }
        touchStateTextAlpha = 1.0f;
        mStateView.setAlpha(touchStateTextAlpha);
      }
    });
  }

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    super.onTouchEvent(me);

    switch(me.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        prevTouchX = me.getX();
        prevTouchY = me.getY();

        Log.d("DEBUG", "touch down..." + " " + me.getX() + " " + me.getY());

        if(multiTouchNum == ONE_FINGER_SINGLE_TAP) {
          multiTouchNum = ONE_FINGER_DOUBLE_TAP;
          mHandler.removeCallbacksAndMessages(null);
        }
        else if(multiTouchNum == ONE_FINGER_SLIDE)
          ;
        else
          multiTouchNum = ONE_FINGER_SINGLE_TAP;

        multiTouchNum = ONE_FINGER_SINGLE_TAP;

        Log.d("DEBUG", "tap... " + multiTouchNum);

        /*
        wosc.setOSCAddress("/wosc", "/touch");
        wosc.setOSCTypeTag("ffi");
        wosc.addOSCFloatArgument(me.getX());
        wosc.addOSCFloatArgument(me.getY());
        wosc.addOSCIntArgument(multiTouchNum);
        wosc.flushOSCMessage();
        */

        pointerMoveCount = 0;
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        if(multiTouchNum != TOUCH_NONE) {
          prevMultiTouchNum = multiTouchNum;

          if (multiTouchNum == ONE_FINGER_SINGLE_TAP)
            multiTouchNum = TWO_FINGERS_SINGLE_TAP;
          else if (multiTouchNum == ONE_FINGER_SLIDE)
            multiTouchNum = TOUCH_NONE;
          Log.d("DEBUG", "touch pointer down... " + multiTouchNum + " " + me.getX() + " " + me.getY());

          /*
          wosc.setOSCAddress("/wosc", "/touch");
          wosc.setOSCTypeTag("ffi");
          wosc.addOSCFloatArgument(me.getX());
          wosc.addOSCFloatArgument(me.getY());
          wosc.addOSCIntArgument(multiTouchNum);
          wosc.flushOSCMessage();
          */

          setTouchState(multiTouchNum);

          if(multiTouchNum == TOUCH_NONE)
            bStartFadeout = true;
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if(Math.abs(prevTouchX - me.getX()) > 1.0 || Math.abs(prevTouchY - me.getY()) > 1.0) {

          //if(pointerMoveCount > 3) {
          if (multiTouchNum != TOUCH_NONE) {
            Log.d("DEBUG", "touch move... " + multiTouchNum + " " + me.getX() + " " + me.getY());

            if (multiTouchNum == TWO_FINGERS_SINGLE_TAP || multiTouchNum == TWO_FINGERS_SLIDE)
              multiTouchNum = TWO_FINGERS_SLIDE;
            else
              multiTouchNum = ONE_FINGER_SLIDE;

            wosc.setOSCAddress("/wosc", "/touch");
            wosc.setOSCTypeTag("ffi");
            wosc.addOSCFloatArgument(me.getX());
            wosc.addOSCFloatArgument(me.getY());
            wosc.addOSCIntArgument(multiTouchNum);
            wosc.flushOSCMessage();

            setTouchState(multiTouchNum);
          }
          //}

          pointerMoveCount++;
        }
        break;
      case MotionEvent.ACTION_UP:
        Log.d("DEBUG", "touch up... " + pointerMoveCount + " " + me.getX() + " " + me.getY());

        final float releasePositionX = me.getX();
        final float releasePositionY = me.getY();

        prevMultiTouchNum = multiTouchNum;
        repeatTapNum++;

        if(multiTouchNum == ONE_FINGER_SLIDE && pointerMoveCount > 1) {
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              Log.d("DEBUG", "release soon...");

              wosc.setOSCAddress("/wosc", "/touch");
              wosc.setOSCTypeTag("ffi");
              wosc.addOSCFloatArgument(releasePositionX);
              wosc.addOSCFloatArgument(releasePositionY);
              wosc.addOSCIntArgument(multiTouchNum);
              wosc.flushOSCMessage();

              multiTouchNum = TOUCH_NONE;
              repeatTapNum = 0;
            }
          });

          setTouchState(multiTouchNum);

          bStartFadeout = true;
        }
        else if(multiTouchNum != TOUCH_NONE) {
          mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
              Log.d("DEBUG", "release... " + multiTouchNum + " " + repeatTapNum);

              wosc.setOSCAddress("/wosc", "/touch");
              wosc.setOSCTypeTag("ffi");
              wosc.addOSCFloatArgument(releasePositionX);
              wosc.addOSCFloatArgument(releasePositionY);
              if(repeatTapNum == 2)
                wosc.addOSCIntArgument(2);
              else if(multiTouchNum == -1) {
                prevMultiTouchNum = 1;
                wosc.addOSCIntArgument(1);
              }
              else
                wosc.addOSCIntArgument(multiTouchNum);
              wosc.flushOSCMessage();

              multiTouchNum = TOUCH_NONE;
              if(repeatTapNum == 2)
                prevMultiTouchNum = ONE_FINGER_DOUBLE_TAP;

              setTouchState(multiTouchNum);

              repeatTapNum = 0;
              bStartFadeout = true;
            }
          }, 250);
        }

        break;
      case MotionEvent.ACTION_POINTER_UP:
        if(multiTouchNum != TOUCH_NONE) {
          Log.d("DEBUG", "touch pointer up...");

          wosc.setOSCAddress("/wosc", "/touch");
          wosc.setOSCTypeTag("ffi");
          wosc.addOSCFloatArgument(me.getX());
          wosc.addOSCFloatArgument(me.getY());
          wosc.addOSCIntArgument(multiTouchNum);
          wosc.flushOSCMessage();

          prevMultiTouchNum = multiTouchNum;
          multiTouchNum = TOUCH_NONE;

          setTouchState(multiTouchNum);

          bStartFadeout = true;
        }
        break;
    }

    return true;
  }
}
