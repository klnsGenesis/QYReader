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


import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import top.cronos.myreader.application.App;
import top.cronos.myreader.base.observer.MyObserver;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.entity.Setting;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.greendao.service.ChapterService;
import top.cronos.myreader.util.help.StringHelper;
import top.cronos.myreader.util.utils.FileUtils;
import top.cronos.myreader.util.utils.RxUtils;
import top.cronos.myreader.webapi.BookApi;
import top.cronos.myreader.webapi.crawler.base.ReadCrawler;

public class NetPageLoader extends PageLoader {
    private static final String TAG = "PageFactory";
    private ChapterService mChapterService;
    private ReadCrawler mReadCrawler;
    private List<Chapter> loadingChapters = new CopyOnWriteArrayList<>();

    public NetPageLoader(PageView pageView, Book collBook, ChapterService mChapterService,
                         ReadCrawler mReadCrawler, Setting setting) {
        super(pageView, collBook, setting);
        this.mChapterService = mChapterService;
        this.mReadCrawler = mReadCrawler;
    }


    @Override
    public void refreshChapterList() {
        List<Chapter> chapters = mChapterService.findBookAllChapterByBookId(mCollBook.getId());
        if (chapters != null && !chapters.isEmpty()) {
            mChapterList = chapters;
            isChapterListPrepare = true;

            // ??????????????????????????????????????????
            if (mPageChangeListener != null) {
                mPageChangeListener.onCategoryFinish(mChapterList);
            }

            // ?????????????????????
            if (!isChapterOpen()) {
                // ????????????
                openChapter();
            }
            return;
        }
        mStatus = STATUS_LOADING_CHAPTER;
        BookApi.getBookChapters(mCollBook, mReadCrawler)
                .flatMap((Function<List<Chapter>, ObservableSource<List<Chapter>>>) newChapters -> Observable.create(emitter -> {
                   for (Chapter chapter : newChapters){
                       chapter.setId(StringHelper.getStringRandom(25));
                       chapter.setBookId(mCollBook.getId());
                   }
                   mChapterService.addChapters(newChapters);
                   emitter.onNext(newChapters);
                }))
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new MyObserver<List<Chapter>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mChapterDis = d;
                    }

                    @Override
                    public void onNext(@NotNull List<Chapter> chapters) {
                        mChapterDis = null;
                        isChapterListPrepare = true;
                        mChapterList = chapters;
                        //????????????????????????
                        if (mPageChangeListener != null) {
                            mPageChangeListener.onCategoryFinish(mChapterList);
                        }
                        // ???????????????????????????
                        openChapter();
                    }

                    @Override
                    public void onError(Throwable e) {
                        error(STATUS_CATEGORY_ERROR, e.getLocalizedMessage());
                        Log.d(TAG, "file load error:" + e.toString());
                    }
                });
    }

    @Override
    public String getChapterReader(Chapter chapter) throws FileNotFoundException {
        /*File file = new File(APPCONST.BOOK_CACHE_PATH + mCollBook.getId()
                + File.separator + chapter.getTitle() + FileUtils.SUFFIX_FY);
        if (!file.exists()) return null;
        BufferedReader br = new BufferedReader(new FileReader(file));
        return br;*/
        return mChapterService.getChapterCatheContent(chapter);
    }

    @Override
    public boolean hasChapterData(Chapter chapter) {
        return ChapterService.isChapterCached(chapter);
    }

    // ???????????????????????????
    @Override
    boolean parsePrevChapter() {
        boolean isRight = super.parsePrevChapter();

        if (mStatus == STATUS_FINISH) {
            loadPrevChapter();
        } else if (mStatus == STATUS_LOADING) {
            loadCurrentChapter();
        }
        return isRight;
    }

    // ????????????????????????
    @Override
    boolean parseCurChapter() {
        boolean isRight = super.parseCurChapter();

        if (mStatus == STATUS_FINISH) {
            loadPrevChapter();
            loadNextChapter();
        } else if (mStatus == STATUS_LOADING) {
            loadCurrentChapter();
        }
        return isRight;
    }

    // ???????????????????????????
    @Override
    boolean parseNextChapter() {
        boolean isRight = super.parseNextChapter();

        if (mStatus == STATUS_FINISH) {
            loadNextChapter();
        } else if (mStatus == STATUS_LOADING) {
            loadCurrentChapter();
        }

        return isRight;
    }

    /**
     * ????????????????????????????????????
     */
    private void loadPrevChapter() {
        if (mPageChangeListener != null) {
            int end = mCurChapterPos;
            int begin = end - 1;
            if (begin < 0) {
                begin = 0;
            }
            requestChapters(begin, end);
        }
    }

    /**
     * ??????????????????????????????????????????
     */
    private void loadCurrentChapter() {
        if (mPageChangeListener != null) {
            int begin = mCurChapterPos;
            int end = mCurChapterPos;

            // ??????????????????????????????
            if (end < mChapterList.size()) {
                end = end + 1;
                if (end >= mChapterList.size()) {
                    end = mChapterList.size() - 1;
                }
            }

            // ???????????????????????????
            if (begin != 0) {
                begin = begin - 1;
                if (begin < 0) {
                    begin = 0;
                }
            }
            requestChapters(begin, end);
        }
    }

    /**
     * ?????????????????????????????????
     */
    private void loadNextChapter() {
        if (mPageChangeListener != null) {

            // ?????????????????????
            int begin = mCurChapterPos + 1;
            int end = begin + 3;

            // ??????????????????????????????
            if (begin >= mChapterList.size()) {
                // ?????????????????????????????????????????????????????????
                return;
            }

            if (end > mChapterList.size()) {
                end = mChapterList.size() - 1;
            }
            requestChapters(begin, end);
        }
    }

    private void requestChapters(int start, int end) {
        // ???????????????
        if (start < 0) {
            start = 0;
        }

        if (end >= mChapterList.size()) {
            end = mChapterList.size() - 1;
        }


        List<Chapter> chapters = new ArrayList<>();

        // ????????????????????????????????????/????????????
        for (int i = start; i <= end; ++i) {
            Chapter txtChapter = mChapterList.get(i);
            if (!hasChapterData(txtChapter) && !loadingChapters.contains(txtChapter)) {
                chapters.add(txtChapter);
            }
        }

        if (!chapters.isEmpty()) {
            loadingChapters.addAll(chapters);
            for (Chapter chapter : chapters) {
                getChapterContent(chapter);
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param chapter
     */
    public void getChapterContent(Chapter chapter) {
        BookApi.getChapterContent(chapter, mCollBook, mReadCrawler).flatMap(s -> Observable.create(emitter -> {
            loadingChapters.remove(chapter);
            String content = StringHelper.isEmpty(s) ? "??????????????????" : s;
            mChapterService.saveOrUpdateChapter(chapter, content);
            emitter.onNext(content);
            emitter.onComplete();
        })).compose(RxUtils::toSimpleSingle).subscribe(new MyObserver<Object>() {
            @Override
            public void onNext(@NotNull Object o) {
                if (isClose()) return;
                if (getPageStatus() == PageLoader.STATUS_LOADING && mCurChapterPos == chapter.getNumber()) {
                    if (isPrev) {
                        openChapterInLastPage();
                    } else {
                        openChapter();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                loadingChapters.remove(chapter);
                if (isClose()) return;
                if (mCurChapterPos == chapter.getNumber())
                    chapterError("??????????????????????????????\n" + e.getLocalizedMessage());
                if (App.isDebug()) e.printStackTrace();
            }
        });
    }

}

