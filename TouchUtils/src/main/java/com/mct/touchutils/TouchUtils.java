package com.mct.touchutils;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.DynamicAnimation.OnAnimationEndListener;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

public class TouchUtils {

    private static final String TAG = "TouchMoveUtils";

    public static final int TOP_LEFT = 0;
    public static final int TOP_RIGHT = 1;
    public static final int BOT_LEFT = 2;
    public static final int BOT_RIGHT = 3;

    public static final int LEFT = 0;
    public static final int TOP = 1;
    public static final int RIGHT = 2;
    public static final int BOT = 3;
    public static final int UNSET = 4;

    @IntDef({TOP_LEFT, TOP_RIGHT, BOT_LEFT, BOT_RIGHT})
    public @interface Corner {
    }

    @IntDef({LEFT, TOP, RIGHT, BOT})
    public @interface Wall {
    }

    public static final int TYPE_GROW = 0;
    public static final int TYPE_SHRINK = 1;

    @IntDef({TYPE_GROW, TYPE_SHRINK})
    public @interface ScaleType {
    }

    private TouchUtils() {
        throw new UnsupportedOperationException("u can't instantiate this...");
    }

    public static <T extends BaseTouchListener> void setTouchListener(View v, T listener) {
        if (v == null) {
            return;
        }
        v.setOnTouchListener(listener);
    }

    public interface Initializable {
        void init(View v);
    }

    ///////////////////////////////////////////////////////////////////////////
    // LISTENERS
    ///////////////////////////////////////////////////////////////////////////

    private static abstract class BaseTouchListener implements View.OnTouchListener, Initializable {

        protected static final int STATE_DOWN = 0;
        protected static final int STATE_MOVE = 1;

        private int touchSlop;
        private int state;
        private int lastX, lastY;

        protected final int getState() {
            return state;
        }

        @Override
        public final boolean onTouch(View view, MotionEvent event) {
            onActionTouch(view, event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return onDown(view, event);
                case MotionEvent.ACTION_MOVE:
                    return onMove(view, event);
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return onStop(view, event);
                default:
                    return false;
            }
        }

        private boolean onDown(View view, MotionEvent event) {
            Log.d(TAG, "onActionDown ----- state = STATE_DOWN");
            if (touchSlop == 0) {
                touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            }
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            resetTouch(x, y);
            view.setPressed(true);
            return onActionDown(view, event);
        }

        private boolean onMove(View view, @NonNull MotionEvent event) {
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            if (lastX == -1) {
                // not receive down should reset
                resetTouch(x, y);
                view.setPressed(true);
            }
            if (state != STATE_MOVE) {
                if (Math.abs(x - lastX) >= touchSlop || Math.abs(y - lastY) >= touchSlop) {
                    Log.d(TAG, "onActionMove ----- state = STATE_MOVE");
                    state = STATE_MOVE;
                }
            }
            return onActionMove(view, event);
        }

        private boolean onStop(View view, MotionEvent event) {
            boolean b = onActionStop(view, event);
            resetTouch(-1, -1);
            view.setPressed(false);
            return b;
        }

        protected void onActionTouch(@NonNull View view, @NonNull MotionEvent event) {
        }

        protected boolean onActionDown(@NonNull View view, @NonNull MotionEvent event) {
            return false;
        }

        protected boolean onActionMove(@NonNull View view, @NonNull MotionEvent event) {
            return false;
        }

        protected boolean onActionStop(@NonNull View view, @NonNull MotionEvent event) {
            return false;
        }

        protected boolean isTouching() {
            return state == STATE_MOVE;
        }

        protected void resetTouch(int x, int y) {
            lastX = x;
            lastY = y;
            state = STATE_DOWN;
        }

    }

    public static abstract class FlingMoveListener extends BaseTouchListener {

        private static final int MIN_TAP_TIME = 1000;
        private int maximumFlingVelocity;

