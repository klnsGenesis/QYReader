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

package top.cronos.myreader.widget.page;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;

import android.text.TextPaint;
import android.util.DisplayMetrics;

import com.gyf.immersionbar.ImmersionBar;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.application.SysManager;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.enums.Font;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.entity.Setting;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.greendao.service.ChapterService;
import top.cronos.myreader.model.audio.ReadAloudService;
import top.cronos.myreader.util.IOUtils;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.help.ChapterContentHelp;
import top.cronos.myreader.util.utils.BitmapUtil;
import top.cronos.myreader.util.utils.MeUtils;
import top.cronos.myreader.util.utils.RxUtils;
import top.cronos.myreader.util.utils.ScreenUtils;
import top.cronos.myreader.util.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by fengyue on 20-11-21
 */

public abstract class PageLoader {
    private static final String TAG = "PageLoader";

    // ?????????????????????
    public static final int STATUS_LOADING = 1;         // ????????????
    public static final int STATUS_LOADING_CHAPTER = 2; // ??????????????????
    public static final int STATUS_FINISH = 3;          // ????????????
    public static final int STATUS_ERROR = 4;           // ???????????? (???????????????????????????)
    public static final int STATUS_EMPTY = 5;           // ?????????
    public static final int STATUS_PARING = 6;          // ???????????? (??????????????????)
    public static final int STATUS_PARSE_ERROR = 7;     // ????????????????????????(???????????????)
    public static final int STATUS_CATEGORY_EMPTY = 8;  // ????????????????????????
    public static final int STATUS_CATEGORY_ERROR = 9;  // ??????????????????

    private String errorMsg = "";
    // ???????????????????????????
    public static final int DEFAULT_MARGIN_HEIGHT = 28;
    public static final int DEFAULT_MARGIN_WIDTH = 15;
    private static final int DEFAULT_TIP_SIZE = 12;
    private static final int EXTRA_TITLE_SIZE = 4;
    private static final int TIP_ALPHA = 180;

    // ??????????????????
    protected List<Chapter> mChapterList;
    // ????????????
    protected Book mCollBook;
    // ?????????
    protected OnPageChangeListener mPageChangeListener;

    private Context mContext;
    // ???????????????
    private PageView mPageView;
    // ??????????????????
    private TxtPage mCurPage;
    // ??????????????????????????????
    private TxtChapter mPreChapter;
    // ???????????????????????????
    private TxtChapter mCurChapter;
    // ??????????????????????????????
    private TxtChapter mNextChapter;

    // ?????????????????????
    private Paint mBatteryPaint;
    // ?????????????????????
    private TextPaint mTipPaint;
    // ?????????????????????
    private TextPaint mTitlePaint;
    // ???????????????????????????(?????????????????????????????????)
    private Paint mBgPaint;
    // ???????????????????????????
    public TextPaint mTextPaint;
    // ????????????????????????
    private Setting mSettingManager;
    // ???????????????????????????????????????????????????
    private TxtPage mCancelPage;
    // ?????????????????????
//    private BookRecordBean mBookRecord;
    //??????
    String indent;
    private Disposable mPreLoadNextDisp;
    private Disposable mPreLoadPrevDisp;
    private CompositeDisposable compositeDisposable;

    /*****************params**************************/
    // ???????????????
    protected int mStatus = STATUS_LOADING;
    // ????????????????????????????????????
    protected boolean isChapterListPrepare;

    // ?????????????????????
    private boolean isChapterOpen;
    private boolean isFirstOpen = true;
    private boolean isClose;
    // ???????????????????????????
    private PageMode mPageMode;
    // ????????????????????????
//    private PageStyle mPageStyle;
    //???????????????????????????
    protected int mVisibleWidth;
    protected int mVisibleHeight;
    //???????????????
    protected int mDisplayWidth;
    protected int mDisplayHeight;
    //??????
    private int mMarginTop;
    private int mMarginBottom;
    private int mMarginLeft;
    private int mMarginRight;
    //???????????????
    private int mTextColor;
    //????????????
    private Typeface mTypeFace;
    //???????????????
    private float mTitleSize;
    //???????????????
    private float mTextSize;
    //?????????
    private float mTextInterval;
    //??????????????????
    private float mTitleInterval;
    //????????????(??????????????????????????????)
    private float mTextPara;
    private float mTitlePara;
    //??????????????????
    private int mBatteryLevel;
    //?????????????????????
    private int mBgColor;
    // ?????????
    protected int mCurChapterPos = 0;
    //??????????????????
    protected boolean isPrev;
    //??????????????????
    private int mLastChapterPos = 0;
    private int readTextLength; //???????????????
    private boolean resetReadAloud; //??????????????????
    private int readAloudParagraph = -1; //??????????????????

    private Bitmap bgBitmap;
    public ChapterContentHelp contentHelper = new ChapterContentHelp();

    protected Disposable mChapterDis = null;

    public void resetReadAloudParagraph() {
        readAloudParagraph = -1;
    }

    /*****************************init params*******************************/
    public PageLoader(PageView pageView, Book collBook, Setting setting) {
        mPageView = pageView;
        mContext = pageView.getContext();
        mCollBook = collBook;
        mChapterList = new ArrayList<>(1);
        compositeDisposable = new CompositeDisposable();
        // ?????????????????????
        mSettingManager = setting;
        // ???????????????
        initData();
        // ???????????????
        initPaint();
        // ?????????PageView
        initPageView();
        /*// ???????????????
        prepareBook();*/
    }

    public void init() {
        // ???????????????
        initData();
        // ???????????????
        initPaint();
        // ?????????PageView
        initPageView();
        // ???????????????
        prepareBook();
    }

    /**
     * ????????????
     */
    public void refreshUi() {
        initData();
        initPaint();
        upMargin();
    }

    private void initData() {
        /*// ?????????????????????
        mSettingManager = SysManager.getSetting();*/
        // ??????????????????
        mPageMode = mSettingManager.getPageMode();
        //????????????
        getFont(mSettingManager.getFont());
        indent = StringUtils.repeat(StringUtils.halfToFull(" "), mSettingManager.getIntent());

        initBgBitmap();
        // ???????????????????????????
        setUpTextParams();
    }

    private void initBgBitmap() {
        if (!mSettingManager.bgIsColor()) {
            String bgPath = mSettingManager.getBgPath();
            if (bgPath == null) {
                return;
            }
            Resources resources = App.getApplication().getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            int width = dm.widthPixels;
            int height = dm.heightPixels;
            try {
                if (mSettingManager.bgIsAssert()) {
                    bgBitmap = MeUtils.getFitAssetsSampleBitmap(mContext.getAssets(), bgPath, width, height);
                } else {
                    bgBitmap = BitmapUtil.getFitSampleBitmap(bgPath, width, height);
                }
                if (bgBitmap == null) {
                    ToastUtils.showError("??????????????????????????????????????????");
                    bgBitmap = MeUtils.getFitAssetsSampleBitmap(mContext.getAssets(), "bg/p01.jpg", width, height);
                }
            } catch (Exception e) {
                ToastUtils.showError("??????????????????????????????????????????");
                bgBitmap = MeUtils.getFitAssetsSampleBitmap(mContext.getAssets(), "bg/p01.jpg", width, height);
            }
        }
    }

    /**
     * ???????????????????????????????????????
     */
    private void setUpTextParams() {
        // ????????????
        mTextSize = ScreenUtils.spToPx(mSettingManager.getReadWordSize());
        mTitleSize = mTextSize + ScreenUtils.spToPx(EXTRA_TITLE_SIZE);
        // ?????????(????????????????????????)
        mTextInterval = (int) (mTextSize * mSettingManager.getLineMultiplier() / 2);
        mTitleInterval = (int) (mTitleSize * mSettingManager.getLineMultiplier() / 2);
        // ????????????(????????????????????????)
        mTextPara = (int) (mTextSize * mSettingManager.getParagraphSize());
        mTitlePara = (int) (mTitleSize * mSettingManager.getParagraphSize());
    }

