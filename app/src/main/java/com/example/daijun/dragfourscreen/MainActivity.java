package com.example.daijun.dragfourscreen;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.example.daijun.dragfourscreen.view.RealPlayFragment;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout rlRealPlayContent;
    /** 屏幕宽度 */
    private int screenWidth;
    /** 屏幕高度 */
    private int screenHeight;
    /** 导航栏高度 */
    private int navigationBarHeight;
    public static final float LIVE_VIEW_RATIO = 0.5625F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
    }

    private void findViews(){
        rlRealPlayContent = (RelativeLayout) findViewById(R.id.rlRealPlay);

    }

    private void initViews(){
        //添加预览Fragment
        RealPlayFragment fragment = new RealPlayFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.rlRealPlay, fragment);
        transaction.commit();
    }

}
