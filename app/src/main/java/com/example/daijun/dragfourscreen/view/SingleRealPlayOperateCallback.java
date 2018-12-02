package com.example.daijun.dragfourscreen.view;

import android.view.View;

/**
 * 【说明】：
 *
 * @author daijun
 * @version 2.0
 * @date 2018/1/3 15:43
 */

public interface SingleRealPlayOperateCallback {
    /**
    *【说明】：单个预览回放窗口双击后放大/缩小回调
    *@author daijun
    *@date 2018/1/3 17:01
    *@param v 所双击的预览view
    *@return
    */
    void singleRealPlayDoubleClick(View v);

    /**
    *【说明】：单击预览窗口“+”号按钮，进行窗口选中及对该窗口选择监控点进行预览
    *@author daijun
    *@date 2018/1/3 18:47
    *@param
    *@return
    */
    void singleRealPlayAdd(View v);

    /**
     *【说明】：单击预览窗口号按钮进行选中
     *@author daijun
     *@date 2018/1/3 18:47
     *@param
     *@return
     */
    void singleRealPlayClick(View v);
}
