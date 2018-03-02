package com.xiaomi.zkplug.intelligent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;

/**
 * 作者：liwenqi on 18/3/02 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：指定开锁联动页面
 */
public class AppointAOpenActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "AppointAOpenActivity";
    public final String[] BOOKS = {"西游记", "水浒传", "三国演义", "红楼梦"};
    public final String[][] FIGURES = {
            {"唐三藏", "孙悟空", "猪八戒", "沙和尚"},
            {"宋江", "林冲", "李逵", "鲁智深"},
            {"曹操", "刘备", "孙权", "诸葛亮", "周瑜"},
            {"贾宝玉", "林黛玉", "薛宝钗", "王熙凤"}
    };

    public final String BOOK_NAME = "book_name";
    public final String FIGURE_NAME = "figure_name";
    private ExpandableListView mExpandableListView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appoint_mode);

        initView();
    }

    private void initView(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.zelkova_lock_name);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.title_bar_more).setVisibility(View.GONE);

        mExpandableListView = (ExpandableListView) findViewById(R.id.expandable_list);
        final NormalExpandableListAdapter adapter = new NormalExpandableListAdapter(BOOKS, FIGURES);
        mExpandableListView.setAdapter(adapter);
        adapter.setOnGroupExpandedListener(new OnGroupExpandedListener() {
            @Override
            public void onGroupExpanded(int groupPosition) {
                expandOnlyOne(groupPosition);
            }
        });

        //  设置分组项的点击监听事件
        mExpandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                Log.d(TAG, "onGroupClick: groupPosition:" + groupPosition + ", id:" + id);
                // 请务必返回 false，否则分组不会展开
                return false;
            }
        });

        //  设置子选项点击监听事件
        mExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Toast.makeText(activity(), FIGURES[groupPosition][childPosition], Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
    // 每次展开一个分组后，关闭其他的分组
    private boolean expandOnlyOne(int expandedPosition) {
        boolean result = true;
        int groupLength = mExpandableListView.getExpandableListAdapter().getGroupCount();
        for (int i = 0; i < groupLength; i++) {
            if (i != expandedPosition && mExpandableListView.isGroupExpanded(i)) {
                result &= mExpandableListView.collapseGroup(i);
            }
        }
        return result;
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
        }
    }

}