        private boolean isInit;
        private Rect area, moveArea;
        private SpringAnimation springX, springY;
        private VelocityTracker velocityTracker;
        private float dX, dY;

        @NonNull
        protected abstract Rect initArea(View view);

        protected abstract boolean isCanClick(View view);

        protected abstract void handleFling(View view, @Nullable Point predictPosition);

        @Override
        public void init(View v) {
            isInit = true;
            setArea(v, initArea(v));
            Rect animArea = initAnimArea(v);

            maximumFlingVelocity = ViewConfiguration.get(v.getContext()).getScaledMaximumFlingVelocity();
            springX = new SpringAnimation(v, getPropX(), 0);
            springX.setMinValue(animArea.left);
            springX.setMaxValue(animArea.right);
            springY = new SpringAnimation(v, getPropY(), 0);
            springY.setMinValue(animArea.top);
            springY.setMaxValue(animArea.bottom);
        }

        @Override
        protected void onActionTouch(@NonNull View view, @NonNull MotionEvent event) {
            event.offsetLocation(getPropX().getValue(view), getPropY().getValue(view));
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(event);
        }

        @Override
        protected boolean onActionDown(@NonNull View view, @NonNull MotionEvent event) {
            if (!isInit) init(view);
            dX = getPropX().getValue(view) - event.getRawX();
            dY = getPropY().getValue(view) - event.getRawY();
            resetForce(false);
            clearAnimation();
            return onDown(view, event);
        }

        @Override
        protected boolean onActionMove(@NonNull View view, @NonNull MotionEvent event) {
            float x = event.getRawX() + dX;
            float y = event.getRawY() + dY;
            if (!isCanMoveOutArea()) {
                x = coerceIn(x, moveArea.left, moveArea.right);
                y = coerceIn(y, moveArea.top, moveArea.bottom);
            }
            springX.animateToFinalPosition(x);
            springY.animateToFinalPosition(y);
            if (isTouching()) {
                return onMove(view, event);
            }
            return true;
        }

        @Override
        protected boolean onActionStop(@NonNull View view, @NonNull MotionEvent event) {
            long eventTime = event.getEventTime() - event.getDownTime();
            boolean isHandleClick = !isTouching() && isCanClick(view);
            Point predictPosition = null;
            if (isHandleClick) {
                if (eventTime <= getMinTapTime()) {
                    view.performClick();
                } else {
                    view.performLongClick();
                }
            } else {
                if (velocityTracker != null) {
                    // compute velocity
                    velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
                    // handle velocity
                    float vx = velocityTracker.getXVelocity() * (100 - getLostVelocityPercent()) / 100;
                    float vy = velocityTracker.getYVelocity() * (100 - getLostVelocityPercent()) / 100;
                    predictPosition = new Point(
                            (int) coerceIn(getPropX().getValue(view) + vx, moveArea.left, moveArea.right),
                            (int) coerceIn(getPropY().getValue(view) + vy, moveArea.top, moveArea.bottom)
                    );
                }
            }
            resetForce(true);
            handleFling(view, predictPosition);
            releaseTracker();
            return onStop(view, event);
        }

        protected final void setArea(@NonNull View v, @NonNull Rect rect) {
            if (rect.equals(area)) {
                return;
            }
            int right = rect.right - v.getWidth();
            int bottom = rect.bottom - v.getHeight();
            area = rect;
            moveArea = new Rect(area.left, area.top, right, bottom);
        }

        @NonNull
        protected final Rect getArea() {
            return new Rect(area);
        }

        @NonNull
        protected final Rect getMoveArea() {
            return new Rect(moveArea);
        }

        protected final SpringAnimation getSpringX() {
            return springX;
        }

        protected final SpringAnimation getSpringY() {
            return springY;
        }

        protected final VelocityTracker getVelocityTracker() {
            return velocityTracker;
        }

        protected final void releaseTracker() {
            // release tracker
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }

