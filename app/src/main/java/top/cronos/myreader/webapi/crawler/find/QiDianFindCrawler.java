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

package top.cronos.myreader.webapi.crawler.find;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.Observable;
import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.entity.FindKind;
import top.cronos.myreader.entity.StrResponse;
import top.cronos.myreader.entity.bookstore.BookType;
import top.cronos.myreader.entity.bookstore.QDBook;
import top.cronos.myreader.entity.bookstore.RankBook;
import top.cronos.myreader.entity.bookstore.SortBook;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.util.SharedPreUtils;
import top.cronos.myreader.util.help.StringHelper;
import top.cronos.myreader.webapi.crawler.base.BaseFindCrawler;

/**
 * @author fengyue
 * @date 2021/7/21 22:25
 */
public class QiDianFindCrawler extends BaseFindCrawler {
    private String sourceUrl = "https://m.qidian.com";
    private String rankUrl = "https://m.qidian.com/majax/rank/{rankName}list?_csrfToken={cookie}&gender={sex}&pageNum={page}&catId=-1";
    private String sortUrl = "https://m.qidian.com/majax/category/list?_csrfToken={cookie}&gender={sex}&pageNum={page}&orderBy=&catId={catId}&subCatId=";
    private String[] sex = {"male", "female"};
    private String yuepiaoParam = "&yearmonth={yearmonth}";
    private String imgUrl = "https://bookcover.yuewen.com/qdbimg/349573/{bid}/150";
    private String defaultCookie = "eXRDlZxmRDLvFAmdgzqvwWAASrxxp2WkVlH4ZM7e";
    private String yearmonthFormat = "yyyyMM";
    private LinkedHashMap<String, String> rankName = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> sortName = new LinkedHashMap<>();
    private boolean isFemale;

    public QiDianFindCrawler() {
    }

    public QiDianFindCrawler(boolean isFemale) {
        this.isFemale = isFemale;
    }

    @Override
    public String getName() {
        return isFemale ? "???????????????" : "???????????????";
    }

    @Override
    public String getTag() {
        return sourceUrl;
    }

    @Override
    public boolean needSearch() {
        return true;
    }

    private void initMaleRankName() {
        if (!isFemale) {
            rankName.put("?????????", "yuepiao");
            rankName.put("?????????", "hotsales");
            rankName.put("?????????", "readIndex");
            rankName.put("?????????", "newfans");
            rankName.put("?????????", "rec");
            rankName.put("?????????", "update");
            rankName.put("?????????", "sign");
            rankName.put("?????????", "newbook");
            rankName.put("?????????", "newauthor");
        } else {
            rankName.put("?????????", "yuepiao");
            rankName.put("?????????", "readIndex");
            rankName.put("?????????", "newfans");
            rankName.put("?????????", "rec");
            rankName.put("?????????", "update");
            rankName.put("?????????", "collect");
            rankName.put("?????????", "free");
        }
    }

    private void initSortNames() {
        /*
        {value: -1, text: "??????"}
        1: {value: 21, text: "??????"}
        2: {value: 1, text: "??????"}
        3: {value: 2, text: "??????"}
        4: {value: 22, text: "??????"}
        5: {value: 4, text: "??????"}
        6: {value: 15, text: "??????"}
        7: {value: 6, text: "??????"}
        8: {value: 5, text: "??????"}
        9: {value: 7, text: "??????"}
        10: {value: 8, text: "??????"}
        11: {value: 9, text: "??????"}
        12: {value: 10, text: "??????"}
        13: {value: 12, text: "?????????"}
         */
        if (!isFemale) {
            sortName.put("????????????", 21);
            sortName.put("????????????", 1);
            sortName.put("????????????", 2);
            sortName.put("????????????", 4);
            sortName.put("????????????", 15);
            sortName.put("????????????", 6);
            sortName.put("????????????", 5);
            sortName.put("????????????", 8);
            sortName.put("????????????", 9);
            sortName.put("????????????", 10);
            sortName.put("?????????", 12);
            sortName.put("????????????", 20076);
        } else {
            sortName.put("????????????", 80);
            sortName.put("????????????", 81);
            sortName.put("????????????", 82);
            sortName.put("????????????", 83);
            sortName.put("????????????", 84);
            sortName.put("????????????", 85);
            sortName.put("????????????", 30083);
            sortName.put("????????????", 86);
            sortName.put("????????????", 88);
            sortName.put("?????????", 87);
            sortName.put("????????????", 30120);
        }
    }

