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

package top.cronos.myreader.ui.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.application.SysManager;
import top.cronos.myreader.base.BaseActivity;
import top.cronos.myreader.base.BitIntentDataManager;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.databinding.ActivityMainBinding;
import top.cronos.myreader.entity.SharedBook;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.BookGroup;
import top.cronos.myreader.greendao.service.BookGroupService;
import top.cronos.myreader.model.sourceAnalyzer.BookSourceManager;
import top.cronos.myreader.model.storage.BackupRestoreUi;
import top.cronos.myreader.ui.dialog.DialogCreator;
import top.cronos.myreader.ui.dialog.MyAlertDialog;
import top.cronos.myreader.ui.fragment.BookcaseFragment;
import top.cronos.myreader.ui.fragment.FindFragment;
import top.cronos.myreader.ui.fragment.MineFragment;
import top.cronos.myreader.util.SharedPreUtils;
import top.cronos.myreader.util.help.StringHelper;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.utils.AdUtils;
import top.cronos.myreader.util.utils.GsonExtensionsKt;
import top.cronos.myreader.webapi.LanZouApi;
import top.cronos.myreader.widget.NoScrollViewPager;

import static androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

/**
 * @author fengyue
 * @date 2020/9/13 13:03
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> {
    public static final String TAG = MainActivity.class.getSimpleName();

    private List<Fragment> mFragments = new ArrayList<>();
    private String[] titles;
    private String groupName;
    private File appFile;
    private boolean isForceUpdate;
    private BookcaseFragment mBookcaseFragment;
    private FindFragment mFindFragment;
    private MineFragment mMineFragment;
    private Animation mBottomInAnim;
    private Animation mBottomOutAnim;

    @Override
    protected void bindView() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean startFromSplash = getIntent().getBooleanExtra("startFromSplash", false);
        if (!startFromSplash && BookGroupService.getInstance().curGroupIsPrivate()) {
            SharedPreUtils.getInstance().putString(getString(R.string.curBookGroupId), "");
            SharedPreUtils.getInstance().putString(getString(R.string.curBookGroupName), "");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
        }
        getSupportActionBar().setTitle(titles[0]);
        getSupportActionBar().setSubtitle(groupName);
        setStatusBarColor(R.color.colorPrimary, true);
    }

    @Override
    protected boolean initSwipeBackEnable() {
        return false;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        super.initData(savedInstanceState);
        groupName = SharedPreUtils.getInstance().getString(getString(R.string.curBookGroupName), "");
        titles = new String[]{"??????", "??????", "??????"};
        mBookcaseFragment = new BookcaseFragment();
        mFindFragment = new FindFragment();
        mMineFragment = new MineFragment();
        mFragments.add(mBookcaseFragment);
        mFragments.add(mFindFragment);
        mFragments.add(mMineFragment);
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        binding.viewPagerMain.setOffscreenPageLimit(2);
        binding.viewPagerMain.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            @Override
            public int getCount() {
                return mFragments.size();
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }
        });

    }

    @Override
    protected void initClick() {
        super.initClick();

        mToolbar.setOnLongClickListener(v -> {
            if (binding.viewPagerMain.getCurrentItem() == 0
                    && (mBookcaseFragment.getmBookcasePresenter() != null
                    && !mBookcaseFragment.getmBookcasePresenter().ismEditState())) {
                if (BookGroupService.getInstance().curGroupIsPrivate()) {
                    goBackNormalBookcase();
                } else {
                    goPrivateBookcase();
                }
                return true;
            }
            return false;
        });

        //BottomNavigationView ??????????????????
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            int menuId = menuItem.getItemId();
            // ?????????????????????Fragment
            switch (menuId) {
                case R.id.menu_bookshelf:
                    binding.viewPagerMain.setCurrentItem(0);
                    break;
                case R.id.menu_find_book:
                    binding.viewPagerMain.setCurrentItem(1);
                    break;
                case R.id.menu_my_config:
                    binding.viewPagerMain.setCurrentItem(2);
                    break;
            }
            return false;
        });

        // ViewPager ??????????????????
        binding.viewPagerMain.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                //?????????????????????????????? menu ?????????????????????
                binding.bottomNavigationView.getMenu().getItem(i).setChecked(true);
                getSupportActionBar().setTitle(titles[i]);
                if (i == 0) {
                    getSupportActionBar().setSubtitle(groupName);
                } else {
                    getSupportActionBar().setSubtitle("");
                }
                invalidateOptionsMenu();
                /*if (i == 1){
                    ((BookStoreFragment) mFragments.get(i)).lazyLoad();
                }*/
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

    }

    @Override
    protected void processLogic() {
        super.processLogic();
        try {
            int settingVersion = SysManager.getSetting().getSettingVersion();
            if (settingVersion < APPCONST.SETTING_VERSION) {
                SysManager.resetSetting();
                Log.d(TAG, "resetSetting");
            }
        } catch (Exception e) {
            ToastUtils.showError(e.getLocalizedMessage());
            e.printStackTrace();
        }
        try {
            int sourceVersion = SysManager.getSetting().getSourceVersion();
            if (sourceVersion < APPCONST.SOURCE_VERSION) {
                SysManager.resetSource();
                Log.d(TAG, "resetSource");
            }
        } catch (Exception e) {
            ToastUtils.showError(e.getLocalizedMessage());
            e.printStackTrace();
        }
        firstInit();
        LanZouApi.INSTANCE.checkSubscribeUpdate(this);
        AdUtils.adRecord("Usage", "usTimes");
    }

    private void firstInit() {
        SharedPreUtils sru = SharedPreUtils.getInstance();
        if (!sru.getBoolean("firstInit")) {
            BookSourceManager.initDefaultSources();
            DialogCreator.createCommonDialog(this, "??????????????????????????????",
                    "???????????????????????????????????????????????????????????????" +
                            "??????????????????????????????????????????(??????????????????????????????????????????)??????????????????????????????",
                    false, (dialog, which) -> startActivity(new Intent(this, SourceSubscribeActivity.class)),
                    null);
            sru.putBoolean("firstInit", true);
        }
    }

    private void reLoadFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        mBookcaseFragment = (BookcaseFragment) fragments.get(0);
        mFindFragment = (FindFragment) fragments.get(1);
        mMineFragment = (MineFragment) fragments.get(2);
    }

    public NoScrollViewPager getViewPagerMain() {
        return binding.viewPagerMain;
    }

    /********************************Event***************************************/
    /**
     * ????????????
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_book, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (binding.viewPagerMain.getCurrentItem() == 0) {
            if (mBookcaseFragment.getmBookcasePresenter() != null && mBookcaseFragment.getmBookcasePresenter().ismEditState()) {
                menu.findItem(R.id.action_finish).setVisible(true);
                menu.setGroupVisible(R.id.bookcase_menu, false);
            } else {
                menu.setGroupVisible(R.id.bookcase_menu, true);
                menu.findItem(R.id.action_finish).setVisible(false);
                menu.findItem(R.id.action_change_group).setVisible(SharedPreUtils
                        .getInstance().getBoolean("openGroup", true));
            }
        } else {
            menu.setGroupVisible(R.id.bookcase_menu, false);
            menu.findItem(R.id.action_finish).setVisible(false);
        }
        menu.setGroupVisible(R.id.find_menu, binding.viewPagerMain.getCurrentItem() == 1);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * ???????????????????????????
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mBookcaseFragment.isRecreate()) {
            reLoadFragment();
        }
        int itemId = item.getItemId();
        if (itemId == R.id.action_search) {
            Intent searchBookIntent = new Intent(this, SearchBookActivity.class);
            startActivity(searchBookIntent);
            return true;
        } else if (itemId == R.id.action_finish) {
            cancelEdit();
            return true;
        } else if (itemId == R.id.action_change_group || itemId == R.id.action_group_man) {
            if (!mBookcaseFragment.getmBookcasePresenter().hasOnGroupChangeListener()) {
                mBookcaseFragment.getmBookcasePresenter().addOnGroupChangeListener(() -> {
                    groupName = SharedPreUtils.getInstance().getString(getString(R.string.curBookGroupName), "????????????");
                    getSupportActionBar().setSubtitle(groupName);
                });
            }
        } else if (itemId == R.id.action_edit) {
            if (mBookcaseFragment.getmBookcasePresenter().canEditBookcase()) {
                invalidateOptionsMenu();
                initMenuAnim();
                binding.bottomNavigationView.setVisibility(View.GONE);
                binding.bottomNavigationView.startAnimation(mBottomOutAnim);
            }
        } else if (itemId == R.id.action_qr_scan) {
            Intent intent = new Intent(this, QRCodeScanActivity.class);
            startActivityForResult(intent, APPCONST.REQUEST_QR_SCAN);
        } else if (itemId == R.id.action_refresh_find) {
            mFindFragment.refreshFind();
            return true;
        }
        return mBookcaseFragment.getmBookcasePresenter().onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mBookcaseFragment.getmBookcasePresenter() != null && mBookcaseFragment.getmBookcasePresenter().ismEditState()) {
            cancelEdit();
            return;
        }
        if (System.currentTimeMillis() - APPCONST.exitTime > APPCONST.exitConfirmTime) {
            ToastUtils.showExit("??????????????????");
            APPCONST.exitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String curBookGroupId = SharedPreUtils.getInstance().getString(this.getString(R.string.curBookGroupId), "");
        BookGroup bookGroup = BookGroupService.getInstance().getGroupById(curBookGroupId);
        if (bookGroup == null) {
            groupName = "";
        } else {
            groupName = bookGroup.getName();
        }
        if (binding.viewPagerMain.getCurrentItem() == 0) {
            getSupportActionBar().setSubtitle(groupName);
        }
//        App.checkVersionByServer(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BackupRestoreUi.INSTANCE.onActivityResult(requestCode, resultCode, data);
        if ((resultCode == RESULT_OK || resultCode == RESULT_CANCELED) && requestCode == APPCONST.APP_INSTALL_CODE) {
            installProcess(appFile, isForceUpdate);//?????????????????????????????????????????????
        }
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case APPCONST.REQUEST_LOGIN:
                    if (mMineFragment.isRecreate()) {
                        reLoadFragment();
                    }
                    mMineFragment.onActivityResult(requestCode, resultCode, data);
                    break;
                case APPCONST.REQUEST_QR_SCAN:
                    if (data != null) {
                        String result = data.getStringExtra("result");
                        if (!StringHelper.isEmpty(result)) {
                            String[] string = result.split("#", 2);
                            if (string.length == 2) {
                                SharedBook sharedBook = GsonExtensionsKt.getGSON().fromJson(string[1], SharedBook.class);
                                if (sharedBook != null && !StringHelper.isEmpty(sharedBook.getChapterUrl())) {
                                    Book book = SharedBook.sharedBookToBook(sharedBook);
                                    Intent intent = new Intent(this, BookDetailedActivity.class);
                                    BitIntentDataManager.getInstance().putData(intent, book);
                                    startActivity(intent);
                                } else {
                                    ToastUtils.showError("??????????????????");
                                }
                            } else {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    Uri uri = Uri.parse(result);
                                    intent.setData(uri);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    ToastUtils.showError(e.getLocalizedMessage());
                                }
                            }
                        }
                    }
                    break;
                case APPCONST.REQUEST_GROUP_MANAGER:
                    invalidateOptionsMenu();
                    break;
            }
        }
    }


    @Override
    protected void onDestroy() {
        App.getApplication().shutdownThreadPool();
        super.onDestroy();
    }

    //?????????????????????
    public void installProcess(File file, boolean isForceUpdate) {
        if (appFile == null || !this.isForceUpdate) {
            appFile = file;
            this.isForceUpdate = isForceUpdate;
        }
        boolean haveInstallPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //???????????????????????????????????????????????????
            haveInstallPermission = getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {//????????????
                DialogCreator.createCommonDialog(this, "????????????",
                        "????????????????????????????????????????????????????????????????????????", true,
                        "??????", (dialog, which) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startInstallPermissionSettingActivity();
                            }
                        });
                return;
            }
        }
        //????????????????????????????????????
        installApk(file, isForceUpdate);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        Uri packageURI = Uri.parse("package:" + getPackageName());
        //???????????????8.0???API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        startActivityForResult(intent, APPCONST.APP_INSTALL_CODE);
    }

    /**
     * ????????????
     *
     * @param file
     * @param isForceUpdate
     */
    public void installApk(File file, boolean isForceUpdate) {
        String authority = getApplicationContext().getPackageName() + ".fileprovider";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //?????????????????????7.0??????
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri apkUri = FileProvider.getUriForFile(this, authority, file);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        startActivity(intent);
        if (isForceUpdate) {
            finish();
        }
    }

    private void goPrivateBookcase() {
        MyAlertDialog.showPrivateVerifyDia(this, needGoTo -> {
            if (needGoTo) showPrivateBooks();
        });
    }

    private void goBackNormalBookcase() {
        DialogCreator.createCommonDialog(this, "??????????????????",
                "?????????????????????????????????", true, (dialog, which) -> {
                    groupName = "";
                    SharedPreUtils.getInstance().putString(getString(R.string.curBookGroupId), "");
                    SharedPreUtils.getInstance().putString(getString(R.string.curBookGroupName), groupName);
                    getSupportActionBar().setSubtitle("");
                    if (mBookcaseFragment.isRecreate()) {
                        reLoadFragment();
                    }
                    mBookcaseFragment.init();
                }, null);
    }

    /**
     * ??????????????????
     */
    private void showPrivateBooks() {
        BookGroup bookGroup = BookGroupService.getInstance().
                getGroupById(SharedPreUtils.getInstance().getString("privateGroupId"));
        groupName = bookGroup.getName();
        SharedPreUtils.getInstance().putString(getString(R.string.curBookGroupId), bookGroup.getId());
        SharedPreUtils.getInstance().putString(getString(R.string.curBookGroupName), groupName);
        getSupportActionBar().setSubtitle(groupName);
        if (mBookcaseFragment.isRecreate()) {
            reLoadFragment();
        }
        mBookcaseFragment.init();
    }

    /**
     * ??????????????????
     */
    private void cancelEdit() {
        mBookcaseFragment.getmBookcasePresenter().cancelEdit();
        invalidateOptionsMenu();
        initMenuAnim();
        binding.bottomNavigationView.setVisibility(View.VISIBLE);
        binding.bottomNavigationView.startAnimation(mBottomInAnim);
    }

    //?????????????????????
    public void initMenuAnim() {
        if (mBottomInAnim != null) return;
        mBottomInAnim = AnimationUtils.loadAnimation(this, R.anim.slide_bottom_in);
        mBottomOutAnim = AnimationUtils.loadAnimation(this, R.anim.slide_bottom_out);
    }

    public Animation getmBottomInAnim() {
        return mBottomInAnim;
    }

    public Animation getmBottomOutAnim() {
        return mBottomOutAnim;
    }

    public interface OnGroupChangeListener {
        void onChange();
    }
}