        protected final void clearAnimation() {
            if (isInit) {
                springX.cancel();
                springY.cancel();
            }
        }

        /* ----------------- CAN OVERRIDE TO RECEIVE OR MODIFY ---------------------------------- */

        /**
         * This func can control min and max position
         * when anim move to predict position
         */
        @NonNull
        protected Rect initAnimArea(@NonNull View view) {
            int width = 2 * view.getWidth();
            int height = 2 * view.getHeight();
            return new Rect(
                    area.left - width,
                    area.top - height,
                    area.right - view.getWidth() + width,
                    area.bottom - view.getHeight() + height
            );
        }

        protected boolean onDown(View view, MotionEvent event) {
            return true;
        }

        protected boolean onMove(View view, MotionEvent event) {
            return true;
        }

        protected boolean onStop(View view, MotionEvent event) {
            return true;
        }

        protected FloatPropertyCompat<View> getPropX() {
            return DynamicAnimation.X;
        }

        protected FloatPropertyCompat<View> getPropY() {
            return DynamicAnimation.Y;
        }

        protected float getStiffnessX() {
            return 150;
        }

        protected float getDampingRatioX() {
            return 0.6f;
        }

        protected float getStiffnessY() {
            return 150;
        }

        protected float getDampingRatioY() {
            return 0.6f;
        }

        protected boolean isCanMoveOutArea() {
            return true;
        }

        protected boolean isMovingCanClick() {
            return false;
        }

        @IntRange(from = 0, to = 100)
        protected int getLostVelocityPercent() {
            return 90;
        }

        protected int getMinTapTime() {
            return MIN_TAP_TIME;
        }

        /* -------------------------------- PRIVATE AREA ---------------------------------------- */

        private void resetForce(boolean isMoveToPredictPosition) {
            if (isMoveToPredictPosition) {
                springX.getSpring().setDampingRatio(getDampingRatioX()).setStiffness(getStiffnessX());
                springY.getSpring().setDampingRatio(getDampingRatioY()).setStiffness(getStiffnessY());
            } else {
                float dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY;
                float stiffness = SpringForce.STIFFNESS_HIGH;
                springX.getSpring().setDampingRatio(dampingRatio).setStiffness(stiffness);
                springY.getSpring().setDampingRatio(dampingRatio).setStiffness(stiffness);
            }
        }

    }

    public static abstract class FlingMoveCornerListener extends FlingMoveListener {

        @Override
        protected boolean isCanClick(View view) {
            return isMovingCanClick() || isNearCornerPoint(new Point(
                    (int) getPropX().getValue(view),
                    (int) getPropY().getValue(view)), getMoveArea());
        }

        @Override
        protected void handleFling(View view, Point predictPosition) {
            moveToCorner(view, getCorner(view, predictPosition));
        }

        @Corner
        protected int getCorner(View view) {
            return getCorner(view, null);
        }

        @Corner
        protected int getCorner(View view, @Nullable Point predictPosition) {
            return predictPosition == null
                    ? calcCorner(getCenter(view, getPropX(), getPropY()), getArea())
                    : calcCorner(getCenter(view, predictPosition), getArea());
        }

        protected void moveToCorner(@NonNull View view, @Corner int corner) {
            Point cornerPoint = TouchUtils.getCorner(getMoveArea(), corner);
            OnAnimationEndListener endListener = new OnAnimationEndListener() {
                @Override
                public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                    if (!canceled && !getSpringX().isRunning() && !getSpringY().isRunning()) {
                        onMovedToCorner(view, corner, cornerPoint);
                    }
                    if (!getSpringX().isRunning()) getSpringX().removeEndListener(this);
                    if (!getSpringY().isRunning()) getSpringY().removeEndListener(this);
                }
            };
            getSpringX().addEndListener(endListener).animateToFinalPosition(cornerPoint.x);
            getSpringY().addEndListener(endListener).animateToFinalPosition(cornerPoint.y);
        }

