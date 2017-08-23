package com.remobile.toast;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.*;

public class Toast extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private android.widget.Toast mostRecentToast;

    // note that webView.isPaused() is not Xwalk compatible, so tracking it poor-man style
    private boolean isPaused;

    public Toast(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RCTToast";
    }

    @ReactMethod
    public void show(ReadableMap options) throws Exception {
        if (this.isPaused) {
            return;
        }


        final String message = options.getString("message");
        final String duration = options.getString("duration");
        final String position = options.getString("position");
        final int addPixelsY = options.hasKey("addPixelsY") ? options.getInt("addPixelsY") : 0;
        final JSONObject styling = options.optJSONObject("styling");

        UiThreadUtil.runOnUiThread(new Runnable() {
            public void run() {
                android.widget.Toast toast = android.widget.Toast.makeText(
                        getReactApplicationContext(),
                        message,
                        "short".equals(duration) ? android.widget.Toast.LENGTH_SHORT : android.widget.Toast.LENGTH_LONG);

                if ("top".equals(position)) {
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 20 + addPixelsY);
                } else if ("bottom".equals(position)) {
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 20 - addPixelsY);
                } else if ("center".equals(position)) {
                    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, addPixelsY);
                } else {
                    FLog.e("RCTToast", "invalid position. valid options are 'top', 'center' and 'bottom'");
                    return;
                }

                // if one of the custom layout options have been passed in, draw our own shape
                if (styling != null && Build.VERSION.SDK_INT >= 16) {
      
                    // the defaults mimic the default toast as close as possible
                    final String backgroundColor = styling.optString("backgroundColor", "#333333");
                    final String textColor = styling.optString("textColor", "#ffffff");
                    final Double textSize = styling.optDouble("textSize", -1);
                    final double opacity = styling.optDouble("opacity", 0.8);
                    final int cornerRadius = styling.optInt("cornerRadius", 100);
                    final int horizontalPadding = styling.optInt("horizontalPadding", 50);
                    final int verticalPadding = styling.optInt("verticalPadding", 30);
      
                    GradientDrawable shape = new GradientDrawable();
                    shape.setCornerRadius(cornerRadius);
                    shape.setAlpha((int)(opacity * 255)); // 0-255, where 0 is an invisible background
                    shape.setColor(Color.parseColor(backgroundColor));
                    toast.getView().setBackground(shape);
      
                    final TextView toastTextView;
                    toastTextView = (TextView) toast.getView().findViewById(android.R.id.message);
                    toastTextView.setTextColor(Color.parseColor(textColor));
                    if (textSize > -1) {
                        toastTextView.setTextSize(textSize.floatValue());
                    }

                    toast.getView().setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

                    // this gives the toast a very subtle shadow on newer devices
                    if (Build.VERSION.SDK_INT >= 21) {
                      toast.getView().setElevation(6);
                    }
                }

                toast.show();
                mostRecentToast = toast;
            }
        });
    }

    @ReactMethod
    public void hide() throws Exception {
        if (mostRecentToast != null) {
            mostRecentToast.cancel();
        }
    }

    @Override
    public void initialize() {
        getReactApplicationContext().addLifecycleEventListener(this);
    }

    @Override
    public void onHostPause() {
        if (mostRecentToast != null) {
            mostRecentToast.cancel();
        }
        this.isPaused = true;
    }

    @Override
    public void onHostResume() {
        this.isPaused = false;
    }

    @Override
    public void onHostDestroy() {
        this.isPaused = true;
    }
}
