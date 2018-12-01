package com.lwons.nestedscrollexample;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class NestedWebView extends WebView {
    private static final String TAG = NestedWebView.class.getSimpleName();

    public NestedWebView(Context context) {
        this(context, null);
    }

    public NestedWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
    }

    public int canScrollUp() {
        int scrollY = getScrollY();
        int height = getHeight();
        int contentHeight = computeVerticalScrollRange();
        int canUp = contentHeight - height - scrollY;
        Log.d(TAG, "canUp: [contentHeight]" + contentHeight + "  [height]" + height + "  [scrollY]" + scrollY + "  [canUp]" + canUp);
        return canUp;
    }

    public int canScrollDown() {
        return getScrollY();
    }
}
