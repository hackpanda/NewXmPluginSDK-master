package com.xiaomi.zkplug.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {

    private OnScrollChangedListeneer onScrollChangedListeneer;// 滚动监听接口

    public MyScrollView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) { // 屏蔽touch事件,才能在监听其子控件的touch事件
        // TODO Auto-generated method stub
        super.onTouchEvent(ev);
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)// 屏蔽touch事件传递,才能在监听其子控件的touch事件
    {
        super.onInterceptTouchEvent(event);
        return false;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // TODO Auto-generated method stub
        super.onScrollChanged(l, t, oldl, oldt);
        if(onScrollChangedListeneer != null)
        {
            onScrollChangedListeneer.onScrollChanged(l, t, oldl, oldt);
        }
    }

    // 滚动事件监听，获取滚动的距离，用户处理一些其他事
    public interface OnScrollChangedListeneer
    {
        public void onScrollChanged(int l, int t, int oldl, int oldt);
    }

    public void setOnScrollChangedListeneer(OnScrollChangedListeneer onScrollChangedListeneer)
    {
        this.onScrollChangedListeneer = onScrollChangedListeneer;
    }

}