        protected void onMovedToCorner(@NonNull View view, @Corner int corner, Point cornerPoint) {
        }

    }

    public static abstract class FlingMoveToWallListener extends FlingMoveListener {

        public enum MoveMode {
            Left(LEFT), Top(TOP), Right(RIGHT), Bot(BOT),
            Vertical(UNSET),   // doc
            Horizontal(UNSET), // ngang
            Nearest(UNSET);    // auto
            @Wall
            private final int wall;

            MoveMode(int wall) {
                this.wall = wall;
            }
        }

        @Override
        protected boolean isCanClick(View view) {
            return isMovingCanClick() || isNearWallPoint(new Point(
                    (int) getPropX().getValue(view),
                    (int) getPropY().getValue(view)), getMoveArea());
        }

        @Override
        protected void handleFling(View view, Point predictPosition) {
            moveToWall(view, predictPosition);
        }

        @Wall
        protected int getWall(View view) {
            return getWall(view, null);
        }

        @Wall
        protected int getWall(View view, @Nullable Point predictPosition) {
            MoveMode mode = getMoveMode();
            if (mode.wall != UNSET) {
                return mode.wall;
            }
            Rect moveArea = getMoveArea();
            float centerAreaX = moveArea.exactCenterX();
            float centerAreaY = moveArea.exactCenterY();
            Point centerView = predictPosition == null
                    ? getCenter(view, getPropX(), getPropY())
                    : getCenter(view, predictPosition);
            switch (mode) {
                default:
                case Vertical:
                    return centerView.x < centerAreaX ? LEFT : RIGHT;
                case Horizontal:
                    return centerView.y < centerAreaY ? TOP : BOT;
                case Nearest:
                    int predictWallVer = centerView.x < centerAreaX ? LEFT : RIGHT;
                    int predictWallHoz = centerView.y < centerAreaY ? TOP : BOT;

                    int predictX = predictWallVer == LEFT ? moveArea.left : moveArea.right;
                    int predictY = predictWallHoz == TOP ? moveArea.top : moveArea.bottom;
                    float distanceToVer = Math.abs(centerView.x - predictX);
                    float distanceToHoz = Math.abs(centerView.y - predictY);

                    return distanceToVer / moveArea.width() > distanceToHoz / moveArea.height() ?
                            predictWallHoz :
                            predictWallVer;
            }
        }

        protected void moveToWall(View view, Point predictPosition) {
            Rect moveArea = getMoveArea();
            Point wallPoint = new Point();
            int wall = getWall(view, predictPosition);
            switch (wall) {
                case LEFT:
                case RIGHT:
                    wallPoint.x = wall == LEFT ? moveArea.left : moveArea.right;
                    wallPoint.y = predictPosition == null ? (int) getPropY().getValue(view) : predictPosition.y;
                    break;
                case TOP:
                case BOT:
                    wallPoint.x = predictPosition == null ? (int) getPropX().getValue(view) : predictPosition.x;
                    wallPoint.y = wall == TOP ? moveArea.top : moveArea.bottom;
                    break;
            }

            OnAnimationEndListener endListener = new OnAnimationEndListener() {
                @Override
                public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                    if (!canceled && !getSpringX().isRunning() && !getSpringY().isRunning()) {
                        onMovedToWall(view, wall, wallPoint);
                    }
                    if (!getSpringX().isRunning()) getSpringX().removeEndListener(this);
                    if (!getSpringY().isRunning()) getSpringY().removeEndListener(this);
                }
            };
            getSpringX().addEndListener(endListener).animateToFinalPosition(wallPoint.x);
            getSpringY().addEndListener(endListener).animateToFinalPosition(wallPoint.y);
        }

        @NonNull
        protected MoveMode getMoveMode() {
            return MoveMode.Vertical;
        }

        protected void onMovedToWall(@NonNull View view, @Wall int wall, Point wallPoint) {
        }

    }

    public static class TouchScaleListener extends BaseTouchListener {

        private static final int PIVOT_TYPE = ScaleAnimation.RELATIVE_TO_SELF;
        private static final float PIVOT_VAL = 0.5f;
        private static final float DEFAULT_REAL_SCALE = 1f;
        private static final float DEFAULT_DELTA_SCALE = 0.25f;
        private static final int DEFAULT_DURATION = 250;
        private static final int MIN_TAP_TIME = 1000;
        private static final int DEFAULT_OFFSET_RELEASE = 0;

        private boolean isRelease;

        @Override
        public void init(View v) {
        }

        @Override
        protected final void onActionTouch(@NonNull View view, @NonNull MotionEvent event) {
            super.onActionTouch(view, event);
        }

        @Override
        protected final boolean onActionDown(@NonNull View view, @NonNull MotionEvent event) {
            isRelease = false;
            float from = getRealScale();
            float to = from + (getScaleType() == TYPE_GROW ? getDeltaScale() : -getDeltaScale());
            ScaleAnimation scaleAnimation = new ScaleAnimation(from, to, from, to, PIVOT_TYPE, PIVOT_VAL, PIVOT_TYPE, PIVOT_VAL);
            scaleAnimation.setDuration(getDuration());
            scaleAnimation.setFillAfter(true);
            view.startAnimation(scaleAnimation);
            return true;
        }

        @Override
        protected final boolean onActionMove(@NonNull View view, @NonNull MotionEvent event) {
            if (isRelease) {
                return true;
            }
            float x = event.getRawX();
            float y = event.getRawY();
            Point position = getLocationOnScreen(view);
            if (x < position.x - getOffsetReleaseX() ||
                    x > position.x + view.getWidth() + getOffsetReleaseX() ||
                    y < position.y - getOffsetReleaseY() ||
                    y > position.y + view.getHeight() + getOffsetReleaseY()) {
                release(view, false);
            }
            return true;
        }

        @Override
        protected final boolean onActionStop(@NonNull View view, @NonNull MotionEvent event) {
            if (isRelease) {
                return true;
            }
            boolean isHasClick = false;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (getState() == STATE_DOWN) {
                    isHasClick = true;
                    long eventTime = event.getEventTime() - event.getDownTime();
                    if (eventTime <= getMinTapTime()) {
                        Log.d(TAG, "onActionStop: PERFORM CLICK");
                        view.performClick();
                    } else {
                        Log.d(TAG, "onActionStop: PERFORM LONG CLICK");
                        view.performLongClick();
                    }
                }
            }
            release(view, isHasClick);
            return true;
        }

        protected void release(View view, boolean isHasClick) {
            isRelease = true;
            float delta = (getDeltaScale() + (isHasClick ? 0.2f : 0)) * (getDeltaScale() == TYPE_GROW ? 1 : -1);
            float to = getRealScale();
            float from = to + delta;
            if (from < 0) {
                from = 0;
            }
            ScaleAnimation scaleAnimation = new ScaleAnimation(from, to, from, to, PIVOT_TYPE, PIVOT_VAL, PIVOT_TYPE, PIVOT_VAL);
            scaleAnimation.setDuration(getDuration());
            if (isHasClick) {
                scaleAnimation.setInterpolator(new OvershootInterpolator());
            }
            view.startAnimation(scaleAnimation);
        }

        @ScaleType
        protected int getScaleType() {
            return TYPE_SHRINK;
        }

        protected float getRealScale() {
            return DEFAULT_REAL_SCALE;
        }

        /**
         * Real scale (+ | -) delta scale => new scale
         */
        protected float getDeltaScale() {
            return DEFAULT_DELTA_SCALE;
        }

        protected int getDuration() {
            return DEFAULT_DURATION;
        }

        protected int getMinTapTime() {
            return MIN_TAP_TIME;
        }

        protected int getOffsetReleaseX() {
            return DEFAULT_OFFSET_RELEASE;
        }

        protected int getOffsetReleaseY() {
            return DEFAULT_OFFSET_RELEASE;
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Below are private UTILS FUNC
    ///////////////////////////////////////////////////////////////////////////

    private static float coerceIn(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    @NonNull
    private static Point getCenter(@NonNull View view,
                                   @NonNull FloatPropertyCompat<View> propertyX,
                                   @NonNull FloatPropertyCompat<View> propertyY) {
        return new Point(
                (int) (propertyX.getValue(view) + (view.getWidth() / 2)),
                (int) (propertyY.getValue(view) + (view.getHeight() / 2)));
    }

    @NonNull
    private static Point getCenter(@NonNull View view, @NonNull Point pos) {
        return new Point(
                pos.x + (view.getWidth() / 2),
                pos.y + (view.getHeight() / 2));
    }

    @NonNull
    private static Point getCorner(Rect area, @Corner int corner) {
        switch (corner) {
            case TOP_LEFT:
                return new Point(area.left, area.top);
            case TOP_RIGHT:
                return new Point(area.right, area.top);
            case BOT_LEFT:
                return new Point(area.left, area.bottom);
            case BOT_RIGHT:
                return new Point(area.right, area.bottom);
        }
        return new Point();
    }

    private static boolean isNearCornerPoint(@NonNull Point p, @NonNull Rect area) {
        List<Pair<Integer, Double>> cornerDistances = getCornerDistances(p, area);
        for (Pair<Integer, Double> cornerDistance : cornerDistances) {
            if (cornerDistance.second <= 8) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return first: corner from 0 -> 4:
     * <br/>second: minDistance
     * <br/>See Also: ${@link Corner}
     */
    @Corner
    private static int calcCorner(Point p, @NonNull Rect area) {
        List<Pair<Integer, Double>> cornerDistances = getCornerDistances(p, area);
        Pair<Integer, Double> minDistance = cornerDistances.get(0);
        for (int i = 0; i < cornerDistances.size(); i++) {
            Pair<Integer, Double> cornerDistance = cornerDistances.get(i);
            if (minDistance.second > cornerDistance.second) {
                minDistance = cornerDistance;
            }
        }
        return minDistance.first;
    }

    @NonNull
    private static List<Pair<Integer, Double>> getCornerDistances(@NonNull Point p, @NonNull Rect area) {
        Point tl = new Point(area.left, area.top);
        Point tr = new Point(area.right, area.top);
        Point bl = new Point(area.left, area.bottom);
        Point br = new Point(area.right, area.bottom);

        List<Pair<Integer, Double>> cornerDistances = new ArrayList<>();
        cornerDistances.add(new Pair<>(TOP_LEFT, distance(p, tl)));
        cornerDistances.add(new Pair<>(TOP_RIGHT, distance(p, tr)));
        cornerDistances.add(new Pair<>(BOT_LEFT, distance(p, bl)));
        cornerDistances.add(new Pair<>(BOT_RIGHT, distance(p, br)));
        return cornerDistances;
    }

    private static boolean isNearWallPoint(@NonNull Point p, @NonNull Rect area) {
        List<Pair<Integer, Double>> cornerDistances = getWallDistances(p, area);
        for (Pair<Integer, Double> cornerDistance : cornerDistances) {
            if (cornerDistance.second <= 8) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static List<Pair<Integer, Double>> getWallDistances(@NonNull Point p, @NonNull Rect area) {
        Point l = new Point(area.left, p.y);
        Point r = new Point(area.right, p.y);

        List<Pair<Integer, Double>> wallDistances = new ArrayList<>();
        wallDistances.add(new Pair<>(LEFT, distance(p, l)));
        wallDistances.add(new Pair<>(RIGHT, distance(p, r)));

        return wallDistances;
    }


    private static double distance(@NonNull Point a, @NonNull Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @NonNull
    public static Point getLocationOnScreen(@NonNull View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Point(location[0], location[1]);
    }

}
