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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.gyf.immersionbar.ImmersionBar;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.interfaces.BaseDialog;
import com.kongzue.dialogx.interfaces.OnMenuItemSelectListener;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import top.cronos.myreader.ActivityManage;
import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.application.SysManager;
import top.cronos.myreader.base.BaseActivity;
import top.cronos.myreader.base.BitIntentDataManager;
import top.cronos.myreader.base.observer.MyObserver;
import top.cronos.myreader.base.observer.MySingleObserver;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.common.URLCONST;
import top.cronos.myreader.databinding.ActivityReadBinding;
import top.cronos.myreader.entity.Setting;
import top.cronos.myreader.entity.ad.AdBean;
import top.cronos.myreader.enums.Font;
import top.cronos.myreader.enums.LocalBookSource;
import top.cronos.myreader.greendao.DbManager;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.BookMark;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.greendao.entity.ReadRecord;
import top.cronos.myreader.greendao.entity.ReplaceRuleBean;
import top.cronos.myreader.greendao.entity.rule.BookSource;
import top.cronos.myreader.greendao.service.BookGroupService;
import top.cronos.myreader.greendao.service.BookMarkService;
import top.cronos.myreader.greendao.service.BookService;
import top.cronos.myreader.greendao.service.ChapterService;
import top.cronos.myreader.greendao.service.ReadRecordService;
import top.cronos.myreader.model.audio.ReadAloudService;
import top.cronos.myreader.model.sourceAnalyzer.BookSourceManager;
import top.cronos.myreader.model.storage.Backup;
import top.cronos.myreader.ui.dialog.AudioPlayerDialog;
import top.cronos.myreader.ui.dialog.DialogCreator;
import top.cronos.myreader.ui.dialog.MyAlertDialog;
import top.cronos.myreader.ui.dialog.ReplaceDialog;
import top.cronos.myreader.ui.dialog.SourceExchangeDialog;
import top.cronos.myreader.ui.popmenu.AutoPageMenu;
import top.cronos.myreader.ui.popmenu.BrightnessEyeMenu;
import top.cronos.myreader.ui.popmenu.CustomizeComMenu;
import top.cronos.myreader.ui.popmenu.CustomizeLayoutMenu;
import top.cronos.myreader.ui.popmenu.ReadSettingMenu;
import top.cronos.myreader.util.BrightUtil;
import top.cronos.myreader.util.ShareUtils;
import top.cronos.myreader.util.SharedPreUtils;
import top.cronos.myreader.util.SystemUtil;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.help.DateHelper;
import top.cronos.myreader.util.help.StringHelper;
import top.cronos.myreader.util.notification.NotificationClickReceiver;
import top.cronos.myreader.util.notification.NotificationUtil;
import top.cronos.myreader.util.utils.AdUtils;
import top.cronos.myreader.util.utils.ColorUtil;
import top.cronos.myreader.util.utils.FileUtils;
import top.cronos.myreader.util.utils.NetworkUtils;
import top.cronos.myreader.util.utils.ScreenUtils;
import top.cronos.myreader.util.utils.StringUtils;
import top.cronos.myreader.util.utils.SystemBarUtils;
import top.cronos.myreader.webapi.BookApi;
import top.cronos.myreader.webapi.crawler.ReadCrawlerUtil;
import top.cronos.myreader.webapi.crawler.base.ReadCrawler;
import top.cronos.myreader.widget.BubblePopupView;
import top.cronos.myreader.widget.page.PageLoader;
import top.cronos.myreader.widget.page.PageMode;
import top.cronos.myreader.widget.page.PageView;
import top.cronos.myreader.widget.page.TxtChar;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static top.cronos.myreader.util.UriFileUtil.getPath;

/**
 * @author fengyue
 * @date 2020/10/21 16:46
 */
public class ReadActivity extends BaseActivity<ActivityReadBinding> implements ColorPickerDialogListener, View.OnTouchListener {
    private static final String TAG = ReadActivity.class.getSimpleName();

    /***************************variable*****************************/
    private Book mBook;
    private List<Book> aBooks;
    private List<Chapter> mChapters = new ArrayList<>();
    private ChapterService mChapterService;
    private BookService mBookService;
    private BookMarkService mBookMarkService;
    private NotificationUtil notificationUtil;
    private Setting mSetting;
    private ReadRecord record;

    private boolean isCollected = true;//??????????????????

    private boolean autoPage = false;//??????????????????

    private boolean loadFinish = false;

    private int curCacheChapterNum = 0;//???????????????

    private int needCacheChapterNum;//?????????????????????

    private PageLoader mPageLoader;//???????????????

    private int screenTimeOut;//?????????????????????????????????dengy???????????????

    private Runnable keepScreenRunnable;//????????????
    private Runnable autoPageRunnable;//????????????
    private Runnable sendDownloadNotification;

    private static boolean isStopDownload = true;

    private int tempCacheChapterNum;
    private int tempCount;
    private String downloadingChapter;

    private ReadCrawler mReadCrawler;

    private int downloadInterval = 150;

    private SourceExchangeDialog mSourceDialog;
    private AudioPlayerDialog mAudioPlayerDialog;

    private boolean hasChangeSource;

    private int pagePos;

    private int chapterPos;

    private Animation mTopInAnim;
    private Animation mTopOutAnim;
    private Animation mBottomInAnim;
    private Animation mBottomOutAnim;
    private int lastX, lastY;

    private long lastRecordTime;//??????????????????

    private boolean isPrivate;//??????????????????

    private boolean isFirstLoad = true;


