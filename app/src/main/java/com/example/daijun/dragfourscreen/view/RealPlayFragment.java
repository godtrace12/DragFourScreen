package com.example.daijun.dragfourscreen.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.example.daijun.dragfourscreen.R;

/**
 * 【说明】：预览界面
 *
 * @author daijun
 * @version 2.0
 * @date 2018/1/5 10:27
 */

public class RealPlayFragment extends Fragment implements RealPlayLayout.RealPlayCallback{
    private static final String TAG = "RealPlayFragment";
    private RealPlayLayout realPlayLayout;
    private View vDelRealplay;
    private ImageView ivDeleteIco;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_real_play,container,false);
        findViews(view);
        initViews();
        return view;
    }

    private void findViews(View view) {
        realPlayLayout = (RealPlayLayout) view.findViewById(R.id.realPlayLayout);
        vDelRealplay = view.findViewById(R.id.vDelRealplay);
        ivDeleteIco = (ImageView) view.findViewById(R.id.ivDeleteIco);
    }

    private void initViews() {
        //设置删除标志视图
        realPlayLayout.setDeleteView(vDelRealplay);
        realPlayLayout.setOpereateCallback(this);
    }

    @Override
    public void prepareDeleteRealPlay() {
        vDelRealplay.setVisibility(View.VISIBLE);
        ivDeleteIco.setImageResource(R.drawable.realplay_delete_bg);
    }

    @Override
    public void satisfyDeleteRealPlayCondition() {
        if (vDelRealplay.getVisibility() == View.GONE) {
            vDelRealplay.setVisibility(View.VISIBLE);
        }
        ivDeleteIco.setImageResource(R.drawable.realplay_delete_bg_sel);
    }

    @Override
    public void cancelOrFinishDelete() {
        vDelRealplay.setVisibility(View.GONE);
        ivDeleteIco.setImageResource(R.drawable.realplay_delete_bg);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.e(TAG, "onAttach: ");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: " );
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: " );
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: " );
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: " );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: " );
    }
}