    private void initPaint() {
        // ?????????????????????
        mTipPaint = new TextPaint();
        mTipPaint.setColor(mTextColor);
        mTipPaint.setTextAlign(Paint.Align.LEFT); // ??????????????????
        mTipPaint.setTextSize(ScreenUtils.spToPx(DEFAULT_TIP_SIZE)); // Tip?????????????????????
        mTipPaint.setTypeface(mTypeFace);
        mTipPaint.setAntiAlias(true);
        mTipPaint.setSubpixelText(true);

        // ???????????????????????????
        mTextPaint = new TextPaint();
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setLetterSpacing(mSettingManager.getTextLetterSpacing());
        mTextPaint.setTypeface(mTypeFace);
        mTextPaint.setAntiAlias(true);

        // ?????????????????????
        mTitlePaint = new TextPaint();
        mTitlePaint.setColor(mTextColor);
        mTitlePaint.setTextSize(mTitleSize);
        mTitlePaint.setLetterSpacing(mSettingManager.getTextLetterSpacing());
        mTitlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTitlePaint.setTypeface(Typeface.create(mTypeFace, Typeface.BOLD));
        mTitlePaint.setAntiAlias(true);

        // ?????????????????????
        mBatteryPaint = new TextPaint();
        mBatteryPaint.setAntiAlias(true);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setTextSize(ScreenUtils.spToPx(DEFAULT_TIP_SIZE - 3));
        mBatteryPaint.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/number.ttf"));

        // ?????????????????????
        setPageStyle();
    }

    private void initPageView() {
        //????????????
        mPageView.setPageMode(mPageMode);
        mPageView.setBgColor(mBgColor);
    }

