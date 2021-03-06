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

package top.cronos.myreader.base;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.lang.reflect.Method;
import java.util.ArrayList;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.imid.swipebacklayout.lib.app.SwipeBackActivity;
import top.cronos.myreader.ActivityManage;
import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.application.CrashHandler;
import top.cronos.myreader.application.SysManager;
import top.cronos.myreader.entity.Setting;
import top.cronos.myreader.ui.activity.SplashActivity;
import top.cronos.myreader.util.StatusBarUtil;
import top.cronos.myreader.util.utils.AdUtils;

/**
 * @author fengyue
 * @date 2020/8/12 20:02
 */
public abstract class BaseActivity<VB> extends SwipeBackActivity {
    private static final int INVALID_VAL = -1;

    protected VB binding;

    protected static final String INTENT = "intent";

    protected CompositeDisposable mDisposable;

    protected Toolbar mToolbar;


    /****************************abstract area*************************************/
    /**
     * ????????????
     */
    protected abstract void bindView();

    /************************init area************************************/
    public void addDisposable(Disposable d) {
        if (mDisposable == null) {
            mDisposable = new CompositeDisposable();
        }
        mDisposable.add(d);
    }


    /**
     * ??????Toolbar
     *
     * @param toolbar
     */
    protected void setUpToolbar(Toolbar toolbar) {
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    protected boolean initSwipeBackEnable() {
        return true;
    }

    protected void initData(Bundle savedInstanceState) {
    }

    /**
     * ???????????????
     */
    protected void initWidget() {

    }

    /**
     * ?????????????????????
     */
    protected void initClick() {
    }

    /**
     * ???????????????
     */
    protected void processLogic() {
    }


    /**
     * @return ??????????????????
     */
    protected boolean isNightTheme() {
        return !SysManager.getSetting().isDayStyle();
    }

    /**
     * ??????????????????
     *
     * @param isNightMode
     */
    protected void setNightTheme(boolean isNightMode) {
        Setting setting = SysManager.getSetting();
        setting.setDayStyle(!isNightMode);
        App.getApplication().initNightTheme();
    }


    /*************************lifecycle area*****************************************************/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initIntent(savedInstanceState);
        initTheme();
        ActivityManage.addActivity(this);
        bindView();
        setSwipeBackEnable(initSwipeBackEnable());
        initData(savedInstanceState);
        initToolbar();
        initWidget();
        initClick();
        processLogic();
    }

    private void initIntent(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Intent intent = savedInstanceState.getParcelable(INTENT);
            if (intent != null) {
                setIntent(intent);
            }
        }
    }

    private void initToolbar() {
        //??????????????????????????????????????????Toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            supportActionBar(mToolbar);
            setUpToolbar(mToolbar);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        AdUtils.backTime();
        ActivityManage.mResumeActivityCount--;
        if (ActivityManage.mResumeActivityCount <= 0
                && !App.isBackground){
            App.isBackground = true;
            Log.d("QYReader", "onActivityStarted: ??????????????????");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityManage.mResumeActivityCount++;
        if (ActivityManage.mResumeActivityCount == 1 &&
                App.isBackground) {
            App.isBackground = false;
            Log.d("QYReader", "onActivityStarted: ??????????????????");
            if (!(this instanceof SplashActivity) && AdUtils.backSplashAd()) {
                SplashActivity.start(this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityManage.removeActivity(this);
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }

    /**
     * ???????????????
     */
    public void initTheme() {
        //if (isNightTheme()) {
        //setTheme(R.style.AppNightTheme);
        /*} else {
            //curNightMode = false;
            //setTheme(R.style.AppDayTheme);
        }*/
    }

    /**************************used method area*******************************************/

    protected void startActivity(Class<? extends AppCompatActivity> activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }

    protected ActionBar supportActionBar(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        mToolbar.setNavigationOnClickListener(
                (v) -> finish()
        );
        return actionBar;
    }

    protected void setStatusBarColor(int statusColor, boolean dark) {
        //?????????????????????
        //???FitsSystemWindows?????? true ?????????????????????????????????????????????????????? padding
        StatusBarUtil.setRootViewFitsSystemWindows(this, true);
        //?????????????????????
        StatusBarUtil.setTranslucentStatus(this);
        StatusBarUtil.setStatusBarColor(this, getResources().getColor(statusColor));

        //?????????????????????????????????????????????????????????, ???????????????????????????????????????, ?????????????????????????????????
        //??????????????????????????????,?????????????????????, ??????????????????????????????????????????, ???????????????????????????????????????if??????
        if (!dark) {
            if (!StatusBarUtil.setStatusBarDarkTheme(this, true)) {
                //????????????????????????????????? ???????????????????????????????????????????????????, ?????????????????????????????????????????????,
                //???????????????+???=???, ??????????????????????????????
                StatusBarUtil.setStatusBarColor(this, 0x55000000);
            }
        }
    }

    /**
     * ??????MENU????????????
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            Drawable drawable = menu.getItem(i).getIcon();
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(getResources().getColor(R.color.textPrimary), PorterDuff.Mode.SRC_ATOP);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("PrivateApi")
    @SuppressWarnings("unchecked")
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            //????????????????????????
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                    method = menu.getClass().getDeclaredMethod("getNonActionItems");
                    ArrayList<MenuItem> menuItems = (ArrayList<MenuItem>) method.invoke(menu);
                    if (!menuItems.isEmpty()) {
                        for (MenuItem menuItem : menuItems) {
                            Drawable drawable = menuItem.getIcon();
                            if (drawable != null) {
                                drawable.mutate();
                                drawable.setColorFilter(getResources().getColor(R.color.textPrimary), PorterDuff.Mode.SRC_ATOP);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

        }
        return super.onMenuOpened(featureId, menu);
    }

}
