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

package top.cronos.myreader.model.third2.content;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.jsoup.nodes.Element;
import org.mozilla.javascript.NativeObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import top.cronos.myreader.entity.StrResponse;
import top.cronos.myreader.entity.WebChapterBean;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.greendao.entity.rule.BookSource;
import top.cronos.myreader.greendao.entity.rule.TocRule;
import top.cronos.myreader.model.third2.analyzeRule.AnalyzeByRegex;
import top.cronos.myreader.model.third2.analyzeRule.AnalyzeRule;
import top.cronos.myreader.model.third2.analyzeRule.AnalyzeUrl;
import top.cronos.myreader.util.utils.OkHttpUtils;

public class BookChapterList {
    private String tag;
    private BookSource source;
    private TocRule tocRule;
    private List<WebChapterBean> webChapterBeans;
    private boolean dx = false;
    private boolean analyzeNextUrl;
    private CompositeDisposable compositeDisposable;
    private String chapterListUrl;

    public BookChapterList(String tag, BookSource source, boolean analyzeNextUrl) {
        this.tag = tag;
        this.source = source;
        this.analyzeNextUrl = analyzeNextUrl;
        tocRule = source.getTocRule();
    }

    public Observable<List<Chapter>> analyzeChapterList(final String s, final Book book, Map<String, String> headerMap) {
        return Observable.create(e -> {
            if (TextUtils.isEmpty(s)) {
                e.onError(new Throwable("??????????????????" + book.getChapterUrl()));
                return;
            } else {
                Log.d(tag, "????????????????????????");
                Log.d(tag, "???" + book.getChapterUrl());
            }
            book.setTag(tag);
            AnalyzeRule analyzer = new AnalyzeRule(book);
            String ruleChapterList = tocRule.getChapterList();
            if (ruleChapterList != null && ruleChapterList.startsWith("-")) {
                dx = true;
                ruleChapterList = ruleChapterList.substring(1);
            }
            chapterListUrl = book.getChapterUrl();
            WebChapterBean webChapterBean = analyzeChapterList(s, chapterListUrl, ruleChapterList, analyzeNextUrl, analyzer, dx);
            final List<Chapter> chapterList = webChapterBean.getData();

            final List<String> chapterUrlS = new ArrayList<>(webChapterBean.getNextUrlList());
            if (chapterUrlS.isEmpty() || !analyzeNextUrl) {
                finish(chapterList, e);
            }
            //??????????????????
            else if (chapterUrlS.size() == 1) {
                List<String> usedUrl = new ArrayList<>();
                usedUrl.add(book.getChapterUrl());
                //?????????????????????????????????
                Log.d(tag, "?????????????????????");
                while (!chapterUrlS.isEmpty() && !usedUrl.contains(chapterUrlS.get(0))) {
                    usedUrl.add(chapterUrlS.get(0));
                    AnalyzeUrl analyzeUrl = new AnalyzeUrl(chapterUrlS.get(0), headerMap, tag);
                    try {
                        String body;
                        StrResponse response = OkHttpUtils.getStrResponse(analyzeUrl)
                                .blockingFirst();
                        body = response.body();
                        webChapterBean = analyzeChapterList(body, chapterUrlS.get(0), ruleChapterList, false, analyzer, dx);
                        chapterList.addAll(webChapterBean.getData());
                        chapterUrlS.clear();
                        chapterUrlS.addAll(webChapterBean.getNextUrlList());
                    } catch (Exception exception) {
                        if (!e.isDisposed()) {
                            e.onError(exception);
                        }
                    }
                }
                Log.d(tag, "????????????????????????" + usedUrl.size() + "???");
                finish(chapterList, e);
            }
            //??????????????????
            else {
                Log.d(tag, "??????????????????" + chapterUrlS.size() + "???");
                compositeDisposable = new CompositeDisposable();
                webChapterBeans = new ArrayList<>();
                AnalyzeNextUrlTask.Callback callback = new AnalyzeNextUrlTask.Callback() {
                    @Override
                    public void addDisposable(Disposable disposable) {
                        compositeDisposable.add(disposable);
                    }

                    @Override
                    public void analyzeFinish(WebChapterBean bean, List<Chapter> chapterListBeans) {
                        if (nextUrlFinish(bean, chapterListBeans)) {
                            for (WebChapterBean chapterBean : webChapterBeans) {
                                chapterList.addAll(chapterBean.getData());
                            }
                            Log.d(tag, "?????????????????????,?????????" + chapterList.size() + "???");
                            finish(chapterList, e);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        compositeDisposable.dispose();
                        e.onError(throwable);
                    }
                };
                for (String url : chapterUrlS) {
                    final WebChapterBean bean = new WebChapterBean(url);
                    webChapterBeans.add(bean);
                }
                for (WebChapterBean bean : webChapterBeans) {
                    BookChapterList bookChapterList = new BookChapterList(tag, source, false);
                    AnalyzeUrl analyzeUrl = new AnalyzeUrl(bean.getUrl(), headerMap, tag);
                    new AnalyzeNextUrlTask(bookChapterList, bean, book, headerMap)
                            .setCallback(callback)
                            .analyzeUrl(analyzeUrl);
                }
            }
        });
    }

    private synchronized boolean nextUrlFinish(WebChapterBean webChapterBean, List<Chapter> bookChapterBeans) {
        webChapterBean.setData(bookChapterBeans);
        for (WebChapterBean bean : webChapterBeans) {
            if (bean.noData()) return false;
        }
        return true;
    }

    private void finish(List<Chapter> chapterList, Emitter<List<Chapter>> emitter) {
        //????????????,???????????????,?????????,?????????????????????
        if (!dx) {
            Collections.reverse(chapterList);
        }
        LinkedHashSet<Chapter> lh = new LinkedHashSet<>(chapterList);
        chapterList = new ArrayList<>(lh);
        Collections.reverse(chapterList);
        Log.d(tag, "-??????????????????" + analyzeNextUrl);
        if (chapterList.isEmpty()) {
            emitter.onError(new Throwable("??????????????????"));
            return;
        }
        emitter.onNext(chapterList);
        emitter.onComplete();
    }

    private WebChapterBean analyzeChapterList(String s, String chapterUrl, String ruleChapterList,
                                              boolean printLog, AnalyzeRule analyzer, boolean dx) throws Exception {
        List<String> nextUrlList = new ArrayList<>();
        analyzer.setContent(s, chapterUrl);
        if (!TextUtils.isEmpty(tocRule.getTocUrlNext()) && analyzeNextUrl) {
            if (printLog) Log.d(tag, "??????????????????????????????");
            nextUrlList = analyzer.getStringList(tocRule.getTocUrlNext(), true);
            int thisUrlIndex = nextUrlList.indexOf(chapterUrl);
            if (thisUrlIndex != -1) {
                nextUrlList.remove(thisUrlIndex);
            }
            if (printLog) Log.d(tag, "???" + nextUrlList.toString());
        }

        List<Chapter> chapterBeans = new ArrayList<>();
        if (printLog) Log.d(tag,  "?????????????????????");
        // ?????????java?????????????????????????????????
        if (ruleChapterList.startsWith(":")) {
            ruleChapterList = ruleChapterList.substring(1);
            regexChapter(s, ruleChapterList.split("&&"), 0, analyzer, chapterBeans);
            if (chapterBeans.size() == 0) {
                if (printLog)  Log.d(tag, "????????? 0 ?????????");
                return new WebChapterBean(chapterBeans, new LinkedHashSet<>(nextUrlList));
            }
        }
        // ??????AllInOne??????????????????????????????
        else if (ruleChapterList.startsWith("+")) {
            ruleChapterList = ruleChapterList.substring(1);
            List<Object> collections = analyzer.getElements(ruleChapterList);
            if (collections.size() == 0) {
                Log.d(tag, "????????? 0 ?????????");
                return new WebChapterBean(chapterBeans, new LinkedHashSet<>(nextUrlList));
            }
            String nameRule = tocRule.getChapterName();
            String linkRule = tocRule.getChapterUrl();
            String name = "";
            String link = "";
            for (Object object : collections) {
                if (object instanceof NativeObject) {
                    name = String.valueOf(((NativeObject) object).get(nameRule));
                    link = String.valueOf(((NativeObject) object).get(linkRule));
                } else if (object instanceof Element) {
                    name = ((Element) object).text();
                    link = ((Element) object).attr(linkRule);
                }
                addChapter(chapterBeans, name, link);
            }
        }
        // ????????????????????????????????????????????????
        else {
            List<Object> collections = analyzer.getElements(ruleChapterList);
            if (collections.size() == 0) {
                Log.d(tag, "????????? 0 ?????????");
                return new WebChapterBean(chapterBeans, new LinkedHashSet<>(nextUrlList));
            }
            List<AnalyzeRule.SourceRule> nameRule = analyzer.splitSourceRule(tocRule.getChapterName());
            List<AnalyzeRule.SourceRule> linkRule = analyzer.splitSourceRule(tocRule.getChapterUrl());
            for (Object object : collections) {
                analyzer.setContent(object, chapterUrl);
                addChapter(chapterBeans, analyzer.getString(nameRule), analyzer.getString(linkRule));
            }
        }
        if (printLog) Log.d(tag,"????????? " + chapterBeans.size() + " ?????????");
        Chapter firstChapter;
        if (dx) {
            if (printLog) Log.d(tag, "-??????");
            firstChapter = chapterBeans.get(chapterBeans.size() - 1);
        } else {
            firstChapter = chapterBeans.get(0);
        }
        if (printLog) Log.d(tag, "?????????????????????");
        if (printLog) Log.d(tag,  "???" + firstChapter.getTitle());
        if (printLog) Log.d(tag,  "?????????????????????");
        if (printLog) Log.d(tag,  "???" + firstChapter.getUrl());
        return new WebChapterBean(chapterBeans, new LinkedHashSet<>(nextUrlList));
    }

    private void addChapter(final List<Chapter> chapterBeans, String name, String link) {
        if (TextUtils.isEmpty(name)) return;
        if (TextUtils.isEmpty(link)) link = chapterListUrl;
        Chapter chapter = new Chapter();
        chapter.setTitle(name);
        chapter.setUrl(link);
        chapterBeans.add(chapter);
    }

    // region ???java???????????????????????????????????????
    private void regexChapter(String str, String[] regex, int index, AnalyzeRule analyzer, final List<Chapter> chapterBeans) throws Exception {
        Matcher resM = Pattern.compile(regex[index]).matcher(str);
        if (!resM.find()) {
            return;
        }
        if (index + 1 == regex.length) {
            // ??????????????????
            String nameRule = tocRule.getChapterName();
            String linkRule = tocRule.getChapterUrl();
            if (TextUtils.isEmpty(nameRule) || TextUtils.isEmpty(linkRule)) return;
            // ??????@get??????
            nameRule = analyzer.replaceGet(tocRule.getChapterName());
            linkRule = analyzer.replaceGet(tocRule.getChapterUrl());
            // ??????????????????
            List<String> nameParams = new ArrayList<>();
            List<Integer> nameGroups = new ArrayList<>();
            AnalyzeByRegex.splitRegexRule(nameRule, nameParams, nameGroups);
            List<String> linkParams = new ArrayList<>();
            List<Integer> linkGroups = new ArrayList<>();
            AnalyzeByRegex.splitRegexRule(linkRule, linkParams, linkGroups);
            // ????????????VIP??????(hasVipRule>1 ???????????????vip??????)
            int hasVipRule = 0;
            for (int i = nameGroups.size(); i-- > 0; ) {
                if (nameGroups.get(i) != 0) {
                    ++hasVipRule;
                }
            }
            String vipNameGroup = "";
            int vipNumGroup = 0;
            if ((nameGroups.get(0) != 0) && (hasVipRule > 1)) {
                vipNumGroup = nameGroups.remove(0);
                vipNameGroup = nameParams.remove(0);
            }
            // ??????????????????
            StringBuilder cName = new StringBuilder();
            StringBuilder cLink = new StringBuilder();
            // ??????????????????
            if (vipNumGroup != 0) {
                do {
                    cName.setLength(0);
                    cLink.setLength(0);
                    for (int i = nameParams.size(); i-- > 0; ) {
                        if (nameGroups.get(i) > 0) {
                            cName.insert(0, resM.group(nameGroups.get(i)));
                        } else if (nameGroups.get(i) < 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            cName.insert(0, resM.group(nameParams.get(i)));
                        } else {
                            cName.insert(0, nameParams.get(i));
                        }
                    }
                    if (vipNumGroup > 0) {
                        cName.insert(0, resM.group(vipNumGroup) == null ? "" : "\uD83D\uDD12");
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        cName.insert(0, resM.group(vipNameGroup) == null ? "" : "\uD83D\uDD12");
                    } else {
                        cName.insert(0, vipNameGroup);
                    }

                    for (int i = linkParams.size(); i-- > 0; ) {
                        if (linkGroups.get(i) > 0) {
                            cLink.insert(0, resM.group(linkGroups.get(i)));
                        } else if (linkGroups.get(i) < 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            cLink.insert(0, resM.group(linkParams.get(i)));
                        } else {
                            cLink.insert(0, linkParams.get(i));
                        }
                    }

                    addChapter(chapterBeans, cName.toString(), cLink.toString());
                } while (resM.find());
            } else {
                do {
                    cName.setLength(0);
                    cLink.setLength(0);
                    for (int i = nameParams.size(); i-- > 0; ) {
                        if (nameGroups.get(i) > 0) {
                            cName.insert(0, resM.group(nameGroups.get(i)));
                        } else if (nameGroups.get(i) < 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            cName.insert(0, resM.group(nameParams.get(i)));
                        } else {
                            cName.insert(0, nameParams.get(i));
                        }
                    }

                    for (int i = linkParams.size(); i-- > 0; ) {
                        if (linkGroups.get(i) > 0) {
                            cLink.insert(0, resM.group(linkGroups.get(i)));
                        } else if (linkGroups.get(i) < 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            cLink.insert(0, resM.group(linkParams.get(i)));
                        } else {
                            cLink.insert(0, linkParams.get(i));
                        }
                    }

                    addChapter(chapterBeans, cName.toString(), cLink.toString());
                } while (resM.find());
            }
        } else {
            StringBuilder result = new StringBuilder();
            do {
                result.append(resM.group(0));
            } while (resM.find());
            regexChapter(result.toString(), regex, ++index, analyzer, chapterBeans);
        }
    }
    // endregion
}