    /****************************** public method***************************/
    /**
     * ??????????????????
     *
     * @return
     */
    public boolean skipPreChapter() {
        isPrev = false;
        if (!hasPrevChapter()) {
            return false;
        }

        // ??????????????????
        if (parsePrevChapter()) {
            mCurPage = getCurPage(0);
        } else {
            mCurPage = new TxtPage();
        }
        mPageView.drawCurPage(false);
        return true;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    public boolean skipNextChapter() {
        isPrev = false;
        if (!hasNextChapter()) {
            return false;
        }

        //????????????????????????????????????
        if (parseNextChapter()) {
            mCurPage = getCurPage(0);
        } else {
            mCurPage = new TxtPage();
        }
        mPageView.drawCurPage(false);
        return true;
    }

    /**
     * ?????????????????????
     *
     * @param pos:??? 0 ?????????
     */
    public void skipToChapter(int pos) {
        isPrev = false;
        // ????????????
        mCurChapterPos = pos;

        // ??????????????????????????????null
        mPreChapter = null;
        // ???????????????????????????????????????????????????
        if (mPreLoadNextDisp != null) {
            mPreLoadNextDisp.dispose();
        }
        if (mPreLoadPrevDisp != null) {
            mPreLoadPrevDisp.dispose();
        }
        // ???????????????????????????null
        mNextChapter = null;

        // ??????????????????
        openChapter();
    }

    /**
     * ?????????????????????
     *
     * @param pos
     */
    public boolean skipToPage(int pos) {
        if (!isChapterListPrepare) {
            return false;
        }
        mCurPage = getCurPage(pos);
        mPageView.drawCurPage(false);
        return true;
    }

    /**
     * ???????????????
     *
     * @return
     */
    public boolean skipToPrePage() {
        return mPageView.autoPrevPage();
    }

    /**
     * ???????????????
     *
     * @return
     */
    public boolean skipToNextPage() {
        return mPageView.autoNextPage();
    }

    /**
     * ???????????????,?????????
     */
    private void noAnimationToNextPage() {
        if (getPagePos() < mCurChapter.getPageSize() - 1) {
            skipToPage(getPagePos() + 1);
            return;
        }
        skipNextChapter();
    }

    /**
     * ???????????????,?????????
     */
    void noAnimationToPrePage() {
        if (getPagePos() > 0) {
            skipToPage(getPagePos() - 1);
            return;
        }
        skipPreChapter();
        skipToPage(mCurChapter.getPageSize() - 1);
    }

    /**
     * ????????????
     */
    public void updateTime() {
        if (!mPageView.isRunning() && !mSettingManager.isShowStatusBar()) {
            mPageView.drawCurPage(mPageMode == PageMode.SCROLL);
        }
    }

    /**
     * ????????????
     *
     * @param level
     */
    public void updateBattery(int level) {
        if (!mPageView.isRunning() && !mSettingManager.isShowStatusBar()) {
            mBatteryLevel = level;
            mPageView.drawCurPage(mPageMode == PageMode.SCROLL);
        }
    }

    /**
     * ???????????????????????????
     *
     * @param textSize:????????? px???
     */
    public void setTipTextSize(int textSize) {
        mTipPaint.setTextSize(textSize);

        // ??????????????????????????????
        mPageView.drawCurPage(false);
    }

    /**
     * ????????????????????????
     */
    public void setTextSize() {
        // ????????????????????????
        setUpTextParams();
        initPaint();
        refreshPagePara();
    }

    /**
     * ??????????????????
     *
     * @param dayMode
     */
    public void setNightMode(boolean dayMode) {
        if (!dayMode) {
            mBatteryPaint.setColor(Color.WHITE);
        } else {
            mBatteryPaint.setColor(Color.BLACK);
        }
    }

    /**
     * ??????????????????
     */
    public void setPageStyle() {
        int textColorId;
        int bgColorId;
        textColorId = mSettingManager.getTextColor();
        bgColorId = mSettingManager.getBgColor();

        mTextColor = textColorId;
        mBgColor = bgColorId;
        mBatteryPaint.setColor(Color.BLACK);

        // ????????????????????????
        mTipPaint.setColor(mTextColor);
        mTitlePaint.setColor(mTextColor);
        mTextPaint.setColor(mTextColor);
        mBatteryPaint.setColor(mTextColor);

        mBatteryPaint.setAlpha(TIP_ALPHA);

        mPageView.drawCurPage(false);
    }

    /**
     * ????????????
     *
     * @param pageMode:????????????
     * @see PageMode
     */
    public void setPageMode(PageMode pageMode) {
        mPageMode = pageMode;

        mPageView.setPageMode(mPageMode);

        // ?????????????????????
        mPageView.drawCurPage(false);
    }

    /**
     * ????????????
     *
     * @param font
     */
    public void setFont(Font font) {
        mSettingManager = SysManager.getSetting();
        //????????????
        getFont(font);
        mTipPaint.setTypeface(mTypeFace);
        mTextPaint.setTypeface(mTypeFace);
        mTitlePaint.setTypeface(mTypeFace);
        refreshPagePara();
    }

    public void refreshPagePara() {
        // ????????????
        mPreChapter = null;
        mNextChapter = null;

        // ??????????????????????????????
        if (isChapterListPrepare && mStatus == STATUS_FINISH) {
            // ????????????????????????
            dealLoadPageList(mCurChapterPos);

            // ??????????????????????????????????????????????????????????????????????????????????????????
            if (mCurPage.position >= mCurChapter.getPageSize()) {
                mCurPage.position = mCurChapter.getPageSize() - 1;
            }

            // ????????????????????????
            mCurPage = mCurChapter.getPage(mCurPage.position);
        }

        mPageView.drawCurPage(false);
    }

    /**
     * ????????????
     *
     * @param font
     */
    public void getFont(Font font) {
        String fontFileName = mSettingManager.getFont().toString() + ".ttf";
        if (font == Font.????????????) {
            fontFileName = mSettingManager.getLocalFontName();
        }
        File fontFile = new File(APPCONST.FONT_BOOK_DIR + fontFileName);
        if (font == Font.???????????? || !fontFile.exists()) {
            mTypeFace = null;
            if (!fontFile.exists()) {
                mSettingManager.setFont(Font.????????????);
                SysManager.saveSetting(mSettingManager);
            }
        } else {
            try {
                mTypeFace = Typeface.createFromFile(fontFile);
            } catch (Exception e) {
                ToastUtils.showError(e.getLocalizedMessage());
                mSettingManager.setFont(Font.????????????);
                SysManager.saveSetting(mSettingManager);
            }
        }
    }

    /**
     * ????????????
     *
     * @param chapter
     */
    public void refreshChapter(Chapter chapter) {
        chapter.setContent(null);
        ChapterService.getInstance().deleteChapterCacheFile(chapter);
        openChapter();
    }

    /**
     * ?????????????????????????????? ????????? px
     */
    public void upMargin() {
        prepareDisplay(mDisplayWidth, mDisplayHeight);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mPageChangeListener = listener;

        // ?????????????????????????????????????????????????????????????????????
        if (isChapterListPrepare) {
            mPageChangeListener.onCategoryFinish(mChapterList);
        }
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public int getPageStatus() {
        return mStatus;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    public Book getCollBook() {
        return mCollBook;
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    public List<Chapter> getChapterCategory() {
        return mChapterList;
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public int getPagePos() {
        if (mCurPage == null) {
            return 0;
        }
        return mCurPage.position;
    }

    /**
     * ???????????????
     *
     * @return
     */
    public int getAllPagePos() {
        if (mCurChapter == null) {
            return 0;
        }
        return mCurChapter.getPageSize();
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public int getChapterPos() {
        return mCurChapterPos;
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    public int getMarginTop() {
        return mMarginTop;
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    public int getMarginBottom() {
        return mMarginBottom;
    }

    /**
     * ???????????????
     */
    private void prepareBook() {
        mCurChapterPos = mCollBook.getHisttoryChapterNum();
        mLastChapterPos = mCurChapterPos;
    }

    /**
     * ??????????????????
     */
    public void openChapter() {
        isFirstOpen = false;

        if (!mPageView.isPrepare()) {
            return;
        }

        // ?????????????????????????????????
        if (!isChapterListPrepare) {
            mStatus = STATUS_LOADING_CHAPTER;
            mPageView.drawCurPage(false);
            return;
        }

        // ????????????????????????????????????
        if (mChapterList.isEmpty()) {
            mStatus = STATUS_CATEGORY_EMPTY;
            mPageView.drawCurPage(false);
            return;
        }

        if (parseCurChapter()) {
            // ????????????????????????
            if (!isChapterOpen) {
                int position = mCollBook.getLastReadPosition();

                // ???????????????????????????????????????????????????
                if (position >= mCurChapter.getPageSize()) {
                    position = mCurChapter.getPageSize() - 1;
                }
                mCurPage = getCurPage(position);
                mCancelPage = mCurPage;
                // ????????????
                isChapterOpen = true;
            } else {
                mCurPage = getCurPage(0);
            }
        } else {
            mCurPage = new TxtPage();
        }

        mPageView.drawCurPage(false);
    }

    /**
     * ????????????????????????????????????
     */
    protected void openChapterInLastPage() {
        if (parseCurChapter()) {
            mCurPage = getCurPage(getAllPagePos() - 1);
        } else {
            mCurPage = new TxtPage();
        }
        mPageView.drawCurPage(false);
    }

    public void chapterError(String msg) {
        error(STATUS_ERROR, msg);
    }

    public void error(int status, String msg) {
        //????????????
        mStatus = status;
        errorMsg = msg;
        mPageView.drawCurPage(false);
    }

    /**
     * ????????????
     */
    public void closeBook() {
        isChapterListPrepare = false;
        isClose = true;

        if (mPreLoadNextDisp != null) {
            mPreLoadNextDisp.dispose();
        }
        if (mPreLoadPrevDisp != null) {
            mPreLoadPrevDisp.dispose();
        }

        clearList(mChapterList);

        mChapterList = null;
        mPreChapter = null;
        mCurChapter = null;
        mNextChapter = null;
        mPageView = null;
        mCurPage = null;
        if (mChapterDis != null) {
            mChapterDis.dispose();
            mChapterDis = null;
        }
    }

    private void clearList(List list) {
        if (list != null) {
            list.clear();
        }
    }

    public boolean isClose() {
        return isClose;
    }

    public boolean isChapterOpen() {
        return isChapterOpen;
    }

    /**
     * ??????????????????
     *
     * @param chapterPos:????????????
     * @return
     */
    private TxtChapter loadPageList(int chapterPos) throws Exception {
        // ????????????
        Chapter chapter = mChapterList.get(chapterPos);
        // ????????????????????????
        if (!hasChapterData(chapter)) {
            return null;
        }
        // ????????????????????????
        String reader = getChapterReader(chapter);
        TxtChapter txtChapter = loadPages(chapter, reader);

        return txtChapter;
    }


    /*******************************abstract method***************************************/

    /**
     * ??????????????????
     */
    public abstract void refreshChapterList();

    /**
     * ?????????????????????
     *
     * @param chapter
     * @return
     */
    public abstract String getChapterReader(Chapter chapter) throws Exception;

    /**
     * ????????????????????????
     *
     * @return
     */
    public abstract boolean hasChapterData(Chapter chapter);

    /***********************************default method***********************************************/

    void drawPage(Bitmap bitmap, boolean isUpdate) {
        drawBackground(mPageView.getBgBitmap());
        if (!isUpdate) {
            drawContent(bitmap);
        }
        //????????????
        mPageView.invalidate();
    }

    /**
     * ????????????????????????
     */
    private synchronized void drawBackground(Bitmap bitmap) {
        if (bitmap == null) return;
        Canvas canvas = new Canvas(bitmap);
        if (!mSettingManager.bgIsColor()) {
            if (bgBitmap == null || bgBitmap.isRecycled()) {
                initBgBitmap();
            }
            Rect mDestRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(bgBitmap, null, mDestRect, null);
        } else {
            canvas.drawColor(mBgColor);
        }
        drawBackground(canvas);
    }

    public Bitmap getBgBitmap() {
        initBgBitmap();
        return bgBitmap;
    }

    private void drawBackground(Canvas canvas) {
        if (canvas == null) return;
        int tipMarginHeight = ScreenUtils.dpToPx(3);
        String progress = (mStatus != STATUS_FINISH) ? ""
                : getReadProgress(getChapterPos(), mChapterList.size(), getPagePos(), getAllPagePos());
        /****????????????****/
        if (!mSettingManager.isShowStatusBar()) {
            //??????????????????:??????text???y???????????????text????????????????????????????????????text??????????????????
            float tipTop = tipMarginHeight - mTipPaint.getFontMetrics().top;
            if (!mChapterList.isEmpty() && mStatus == STATUS_FINISH) {
                /*****????????????????????????********/
                String title = getPagePos() == 0 ? mCollBook.getName() : mCurPage.title;
                title = contentHelper.replaceContent(mCollBook.getName() + "-" + mCollBook.getAuthor(), mCollBook.getSource(), title, true);
                title = TextUtils.ellipsize(title, mTipPaint, mDisplayWidth - mMarginLeft - mMarginRight - mTipPaint.measureText(progress), TextUtils.TruncateAt.END).toString();
                canvas.drawText(title, mMarginLeft, tipTop, mTipPaint);
                /******????????????********/
                // ???????????????????????????Y
                float y = mDisplayHeight - mTipPaint.getFontMetrics().bottom - tipMarginHeight;
                String percent = (mCurPage.position + 1) + "/" + mCurChapter.getPageSize();
                canvas.drawText(percent, mMarginLeft, y, mTipPaint);
            } else {
                String title = mCollBook.getName();
                title = contentHelper.replaceContent(mCollBook.getName() + "-" + mCollBook.getAuthor(), mCollBook.getSource(), title, true);
                canvas.drawText(title, mMarginLeft, tipTop, mTipPaint);
            }
            /*******????????????*******/
            float progressTipLeft = mDisplayWidth - mMarginRight - mTipPaint.measureText(progress);
            canvas.drawText(progress, progressTipLeft, tipTop, mTipPaint);
        } else {
            float tipBottom = mDisplayHeight - mTipPaint.getFontMetrics().bottom - tipMarginHeight;
            if (!mChapterList.isEmpty() && mStatus == STATUS_FINISH) {

                /******????????????********/
                String percent = (mCurPage.position + 1) + "/" + mCurChapter.getPageSize();
                //?????????x??????
                float tipLeft = mDisplayWidth - 2 * mMarginRight - mTipPaint.measureText(percent + progress);
                canvas.drawText(percent, tipLeft, tipBottom, mTipPaint);

                String title = getPagePos() == 0 ? mCollBook.getName() : mCurPage.title;
                title = contentHelper.replaceContent(mCollBook.getName() + "-" + mCollBook.getAuthor(), mCollBook.getSource(), title, true);
                title = TextUtils.ellipsize(title, mTipPaint, tipLeft - 2 * mMarginRight, TextUtils.TruncateAt.END).toString();
                canvas.drawText(title, mMarginLeft, tipBottom, mTipPaint);
            } else {
                String title = mCollBook.getName();
                title = contentHelper.replaceContent(mCollBook.getName() + "-" + mCollBook.getAuthor(), mCollBook.getSource(), title, true);
                canvas.drawText(title, mMarginLeft, tipBottom, mTipPaint);
            }
            /*******????????????*******/
            float progressTipLeft = mDisplayWidth - mMarginRight - mTipPaint.measureText(progress);
            canvas.drawText(progress, progressTipLeft, tipBottom, mTipPaint);
        }


        if (!mSettingManager.isShowStatusBar()) {
            /******????????????********/

            int visibleRight = mDisplayWidth - mMarginRight;
            int visibleBottom = mDisplayHeight - tipMarginHeight;

            int outFrameWidth = (int) mTipPaint.measureText("xxx");
            int outFrameHeight = (int) mTipPaint.getTextSize();

            int polarHeight = ScreenUtils.dpToPx(6);
            int polarWidth = ScreenUtils.dpToPx(2);
            int border = 1;
            int innerMargin = 1;

            //???????????????
            int polarLeft = visibleRight - polarWidth;
            int polarTop = visibleBottom - (outFrameHeight + polarHeight) / 2;
            Rect polar = new Rect(polarLeft, polarTop, visibleRight,
                    polarTop + polarHeight - ScreenUtils.dpToPx(2));

            mBatteryPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(polar, mBatteryPaint);

            //???????????????
            int outFrameLeft = polarLeft - outFrameWidth;
            int outFrameTop = visibleBottom - outFrameHeight;
            int outFrameBottom = visibleBottom - ScreenUtils.dpToPx(2);
            Rect outFrame = new Rect(outFrameLeft, outFrameTop, polarLeft, outFrameBottom);

            mBatteryPaint.setStyle(Paint.Style.STROKE);
            mBatteryPaint.setStrokeWidth(border);
            canvas.drawRect(outFrame, mBatteryPaint);


            //????????????
            mBatteryPaint.setStyle(Paint.Style.FILL);
            Paint.FontMetrics fontMetrics = mBatteryPaint.getFontMetrics();
            String batteryLevel = String.valueOf(mBatteryLevel);
            float batTextLeft = outFrameLeft + (outFrameWidth - mBatteryPaint.measureText(batteryLevel)) / 2 - ScreenUtils.dpToPx(1) / 2f;
            float batTextBaseLine = visibleBottom - outFrameHeight / 2f - fontMetrics.top / 2 - fontMetrics.bottom / 2 - ScreenUtils.dpToPx(1);
            canvas.drawText(batteryLevel, batTextLeft, batTextBaseLine, mBatteryPaint);

            /******??????????????????********/
            //???????????????????????????Y
            float y = mDisplayHeight - mTipPaint.getFontMetrics().bottom - tipMarginHeight;
            String time = StringUtils.dateConvert(System.currentTimeMillis(), "HH:mm");
            float x = (mDisplayWidth - mTipPaint.measureText(time)) / 2;
            canvas.drawText(time, x, y, mTipPaint);
        }
    }

    private void drawContent(Bitmap bitmap) {
        Canvas canvas = new Canvas(bitmap);

        if (mPageMode == PageMode.SCROLL) {
            canvas.drawColor(mBgColor);
        }
        /******????????????****/

        if (mStatus != STATUS_FINISH) {
            //????????????
            String tip = "";
            switch (mStatus) {
                case STATUS_LOADING:
                    if (isChapterListPrepare) {
                        tip = mChapterList.get(mCurChapterPos).getTitle();
                    }
                    tip += "\n????????????????????????...";
                    break;
                case STATUS_LOADING_CHAPTER:
                    tip = "????????????????????????...";
                    break;
                case STATUS_ERROR:
                    tip = "????????????????????????\n" + errorMsg;
                    break;
                case STATUS_EMPTY:
                    tip = "??????????????????";
                    break;
                case STATUS_PARING:
                    tip = "???????????????????????????...";
                    break;
                case STATUS_PARSE_ERROR:
                    tip = "??????????????????\n" + errorMsg;
                    break;
                case STATUS_CATEGORY_EMPTY:
                    tip = "??????????????????";
                    break;
                case STATUS_CATEGORY_ERROR:
                    tip = "??????????????????\n" + errorMsg;
                    break;
            }
            if (mStatus == STATUS_ERROR || mStatus == STATUS_CATEGORY_ERROR
                    || mStatus == STATUS_PARSE_ERROR
                    || (isChapterListPrepare && mStatus == STATUS_LOADING)) {
                drawErrorMsg(canvas, tip, 0);
            } else {
                //??????????????????????????????
                Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
                float textHeight = fontMetrics.top - fontMetrics.bottom;
                float textWidth = mTextPaint.measureText(tip);
                float pivotX = (mDisplayWidth - textWidth) / 2;
                float pivotY = (mDisplayHeight - textHeight) / 2;
                canvas.drawText(tip, pivotX, pivotY, mTextPaint);
            }
        } else {
            float top;
            if (mPageMode == PageMode.SCROLL) {
                top = -mTextPaint.getFontMetrics().top;
            } else {
                top = mMarginTop - mTextPaint.getFontMetrics().top;
                if (mSettingManager.isShowStatusBar()) {
                    top += ImmersionBar.getStatusBarHeight((Activity) mContext);
                }
            }
            Paint.FontMetrics fontMetricsForTitle = mTitlePaint.getFontMetrics();
            Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
            //???????????????
            float interval = mTextInterval + mTextPaint.getTextSize();
            float para = mTextPara + mTextPaint.getTextSize();
            float titleInterval = mTitleInterval + mTitlePaint.getTextSize();
            float titlePara = mTitlePara + mTextPaint.getTextSize();
            String str = null;
            int ppp = 0;//pzl,????????????
            //?????????????????????
            boolean isLight;
            int titleLen = 0;
            for (int i = 0; i < mCurPage.titleLines; ++i) {
                str = mCurPage.lines.get(i);
                titleLen += str.length();
                isLight = ReadAloudService.running && readAloudParagraph == 0;
                mTitlePaint.setColor(isLight ? mContext.getResources().getColor(R.color.sys_color) : mTextColor);

                //??????????????????
                if (i == 0) {
                    top += mTitlePara;
                }
                //??????????????????????????????
                int start = (int) (mDisplayWidth - mTitlePaint.measureText(str)) / 2;
                //????????????
                canvas.drawText(str, start, top, mTitlePaint);

                //pzl
                float leftposition = start;
                float rightposition = 0;
                float bottomposition = top + mTitlePaint.getFontMetrics().descent;
                float TextHeight = Math.abs(fontMetricsForTitle.ascent) + Math.abs(fontMetricsForTitle.descent);

                if (mCurPage.txtLists != null) {
                    for (TxtChar c : mCurPage.txtLists.get(i).getCharsData()) {
                        rightposition = leftposition + c.getCharWidth();
                        Point tlp = new Point();
                        c.setTopLeftPosition(tlp);
                        tlp.x = (int) leftposition;
                        tlp.y = (int) (bottomposition - TextHeight);

                        Point blp = new Point();
                        c.setBottomLeftPosition(blp);
                        blp.x = (int) leftposition;
                        blp.y = (int) bottomposition;

                        Point trp = new Point();
                        c.setTopRightPosition(trp);
                        trp.x = (int) rightposition;
                        trp.y = (int) (bottomposition - TextHeight);

                        Point brp = new Point();
                        c.setBottomRightPosition(brp);
                        brp.x = (int) rightposition;
                        brp.y = (int) bottomposition;
                        ppp++;
                        c.setIndex(ppp);

                        leftposition = rightposition;
                    }
                }

                //??????????????????
                if (i == mCurPage.titleLines - 1) {
                    top += titlePara;
                } else {
                    //?????????
                    top += titleInterval;
                }
            }

            //?????????????????????
            int strLength = 0;
            for (int i = mCurPage.titleLines; i < mCurPage.lines.size(); ++i) {
                str = mCurPage.lines.get(i);
                strLength = strLength + str.length();
                int paragraphLength = mCurPage.position == 0 ? strLength + titleLen : mCurChapter.getPageLength(mCurPage.position - 1) + strLength;
                isLight = ReadAloudService.running && readAloudParagraph == mCurChapter.getParagraphIndex(paragraphLength);
                mTextPaint.setColor(isLight ? mContext.getResources().getColor(R.color.sys_color) : mTextColor);
                if (!mSettingManager.isTightCom()) {
                    Layout tempLayout = new StaticLayout(str, mTextPaint, mVisibleWidth, Layout.Alignment.ALIGN_NORMAL, 0, 0, false);
                    float width = StaticLayout.getDesiredWidth(str, tempLayout.getLineStart(0), tempLayout.getLineEnd(0), mTextPaint);
                    if (needScale(str)) {
                        drawScaledText(canvas, str, width, mTextPaint, top, i, mCurPage.txtLists);
                    } else {
                        canvas.drawText(str, mMarginLeft, top, mTextPaint);
                    }
                } else {
                    canvas.drawText(str, mMarginLeft, top, mTextPaint);
                }
                //?????????????????? --?????? pzl
                float leftposition = mMarginLeft;
                if (isFirstLineOfParagraph(str)) {
                    //canvas.drawText(blanks, x, top, mTextPaint);
                    float bw = StaticLayout.getDesiredWidth(indent, mTextPaint);
                    leftposition += bw;
                }
                float rightposition = 0;
                float bottomposition = top + mTextPaint.getFontMetrics().descent;
                float textHeight = Math.abs(fontMetrics.ascent) + Math.abs(fontMetrics.descent);

                if (mCurPage.txtLists != null) {
                    for (TxtChar c : mCurPage.txtLists.get(i).getCharsData()) {
                        rightposition = leftposition + c.getCharWidth();
                        Point tlp = new Point();
                        c.setTopLeftPosition(tlp);
                        tlp.x = (int) leftposition;
                        tlp.y = (int) (bottomposition - textHeight);

                        Point blp = new Point();
                        c.setBottomLeftPosition(blp);
                        blp.x = (int) leftposition;
                        blp.y = (int) bottomposition;

                        Point trp = new Point();
                        c.setTopRightPosition(trp);
                        trp.x = (int) rightposition;
                        trp.y = (int) (bottomposition - textHeight);

                        Point brp = new Point();
                        c.setBottomRightPosition(brp);
                        brp.x = (int) rightposition;
                        brp.y = (int) bottomposition;

                        leftposition = rightposition;

                        ppp++;
                        c.setIndex(ppp);
                    }
                }
                //?????????????????? --?????? pzl
                if (str.endsWith("\n")) {
                    top += para;
                } else {
                    top += interval;
                }
            }
        }
    }

    private void drawScaledText(Canvas canvas, String line, float lineWidth, TextPaint paint, float top, int y, List<TxtLine> txtLists) {
        float x = mMarginLeft;

        if (isFirstLineOfParagraph(line)) {
            canvas.drawText(indent, x, top, paint);
            float bw = StaticLayout.getDesiredWidth(indent, paint);
            x += bw;
            line = line.substring(mSettingManager.getIntent());
        }
        int gapCount = line.length() - 1;
        int i = 0;

        TxtLine txtList = new TxtLine();//?????????pzl
        txtList.setCharsData(new ArrayList<>());//pzl

        float d = ((mDisplayWidth - (mMarginLeft + mMarginRight)) - lineWidth) / gapCount;
        for (; i < line.length(); i++) {
            String c = String.valueOf(line.charAt(i));
            float cw = StaticLayout.getDesiredWidth(c, paint);
            canvas.drawText(c, x, top, paint);
            //pzl
            TxtChar txtChar = new TxtChar();
            txtChar.setChardata(line.charAt(i));
            if (i == 0) txtChar.setCharWidth(cw + d / 2);
            if (i == gapCount) txtChar.setCharWidth(cw + d / 2);
            txtChar.setCharWidth(cw + d);
            ;//??????
            //txtChar.Index = y;//????????????????????????
            txtList.getCharsData().add(txtChar);
            //pzl
            x += cw + d;
        }
        if (txtLists != null) {
            txtLists.set(y, txtList);//pzl
        }
    }

    private void drawErrorMsg(Canvas canvas, String msg, float offset) {
        float textInterval = mTextInterval + mTextPaint.getTextSize();
        Layout tempLayout = new StaticLayout(msg, mTextPaint, mVisibleWidth, Layout.Alignment.ALIGN_NORMAL, 0, 0, false);
        List<String> linesData = new ArrayList<>();
        for (int i = 0; i < tempLayout.getLineCount(); i++) {
            linesData.add(msg.substring(tempLayout.getLineStart(i), tempLayout.getLineEnd(i)));
        }
        float pivotY = (mDisplayHeight - textInterval * linesData.size()) / 2f - offset;
        for (String str : linesData) {
            float textWidth = mTextPaint.measureText(str);
            float pivotX = (mDisplayWidth - textWidth) / 2;
            canvas.drawText(str, pivotX, pivotY, mTextPaint);
            pivotY += textInterval;
        }
    }

    //???????????????d'hou
    private boolean isFirstLineOfParagraph(String line) {
        return line.length() > 3 && line.charAt(0) == (char) 12288 && line.charAt(1) == (char) 12288;
    }

    private boolean needScale(String line) {//??????????????????
        return line != null && line.length() != 0 && line.charAt(line.length() - 1) != '\n';
    }

    void prepareDisplay(int w, int h) {
        // ??????PageView?????????
        mDisplayWidth = w;
        mDisplayHeight = h;

        // ????????????
        mMarginTop = mSettingManager.isShowStatusBar() ?
                ScreenUtils.dpToPx(mSettingManager.getPaddingTop() + DEFAULT_MARGIN_HEIGHT - 8) :
                ScreenUtils.dpToPx(mSettingManager.getPaddingTop() + DEFAULT_MARGIN_HEIGHT);
        mMarginBottom = ScreenUtils.dpToPx(mSettingManager.getPaddingBottom() + DEFAULT_MARGIN_HEIGHT);
        mMarginLeft = ScreenUtils.dpToPx(mSettingManager.getPaddingLeft());
        mMarginRight = ScreenUtils.dpToPx(mSettingManager.getPaddingRight());

        // ?????????????????????????????????
        mVisibleWidth = mDisplayWidth - (mMarginLeft + mMarginRight);
        mVisibleHeight = !mSettingManager.isShowStatusBar() ? mDisplayHeight - (mMarginTop + mMarginBottom)
                : mDisplayHeight - (mMarginTop + mMarginBottom) - mPageView.getStatusBarHeight();

        // ?????? PageMode
        mPageView.setPageMode(mPageMode);


        // ????????????
        mPreChapter = null;
        mNextChapter = null;

        if (!isChapterOpen) {
            // ??????????????????
            mPageView.drawCurPage(false);
            // ????????? display ??????????????? openChapter ???????????????????????????
            // ?????????????????? display ????????????????????????
            if (!isFirstOpen) {
                // ????????????
                openChapter();
            }
        } else {
            // ???????????????????????????????????????????????????
            if (mStatus == STATUS_FINISH) {
                dealLoadPageList(mCurChapterPos);
                // ?????????????????????????????????
                mCurPage = getCurPage(mCurPage.position);
            }
            mPageView.drawCurPage(false);
        }
    }

    /**
     * ???????????????
     *
     * @return
     */
    boolean prev() {

        // ????????????????????????
        if (!canTurnPage()) {
            return false;
        }

        if (mStatus == STATUS_FINISH) {
            // ??????????????????????????????
            TxtPage prevPage = getPrevPage();
            if (prevPage != null) {
                mCancelPage = mCurPage;
                mCurPage = prevPage;
                mPageView.drawNextPage();
                return true;
            }
        }

        if (!hasPrevChapter()) {
            return false;
        }

        mCancelPage = mCurPage;
        if (parsePrevChapter()) {
            mCurPage = getPrevLastPage();
            if (mStatus == STATUS_LOADING)
                mStatus = STATUS_FINISH;
        } else {
            mCurPage = new TxtPage();
        }
        mPageView.drawNextPage();
        return true;
    }

    /**
     * ?????????????????????
     *
     * @return:????????????????????????
     */
    boolean parsePrevChapter() {
        // ?????????????????????
        int prevChapter = mCurChapterPos - 1;

        mLastChapterPos = mCurChapterPos;
        mCurChapterPos = prevChapter;

        // ???????????????????????????
        mNextChapter = mCurChapter;

        // ?????????????????????????????????
        if (mPreChapter != null) {
            mCurChapter = mPreChapter;
            mPreChapter = null;

            // ??????
            chapterChangeCallback();
        } else {
            dealLoadPageList(prevChapter);
        }
        // ?????????????????????
        preLoadPrevChapter();
        return mCurChapter != null;
    }

    private boolean hasPrevChapter() {
        //??????????????????????????????
        if (mCurChapterPos - 1 < 0) {
            return false;
        }
        return true;
    }

    /**
     * ???????????????
     *
     * @return:??????????????????
     */
    boolean next() {
        // ????????????????????????
        if (!canTurnPage()) {
            return false;
        }

        if (mStatus == STATUS_FINISH) {
            // ??????????????????????????????
            TxtPage nextPage = getNextPage();
            if (nextPage != null) {
                mCancelPage = mCurPage;
                mCurPage = nextPage;
                mPageView.drawNextPage();
                return true;
            }
        }

        if (!hasNextChapter()) {
            return false;
        }

        mCancelPage = mCurPage;
        // ?????????????????????
        if (parseNextChapter()) {
            mCurPage = mCurChapter.getPage(0);
            if (mStatus == STATUS_LOADING)
                mStatus = STATUS_FINISH;
        } else {
            mCurPage = new TxtPage();
        }
        mPageView.drawNextPage();
        return true;
    }

    private boolean hasNextChapter() {
        // ????????????????????????????????????
        if (mCurChapterPos + 1 >= mChapterList.size()) {
            return false;
        }
        return true;
    }

    boolean parseCurChapter() {
        // ????????????
        dealLoadPageList(mCurChapterPos);
        // ?????????????????????????????????
        preLoadPrevChapter();
        preLoadNextChapter();
        return mCurChapter != null;
    }

    /**
     * ?????????????????????
     *
     * @return:??????????????????????????????
     */
    boolean parseNextChapter() {
        int nextChapter = mCurChapterPos + 1;

        mLastChapterPos = mCurChapterPos;
        mCurChapterPos = nextChapter;

        // ???????????????????????????????????????????????????
        mPreChapter = mCurChapter;

        // ???????????????????????????????????????
        if (mNextChapter != null) {
            mCurChapter = mNextChapter;
            mNextChapter = null;
            // ??????
            chapterChangeCallback();
        } else {
            // ??????????????????
            dealLoadPageList(nextChapter);
        }
        // ?????????????????????
        preLoadNextChapter();
        return mCurChapter != null;
    }

    private void dealLoadPageList(int chapterPos) {
        try {
            mCurChapter = loadPageList(chapterPos);
            if (mCurChapter != null) {
                if (mCurChapter.getTxtPageList().isEmpty()) {
                    mStatus = STATUS_EMPTY;

                    // ?????????????????????
                    TxtPage page = new TxtPage();
                    page.lines = new ArrayList<>(1);
                    mCurChapter.addPage(page);
                } else {
                    mStatus = STATUS_FINISH;
                }
            } else {
                mStatus = STATUS_LOADING;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mCurChapter = null;
            mStatus = STATUS_ERROR;
        }

        // ??????
        chapterChangeCallback();
    }

    private void chapterChangeCallback() {
        if (mPageChangeListener != null) {
            readAloudParagraph = -1;
            mPageChangeListener.onChapterChange(mCurChapterPos);
            mPageChangeListener.onPageChange(0, resetReadAloud);
            resetReadAloud = true;
            mPageChangeListener.onPageCountChange(mCurChapter != null ? mCurChapter.getPageSize() : 0);
        }
    }

    // ??????????????????
    private void preLoadPrevChapter() {
        int prevChapter = mCurChapterPos - 1;

        // ???????????????????????????????????????????????????????????????????????????
        if (!hasPrevChapter()
                || !hasChapterData(mChapterList.get(prevChapter))) {
            return;
        }
        //?????????????????????????????????
        if (mPreLoadPrevDisp != null) {
            mPreLoadPrevDisp.dispose();
        }

        //?????????????????????????????????
        Single.create((SingleOnSubscribe<TxtChapter>) e -> e.onSuccess(loadPageList(prevChapter)))
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new SingleObserver<TxtChapter>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mPreLoadPrevDisp = d;
                    }

                    @Override
                    public void onSuccess(TxtChapter txtChapter) {
                        if (txtChapter.getPosition() == mCurChapterPos - 1)
                            mPreChapter = txtChapter;
                    }

                    @Override
                    public void onError(Throwable e) {
                        //????????????
                        mPreChapter = null;
                    }
                });
    }

    // ??????????????????
    private void preLoadNextChapter() {
        int nextChapter = mCurChapterPos + 1;

        // ???????????????????????????????????????????????????????????????????????????
        if (!hasNextChapter()
                || !hasChapterData(mChapterList.get(nextChapter))) {
            return;
        }
        //?????????????????????????????????
        if (mPreLoadNextDisp != null) {
            mPreLoadNextDisp.dispose();
        }

        //?????????????????????????????????
        Single.create((SingleOnSubscribe<TxtChapter>) e -> e.onSuccess(loadPageList(nextChapter)))
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new SingleObserver<TxtChapter>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mPreLoadNextDisp = d;
                    }

                    @Override
                    public void onSuccess(TxtChapter txtChapter) {
                        if (txtChapter.getPosition() == mCurChapterPos + 1)
                            mNextChapter = txtChapter;
                    }

                    @Override
                    public void onError(Throwable e) {
                        //????????????
                        mNextChapter = null;
                    }
                });
    }

    // ????????????
    void pageCancel() {
        if (mCurPage.position == 0 && mCurChapterPos > mLastChapterPos) { // ???????????????????????????
            if (mPreChapter != null) {
                cancelNextChapter();
            } else {
                if (parsePrevChapter()) {
                    mCurPage = getPrevLastPage();
                } else {
                    mCurPage = new TxtPage();
                }
            }
        } else if (mCurChapter == null
                || (mCurPage.position == mCurChapter.getPageSize() - 1
                && mCurChapterPos < mLastChapterPos)) {  // ????????????????????????

            if (mNextChapter != null) {
                cancelPreChapter();
            } else {
                if (parseNextChapter()) {
                    mCurPage = mCurChapter.getPage(0);
                } else {
                    mCurPage = new TxtPage();
                }
            }
        } else {
            // ?????????????????????????????????????????????????????????????????????
            mCurPage = mCancelPage;
        }
    }

    private void cancelNextChapter() {
        int temp = mLastChapterPos;
        mLastChapterPos = mCurChapterPos;
        mCurChapterPos = temp;

        mNextChapter = mCurChapter;
        mCurChapter = mPreChapter;
        mPreChapter = null;

        chapterChangeCallback();

        mCurPage = getPrevLastPage();
        mCancelPage = null;
    }

    private void cancelPreChapter() {
        // ???????????????
        int temp = mLastChapterPos;
        mLastChapterPos = mCurChapterPos;
        mCurChapterPos = temp;
        // ??????????????????
        mPreChapter = mCurChapter;
        mCurChapter = mNextChapter;
        mNextChapter = null;

        chapterChangeCallback();

        mCurPage = getCurPage(0);
        mCancelPage = null;
    }

    /**************************************private method********************************************/
    /**
     * ???????????????????????????????????????
     *
     * @param chapter???????????????
     * @param content?????????????????????
     * @return
     */
    private TxtChapter loadPages(Chapter chapter, String content) {
        TxtChapter txtChapter = new TxtChapter(chapter.getNumber());

        content = contentHelper.replaceContent(mCollBook.getName() + "-" + mCollBook.getAuthor(), mCollBook.getSource(), content, true);
        if (mCollBook.getReSeg()) {
            content = contentHelper.LightNovelParagraph2(content, chapter.getTitle());
        }
        String[] allLine = content.split("\n");

        //????????????????????????
        List<String> lines = new ArrayList<>();
        List<TxtLine> txtLists = new ArrayList<>();//???????????????????????? //pzl
        float rHeight = mVisibleHeight;
        int titleLinesCount = 0;
        boolean showTitle = true; // ??????????????????
        boolean firstLine = true;
        String paragraph = contentHelper.replaceContent(mCollBook.getName() + "-" + mCollBook.getAuthor(), mCollBook.getSource(), chapter.getTitle(), true);//??????????????????
        paragraph = paragraph.trim() + "\n";
        int i = 0;
        while (showTitle || i < allLine.length) {
            if (firstLine && !showTitle) {
                paragraph = paragraph.replace(chapter.getTitle(), "");
                firstLine = false;
            }
            // ????????????
            if (!showTitle) {
                paragraph = allLine[i];
                if (mSettingManager.isEnType()) {
                    paragraph = StringUtils.trim(paragraph.replace("\t", ""));
                } else {
                    paragraph = paragraph.replaceAll("\\s", "");
                }
                i++;
                // ??????????????????????????????????????????
                if (paragraph.equals("")) continue;
                paragraph = indent + paragraph + "\n";
            } else {
                //?????? title ???????????????
                rHeight -= mTitlePara;
            }
            addParagraphLength(txtChapter, paragraph.length());
            int wordCount = 0;
            String subStr = null;
            while (paragraph.length() > 0) {
                //??????????????????????????????????????????
                if (showTitle) {
                    rHeight -= mTitlePaint.getTextSize();
                } else {
                    rHeight -= mTextPaint.getTextSize();
                }
                // ????????????????????????????????? TextPage
                if (rHeight <= 0) {
                    // ??????Page
                    TxtPage page = new TxtPage();
                    page.position = txtChapter.getTxtPageList().size();
                    page.title = chapter.getTitle();
                    page.lines = new ArrayList<>(lines);
                    page.txtLists = new ArrayList<>(txtLists);
                    page.titleLines = titleLinesCount;
                    txtChapter.addPage(page);
                    addTxtPageLength(txtChapter, page.getContent().length());
                    // ??????Lines
                    lines.clear();
                    txtLists.clear();//pzl
                    rHeight = mVisibleHeight;
                    titleLinesCount = 0;
                    continue;
                }

                //??????????????????????????????
                if (mSettingManager.isTightCom()) {
                    if (showTitle) {
                        wordCount = mTitlePaint.breakText(paragraph,
                                true, mVisibleWidth, null);
                    } else {
                        wordCount = mTextPaint.breakText(paragraph,
                                true, mVisibleWidth, null);
                    }

                    subStr = paragraph.substring(0, wordCount);
                    if (paragraph.substring(wordCount).equals("\n")) {
                        subStr += "\n";
                    }
                } else {
                    Layout tempLayout;
                    if (showTitle) {
                        tempLayout = new StaticLayout(paragraph, mTitlePaint, mVisibleWidth, Layout.Alignment.ALIGN_NORMAL, 0, 0, false);
                    } else {
                        tempLayout = new StaticLayout(paragraph, mTextPaint, mVisibleWidth, Layout.Alignment.ALIGN_NORMAL, 0, 0, false);
                    }
                    wordCount = tempLayout.getLineEnd(0);
                    subStr = paragraph.substring(0, wordCount);
                }

                if (!subStr.equals("\n")) {
                    //???????????????????????????lines???
                    lines.add(subStr);
                    //begin pzl
                    //????????????????????????
                    char[] cs = subStr.replace((char) 12288, ' ').trim().toCharArray();
                    TxtLine txtList = new TxtLine();//?????????
                    txtList.setCharsData(new ArrayList<>());
                    for (char c : cs) {
                        String mesasrustr = String.valueOf(c);
                        float charwidth = mTextPaint.measureText(mesasrustr);
                        if (showTitle) {
                            charwidth = mTitlePaint.measureText(mesasrustr);
                        }
                        TxtChar txtChar = new TxtChar();
                        txtChar.setChardata(c);
                        txtChar.setCharWidth(charwidth);//??????
                        txtChar.setIndex(66);//????????????????????????
                        txtList.getCharsData().add(txtChar);
                    }
                    txtLists.add(txtList);
                    //end pzl
                    //??????????????????
                    if (showTitle) {
                        titleLinesCount += 1;
                        rHeight -= mTitleInterval;
                    } else {
                        rHeight -= mTextInterval;
                    }
                }
                //??????
                paragraph = paragraph.substring(wordCount);
            }

            //?????????????????????
            if (!showTitle && lines.size() != 0) {
                rHeight = rHeight - mTextPara + mTextInterval;
            }

            if (showTitle) {
                rHeight = rHeight - mTitlePara + mTitleInterval;
                showTitle = false;
            }
        }

        if (lines.size() != 0) {
            //??????Page
            TxtPage page = new TxtPage();
            page.position = txtChapter.getTxtPageList().size();
            page.title = chapter.getTitle();
            page.lines = new ArrayList<>(lines);
            page.txtLists = new ArrayList<>(txtLists);
            page.titleLines = titleLinesCount;
            txtChapter.addPage(page);
            addTxtPageLength(txtChapter, page.getContent().length());
            //??????Lines
            lines.clear();
            txtLists.clear();
        }

        return txtChapter;
    }


    /**
     * ??????TxtChapter?????????
     *
     * @param txtChapter
     * @param length
     */
    private void addTxtPageLength(TxtChapter txtChapter, int length) {
        if (txtChapter.getTxtPageLengthList().isEmpty()) {
            txtChapter.addTxtPageLength(length);
        } else {
            txtChapter.addTxtPageLength(txtChapter.getTxtPageLengthList().get(txtChapter.getTxtPageLengthList().size() - 1) + length);
        }
    }

    /**
     * ??????TxtChapter????????????
     *
     * @param txtChapter
     * @param length
     */
    private void addParagraphLength(TxtChapter txtChapter, int length) {
        if (txtChapter.getParagraphLengthList().isEmpty()) {
            txtChapter.addParagraphLength(length);
        } else {
            txtChapter.addParagraphLength(txtChapter.getParagraphLengthList().get(txtChapter.getParagraphLengthList().size() - 1) + length);
        }
    }

    /**
     * @return:???????????????????????????
     */
    private TxtPage getCurPage(int pos) {
        if (mPageChangeListener != null) {
            mPageChangeListener.onPageChange(pos, resetReadAloud);
            resetReadAloud = true;
        }
        return mCurChapter.getPage(pos);
    }

    public TxtPage curPage() {
        return mCurPage;
    }

    /**
     * @return:?????????????????????
     */
    private TxtPage getPrevPage() {
        int pos = mCurPage.position - 1;
        if (pos < 0) {
            return null;
        }
        if (mPageChangeListener != null) {
            mPageChangeListener.onPageChange(pos, resetReadAloud);
            resetReadAloud = true;
        }
        return mCurChapter.getPage(pos);
    }

    /**
     * @return:?????????????????????
     */
    private TxtPage getNextPage() {
        int pos = mCurPage.position + 1;
        if (pos >= mCurChapter.getPageSize()) {
            return null;
        }
        if (mPageChangeListener != null) {
            mPageChangeListener.onPageChange(pos, resetReadAloud);
            resetReadAloud = true;
        }
        return mCurChapter.getPage(pos);
    }

    /**
     * @return:????????????????????????????????????
     */
    private TxtPage getPrevLastPage() {
        int pos = mCurChapter.getPageSize() - 1;

        if (mPageChangeListener != null) {
            mPageChangeListener.onPageChange(pos, resetReadAloud);
            resetReadAloud = true;
        }

        return mCurChapter.getPage(pos);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @return
     */
    private boolean canTurnPage() {

        if (!isChapterListPrepare) {
            return false;
        }

        if (mStatus == STATUS_PARSE_ERROR
                || mStatus == STATUS_PARING) {
            return false;
        } else if (mStatus == STATUS_ERROR) {
            mStatus = STATUS_LOADING;
        }
        return true;
    }

    /**
     * ??????????????????
     *
     * @param durChapterIndex
     * @param chapterAll
     * @param durPageIndex
     * @param durPageAll
     * @return
     */
    private static String getReadProgress(int durChapterIndex, int chapterAll, int durPageIndex, int durPageAll) {
        DecimalFormat df = new DecimalFormat("0.0%");
        if (chapterAll == 0 || (durPageAll == 0 && durChapterIndex == 0)) {
            return "0.0%";
        } else if (durPageAll == 0) {
            return df.format((durChapterIndex + 1.0f) / chapterAll);
        }
        String percent = df.format(durChapterIndex * 1.0f / chapterAll + 1.0f / chapterAll * (durPageIndex + 1) / durPageAll);
        if (percent.equals("100.0%") && (durChapterIndex + 1 != chapterAll || durPageIndex + 1 != durPageAll)) {
            percent = "99.9%";
        }
        return percent;
    }

    /**
     * * @return curPageLength ???????????????
     */
    public int curPageLength() {
        if (getCurPage(getPagePos()) == null) return 0;
        if (getPageStatus() != STATUS_FINISH) return 0;
        String str;
        int strLength = 0;
        TxtPage txtPage = getCurPage(getPagePos());
        if (txtPage != null) {
            for (int i = txtPage.getTitleLines(); i < txtPage.size(); ++i) {
                str = txtPage.getLine(i);
                strLength = strLength + str.length();
            }
        }
        return strLength;
    }

    /**
     * @return ????????????
     */
    public String getContent() {
        if (mCurChapter == null) return null;
        if (mCurChapter.getPageSize() == 0) return null;
        TxtPage txtPage = mCurPage;
        StringBuilder s = new StringBuilder();
        int size = txtPage.lines.size();
        //int start = mPageMode == PageMode.SCROLL ? Math.min(Math.max(0, linePos), size - 1) : 0;
        int start = 0;
        for (int i = start; i < size; i++) {
            s.append(txtPage.lines.get(i));
        }
        return s.toString();
    }

    /**
     * @return ??????????????????
     */
    public String getUnReadContent() {
        if (mCurPage == null) return null;
        if (mCurChapter == null || mCurChapter.getPageSize() == 0) return null;
        StringBuilder s = new StringBuilder();
        String content = getContent();
        if (content != null) {
            s.append(content);
        }
        int mCurPagePos = getPagePos();
        content = getContentStartPage(mCurPagePos + 1);
        if (content != null) {
            s.append(content);
        }
        readTextLength = mCurPagePos > 0 ? mCurChapter.getPageLength(mCurPagePos - 1) : 0;
        /*if (mPageMode == PageAnimation.Mode.SCROLL) {
            for (int i = 0; i < Math.min(Math.max(0, linePos), curChapter().txtChapter.getPage(mCurPagePos).size() - 1); i++) {
                readTextLength += curChapter().txtChapter.getPage(mCurPagePos).getLine(i).length();
            }
        }*/
        return s.toString();
    }


    /**
     * @param page ????????????
     * @return ???page???????????????????????????????????????
     */
    public String getContentStartPage(int page) {
        if (mCurChapter == null) return null;
        if (mCurChapter.getTxtPageList().isEmpty()) return null;
        StringBuilder s = new StringBuilder();
        if (mCurChapter.getPageSize() > page) {
            for (int i = page; i < mCurChapter.getPageSize(); i++) {
                s.append(mCurChapter.getPage(i).getContent());
            }
        }
        return s.toString();
    }

    /**
     * @param start ??????????????????
     */
    public void readAloudStart(int start) {
        start = readTextLength + start;
        int x = mCurChapter.getParagraphIndex(start);
        if (readAloudParagraph != x) {
            readAloudParagraph = x;
            mPageView.drawCurPage(false);
            //mPageView.invalidate();
            /*mPageView.drawPage(-1);
            mPageView.drawPage(1);
            mPageView.invalidate();*/
        }
    }

    /**
     * @param readAloudLength ???????????????
     */
    public void readAloudLength(int readAloudLength) {
        if (mCurChapter == null) return;
        if (getPageStatus() != STATUS_FINISH) return;
        if (mCurChapter.getPageLength(getPagePos()) < 0) return;
        if (mPageView.isRunning()) return;
        readAloudLength = readTextLength + readAloudLength;
        if (readAloudLength >= mCurChapter.getPageLength(getPagePos())) {
            resetReadAloud = false;
            noAnimationToNextPage();
            mPageView.invalidate();
        }
    }

    enum Detect {
        None, Left, Right
    }

    /**
     * --------------------
     * ??????????????????????????????????????????????????????????????????null
     * --------------------
     * author: huangwei
     * 2017???7???4?????????10:23:19
     */
    TxtChar detectPressTxtChar(float down_X2, float down_Y2, Detect detect) {
        TxtPage txtPage = mCurPage;
        if (txtPage == null) return null;
        List<TxtLine> txtLines = txtPage.txtLists;
        if (txtLines == null) return null;
        for (TxtLine l : txtLines) {
            List<TxtChar> txtChars = l.getCharsData();
            if (txtChars != null && txtChars.size() > 0) {
                TxtChar first = txtChars.get(0);
                TxtChar last = txtChars.get(txtChars.size() - 1);
                for (int i = 0; i < txtChars.size(); i++) {
                    TxtChar c = txtChars.get(i);
                    Point leftPoint = c.getBottomLeftPosition();
                    Point rightPoint = c.getBottomRightPosition();
                    if (leftPoint != null && down_Y2 > leftPoint.y) {
                        break;// ?????????????????????
                    }
                    if (leftPoint != null && rightPoint != null) {
                        boolean flag = down_X2 >= leftPoint.x && down_X2 <= rightPoint.x;
                        switch (detect) {
                            case Left:
                                flag = flag || (i == 0 && (down_X2 < leftPoint.x ||
                                        down_X2 > last.getBottomRightPosition().x));
                                break;
                            case Right:
                                flag = flag || (i == txtChars.size() - 1 && (down_X2 > rightPoint.x ||
                                        down_X2 < first.getBottomLeftPosition().x));
                                break;
                        }
                        if (flag) return c;
                    }

                }
            }
        }
        return null;
    }

    TxtChar detectTxtCharByIndex(int index) {
        TxtPage txtPage = mCurPage;
        if (txtPage == null) return null;
        List<TxtLine> txtLines = txtPage.txtLists;
        if (txtLines == null) return null;
        for (TxtLine l : txtLines) {
            List<TxtChar> txtChars = l.getCharsData();
            if (txtChars != null) {
                for (TxtChar c : txtChars) {
                    if (c != null && c.getIndex() == index) return c;
                }
            }
        }
        return null;
    }

    public void skipToSearch(int chapterNum, int countInChapter, String keyword) {
        skipToChapter(chapterNum);
        if (mStatus != STATUS_FINISH) {
            App.getHandler().postDelayed(() -> skipToSearch(chapterNum, countInChapter, keyword), 300);
            return;
        }
        int[] position = searchWordPositions(countInChapter, keyword);
        skipToPage(position[0]);

        try {
            mPageView.setFirstSelectTxtChar(mCurPage.txtLists.get(position[1]).
                    getCharsData().get(position[2]));
            switch (position[3]) {
                case 0:
                    mPageView.setLastSelectTxtChar(mCurPage.txtLists.get(position[1]).
                            getCharsData().get(position[2] + keyword.length() - 1));
                    break;
                case 1:
                    mPageView.setLastSelectTxtChar(mCurPage.txtLists.get(position[1] + 1).
                            getCharsData().get(position[4]));
                    break;
                case -1:
                    mPageView.setLastSelectTxtChar(mCurPage.txtLists.get(mCurPage.txtLists.size() - 1).
                            getCharsData().get(mCurPage.txtLists.get(mCurPage.txtLists.size() - 1).
                            getCharsData().size() - 1));
                    break;
            }
            mPageView.setSelectMode(PageView.SelectMode.SelectMoveForward);
            mPageView.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[] searchWordPositions(int countInChapter, String keyword) {
        List<TxtPage> pages = mCurChapter.getTxtPageList();
        // calculate search result's pageIndex
        StringBuilder sb = new StringBuilder();
        for (TxtPage page : pages) {
            sb.append(page.getContent());
        }
        String content = sb.toString();
        int count = 0;
        int index = content.indexOf(keyword);
        while (count != countInChapter) {
            index = content.indexOf(keyword, index + 1);
            count++;
        }
        int contentPosition = index;
        int pageIndex = 0;
        int length = mCurChapter.getPageLength(pageIndex);
        while (length < contentPosition) {
            pageIndex++;
            if (pageIndex > pages.size()) {
                pageIndex = pages.size();
                break;
            }
            length = mCurChapter.getPageLength(pageIndex);
        }
        // calculate search word's lineIndex
        TxtPage currentPage = pages.get(pageIndex);
        int lineIndex = 0;
        length = length - currentPage.getContent().length() + currentPage.lines.get(lineIndex).length();
        while (length <= contentPosition) {
            lineIndex += 1;
            if (lineIndex > currentPage.lines.size()) {
                lineIndex = currentPage.lines.size();
                break;
            }
            length += currentPage.lines.get(lineIndex).length();
        }

        // charIndex
        String currentLine = currentPage.lines.get(lineIndex);
        if (currentLine.endsWith("\n")) {
            currentLine = StringUtils.trim(currentLine) + "\n";
        } else {
            currentLine = StringUtils.trim(currentLine);
        }
        length -= currentLine.length();
        int charIndex = contentPosition - length;
        if (charIndex < 0) charIndex = 0;
        int addLine = 0;
        int charIndex2 = 0;
        // change line
        if ((charIndex + keyword.length()) > currentLine.length()) {
            addLine = 1;
            charIndex2 = charIndex + keyword.length() - currentLine.length() - 1;
        }
        // changePage
        if ((lineIndex + addLine + 1) > currentPage.lines.size()) {
            addLine = -1;
            charIndex2 = charIndex + keyword.length() - currentLine.length() - 1;
        }
        if (charIndex2 < 0) charIndex = 0;
        if (charIndex2 >= currentLine.length()) charIndex2 = currentLine.length() - 1;
        return new int[]{pageIndex, lineIndex, charIndex, addLine, charIndex2};
    }

    public boolean isPrev() {
        return isPrev;
    }

    public void setPrev(boolean prev) {
        isPrev = prev;
    }

    public void setmStatus(int mStatus) {
        this.mStatus = mStatus;
    }

    /*****************************************interface*****************************************/

    public interface OnPageChangeListener {
        /**
         * ??????????????????????????????????????????
         *
         * @param pos:?????????????????????
         */
        void onChapterChange(int pos);

        /**
         * ?????????????????????????????????????????????
         *
         * @param chapters?????????????????????
         */
        void onCategoryFinish(List<Chapter> chapters);

        /**
         * ???????????????????????????????????????????????????==> ??????????????????????????????????????????????????????????????????????????????????????????
         *
         * @param count:???????????????
         */
        void onPageCountChange(int count);

        /**
         * ???????????????????????????????????????
         *
         * @param pos:????????????????????????
         */
        void onPageChange(int pos, boolean resetRead);

    }
}
