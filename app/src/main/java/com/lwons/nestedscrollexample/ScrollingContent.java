package com.lwons.nestedscrollexample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;

public class ScrollingContent extends LinearLayout implements View.OnClickListener, NestedScrollingChild2 {
    private static final String TAG = ScrollingContent.class.getSimpleName();

    private static final int SCROLL_STAT_IDLE = 0;
    private static final int SCROLL_STAT_SCROLLING = 1;

    private NestedScrollingChildHelper mChildHelper;

    private ViewGroup mFooter;
    private TextView mFooterBtn;
    private NestedWebView mWebview;

    private int mTouchSlop;
    private float mLastY;
    private boolean mIsDraging;

    private int[] mScrollConsumed = new int[2];
    private int[] mScrollOffset = new int[2];

    private int mScrollState = SCROLL_STAT_IDLE;

    private VelocityTracker mVelocityTracker;
    private ViewFlinger mViewFlinger = new ViewFlinger();
    private OverScroller mScroller;

    public ScrollingContent(Context context) {
        this(context, null);
    }

    public ScrollingContent(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollingContent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mChildHelper = new NestedScrollingChildHelper(this);

        initWebview();
        initFooter();

        setNestedScrollingEnabled(true);
    }

    private void initWebview() {
        mWebview = new NestedWebView(getContext());

        addView(mWebview, new LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mWebview.loadUrl("https://zhuanlan.zhihu.com/p/51299664");
    }

    private void initFooter() {
        mFooter = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.scrolling_footer, null);
        mFooterBtn = mFooter.findViewById(R.id.footer_btn);
        mFooterBtn.setOnClickListener(this);
        addView(mFooter,
                new LinearLayoutCompat.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getResources().getDimensionPixelSize(R.dimen.footer_height)));
    }

    @Override
    public void onClick(View v) {
        if (v == mFooterBtn) {
            Log.d(TAG, "footer btn clicked");
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsDraging = false;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                if (mIsDraging) {
                    return true;
                }
                final float yoff = Math.abs(mLastY - ev.getRawY());

                if (yoff > mTouchSlop) {
                    // Start scrolling!
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean returnValue = false;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = MotionEventCompat.getActionMasked(event);
        float eventY = event.getRawY();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (getScrollState() == SCROLL_STAT_SCROLLING) {
                    stopScroll();
                }
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }
                if (!mIsDraging) {
                    mIsDraging = true;
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                }
                int deltaY = (int) (mLastY - eventY);
                mLastY = eventY;
                // nested pre scroll
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset, ViewCompat.TYPE_TOUCH)) {
                    deltaY -= mScrollConsumed[1];
                    event.offsetLocation(0, -mScrollOffset[1]);
                }
                mVelocityTracker.addMovement(event);

                // internal scroll
                int scrollInternalY = 0;
                if (deltaY != 0) {
                    scrollInternalY = scrollY(deltaY);
                    deltaY -= scrollInternalY;
                }

                // nested scroll
                if (deltaY != 0) {
                    dispatchNestedScroll(0, mScrollConsumed[1]+scrollInternalY, 0, deltaY, mScrollOffset, ViewCompat.TYPE_TOUCH);
                }
                returnValue = true;
                break;
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                returnValue = true;
                mVelocityTracker.computeCurrentVelocity(1000);
                onFlingY((int) -mVelocityTracker.getYVelocity());
                mVelocityTracker.clear();
                mIsDraging = false;
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                break;
        }
        return returnValue;
    }

    private int scrollY(int deltaY) {
        int remainY = deltaY;
        int consumedY = 0;
        if (remainY > 0) {
            // scroll upward

            if (mWebview != null && mWebview.canScrollUp() > 0) {
                // reader still can scroll up
                int readerScroll = Math.min(mWebview.canScrollUp(), remainY);
                mWebview.scrollBy(0, readerScroll);
                remainY -= readerScroll;
                consumedY += readerScroll;
            }

            if (remainY > 0 && getScrollY() < mFooter.getHeight()) {
                // layout can scroll up
                int layoutScroll = Math.min(mFooter.getHeight() - getScrollY(), remainY);
                scrollBy(0, layoutScroll);
                consumedY += layoutScroll;
            }
        } else {
            // scroll downward

            if (getScrollY() > 0) {
                // layout can scroll down
                int layoutScroll = Math.max(-getScrollY(), remainY);
                scrollBy(0, layoutScroll);
                remainY -= layoutScroll;
                consumedY += layoutScroll;
            }

            if (mWebview != null && mWebview.canScrollDown() > 0) {
                // reader still can scroll down
                int readerScroll = Math.max(-mWebview.canScrollDown(), remainY);
                mWebview.scrollBy(0, readerScroll);
                consumedY += readerScroll;
            }
        }

        return consumedY;
    }

    private void onFlingY(int velocity) {
        Log.d(TAG, "onFlingY: " + velocity);
        if (mScroller == null) {
            mScroller = new OverScroller(getContext());
        }
        mScroller.fling(0, 0, 0, velocity, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        setScrollState(SCROLL_STAT_SCROLLING);

        mViewFlinger.reset();
        ViewCompat.postOnAnimation(this, mViewFlinger);
    }

    private synchronized void setScrollState(int state) {
        mScrollState = state;
    }

    private synchronized int getScrollState() {
        return mScrollState;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopScroll();
    }

    private void stopScroll() {
        mViewFlinger.stop();
    }

    class ViewFlinger implements Runnable {
        private int mLastY = 0;

        public void reset() {
            mLastY = 0;
        }

        @Override
        public void run() {
            if (getScrollState() == SCROLL_STAT_IDLE) {
                return;
            }

            if (mScroller.isFinished() || !mScroller.computeScrollOffset()) {
                setScrollState(SCROLL_STAT_IDLE);
            } else {
                int curY = mScroller.getCurrY();
                int deltaY = curY - mLastY;
                mLastY = curY;

                // nested pre scroll
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset, ViewCompat.TYPE_NON_TOUCH)) {
                    deltaY -= mScrollConsumed[1];
                }

                // internal scroll
                int scrollInternalY = 0;
                if (deltaY != 0) {
                    scrollInternalY = scrollY(deltaY);
                    deltaY -= scrollInternalY;
                }

                // nested scroll
                if (deltaY != 0) {
                    dispatchNestedScroll(0, mScrollConsumed[1]+scrollInternalY, 0, deltaY, mScrollOffset, ViewCompat.TYPE_NON_TOUCH);
                }

                ViewCompat.postOnAnimation(ScrollingContent.this, this);
            }
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
            setScrollState(SCROLL_STAT_IDLE);
        }
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }
}