    // ??????????????????????????????????????????
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra("level", 0);
                try {
                    mPageLoader.updateBattery(level);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // ?????????????????????
            else if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                try {
                    mPageLoader.updateTime();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Handler mHandler = new Handler();

    @Override
    protected void bindView() {
        binding = ActivityReadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**************************override***********************************/


    @Override
    protected void onStop() {
        recordReadTime();
        super.onStop();
    }

    @Override
    protected void onStart() {
        lastRecordTime = System.currentTimeMillis();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        aBooks = mSourceDialog.getaBooks();
        Intent intent = new Intent();
        if (aBooks != null && aBooks.size() > 0) {
            int sourceIndex = mSourceDialog.getSourceIndex();
            if (mSourceDialog.hasCurBookSource())
                aBooks.set(sourceIndex, mBook);
            BitIntentDataManager.getInstance().putData(intent, aBooks);
            intent.putExtra(APPCONST.SOURCE_INDEX, sourceIndex);
        } else {
            BitIntentDataManager.getInstance().putData(intent, mBook);
        }
        intent.putExtra("isCollected", isCollected);
        outState.putParcelable(INTENT, intent);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar);
        getSupportActionBar().setTitle(mBook.getName());
    }

    @Override
    protected boolean initSwipeBackEnable() {
        return false;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        super.initData(savedInstanceState);
        mBookService = BookService.getInstance();
        mChapterService = ChapterService.getInstance();
        mBookMarkService = BookMarkService.getInstance();
        mSetting = SysManager.getSetting();
        if (!loadBook()) {
            finish();
            return;
        }
        pagePos = mBook.getLastReadPosition();
        chapterPos = mBook.getHisttoryChapterNum();
        if (SharedPreUtils.getInstance().getBoolean(getString(R.string.isNightFS), false)) {
            mSetting.setDayStyle(!ColorUtil.isColorLight(getResources().getColor(R.color.textPrimary)));
        }
        //????????????
        screenTimeOut = mSetting.getResetScreen() * 60;
        //??????????????????
        keepScreenRunnable = this::unKeepScreenOn;
        autoPageRunnable = this::nextPage;
        sendDownloadNotification = this::sendNotification;

        notificationUtil = NotificationUtil.getInstance();
        isCollected = getIntent().getBooleanExtra("isCollected", true);
        hasChangeSource = getIntent().getBooleanExtra("hasChangeSource", false);

        mReadCrawler = ReadCrawlerUtil.getReadCrawler(mBook.getSource());

        mPageLoader = binding.readPvContent.getPageLoader(mBook, mReadCrawler, mSetting);
        //Dialog
        mSourceDialog = new SourceExchangeDialog(this, mBook);
        if (aBooks != null) {
            mSourceDialog.setABooks(aBooks);
        }
        String privateGroupId = SharedPreUtils.getInstance().getString("privateGroupId");
        isPrivate = !TextUtils.isEmpty(mBook.getGroupId()) && privateGroupId.equals(mBook.getGroupId());
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        ImmersionBar.with(this).fullScreen(true).init();
        //??????StatusBar
        binding.readPvContent.post(
                this::hideSystemBar
        );
        if (!mSetting.isBrightFollowSystem()) {
            BrightUtil.setBrightness(this, mSetting.getBrightProgress());
        }
        initEyeView();
        initSettingListener();
        initTopMenu();
        initBottomMenu();
        setOrientation(mSetting.isHorizontalScreen());
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initClick() {
        super.initClick();
        binding.readPvContent.setTouchListener(new PageView.TouchListener() {
            @Override
            public boolean onTouch() {
                screenOffTimerStart();
                return !hideReadMenu();
            }

            @Override
            public void center() {
                toggleMenu(true);
            }

            @Override
            public void prePage() {
                mPageLoader.setPrev(true);
            }

            @Override
            public void nextPage(boolean hasNextPage) {
                mPageLoader.setPrev(false);
                if (!hasNextPage) {
                    if (autoPage) {
                        autoPageStop();
                    }
                }
            }

            @Override
            public void cancel() {
            }

            @Override
            public void onTouchClearCursor() {
                binding.cursorLeft.setVisibility(View.INVISIBLE);
                binding.cursorRight.setVisibility(View.INVISIBLE);
                longPressMenu.hidePopupListWindow();
            }

            @Override
            public void onLongPress() {
                if (!binding.readPvContent.isRunning()) {
                    selectTextCursorShow();
                    showAction();
                }
            }
        });

        mPageLoader.setOnPageChangeListener(
                new PageLoader.OnPageChangeListener() {
                    @Override
                    public void onChapterChange(int pos) {
                        chapterPos = pos;
                        mBook.setHistoryChapterId(mChapters.get(pos).getTitle());
                    }

                    @Override
                    public void onCategoryFinish(List<Chapter> chapters) {
                        mChapters = chapters;
                        mBook.setNoReadNum(0);
                        mBook.setChapterTotalNum(chapters.size());
                        mBook.setNewestChapterTitle(chapters.get(chapters.size() - 1).getTitle());
                        if (hasChangeSource) {
                            boolean flag = mBookService.matchHistoryChapterPos(mBook, mChapters);
                            Log.d(TAG, "matchHistoryChapterPos=" + flag);
                            hasChangeSource = false;
                            if (flag) {
                                mPageLoader.skipToChapter(mBook.getHisttoryChapterNum());
                            }
                        }
                        mBookService.updateEntity(mBook);
                        loadFinish = true;
                        invalidateOptionsMenu();
                    }

                    @Override
                    public void onPageCountChange(int count) {

                    }

                    @Override
                    public void onPageChange(int pos, boolean resetRead) {
                        if (isFirstLoad) {
                            pagePos = mBook.getLastReadPosition();
                            isFirstLoad = false;
                        } else {
                            pagePos = pos;
                            saveLastChapterReadPosition();
                        }
                        mHandler.post(() -> {
                            screenOffTimerStart();
                            initMenu();
                        });
                        recordReadTime();
                        if (ReadAloudService.running) {
                            if (mPageLoader.hasChapterData(mChapters.get(mPageLoader.getChapterPos()))) {
                                if (resetRead) {
                                    if (mAudioPlayerDialog != null) {
                                        mHandler.postDelayed(() -> mAudioPlayerDialog.readAloud(), 400);
                                    }
                                    return;
                                }
                                if (pos == 0) {
                                    if (mAudioPlayerDialog != null) {
                                        mHandler.postDelayed(() -> mAudioPlayerDialog.readAloud(), 400);
                                    }
                                }
                            } else {
                                ReadAloudService.stop(ReadActivity.this);
                            }
                        }
                    }

                }
        );

        binding.readSbChapterProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.readTvPageTip.setText(String.format("%s/%s", progress + 1, seekBar.getMax() + 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //????????????
                int pagePos = seekBar.getProgress();
                if (pagePos != mPageLoader.getPagePos() && pagePos < mPageLoader.getAllPagePos()) {
                    mPageLoader.skipToPage(pagePos);
                }
            }
        });

        initBottomMenuClick();

        mSourceDialog.setOnSourceChangeListener((bean, pos) -> {
            Book bookTem = mBook.changeSource(bean);
            mBookService.updateBook(mBook, bookTem);
            mBook = bookTem;
            aBooks = mSourceDialog.getaBooks();
            aBooks.set(pos, mBook);
            toggleMenu(true);
            Intent intent = new Intent(this, ReadActivity.class)
                    .putExtra("hasChangeSource", true)
                    .putExtra(APPCONST.SOURCE_INDEX, pos);
            BitIntentDataManager.getInstance().putData(intent, aBooks);
            if (!isCollected) {
                intent.putExtra("isCollected", false);
            }
            //finish();
            exit();
            startActivity(intent);
        });

        binding.readTvListenBook.setOnClickListener(v -> {
            if (mSetting.getPageMode() == PageMode.SCROLL) {
                ToastUtils.showWarring("????????????????????????????????????!");
                return;
            }
            toggleMenu(true);
            if (mAudioPlayerDialog == null) {
                mAudioPlayerDialog = new AudioPlayerDialog(this, mPageLoader);
                mAudioPlayerDialog.setOnDismissListener(dialog -> hideSystemBar());
            }
            if (!ReadAloudService.running) {
                mAudioPlayerDialog.aloudStatus = ReadAloudService.Status.STOP;
                SystemUtil.ignoreBatteryOptimization(this);
            }
            mAudioPlayerDialog.show();
        });
        initReadLongPressPop();
        binding.cursorLeft.setOnTouchListener(this);
        binding.cursorRight.setOnTouchListener(this);
        binding.rlContent.setOnTouchListener(this);
    }

    protected void initBottomMenuClick() {
        //??????
        binding.readTvSetting.setOnClickListener(v -> {
            toggleMenu(false);
            binding.readSettingMenu.startAnimation(mBottomInAnim);
            binding.readSettingMenu.setVisibility(VISIBLE);
        });
        //?????????
        binding.readTvPreChapter.setOnClickListener(v -> mPageLoader.skipPreChapter());
        //?????????
        binding.readTvNextChapter.setOnClickListener(v -> mPageLoader.skipNextChapter());
        //??????
        binding.readTvBrightnessEye.setOnClickListener(v -> {
            hideReadMenu();
            binding.readBrightnessEyeMenu.initWidget();
            binding.readBrightnessEyeMenu.setVisibility(VISIBLE);
            binding.readBrightnessEyeMenu.startAnimation(mBottomInAnim);
        });
        //??????
        binding.readTvCategory.setOnClickListener(v -> {
            //????????????
            toggleMenu(true);
            //??????
            mHandler.postDelayed(() -> {
                Intent intent = new Intent(this, CatalogActivity.class);
                BitIntentDataManager.getInstance().putData(intent, mBook);
                this.startActivityForResult(intent, APPCONST.REQUEST_CHAPTER_PAGE);
            }, mBottomOutAnim.getDuration());
        });
        //????????????
        binding.llChapterView.setOnClickListener(v -> {
            if (mChapters != null && mChapters.size() != 0) {
                Chapter curChapter = mChapters.get(mPageLoader.getChapterPos());
                String url = NetworkUtils.getAbsoluteURL(mReadCrawler.getNameSpace(), curChapter.getUrl());
                if (!"????????????".equals(mBook.getType()) && !StringHelper.isEmpty(url)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.parse(url);
                        intent.setData(uri);
                        startActivity(intent);
                    } catch (Exception e) {
                        ToastUtils.showError(e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    @Override
    protected void processLogic() {
        super.processLogic();
        //????????????
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mReceiver, intentFilter);
        //?????????Collected?????????id??????????????????????????????????????????
        if (isCollected && !StringHelper.isEmpty(mBook.getId())
                && !isPrivate) {
            //????????????????????????
            SharedPreUtils.getInstance().putString(getString(R.string.lastRead), mBook.getId());
        }
        //????????????????????????
        mBook.setLastReadTime(DateHelper.getLongDate());
        init();

        initAd();
    }

    private void initAd() {
        AdUtils.checkHasAd().subscribe(new MySingleObserver<Boolean>() {
            @Override
            public void onSuccess(@NonNull Boolean aBoolean) {
                AdBean adBean = AdUtils.getAdConfig().getRead();
                if (aBoolean && AdUtils.adTime("read", adBean)) {
                    if (adBean.getStatus() > 0) {
                        AdUtils.showInterAd(ReadActivity.this, "read");
                    }
                }
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isVolumeTurnPage = SysManager.getSetting().isVolumeTurnPage();
        if (binding.readAblTopMenu.getVisibility() != View.VISIBLE &&
                binding.readCustomizeLayoutMenu.getVisibility() != View.VISIBLE &&
                binding.readAutoPageMenu.getVisibility() != View.VISIBLE &&
                binding.readCustomizeMenu.getVisibility() != View.VISIBLE &&
                binding.readSettingMenu.getVisibility() != View.VISIBLE &&
                binding.readBrightnessEyeMenu.getVisibility() != View.VISIBLE) {
            if (binding.readPvContent.getSelectMode() != PageView.SelectMode.Normal) {
                clearSelect();
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (ReadAloudService.running) {
                        if (mSetting.isReadAloudVolumeTurnPage()) {
                            return mPageLoader.skipToPrePage();
                        }
                    } else {
                        if (isVolumeTurnPage) {
                            return mPageLoader.skipToPrePage();
                        }
                    }
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (ReadAloudService.running) {
                        if (mSetting.isReadAloudVolumeTurnPage()) {
                            return mPageLoader.skipToNextPage();
                        }
                    } else {
                        if (isVolumeTurnPage) {
                            return mPageLoader.skipToNextPage();
                        }
                    }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (hideReadMenu()) {
            return;
        } else if (ReadAloudService.running && mAudioPlayerDialog.aloudStatus == ReadAloudService.Status.PLAY) {
            ReadAloudService.pause(this);
            ToastUtils.showInfo("???????????????");
            return;
        }
        finish();
    }

    @Override
    public void finish() {
        if (!isCollected) {
            DialogCreator.createCommonDialog(this, "????????????", "??????????????????????????????", true, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveLastChapterReadPosition();
                            isCollected = true;
                            exit();
                        }
                    }
                    , (dialog, which) -> {
                        isCollected = false;
                        mBookService.deleteBookById(mBook.getId());
                        exit();
                    });
        } else {
            saveLastChapterReadPosition();
            exit();
        }
    }

    private void exit() {
        // ?????????BookDetail
        Intent result = new Intent();
        result.putExtra(APPCONST.RESULT_IS_COLLECTED, isCollected);
        if (mPageLoader != null) {
            result.putExtra(APPCONST.RESULT_LAST_READ_POSITION, mPageLoader.getPagePos());
            result.putExtra(APPCONST.RESULT_HISTORY_CHAPTER, mPageLoader.getChapterPos());
        }
        setResult(AppCompatActivity.RESULT_OK, result);
        if (!ActivityManage.isExist(MainActivity.class)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        Backup.INSTANCE.autoBack();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mHandler.removeCallbacks(keepScreenRunnable);
        mHandler.removeCallbacks(autoPageRunnable);
        /*mHandler.removeCallbacks(sendDownloadNotification);
        notificationUtil.cancelAll();
        App.getApplication().shutdownThreadPool();*/
        if (autoPage) {
            autoPageStop();
        }
        ReadAloudService.stop(this);
        if (mPageLoader != null) {
            mPageLoader.closeBook();
            mPageLoader = null;
        }
        if (mSourceDialog != null) {
            mSourceDialog.stopSearch();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_read, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.setGroupVisible(R.id.action_load_finish, loadFinish);
        if ("????????????".equals(mBook.getType())) {
            menu.findItem(R.id.action_change_source).setVisible(false);
            menu.findItem(R.id.action_open_link).setVisible(false);
            menu.findItem(R.id.action_download).setVisible(false);
        }
        menu.findItem(R.id.action_re_seg).setChecked(mBook.getReSeg());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_change_source) {
            mSourceDialog.show();
        } else if (itemId == R.id.action_reload) {
            mPageLoader.setPrev(false);
            if (!"????????????".equals(mBook.getType()) && !mChapters.isEmpty()) {
                mChapterService.deleteChapterCacheFile(mChapters.get(mPageLoader.getChapterPos()));
            }
            if (!mChapters.isEmpty())
                mPageLoader.refreshChapter(mChapters.get(mPageLoader.getChapterPos()));
        } else if (itemId == R.id.action_add_bookmark) {
            if (mChapters == null || mChapters.size() == 0) {
                if ("????????????".equals(mBook.getType())) {
                    ToastUtils.showWarring("?????????????????????????????????????????????");
                } else {
                    ToastUtils.showError("???????????????????????????????????????!");
                }
                return true;
            }
            Chapter curChapter = mChapters.get(mPageLoader.getChapterPos());
            BookMark bookMark = new BookMark();
            bookMark.setBookId(mBook.getId());
            bookMark.setTitle(curChapter.getTitle());
            bookMark.setBookMarkChapterNum(mPageLoader.getChapterPos());
            bookMark.setBookMarkReadPosition(mPageLoader.getPagePos());
            mBookMarkService.addOrUpdateBookMark(bookMark);
            DialogCreator.createTipDialog(this, "???" + mBook.getName() +
                    "??????" + bookMark.getTitle() + "[" + (bookMark.getBookMarkReadPosition() + 1) +
                    "]\n????????????????????????????????????????????????????????????");
            return true;
        } else if (itemId == R.id.action_replace_content) {
            Intent ruleIntent = new Intent(this, ReplaceRuleActivity.class);
            startActivityForResult(ruleIntent, APPCONST.REQUEST_REFRESH_READ_UI);
        } else if (itemId == R.id.action_re_seg) {
            mBook.setReSeg(!mBook.getReSeg());
            mBookService.updateEntity(mBook);
            if (mPageLoader != null) {
                mPageLoader.refreshUi();
            }
        } else if (itemId == R.id.action_copy_content) {
            String content = mPageLoader.getContentStartPage(0);
            MyAlertDialog.showTipDialogWithLink(this, "????????????", content == null ? "?????????????????????" : content);
        } else if (itemId == R.id.action_open_link) {
            Uri uri = Uri.parse(NetworkUtils.getAbsoluteURL(mReadCrawler.getNameSpace(), mBook.getChapterUrl()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (itemId == R.id.action_download) {
            download();
        } else if (itemId == R.id.action_edit_source) {
            BookSource source = BookSourceManager.getBookSourceByStr(mBook.getSource());
            if (TextUtils.isEmpty(source.getSourceType())) {
                ToastUtils.showWarring("???????????????????????????");
            } else {
                Intent sourceIntent = new Intent(this, SourceEditActivity.class);
                sourceIntent.putExtra(APPCONST.BOOK_SOURCE, source);
                startActivity(sourceIntent);
            }
        } else if (itemId == R.id.action_search) {
            BitIntentDataManager.getInstance().putData(APPCONST.BOOK_KEY, mBook);
            BitIntentDataManager.getInstance().putData(APPCONST.CHAPTERS_KEY, mChapters);
            BitIntentDataManager.getInstance().putData(APPCONST.PAGE_LOADER_KEY, mPageLoader);
            //????????????
            toggleMenu(true);
            //??????
            mHandler.postDelayed(() -> {
                startActivityForResult(new Intent(this, SearchWordActivity.class), APPCONST.REQUEST_SEARCH_WORD);
            }, mBottomOutAnim.getDuration());
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ????????????
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case APPCONST.REQUEST_FONT:
                    Font font = (Font) data.getSerializableExtra(APPCONST.FONT);
                    mSetting.setFont(font);
//                    init();
                    App.runOnUiThread(() -> mPageLoader.setFont(font));
                    break;
                case APPCONST.REQUEST_CHAPTER_PAGE:
                    int[] chapterAndPage = data.getIntArrayExtra(APPCONST.CHAPTER_PAGE);
                    /*LLog.i(TAG, "chapterAndPage == null" + (chapterAndPage == null));
                    LLog.i(TAG, "chapterAndPage.length" + (chapterAndPage.length));*/
                    if (chapterAndPage == null) {
                        ToastUtils.showError("??????????????????!");
                        return;
                    }
                    try {
                        skipToChapterAndPage(chapterAndPage[0], chapterAndPage[1]);
                    } catch (Exception e) {
                        ToastUtils.showError("??????????????????\n" +
                                e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case APPCONST.REQUEST_REFRESH_READ_UI:
                    screenTimeOut = mSetting.getResetScreen() * 60;
                    screenOffTimerStart();
                    boolean needRefresh = data.getBooleanExtra(APPCONST.RESULT_NEED_REFRESH, false);
                    boolean upMenu = data.getBooleanExtra(APPCONST.RESULT_UP_MENU, false);
                    if (needRefresh) {
                        if (mPageLoader != null) {
                            mPageLoader.refreshUi();
                        }
                    }
                    if (upMenu) {
                        initTopMenu();
                    }
                    break;
                case APPCONST.REQUEST_SELECT_BG:
                    String bgPath = getPath(this, data.getData());
                    binding.readCustomizeLayoutMenu.setCustomBg(bgPath);
                    break;
                case APPCONST.REQUEST_IMPORT_LAYOUT:
                    String zipPath = getPath(this, data.getData());
                    binding.readCustomizeLayoutMenu.zip2Layout(zipPath);
                    break;
                case APPCONST.REQUEST_SEARCH_WORD:
                    int chapterNum = data.getIntExtra("chapterNum", chapterPos);
                    int countInChapter = data.getIntExtra("countInChapter", 0);
                    String keyword = data.getStringExtra("keyword");
                    if (!TextUtils.isEmpty(keyword))
                        mPageLoader.skipToSearch(chapterNum, countInChapter, keyword);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Callback that is invoked when a color is selected from the color picker dialog.
     *
     * @param dialogId The dialog id used to create the dialog instance.
     * @param color    The selected color
     */
    @Override
    public void onColorSelected(int dialogId, int color) {
        switch (dialogId) {
            case APPCONST.SELECT_TEXT_COLOR:
                mSetting.setTextColor(color);
                mPageLoader.setTextSize();
                break;
            case APPCONST.SELECT_BG_COLOR:
                mSetting.setBgIsColor(true);
                mSetting.setBgColor(color);
                mPageLoader.refreshUi();
                break;
        }
        SysManager.saveSetting(mSetting);
        binding.readCustomizeLayoutMenu.upColor();
    }

    /**
     * Callback that is invoked when the color picker dialog was dismissed.
     *
     * @param dialogId The dialog id used to create the dialog instance.
     */
    @Override
    public void onDialogDismissed(int dialogId) {

    }

    /**************************method*********************************/
    /**
     * ?????????
     */
    private void init() {
        if (App.isDestroy(this)) return;
        screenOffTimerStart();
        mPageLoader.init();
        mPageLoader.refreshChapterList();
    }

    private void initMenu() {
        if (mChapters != null && mChapters.size() != 0) {
            Chapter curChapter = mChapters.get(chapterPos);
            String url = curChapter.getUrl();
            binding.tvChapterTitleTop.setText(curChapter.getTitle());
            binding.tvChapterUrl.setText(StringHelper.isEmpty(url) ? curChapter.getId() :
                    NetworkUtils.getAbsoluteURL(mReadCrawler.getNameSpace(), url));
            binding.readSbChapterProgress.setProgress(pagePos);
            binding.readSbChapterProgress.setMax(mPageLoader.getAllPagePos() - 1);
            binding.readTvPageTip.setText(String.format("%s/%s",
                    binding.readSbChapterProgress.getProgress() + 1, binding.readSbChapterProgress.getMax() + 1));
        }
    }
    /************************????????????******************************/
    /**
     * ????????????????????????????????????
     * 1???????????????????????????????????????????????????Collected
     * 2??????????????????txt???????????????????????????????????????
     * 3??????????????????????????????????????????
     *
     * @return ??????????????????
     */
    private boolean loadBook() {
        //????????????????????????txt??????
        String path = null;
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                path = getPath(this, uri);
            }
        }
        if (!StringHelper.isEmpty(path)) {
            //??????txt????????????????????????????????????
            addLocalBook(path);
        } else {
            //???????????????????????????????????????txt??????
            Object obj = BitIntentDataManager.getInstance().getData(getIntent());
            if (obj instanceof Book) {
                mBook = (Book) obj;
            } else if (obj instanceof List) {
                aBooks = (List<Book>) obj;
                int bookPos = getIntent().getIntExtra(APPCONST.SOURCE_INDEX, 0);
                if (aBooks.size() > bookPos) {
                    mBook = aBooks.get(bookPos);
                }
            }
            //mBook???????????????????????????????????????
            if (mBook == null) {
                String bookId = SharedPreUtils.getInstance().getString(getString(R.string.lastRead), "");
                if ("".equals(bookId)) {//????????????????????????
                    ToastUtils.showWarring("??????????????????????????????????????????????????????????????????");
                    finish();
                    return false;
                } else {//?????????
                    mBook = mBookService.getBookById(bookId);
                    if (mBook == null) {//??????????????????????????????
                        ToastUtils.showWarring("??????????????????????????????/??????????????????????????????");
                        finish();
                        return false;
                    }//?????????????????????
                }
            }
        }
        return true;
    }

    /**
     * ??????????????????
     *
     * @param path
     */
    private void addLocalBook(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        Book book = new Book();
        book.setName(file.getName().replace(".txt", ""));
        book.setChapterUrl(path);
        book.setType(getString(R.string.local_book));
        book.setHistoryChapterId("???????????????");
        book.setNewestChapterTitle("???????????????");
        book.setAuthor(getString(R.string.local_book));
        book.setSource(LocalBookSource.local.toString());
        book.setDesc("???");
        book.setIsCloseUpdate(true);
        //??????????????????????????????
        Book existsBook = mBookService.findBookByAuthorAndName(book.getName(), book.getAuthor());
        if (book.equals(existsBook)) {
            mBook = existsBook;
            return;
        }

        if (BookGroupService.getInstance().curGroupIsPrivate()) {
            mBookService.addBookNoGroup(book);
        } else {
            mBookService.addBook(book);
        }
        mBook = book;
    }

    /**
     * ??????????????????????????????
     */
    private void addBookToCaseAndDownload() {
        DialogCreator.createCommonDialog(this, this.getString(R.string.tip), this.getString(R.string.download_no_add_tips), true, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadBook();
                isCollected = true;
            }
        }, (dialog, which) -> dialog.dismiss());
    }
    /************************????????????*************************/

    /**
     * ???????????????
     */
    private void initChapters() {
        mBook.setNoReadNum(0);
        mBook.setChapterTotalNum(mChapters.size());
        if (!StringHelper.isEmpty(mBook.getId())) {
            mBookService.updateEntity(mBook);
        }
        if (mChapters.size() == 0) {
            ToastUtils.showWarring("??????????????????????????????");
        } else {
            if (mBook.getHisttoryChapterNum() < 0) {
                mBook.setHisttoryChapterNum(0);
            } else if (mBook.getHisttoryChapterNum() >= mChapters.size()) {
                mBook.setHisttoryChapterNum(mChapters.size() - 1);
            }
            if ("????????????".equals(mBook.getType())) {
                return;
            }
            if (hasChangeSource) {
                mBookService.matchHistoryChapterPos(mBook, mChapters);
            }
        }
    }


    /**
     * ????????????????????????????????????
     *
     * @param chapterPos
     * @param pagePos
     */
    private void skipToChapterAndPage(int chapterPos, int pagePos) {
        mPageLoader.setPrev(false);
        if (StringHelper.isEmpty(mChapters.get(chapterPos).getContent())) {
            if ("????????????".equals(mBook.getType()) &&
                    !mBook.getChapterUrl().endsWith(FileUtils.SUFFIX_EPUB)) {
                ToastUtils.showWarring("?????????????????????");
                return;
            }
        }
        try {
            mPageLoader.skipToChapter(chapterPos);
            mPageLoader.skipToPage(pagePos);
        } catch (Exception e) {
            //ToastUtils.showError("?????????????????????????????????????????????\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????
     */
    public void saveLastChapterReadPosition() {
        if (mBook != null && !StringHelper.isEmpty(mBook.getId()) && mPageLoader.getPageStatus() == PageLoader.STATUS_FINISH) {
            mBook.setLastReadPosition(pagePos);
            mBook.setHisttoryChapterNum(chapterPos);
            mBookService.updateEntity(mBook);
        }
    }

    /********************????????????*************************/
    /**
     * ?????????????????????
     */
    private void initTopMenu() {
        int statusBarHeight = ImmersionBar.getStatusBarHeight(this);
        binding.readAblTopMenu.setPadding(0, statusBarHeight, 0, 0);
        if (mSetting.isNoMenuChTitle()) {
            binding.llChapterView.setVisibility(GONE);
            binding.toolbar.getLayoutParams().height = 60 + ImmersionBar.getStatusBarHeight(this);
        } else {
            binding.llChapterView.setVisibility(VISIBLE);
            binding.toolbar.getLayoutParams().height = 45 + ImmersionBar.getStatusBarHeight(this);
        }
    }

    /**
     * ?????????????????????
     */
    private void initBottomMenu() {
        //??????????????????
        //if (mSetting.getHideStatusBar()) {
        if (!mSetting.isDayStyle()) {
            binding.btnNightMode.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_sun));
        }
        binding.btnNightMode.setOnClickListener(v -> changeNightAndDaySetting(mSetting.isDayStyle()));
        if (true) {
            //???????????????mBottomMenu???????????????
            if (ImmersionBar.hasNavigationBar(this)) {
                int height = ImmersionBar.getNavigationBarHeight(this);
                binding.vwNavigationBar.getLayoutParams().height = height;
                binding.readSettingMenu.setNavigationBarHeight(height);
                binding.readCustomizeMenu.setNavigationBarHeight(height);
                binding.readCustomizeLayoutMenu.setNavigationBarHeight(height);
                binding.readAutoPageMenu.setNavigationBarHeight(height);
                binding.readBrightnessEyeMenu.setNavigationBarHeight(height);
            }
        } else {
            //??????mBottomMenu???????????????
            binding.vwNavigationBar.getLayoutParams().height = 0;
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @return ??????????????????
     */
    private boolean hideReadMenu() {
        hideSystemBar();
        boolean flag = false;
        if (binding.readAblTopMenu.getVisibility() == VISIBLE) {
            toggleMenu(true);
            flag = true;
        }
        if (binding.readSettingMenu.getVisibility() == View.VISIBLE) {
            binding.readSettingMenu.setVisibility(GONE);
            binding.readSettingMenu.startAnimation(mBottomOutAnim);
            flag = true;
        }
        if (binding.readCustomizeMenu.getVisibility() == VISIBLE) {
            binding.readCustomizeMenu.setVisibility(GONE);
            binding.readCustomizeMenu.startAnimation(mBottomOutAnim);
            flag = true;
        }
        if (binding.readCustomizeLayoutMenu.getVisibility() == VISIBLE) {
            binding.readCustomizeLayoutMenu.setVisibility(GONE);
            binding.readCustomizeLayoutMenu.startAnimation(mBottomOutAnim);
            flag = true;
        }
        if (binding.readAutoPageMenu.getVisibility() == VISIBLE) {
            binding.readAutoPageMenu.setVisibility(GONE);
            binding.readAutoPageMenu.startAnimation(mBottomOutAnim);
            flag = true;
        }
        if (binding.readBrightnessEyeMenu.getVisibility() == VISIBLE) {
            binding.readBrightnessEyeMenu.setVisibility(GONE);
            binding.readBrightnessEyeMenu.startAnimation(mBottomOutAnim);
            flag = true;
        }
        return flag;
    }

    /**
     * ??????????????????????????????
     * ??????????????????
     */
    private void toggleMenu(boolean hideStatusBar) {
        toggleMenu(hideStatusBar, false);
    }

    public void toggleMenu(boolean hideStatusBar, boolean home) {
        initMenuAnim();
        if (binding.readAblTopMenu.getVisibility() == View.VISIBLE) {
            //??????
            binding.readAblTopMenu.startAnimation(mTopOutAnim);
            binding.readLlBottomMenu.startAnimation(mBottomOutAnim);
            binding.readAblTopMenu.setVisibility(GONE);
            binding.readLlBottomMenu.setVisibility(GONE);
            if (hideStatusBar) {
                hideSystemBar();
            }
            return;
        }
        if (ReadAloudService.running && !home) {
            if (mAudioPlayerDialog != null) {
                mAudioPlayerDialog.show();
                return;
            }
        }
        if (autoPage) {
            binding.readAutoPageMenu.setVisibility(VISIBLE);
            binding.readAutoPageMenu.startAnimation(mBottomInAnim);
            return;
        }
        binding.readAblTopMenu.setVisibility(View.VISIBLE);
        binding.readLlBottomMenu.setVisibility(View.VISIBLE);
        binding.readAblTopMenu.startAnimation(mTopInAnim);
        binding.readLlBottomMenu.startAnimation(mBottomInAnim);
        showSystemBar();
    }

    //?????????????????????
    private void initMenuAnim() {
        if (mTopInAnim != null) return;
        mTopInAnim = AnimationUtils.loadAnimation(this, R.anim.slide_top_in);
        mTopOutAnim = AnimationUtils.loadAnimation(this, R.anim.slide_top_out);
        mBottomInAnim = AnimationUtils.loadAnimation(this, R.anim.slide_bottom_in);
        mBottomOutAnim = AnimationUtils.loadAnimation(this, R.anim.slide_bottom_out);
    }

    private void showSystemBar() {
        //??????
        SystemBarUtils.showUnStableStatusBar(this);
        SystemBarUtils.showUnStableNavBar(this);
    }

    private void hideSystemBar() {
        //??????
        if (binding.readAblTopMenu.getVisibility() != VISIBLE || (mAudioPlayerDialog != null && !mAudioPlayerDialog.isShowing())) {
            if (!mSetting.isShowStatusBar()) {
                SystemBarUtils.hideStableStatusBar(this);
            }
            SystemBarUtils.hideStableNavBar(this);
        }
    }

    /******************????????????*****************/

    /**
     * ?????????????????????
     */
    private void initSettingListener() {
        binding.readSettingMenu.setOnClickListener(null);
        binding.readSettingMenu.setListener(this, new ReadSettingMenu.Callback() {
            @Override
            public void onRefreshPage() {
                mPageLoader.refreshPagePara();
            }

            @Override
            public void onPageModeChange() {
                mPageLoader.setPageMode(mSetting.getPageMode());
            }

            @Override
            public void onRefreshUI() {
                mPageLoader.refreshUi();
            }

            @Override
            public void onStyleChange() {
                changeStyle();
            }

            @Override
            public void onTextSizeChange() {
                mPageLoader.setTextSize();
            }

            @Override
            public void onFontClick() {
                hideReadMenu();
                mHandler.postDelayed(() -> {
                    Intent intent = new Intent(ReadActivity.this, FontsActivity.class);
                    startActivityForResult(intent, APPCONST.REQUEST_FONT);
                }, mBottomOutAnim.getDuration());
            }

            @Override
            public void onAutoPageClick() {
                if (ReadAloudService.running) {
                    ToastUtils.showWarring("???????????????????????????");
                    return;
                }
                hideReadMenu();
                ToastUtils.showInfo("??????????????????");
                autoPage = !autoPage;
                autoPage();
            }

            @Override
            public void onHVChange() {
                setOrientation(mSetting.isHorizontalScreen());
            }

            @Override
            public void onMoreSettingClick() {
                hideReadMenu();
                mHandler.postDelayed(() -> {
                    Intent intent = new Intent(ReadActivity.this, MoreSettingActivity.class);
                    startActivityForResult(intent, APPCONST.REQUEST_REFRESH_READ_UI);
                }, mBottomOutAnim.getDuration());
            }
        });
        binding.readCustomizeMenu.setOnClickListener(null);
        binding.readCustomizeMenu.setListener(new CustomizeComMenu.Callback() {
            @Override
            public void onTextPChange() {
                mPageLoader.setTextSize();
                mSetting.setComposition(0);
                SysManager.saveSetting(mSetting);
                binding.readSettingMenu.initComposition();
            }

            @Override
            public void onMarginChange() {
                mPageLoader.upMargin();
                mSetting.setComposition(0);
                SysManager.saveSetting(mSetting);
                binding.readSettingMenu.initComposition();
            }

            @Override
            public void onRefreshUI() {
                mPageLoader.refreshUi();
            }

            @Override
            public void onReset() {
                mPageLoader.setTextSize();
                mPageLoader.upMargin();
                mSetting.setComposition(1);
                SysManager.saveSetting(mSetting);
                binding.readSettingMenu.initComposition();
                ToastUtils.showInfo("??????????????????????????????");
            }
        });
        binding.readAutoPageMenu.setOnClickListener(null);
        binding.readAutoPageMenu.setListener(new AutoPageMenu.Callback() {
            @Override
            public void onSpeedChange() {
                binding.readPvContent.autoPageOnSpeedChange();
                autoPage();
            }

            @Override
            public void onExitClick() {
                ToastUtils.showInfo("??????????????????");
                autoPageStop();
                hideReadMenu();
            }
        });
        binding.readCustomizeLayoutMenu.setOnClickListener(null);
        binding.readCustomizeLayoutMenu.setListener(this, new CustomizeLayoutMenu.Callback() {
            @Override
            public void upBg() {
                mPageLoader.refreshUi();
            }

            @Override
            public void upStyle() {
                binding.readSettingMenu.initStyleImage();
            }

        });
        binding.readBrightnessEyeMenu.setOnClickListener(null);
        binding.readBrightnessEyeMenu.setListener(this, new BrightnessEyeMenu.Callback() {
            @Override
            public void onProtectEyeChange() {
                if (mSetting.isProtectEye()) {
                    openEye();
                } else {
                    closeEye();
                }
            }

            @Override
            public void upProtectEye() {
                openEye();
            }
        });
    }

    /**
     * ??????????????????
     */
    @SuppressLint("SourceLockedOrientationActivity")
    public void setOrientation(boolean isHorizontalScreen) {
        if (isHorizontalScreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * ??????????????????
     */
    private void changeNightAndDaySetting(boolean isNight) {
        mSetting.setDayStyle(!isNight);
        SysManager.saveSetting(mSetting);
        if (Build.VERSION.SDK_INT >= 31) {
            toggleMenu(true);
            mHandler.postDelayed(() -> {
                Intent intent = new Intent(this, ReadActivity.class);
                if (aBooks != null) {
                    intent.putExtra(APPCONST.SOURCE_INDEX, mSourceDialog.getSourceIndex());
                    BitIntentDataManager.getInstance().putData(intent, aBooks);
                } else {
                    BitIntentDataManager.getInstance().putData(intent, mBook);
                }
                if (!isCollected) {
                    intent.putExtra("isCollected", false);
                }
                exit();
                App.getApplication().setNightTheme(isNight);
                startActivity(intent);
            }, mBottomOutAnim.getDuration());
        } else {
            App.getApplication().setNightTheme(isNight);
            mHandler.postDelayed(() -> {
                AppCompatActivity activity = ActivityManage.getByClass(this.getClass());
                if (activity != null) {
                    BaseDialog.initActivityContext(activity);
                }
            }, 1000);
        }
        //mPageLoader.setPageStyle(!isCurDayStyle);
    }


    /**
     * ??????????????????
     */
    private void changeStyle() {
        if (!mSetting.isDayStyle()) {
            changeNightAndDaySetting(false);
        }
        upBrightnessEye();
        mPageLoader.refreshUi();
    }

    /**
     * ?????????????????????
     */
    private void upBrightnessEye() {
        if (!mSetting.isBrightFollowSystem()) {
            BrightUtil.setBrightness(this, mSetting.getBrightProgress());
        } else {
            BrightUtil.followSystemBright(this);
        }
        if (mSetting.isProtectEye()) {
            openEye();
        } else {
            closeEye();
        }
    }


    protected void download() {
        if (!isCollected) {
            addBookToCaseAndDownload();
        } else {
            downloadBook();
        }
    }

    public void showCustomizeMenu() {
        binding.readCustomizeMenu.initWidget();
        binding.readCustomizeMenu.setVisibility(VISIBLE);
        binding.readCustomizeMenu.startAnimation(mBottomInAnim);
    }

    public void showCustomizeLayoutMenu() {
        hideReadMenu();

        binding.readCustomizeLayoutMenu.upColor();
        binding.readCustomizeLayoutMenu.setVisibility(VISIBLE);
        binding.readCustomizeLayoutMenu.startAnimation(mBottomInAnim);
    }

    /****************????????????*****************/
    /**
     * ??????????????????
     */
    private void unKeepScreenOn() {
        keepScreenOn(false);
    }

    /**
     * @param keepScreenOn ??????????????????
     */
    public void keepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * ??????????????????
     */
    private void screenOffTimerStart() {
        if (screenTimeOut < 0) {
            keepScreenOn(false);
            return;
        }
        if (screenTimeOut == 0) {
            keepScreenOn(true);
            return;
        }
        int screenOffTime = screenTimeOut * 1000 - SystemUtil.getScreenOffTime(this);
        if (screenOffTime > 0) {
            mHandler.removeCallbacks(keepScreenRunnable);
            keepScreenOn(true);
            mHandler.postDelayed(keepScreenRunnable, screenOffTime);
        } else {
            keepScreenOn(false);
        }
    }

    /***************************????????????***************************/
    private int selectedIndex;//?????????????????????

    protected void downloadBook() {
        if ("????????????".equals(mBook.getType())) {
            ToastUtils.showWarring("???" + mBook.getName() + "?????????????????????????????????");
            return;
        }
        if (!NetworkUtils.isNetWorkAvailable()) {
            ToastUtils.showWarring("??????????????????");
            return;
        }
        App.runOnUiThread(() -> {
            /*MyAlertDialog.build(this)
                    .setTitle("????????????")
                    .setSingleChoiceItems(getResources().getStringArray(R.array.download), selectedIndex,
                            (dialog, which) -> selectedIndex = which).setNegativeButton("??????", ((dialog, which) -> dialog.dismiss())).setPositiveButton("??????",
                    (dialog, which) -> {
                        switch (selectedIndex) {
                            case 0:
                                addDownload(mPageLoader.getChapterPos(), mPageLoader.getChapterPos() + 50);
                                break;
                            case 1:
                                addDownload(mPageLoader.getChapterPos() - 50, mPageLoader.getChapterPos() + 50);
                                break;
                            case 2:
                                addDownload(mPageLoader.getChapterPos(), mChapters.size());
                                break;
                            case 3:
                                addDownload(0, mChapters.size());
                                break;
                        }
                    }).show();*/
            BottomMenu.show("????????????", getResources().getStringArray(R.array.download))
                    .setSelection(selectedIndex)
                    .setOnMenuItemClickListener(new OnMenuItemSelectListener<BottomMenu>() {
                        @Override
                        public void onOneItemSelect(BottomMenu dialog, CharSequence text, int which) {
                            selectedIndex = which;
                        }
                    }).setCancelButton("??????", (baseDialog, v) -> {
                switch (selectedIndex) {
                    case 0:
                        addDownload(mPageLoader.getChapterPos(), mPageLoader.getChapterPos() + 50);
                        break;
                    case 1:
                        addDownload(mPageLoader.getChapterPos() - 50, mPageLoader.getChapterPos() + 50);
                        break;
                    case 2:
                        addDownload(mPageLoader.getChapterPos(), mChapters.size());
                        break;
                    case 3:
                        addDownload(0, mChapters.size());
                        break;
                }
                return false;
            });
        });
    }

    private void addDownload(int begin, int end) {
        /*//??????????????????
        if (!isStopDownload) {
            isStopDownload = true;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        if (SysManager.getSetting().getCatheGap() != 0) {
            downloadInterval = SysManager.getSetting().getCatheGap();
        }
        //??????????????????
        final int finalBegin = Math.max(0, begin);
        final int finalEnd = Math.min(end, mChapters.size());
        needCacheChapterNum = finalEnd - finalBegin;
        curCacheChapterNum = 0;
        isStopDownload = false;
        ArrayList<Chapter> needDownloadChapters = new ArrayList<>();
        for (int i = finalBegin; i < finalEnd; i++) {
            final Chapter chapter = mChapters.get(i);
            if (StringHelper.isEmpty(chapter.getContent())) {
                needDownloadChapters.add(chapter);
            }
        }
        needCacheChapterNum = needDownloadChapters.size();
        if (needCacheChapterNum > 0) {
            ToastUtils.showInfo("????????????????????????????????????????????????????????????");
            notificationUtil.requestNotificationPermissionDialog(ReadActivity.this);
            mHandler.postDelayed(sendDownloadNotification, 2 * downloadInterval);
        }
        App.getApplication().newThread(() -> {
            for (Chapter chapter : needDownloadChapters) {
                if (StringHelper.isEmpty(chapter.getBookId())) {
                    chapter.setId(mBook.getId());
                }
                BookApi.getChapterContent(chapter, mBook, mReadCrawler)
                        .subscribeOn(Schedulers.from(App.getApplication().getmFixedThreadPool()))
                        .subscribe(new MyObserver<String>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                addDisposable(d);
                            }

                            @Override
                            public void onNext(@NotNull String s) {
                                downloadingChapter = chapter.getTitle();
                                mChapterService.saveOrUpdateChapter(chapter, s);
                                curCacheChapterNum++;
                            }

                            @Override
                            public void onError(Throwable e) {
                                curCacheChapterNum++;
                                if (App.isDebug()) e.printStackTrace();
                            }
                        });
                try {
                    Thread.sleep(downloadInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isStopDownload) {
                    break;
                }
            }
            if (curCacheChapterNum == needCacheChapterNum) {
                ToastUtils.showInfo("???" + mBook.getName() + "???" + getString(R.string.download_already_all_tips));
            }
        });
    }

    /**
     * ????????????
     */
    private void sendNotification() {
        if (curCacheChapterNum == needCacheChapterNum) {
            notificationUtil.cancel(1001);
            return;
        } else {
            Notification notification = notificationUtil.build(APPCONST.channelIdDownload)
                    .setSmallIcon(R.drawable.ic_download)
                    //??????????????????
                    .setLargeIcon(BitmapFactory.decodeResource(App.getApplication().getResources(), R.mipmap.ic_launcher))
                    .setOngoing(true)
                    //???????????????????????????
                    .setAutoCancel(true)
                    .setContentTitle("???????????????" + mBook.getName() +
                            "[" + curCacheChapterNum + "/" + needCacheChapterNum + "]")
                    .setContentText(downloadingChapter == null ? "  " : downloadingChapter)
                    .addAction(R.drawable.ic_stop_black_24dp, "??????",
                            notificationUtil.getChancelPendingIntent(cancelDownloadReceiver.class))
                    .build();
            notificationUtil.notify(1001, notification);
        }
        if (tempCacheChapterNum < curCacheChapterNum) {
            tempCount = 1500 / downloadInterval;
            tempCacheChapterNum = curCacheChapterNum;
        } else if (tempCacheChapterNum == curCacheChapterNum) {
            tempCount--;
            if (tempCount == 0) {
                notificationUtil.cancel(1001);
                return;
            }
        }
        mHandler.postDelayed(sendDownloadNotification, 2 * downloadInterval);
    }

    public static class cancelDownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //todo ??????????????????????????????
            if (NotificationClickReceiver.CANCEL_ACTION.equals(intent.getAction())) {
                isStopDownload = true;
            }
        }
    }


    /*******************************??????????????????************************************/
    /**
     * ????????????
     */
    private void autoPage() {
        mHandler.removeCallbacks(autoPageRunnable);
        if (autoPage) {
            mPageLoader.setPageMode(PageMode.AUTO);
            mPageLoader.skipToNextPage();
            mHandler.postDelayed(autoPageRunnable, mSetting.getAutoScrollSpeed() * 1000);
        }
    }

    /**
     * ??????????????????
     */
    public void autoPageStop() {
        autoPage = false;
        mPageLoader.setPageMode(mSetting.getPageMode());
        autoPage();
    }

    /**
     * ?????????
     */
    private void nextPage() {
        App.runOnUiThread(() -> {
            screenOffTimerStart();
            autoPage();
        });
    }

    /**************************????????????*********************************/
    private View vProtectEye;

    private void initEyeView() {
        ViewGroup content = (ViewGroup) findViewById(android.R.id.content);
        vProtectEye = new FrameLayout(this);
        vProtectEye.setBackgroundColor(mSetting.isProtectEye() ? getFilterColor(mSetting.getBlueFilterPercent()) : Color.TRANSPARENT);          //????????????
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL     //?????????
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE            //????????????
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;           //?????????
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        content.addView(vProtectEye, params);
    }


    /**
     * ??????????????????
     */
    public void openEye() {
        if (vProtectEye == null) {
            initEyeView();
        }
        vProtectEye.setBackgroundColor(getFilterColor(mSetting.getBlueFilterPercent()));
    }

    /**
     * ??????????????????
     */
    public void closeEye() {
        if (vProtectEye == null) {
            initEyeView();
        }
        vProtectEye.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * ????????????
     *
     * @param blueFilterPercent ??????????????????[10-30-80]
     */
    public int getFilterColor(int blueFilterPercent) {
        int realFilter = blueFilterPercent;
        if (realFilter < 10) {
            realFilter = 10;
        } else if (realFilter > 80) {
            realFilter = 80;
        }
        int a = (int) (realFilter / 80f * 180);
        int r = (int) (200 - (realFilter / 80f) * 190);
        int g = (int) (180 - (realFilter / 80f) * 170);
        int b = (int) (60 - realFilter / 80f * 60);
        return Color.argb(a, r, g, b);
    }

    /***********************????????????????????????*************************/
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.cursor_left || v.getId() == R.id.cursor_right) {
            int ea = event.getAction();
            //final int screenWidth = dm.widthPixels;
            //final int screenHeight = dm.heightPixels;
            switch (ea) {
                case MotionEvent.ACTION_DOWN:
                    lastX = (int) event.getRawX();// ???????????????????????????????????????X??????
                    lastY = (int) event.getRawY();
                    longPressMenu.hidePopupListWindow();
                    break;
                case MotionEvent.ACTION_MOVE:
                    ImageView left = binding.getRoot().findViewWithTag("left");
                    ImageView right = binding.getRoot().findViewWithTag("right");

                    int dx = (int) event.getRawX() - lastX;
                    int dy = (int) event.getRawY() - lastY;
                    /*int l = v.getLeft() + dx;
                    int b = v.getBottom() + dy;
                    int r = v.getRight() + dx;
                    int t = v.getTop() + dy;*/

                    //v.layout(l, t, r, b);
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    //v.postInvalidate();

                    //????????????????????????
                    binding.readPvContent.setSelectMode(PageView.SelectMode.SelectMoveForward);

                    int hh = binding.cursorLeft.getHeight();
                    int ww = binding.cursorLeft.getWidth();

                    if (v.getId() == left.getId()) {
                        TxtChar first = binding.readPvContent.getCurrentTxtChar(lastX + ww, lastY - hh, true);
                        if (first != null) {
                            binding.readPvContent.setFirstSelectTxtChar(first);
                        }
                        left.setX(binding.readPvContent.getFirstSelectTxtChar().getBottomLeftPosition().x - ww);
                        left.setY(binding.readPvContent.getFirstSelectTxtChar().getBottomLeftPosition().y);
                    } else {
                        TxtChar last = binding.readPvContent.getCurrentTxtChar(lastX - ww, lastY - hh, false);
                        if (last != null) {
                            binding.readPvContent.setLastSelectTxtChar(last);
                        }
                        right.setX(binding.readPvContent.getLastSelectTxtChar().getBottomRightPosition().x);
                        right.setY(binding.readPvContent.getLastSelectTxtChar().getBottomRightPosition().y);
                    }

                    float leftX = left.getX();
                    float leftY = left.getY();
                    float rightX = right.getX();
                    float rightY = right.getY();

                    if ((leftY == rightY && leftX > rightX) || leftY > rightY) {
                        left.setTag("right");
                        left.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cursor_right));
                        right.setTag("left");
                        right.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cursor_left));

                        if (v.getId() == left.getId()) {
                            TxtChar last = binding.readPvContent.getLastSelectTxtChar();
                            last = binding.readPvContent.getNextTxtChar(last);
                            binding.readPvContent.setFirstSelectTxtChar(last);
                            if (last != null) {
                                right.setX(last.getBottomLeftPosition().x - ww);
                                right.setY(last.getBottomLeftPosition().y);
                            } else {
                                return true;
                            }
                        } else {
                            TxtChar first = binding.readPvContent.getFirstSelectTxtChar();
                            first = binding.readPvContent.getLastTxtChar(first);
                            binding.readPvContent.setLastSelectTxtChar(first);
                            if (first != null) {
                                left.setX(first.getBottomRightPosition().x);
                                left.setY(first.getBottomRightPosition().y);
                            }
                        }
                    }

                    binding.readPvContent.invalidate();

                    break;
                case MotionEvent.ACTION_UP:
                    showAction();
                    //v.layout(l, t, r, b);
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    /**
     * ??????????????????
     */
    public void showAction() {
        ImageView left = binding.getRoot().findViewWithTag("left");
        ImageView right = binding.getRoot().findViewWithTag("right");
        float x, y;
        if (left.getX() - right.getX() > 0) {
            x = right.getX() + (left.getX() - right.getX()) / 2 + ScreenUtils.dpToPx(12);
        } else {
            x = left.getX() + (right.getX() - left.getX()) / 2 + ScreenUtils.dpToPx(12);
        }
        if ((left.getY() - ScreenUtils.spToPx(mSetting.getReadWordSize()) - ScreenUtils.dpToPx(60)) < 0) {
            longPressMenu.setShowBottom(true);
            y = left.getY() + left.getHeight() * 3 / 5;
        } else {
            longPressMenu.setShowBottom(false);
            y = left.getY() - ScreenUtils.spToPx(mSetting.getReadWordSize()) - ScreenUtils.dpToPx(5);
        }
        longPressMenu.showPopupListWindow(binding.rlContent, 0, x, y,
                longPressMenuItems, longPressMenuListener);
    }

    /**
     * ??????
     */
    private void selectTextCursorShow() {
        if (binding.readPvContent.getFirstSelectTxtChar() == null || binding.readPvContent.getLastSelectTxtChar() == null)
            return;
        //show Cursor on current position
        cursorShow();
        //set current word selected
        binding.readPvContent.invalidate();
//        hideSnackBar();
    }

    /**
     * ????????????
     */
    private void cursorShow() {
        ImageView left = binding.getRoot().findViewWithTag("left");
        ImageView right = binding.getRoot().findViewWithTag("right");
        left.setVisibility(View.VISIBLE);
        right.setVisibility(View.VISIBLE);
        int hh = binding.cursorLeft.getHeight();
        int ww = binding.cursorLeft.getWidth();
        if (binding.readPvContent.getFirstSelectTxtChar() != null) {
            left.setX(binding.readPvContent.getFirstSelectTxtChar().getTopLeftPosition().x - ww);
            left.setY(binding.readPvContent.getFirstSelectTxtChar().getBottomLeftPosition().y);
            right.setX(binding.readPvContent.getFirstSelectTxtChar().getBottomRightPosition().x);
            right.setY(binding.readPvContent.getFirstSelectTxtChar().getBottomRightPosition().y);
        }
    }

    private final List<String> longPressMenuItems = new ArrayList<>();
    private BubblePopupView longPressMenu;
    private BubblePopupView.PopupListListener longPressMenuListener;

    /**
     * ??????????????????
     */
    private void initReadLongPressPop() {
        longPressMenuItems.add("??????");
        longPressMenuItems.add("??????");
        longPressMenuItems.add("??????");
        longPressMenuItems.add("??????");
        longPressMenuItems.add("??????");
        longPressMenu = new BubblePopupView(this);
        //?????????????????????????????????false?????????true?????????????????????????????????????????????
        longPressMenu.setShowTouchLocation(true);
        longPressMenu.setFocusable(false);
        longPressMenuListener = new BubblePopupView.PopupListListener() {
            @Override
            public boolean showPopupList(View adapterView, View contextView, int contextPosition) {
                return true;
            }

            @Override
            public void onPopupListClick(View contextView, int contextPosition, int position) {
                String selectString;
                switch (position) {
                    case 0:
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText(null, binding.readPvContent.getSelectStr());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clipData);
                            ToastUtils.showInfo("????????????????????????????????????");
                        }
                        clearSelect();
                        break;
                    case 1:
                        ReplaceRuleBean oldRuleBean = new ReplaceRuleBean();
                        oldRuleBean.setReplaceSummary("");
                        oldRuleBean.setEnable(true);
                        oldRuleBean.setRegex(binding.readPvContent.getSelectStr());
                        oldRuleBean.setIsRegex(false);
                        oldRuleBean.setReplacement("");
                        oldRuleBean.setSerialNumber(0);
                        oldRuleBean.setUseTo(String.format("%s;%s", mBook.getSource(), mBook.getName() + "-" + mBook.getAuthor()));
                        ReplaceDialog replaceDialog = new ReplaceDialog(ReadActivity.this, oldRuleBean
                                , () -> {
                            ToastUtils.showSuccess("?????????????????????????????????");
                            clearSelect();
                            mPageLoader.refreshUi();
                        });
                        replaceDialog.show(getSupportFragmentManager(), "replaceRule");
                        break;
                    case 2:
                        selectString = binding.readPvContent.getSelectStr();
                        speak(ReadActivity.this, selectString);
                        clearSelect();
                        break;
                    case 3:
                        selectString = StringUtils.deleteWhitespace(binding.readPvContent.getSelectStr());
                        /*MyAlertDialog.build(ReadActivity.this)
                                .setTitle(R.string.search)
                                .setItems(R.array.search_way, (dialog, which) -> {
                                    String url = "";
                                    switch (which) {
                                        case 0:
                                            url = URLCONST.BAI_DU_SEARCH;
                                            break;
                                        case 1:
                                            url = URLCONST.GOOGLE_SEARCH;
                                            break;
                                        case 2:
                                            url = URLCONST.YOU_DAO_SEARCH;
                                            break;
                                    }
                                    url = url.replace("{key}", selectString);
                                    Log.d("SEARCH_URL", url);
                                    try {
                                        Uri uri = Uri.parse(url);
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ToastUtils.showError(e.getLocalizedMessage());
                                    }
                                }).setNegativeButton("??????", null)
                                .show();*/
                        BottomMenu.show(R.string.search, getResources().getStringArray(R.array.search_way))
                                .setOnMenuItemClickListener((dialog, text, which) -> {
                                    String url = "";
                                    switch (which) {
                                        case 0:
                                            url = URLCONST.BAI_DU_SEARCH;
                                            break;
                                        case 1:
                                            url = URLCONST.GOOGLE_SEARCH;
                                            break;
                                        case 2:
                                            url = URLCONST.YOU_DAO_SEARCH;
                                            break;
                                    }
                                    url = url.replace("{key}", selectString);
                                    Log.d("SEARCH_URL", url);
                                    try {
                                        Uri uri = Uri.parse(url);
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ToastUtils.showError(e.getLocalizedMessage());
                                    }
                                    return false;
                                }).setCancelButton(R.string.cancel);
                        clearSelect();
                        break;
                    case 4:
                        selectString = binding.readPvContent.getSelectStr();
                        ShareUtils.share(ReadActivity.this, selectString);
                        clearSelect();
                        break;
                }
            }
        };
    }

    /**
     * ????????????
     */
    private void clearSelect() {
        binding.cursorLeft.setVisibility(View.INVISIBLE);
        binding.cursorRight.setVisibility(View.INVISIBLE);
        longPressMenu.hidePopupListWindow();
        binding.readPvContent.clearSelect();
    }

    private TextToSpeech textToSpeech;
    private boolean ttsInitFinish = false;
    private String lastText = "";

    /**
     * ??????
     *
     * @param context
     * @param text
     */
    public void speak(Context context, String text) {
        lastText = text;
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.CHINA);
                    ttsInitFinish = true;
                    speak(context, lastText);
                } else {
                    ToastUtils.showError("TTS??????????????????");
                }
            });
            return;
        }
        if (!ttsInitFinish) return;
        if ("".equals(text)) return;
        if (textToSpeech.isSpeaking())
            textToSpeech.stop();
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "select_text");
        lastText = "";
    }

    /**
     * ??????????????????
     */
    private void recordReadTime() {
        if (isPrivate) return;
        Single.create(emitter -> {
            if (mBook == null) return;
            if (record == null) {
                record = ReadRecordService.getInstance().get(mBook.getName(), mBook.getAuthor());
                if (record == null) {
                    record = new ReadRecord();
                    record.setId(StringHelper.getStringRandom(25));
                    record.setBookName(mBook.getName());
                    record.setBookAuthor(mBook.getAuthor());
                    record.setBookImg(NetworkUtils.getAbsoluteURL(mReadCrawler.getNameSpace(),
                            mBook.getImgUrl()));
                }
            }
            long curTime = System.currentTimeMillis();
            long deltaTime = curTime - lastRecordTime;
            lastRecordTime = curTime;
            record.setUpdateTime(curTime);
            record.setReadTime(record.getReadTime() + deltaTime);
            DbManager.getDaoSession().getReadRecordDao().insertOrReplace(record);
        }).subscribe();
    }
}
