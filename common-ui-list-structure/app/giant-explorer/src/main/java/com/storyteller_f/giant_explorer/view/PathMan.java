package com.storyteller_f.giant_explorer.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


public class PathMan extends HorizontalScrollView {
    private static final String TAG = "PathMan";
    private final StringBuilder pathBuilder = new StringBuilder();
    PathChangeListener pathChangeListener;
    private LinearLayout linearLayout;
    private boolean scrollToEnd = false;
    public PathMan(Context context) {
        super(context);
        init(context);
    }    /**
     * 通过点击PathMan来进行跳转时，只能跳转到上级目录
     */
    OnClickListener clickListener = new OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onClick(View v) {
            //跳转路径
            int index = linearLayout.indexOfChild(v);
            String path = getPath(index);
            drawPath(path);
            if (pathChangeListener != null) {
                pathChangeListener.onSkipOnPathMan(path);
            }
        }
    };

    public PathMan(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PathMan(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PathMan(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @NonNull
    String getPath(int index) {
        if (index == 0) return "/";
        String[] split = pathBuilder.toString().split("/");
        StringBuilder path = new StringBuilder();
        for (int i = 1; i <= index; i++) {
            String name = split[i];
            path.append("/").append(name);
        }
        return path.toString();
    }

    public void append(String name) {
        pathBuilder.append("/").append(name);
        linearLayout.addView(producePathCell(name), linearLayout.getChildCount() - 1, getLayoutParam());
        scroll();
    }

    public LinearLayout getLinearLayout() {
        return linearLayout;
    }

    public void init(Context context) {
//        Log.d(TAG, "init() called");
        linearLayout = new LinearLayout(context);
        addView(linearLayout, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        drawPath("");
        linearLayout.getViewTreeObserver().addOnDrawListener(() -> {
            if (!scrollToEnd) return;
            scrollToEnd = false;
            int contentWidth = linearLayout.getWidth();
            int containerWidth = getWidth();
            int result = contentWidth - containerWidth;
//            Log.i(TAG, "scroll: 内容宽度:" + contentWidth + ";容器宽度:" + containerWidth + ";result:" + result);
            if (result > 0) {
                scrollTo(result, 0);
            }
        });
    }

    /**
     * 展示当前路径的按钮
     *
     * @param path 全路径
     */
    public void drawPath(String path) {
//        Log.d(TAG, "drawPath() called with: path = [" + path + "]");
        pathBuilder.delete(0, pathBuilder.length());
        pathBuilder.append(path);
        LinearLayout.LayoutParams layoutParams = getLayoutParam();
        linearLayout.removeAllViews();
        if (path.length() > 1) {
            String[] strings = path.split("/");
            for (String s : strings) {
                TextView textView = producePathCell(s);
                linearLayout.addView(textView, layoutParams);
            }
        }
        scroll();
    }

    private LinearLayout.LayoutParams getLayoutParam() {
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMarginEnd(20);
        return layoutParams;
    }

    private TextView producePathCell(String s) {
        TextView textView = new TextView(getContext());
        if (s.length() == 0)
            textView.setText("/");
        else
            textView.setText(s);
        textView.setTextSize(20);
        textView.setPadding(20, 10, 20, 10);
        textView.setBackgroundColor(Color.LTGRAY);
        textView.setOnClickListener(clickListener);
        return textView;
    }

    /**
     * 添加监听事件，当可以获取到宽度时进行跳转
     */
    void scroll() {
        scrollToEnd = true;
    }

    public void setPathChangeListener(PathChangeListener pathChangeListener) {
        this.pathChangeListener = pathChangeListener;
    }

    public interface PathChangeListener {
        void onSkipOnPathMan(@NonNull String pathString);

        String root();
    }


}
