package com.mct.touchutils.test;

import static com.mct.touchutils.TouchUtils.BaseTouchListener;
import static com.mct.touchutils.TouchUtils.FlingMoveToCornerListener;
import static com.mct.touchutils.TouchUtils.FlingMoveToWallListener;
import static com.mct.touchutils.TouchUtils.FlingMoveToWallListener.MoveMode;
import static com.mct.touchutils.TouchUtils.ScaleType;
import static com.mct.touchutils.TouchUtils.TouchScaleListener;
import static com.mct.touchutils.TouchUtils.setTouchListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.FloatPropertyCompat;

import com.mct.touchutils.TouchUtils;

public class MainActivity extends AppCompatActivity {

    private BubbleBaseLayout bubbleLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnAdd).setOnClickListener(this::onClick);
        findViewById(R.id.btnRemove).setOnClickListener(this::onClick);
        findViewById(R.id.btnMoveToCorner).setOnClickListener(this::onClick);
        findViewById(R.id.btnMoveToWall).setOnClickListener(this::onClick);
        findViewById(R.id.btnMoveToWallH).setOnClickListener(this::onClick);
        findViewById(R.id.btnMoveToWallV).setOnClickListener(this::onClick);
        findViewById(R.id.btnScaleUp).setOnClickListener(this::onClick);
        findViewById(R.id.btnScaleDown).setOnClickListener(this::onClick);
    }

    private void onClick(@NonNull View view) {
        int id = view.getId();
        if (id == R.id.btnAdd) {
            if (hasOverlayPermission(this)) {
                addBubble();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                }
            }
        } else if (id == R.id.btnRemove) {
            removeBubble();
        } else if (id == R.id.btnMoveToCorner) {
            removeTouchListener();
            setTouchListener(bubbleLayout, createMoveToCornerListener());
        } else if (id == R.id.btnMoveToWall) {
            removeTouchListener();
            setTouchListener(bubbleLayout, createMoveToWallListener(MoveMode.Nearest));
        } else if (id == R.id.btnMoveToWallH) {
            removeTouchListener();
            setTouchListener(bubbleLayout, createMoveToWallListener(MoveMode.Horizontal));
        } else if (id == R.id.btnMoveToWallV) {
            removeTouchListener();
            setTouchListener(bubbleLayout, createMoveToWallListener(MoveMode.Vertical));
        } else if (id == R.id.btnScaleUp) {
            removeTouchListener();
            setTouchListener(bubbleLayout.getChildAt(0), createScaleListener(TouchUtils.TYPE_GROW));
        } else if (id == R.id.btnScaleDown) {
            removeTouchListener();
            setTouchListener(bubbleLayout.getChildAt(0), createScaleListener(TouchUtils.TYPE_SHRINK));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void removeTouchListener() {
        if (bubbleLayout != null) {
            bubbleLayout.setOnTouchListener(null);
            bubbleLayout.getChildAt(0).setOnTouchListener(null);
        }
    }

    @NonNull
    private BaseTouchListener createMoveToCornerListener() {
        return new FlingMoveToCornerListener() {
            @NonNull
            @Override
            protected Rect initArea(View view) {
                return new Rect(50, 150, getScreenWidth() - 50, getScreenHeight() - 150);
            }

            @Override
            protected FloatPropertyCompat<View> getPropX() {
                return BubbleBaseLayout.WINDOW_X.getPropertyCompat();
            }

            @Override
            protected FloatPropertyCompat<View> getPropY() {
                return BubbleBaseLayout.WINDOW_Y.getPropertyCompat();
            }
        };
    }

    @NonNull
    private BaseTouchListener createMoveToWallListener(MoveMode moveMode) {
        return new FlingMoveToWallListener() {
            @NonNull
            @Override
            protected Rect initArea(View view) {
                return new Rect(50, 150, getScreenWidth() - 50, getScreenHeight() - 150);
            }

            @NonNull
            @Override
            protected MoveMode getMoveMode() {
                return moveMode;
            }

            @Override
            protected FloatPropertyCompat<View> getPropX() {
                return BubbleBaseLayout.WINDOW_X.getPropertyCompat();
            }

            @Override
            protected FloatPropertyCompat<View> getPropY() {
                return BubbleBaseLayout.WINDOW_Y.getPropertyCompat();
            }
        };
    }

    @NonNull
    private BaseTouchListener createScaleListener(@ScaleType int scaleType) {
        return new TouchScaleListener() {
            @Override
            protected int getScaleType() {
                return scaleType;
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeBubble();
    }

    private void addBubble() {
        View view = new View(MainActivity.this);
        bubbleLayout = new BubbleBaseLayout(MainActivity.this);
        bubbleLayout.addView(view);

        ((FrameLayout.LayoutParams) view.getLayoutParams()).gravity = Gravity.CENTER;
        view.getLayoutParams().width = 150;
        view.getLayoutParams().height = 150;
        view.setBackgroundColor(Color.BLUE);

        bubbleLayout.setWindowManager((WindowManager) getSystemService(WINDOW_SERVICE));
        bubbleLayout.setViewParams(getWindowParams(
                (getScreenWidth() - 150) / 2, 150,
                200,
                200
        ));
        bubbleLayout.attachToWindow();
    }

    private void removeBubble() {
        if (bubbleLayout != null) {
            bubbleLayout.detachFromWindow();
            bubbleLayout = null;
        }
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return -1;
        }
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        return point.x;
    }

    private int getScreenHeight() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return -1;
        }
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        return point.y;
    }

    @NonNull
    private static WindowManager.LayoutParams getWindowParams(int x, int y, int width, int height) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;
        return params;
    }

    private static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

}