    private List<FindKind> initKinds(boolean isSort) {
        Set<String> names = !isSort ? rankName.keySet() : sortName.keySet();
        List<FindKind> kinds = new ArrayList<>();
        for (String name : names) {
            FindKind kind = new FindKind();
            kind.setName(name);
            String url;
            if (!isSort) {
                url = rankUrl.replace("{rankName}", rankName.get(name));
                kind.setMaxPage(30);
            } else {
                url = sortUrl.replace("{catId}", sortName.get(name) + "");
                kind.setMaxPage(5);
            }
            url = url.replace("{sex}", !isFemale ? sex[0] : sex[1]);
            String cookie = SharedPreUtils.getInstance().getString(App.getmContext().getString(R.string.qdCookie), "");
            if (!cookie.equals("")) {
                url = url.replace("{cookie}", StringHelper.getSubString(cookie, "_csrfToken=", ";"));
            } else {
                url = url.replace("{cookie}", defaultCookie);
            }
            if ("?????????".equals(name)) {
                SimpleDateFormat sdf = new SimpleDateFormat(yearmonthFormat, Locale.CHINA);
                String yearmonth = sdf.format(new Date());
                url = url + yuepiaoParam.replace("{yearmonth}", yearmonth);
            }
            kind.setUrl(url);
            kinds.add(kind);
        }
        return kinds;
    }

    @Override
    public Observable<Boolean> initData() {
        return Observable.create(emitter -> {
            initMaleRankName();
            initSortNames();
            kindsMap.put("?????????", initKinds(false));
            kindsMap.put("??????", initKinds(true));
            emitter.onNext(true);
            emitter.onComplete();
        });
    }

    @Override
    public Observable<List<Book>> getFindBooks(StrResponse strResponse, FindKind kind) {
        return Observable.create(emitter -> {
            List<QDBook> qdBooks = getBooksFromJson(strResponse.body());
            emitter.onNext(convertQDBook2Book(qdBooks));
            emitter.onComplete();
        });
    }

    private List<QDBook> getBooksFromJson(String json) throws JSONException {
        List<QDBook> books = new ArrayList<>();
        JSONObject all = new JSONObject(json);
        JSONObject data = all.getJSONObject("data");
        int total = data.getInt("total");
        JSONArray jsonBooks = data.getJSONArray("records");
        for (int i = 0; i < jsonBooks.length(); i++) {
            JSONObject jsonBook = jsonBooks.getJSONObject(i);
            boolean isSort = jsonBook.has("state");
            QDBook book = !isSort ? new RankBook() : new SortBook();
            book.setbName(jsonBook.getString("bName"));
            book.setbAuth(jsonBook.getString("bAuth"));
            book.setBid(jsonBook.getString("bid"));
            book.setCat(jsonBook.getString("cat"));
            book.setCatId(jsonBook.getInt("catId"));
            book.setCnt(jsonBook.getString("cnt"));
            book.setDesc(jsonBook.getString("desc"));
            book.setImg(imgUrl.replace("{bid}", jsonBook.getString("bid")));
            if (!isSort) {
                ((RankBook) book).setRankCnt(jsonBook.getString("rankCnt"));
                ((RankBook) book).setRankNum(jsonBook.getInt("rankNum"));
            } else {
                ((SortBook) book).setState(jsonBook.getString("state"));
            }
            books.add(book);
        }
        return books;
    }

    private List<Book> convertQDBook2Book(List<QDBook> qdBooks) {
        List<Book> books = new ArrayList<>();
        for (QDBook rb : qdBooks) {
            Book book = new Book();
            book.setName(rb.getbName());
            book.setAuthor(rb.getbAuth());
            book.setImgUrl(rb.getImg());
            String cat = rb.getCat();
            book.setType(cat.contains("??????") || cat.length() >= 4 ? cat : cat + "??????");
//            book.setNewestChapterTitle(rb.getDesc());
            book.setDesc(rb.getDesc());
            if (rb instanceof RankBook) {
                boolean hasRankCnt = !((RankBook) rb).getRankCnt().equals("null");
                book.setWordCount(rb.getCnt());
                book.setStatus(hasRankCnt ? ((RankBook) rb).getRankCnt() : book.getType());
            } else if (rb instanceof SortBook) {
                book.setWordCount(rb.getCnt());
                book.setStatus(((SortBook) rb).getState());
            }
            books.add(book);
        }
        return books;
    }
}
