/*
 * This file is part of QYReader.
 * QYReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QYReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QYReader.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 - 2022 fengyuecanzhu
 */

package top.cronos.myreader.widget.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

/**
 * Created by fengyue on 20-11-22.
 */

public class CoverVerticalPageAnim extends VerticalPageAnim {

    private Rect mSrcRect, mDestRect;
    private GradientDrawable mBackShadowDrawableLR;

    public CoverVerticalPageAnim(int w, int h, View view, OnPageChangeListener listener) {
        super(w, h, view, listener);
        mSrcRect = new Rect(0, 0, mViewWidth, mViewHeight);
        mDestRect = new Rect(0, 0, mViewWidth, mViewHeight);
        int[] mBackShadowColors = new int[] { 0x66000000,0x00000000};
        mBackShadowDrawableLR = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, mBackShadowColors);
        mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);
    }

    @Override
    public void drawStatic(Canvas canvas) {
        if (isCancel){
            mNextBitmap = mCurBitmap.copy(Bitmap.Config.RGB_565, true);
            canvas.drawBitmap(mCurBitmap, 0, 0, null);
        }else {
            canvas.drawBitmap(mNextBitmap, 0, 0, null);
        }
    }

    @Override
    public void drawMove(Canvas canvas) {

        switch (mDirection){
            case NEXT:
                int dis = (int) (mViewHeight - mStartY + mTouchY);
                if (dis > mViewHeight){
                    dis = mViewHeight;
                }
                //??????bitmap???????????????
                mSrcRect.top = mViewHeight - dis;
                //??????bitmap???canvas???????????????
                mDestRect.bottom = dis;
                canvas.drawBitmap(mNextBitmap,0,0,null);
                canvas.drawBitmap(mCurBitmap,mSrcRect,mDestRect,null);
                addShadow(dis,canvas);
                break;
            default:
                mSrcRect.top = (int) (mViewHeight - mTouchY);
                mDestRect.bottom = (int) mTouchY;
                canvas.drawBitmap(mCurBitmap,0,0,null);
                canvas.drawBitmap(mNextBitmap,mSrcRect,mDestRect,null);
                addShadow((int) mTouchY,canvas);
                break;
        }
    }

    //????????????
    public void addShadow(int top, Canvas canvas) {
        mBackShadowDrawableLR.setBounds(0, top, mScreenWidth , top + 30);
        mBackShadowDrawableLR.draw(canvas);
    }

    @Override
    public void startAnim() {
        super.startAnim();
        int dy = 0;
        switch (mDirection){
            case NEXT:
                if (isCancel){
                    int dis = (int) ((mViewHeight - mStartY) + mTouchY);
                    if (dis > mViewHeight){
                        dis = mViewHeight;
                    }
                    dy = mViewHeight - dis;
                }else{
                    dy = (int) -(mTouchY + (mViewHeight - mStartY));
                }
                break;
            default:
                if (isCancel){
                    dy = (int) -mTouchY;
                }else{
                    dy = (int) (mViewHeight - mTouchY);
                }
                break;
        }

        //????????????????????????
        int duration = (animationSpeed * Math.abs(dy)) / mViewHeight;
        mScroller.startScroll(0, (int) mTouchY, 0, dy, duration);
    }
}
