package com.mct.touchutils.test;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.FloatPropertyCompat;

class BubbleBaseLayout extends FrameLayout {
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    void setWindowManager(WindowManager windowManager) {
        this.windowManager = windowManager;
    }

    WindowManager getWindowManager() {
        return this.windowManager;
    }

    public void setViewParams(WindowManager.LayoutParams params) {
        this.params = params;
    }

    public WindowManager.LayoutParams getViewParams() {
        return this.params;
    }

    boolean isAttach;

    public void updateLayoutParams() {
        if (isAttach) {
            getWindowManager().updateViewLayout(this, getViewParams());
        }
    }

    public synchronized void attachToWindow() {
        if (!isAttach) {
            isAttach = true;
            getWindowManager().addView(this, getViewParams());
        }
    }

    public synchronized void detachFromWindow() {
        if (isAttach) {
            isAttach = false;
            getWindowManager().removeView(this);
        }
    }

    public BubbleBaseLayout(Context context) {
        super(context);
    }

    public BubbleBaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BubbleBaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    static abstract class BubbleProperty extends Property<View, Float> {
        public BubbleProperty(String name) {
            super(Float.class, name);
        }

        public FloatPropertyCompat<View> getPropertyCompat() {
            return new FloatPropertyCompat<View>(getName()) {
                @Override
                public float getValue(View object) {
                    return get(object);
                }

                @Override
                public void setValue(View object, float value) {
                    set(object, value);
                }
            };
        }

        protected void refresh(@NonNull BubbleBaseLayout object) {
            if (object.isAttachedToWindow()) {
                object.getWindowManager().updateViewLayout(object, object.getLayoutParams());
            }
        }

        protected BubbleBaseLayout cast(View view) {
            if (view instanceof BubbleBaseLayout) {
                return (BubbleBaseLayout) view;
            }
            return null;
        }
    }

    public static final BubbleProperty WINDOW_X = new BubbleProperty("WINDOW_X") {
        @Override
        public Float get(@NonNull View object) {
            BubbleBaseLayout layout = cast(object);
            if (layout != null) {
                return (float) layout.getViewParams().x;
            }
            return View.X.get(object);
        }

        @Override
        public void set(@NonNull View object, @NonNull Float value) {
            BubbleBaseLayout layout = cast(object);
            if (layout != null) {
                layout.getViewParams().x = value.intValue();
                refresh(layout);
                return;
            }
            View.X.set(object, value);
        }
    };

    static final BubbleProperty WINDOW_Y = new BubbleProperty("WINDOW_Y") {
        @Override
        public Float get(@NonNull View object) {
            BubbleBaseLayout layout = cast(object);
            if (layout != null) {
                return (float) layout.getViewParams().y;
            }
            return View.Y.get(object);
        }

        @Override
        public void set(@NonNull View object, @NonNull Float value) {
            BubbleBaseLayout layout = cast(object);
            if (layout != null) {
                layout.getViewParams().y = value.intValue();
                refresh(layout);
                return;
            }
            View.Y.set(object, value);
        }
    };
}
