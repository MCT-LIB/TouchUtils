package com.mct.test;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.fragment.app.FragmentActivity;

import com.mct.touchutils.R;
import com.mct.touchutils.TouchUtils;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

public class MainActivity extends AppCompatActivity {

    static boolean isAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        requestOverlayPermission(this, (allGranted, grantedList, deniedList) -> {
            if (allGranted && !isAdd) {
                isAdd = true;
                BubbleBaseLayout baseLayout = new BubbleBaseLayout(MainActivity.this);
                View view = new View(MainActivity.this);
                baseLayout.addView(view);

                view.getLayoutParams().width = 150;
                view.getLayoutParams().height = 150;
                view.setBackgroundColor(Color.BLUE);

                baseLayout.setWindowManager((WindowManager) getSystemService(WINDOW_SERVICE));
                baseLayout.setViewParams(getWindowParams(
                        getScreenWidth() - 150, 0,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        false)
                );
                baseLayout.attachToWindow();

                TouchUtils.setTouchListener(baseLayout, new TouchUtils.FlingMoveToWallListener() {
                    @NonNull
                    @Override
                    public Rect initArea(View view) {
                        final int left = 0, top = 0, right = getScreenWidth(), bottom = getScreenHeight() - 140;
                        return new Rect(left, top, right, bottom);
                    }

                    @Override
                    protected FloatPropertyCompat<View> getPropX() {
                        return BubbleBaseLayout.WINDOW_X.getPropertyCompat();
                    }

                    @Override
                    protected FloatPropertyCompat<View> getPropY() {
                        return BubbleBaseLayout.WINDOW_Y.getPropertyCompat();
                    }

                    @Override
                    protected float getFrictionY() {
                        return 2;
                    }

                });
            }
        });

        View box = findViewById(R.id.box);

        TouchUtils.setTouchListener(box, new TouchUtils.TouchMoveCornerListener() {
            @NonNull
            @Override
            public Rect initArea(View view) {
                final int left = 0, top = 0, right = getScreenWidth(), bottom = getScreenHeight() - 140;
                return new Rect(left, top, right, bottom);
            }

//            @NonNull
//            @Override
//            protected Rect initAnimArea(@NonNull View view) {
//                int width = view.getWidth() / 2;
//                int height = view.getHeight() / 2;
//                Rect area = getArea();
//                return new Rect(
//                        area.left - width,
//                        area.top - height,
//                        area.right - view.getWidth() + width,
//                        area.bottom - view.getHeight() + height
//                );
//            }
        });

//        TouchUtils.setTouchListener(box, new TouchUtils.FlingMoveToWallListener() {
//            @NonNull
//            @Override
//            public Rect initArea(View view) {
//                final int left = 0, top = 0, right = getScreenWidth(), bottom = getScreenHeight() - 140;
//                return new Rect(left, top, right, bottom);
//            }
//        });

//        TouchUtils.setTouchListener(box, new TouchUtils.TouchScaleListener());
    }

    int getScreenWidth() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return -1;
        }
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        return point.x;
    }

    int getScreenHeight() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return -1;
        }
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        return point.y;
    }

    @NonNull
    static WindowManager.LayoutParams getWindowParams(int x, int y, int width, int height, boolean focusable) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                getType(), getFlag(focusable),
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;
        return params;
    }

    private static int getType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private static int getFlag(boolean focusable) {
        if (focusable) {
            return WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            // | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        }
    }

    public static void requestOverlayPermission(FragmentActivity activity, RequestCallback callback) {
        PermissionX.init(activity)
                .permissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
                .onExplainRequestReason((scope, deniedList) -> scope.showRequestReasonDialog(deniedList,
                        "You need to grant the app permission to use this feature.",
                        "OK",
                        "Cancel"))
                .request(callback);
    }

}