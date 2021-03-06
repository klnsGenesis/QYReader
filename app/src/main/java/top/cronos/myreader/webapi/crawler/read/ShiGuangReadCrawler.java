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

package top.cronos.myreader.webapi.crawler.read;

import android.text.Html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;

import top.cronos.myreader.entity.SearchBookBean;
import top.cronos.myreader.enums.LocalBookSource;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.model.mulvalmap.ConMVMap;
import top.cronos.myreader.webapi.crawler.base.BaseReadCrawler;

@Deprecated
public class ShiGuangReadCrawler extends BaseReadCrawler {
    public static final String NAME_SPACE = "https://www.youxs.org";
    public static final String NOVEL_SEARCH = "https://www.youxs.org/search.php?key={key}";
    public static final String CHARSET = "gbk";
    public static final String SEARCH_CHARSET = "gbk";

    @Override
    public String getSearchLink() {
        return NOVEL_SEARCH;
    }

    @Override
    public String getCharset() {
        return CHARSET;
    }

    @Override
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @Override
    public Boolean isPost() {
        return false;
    }

    @Override
    public String getSearchCharset() {
        return SEARCH_CHARSET;
    }

    /**
     * ???html?????????????????????
     *
     * @param html
     * @return
     */
    public String getContentFormHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element divContent = doc.getElementById("txt");
        Elements aDiv = divContent.getElementsByTag("dd");
        StringBuilder sb = new StringBuilder();
        Collections.sort(aDiv, (o1, o2) -> Integer.parseInt(o1.attr("data-id")) -
                Integer.parseInt(o2.attr("data-id")));
        for (int i = 0; i < aDiv.size(); i++) {
            Element dd = aDiv.get(i);
            if (i == aDiv.size() - 1) break;
            sb.append(Html.fromHtml(dd.html()).toString());
            sb.append("\n");
        }
        String content = sb.toString();
        char c = 160;
        String spaec = "" + c;
        content = content.replace(spaec, "  ");
        return content;
    }

    /**
     * ???html?????????????????????
     *
     * @param html
     * @return
     */
    public ArrayList<Chapter> getChaptersFromHtml(String html) {
        ArrayList<Chapter> chapters = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Element divList = doc.getElementById("listsss");
        Elements elementsByTag = divList.getElementsByTag("a");
        int i = 0;
        for (int j = 0; j < elementsByTag.size(); j++) {
            Element a = elementsByTag.get(j);
            String title = a.text();
            String url = a.attr("href");
            Chapter chapter = new Chapter();
            chapter.setNumber(i++);
            chapter.setTitle(title);
            chapter.setUrl(url);
            chapters.add(chapter);
        }
        return chapters;
    }

    /**
     * ?????????html??????????????????
     *
     * @param html
     * @return
     */
    /*
    <li><a href="/wuxian/4/4473/" target="_blank" class="book_cov" title="?????????"><img src="/public/images/default.jpg"
             data-original="https://www.youxs.org/files/article/image/0/29/29s.jpg" class="lazyload_book_cover" alt="?????????" /></a>
        <div class="book_inf">
            <h3><a href="/wuxian/4/4473/" title="?????????" target="_blank" mod="data_book_name">
                    <font style="font-weight:bold;color:#f00">?????????</font>
                </a></h3>
            <p class="tags"><span>?????????<a title="????????????">????????????</a></span><span>?????????<a href="/liebiao/8_0_0_0_0_1.html" target="_blank">????????????</a></span><span>??????????????????</span><span>????????????2497??????+</span></p>
            <p><b>???????????????</b><a href="/wuxian/4/4473/15283.html" title="???1598??? ???????????????????????????" target="_blank">???1598??? ???????????????????????????</a></p>
            <p class="int"> ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                ????????????????????????????????????????????????
                ???????????????????????????</p>
        </div>
        <div class="right">
            <span>???????????????09-06 05:25</span>
            <a href="/wuxian/4/4473/" target="_blank" class="read_btn btn">????????????</a>
            <a href="javascript:BookCaseAdd('4473');" class="store_btn btn" btn="book_fav">????????????</a>
        </div>
    </li>
     */
    public ConMVMap<SearchBookBean, Book> getBooksFromSearchHtml(String html) {
        ConMVMap<SearchBookBean, Book> books = new ConMVMap<>();
        Document doc = Jsoup.parse(html);
//        try {
        String urlType = doc.select("meta[property=og:type]").attr("content");
        if ("novel".equals(urlType)) {
            String readUrl = doc.select("meta[property=og:novel:read_url]").attr("content");
            Book book = new Book();
            book.setChapterUrl(readUrl);
            getBookInfo(doc, book);
            SearchBookBean sbb = new SearchBookBean(book.getName(), book.getAuthor());
            books.add(sbb, book);
        } else {
            Element div = doc.getElementsByClass("result").first();
            Elements lis = div.getElementsByTag("li");
            for (Element li : lis) {
                Elements as = li.getElementsByTag("a");
                Book book = new Book();
                book.setName(as.get(1).text());
                book.setAuthor(as.get(2).text());
                book.setType(as.get(3).text());
                book.setNewestChapterTitle(as.get(4).text().replace("???????????????", ""));
                book.setDesc(li.getElementsByClass("int").first().text());
                book.setUpdateDate(li.getElementsByClass("right").first().getElementsByTag("span").text());
                String imgUrl = li.getElementsByTag("img").attr("data-original");
                book.setImgUrl(imgUrl);
                book.setChapterUrl(as.get(1).attr("href"));
                book.setSource(LocalBookSource.shiguang.toString());
                SearchBookBean sbb = new SearchBookBean(book.getName(), book.getAuthor());
                books.add(sbb, book);
            }
        }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return books;
    }

    public Book getBookInfo(Document doc, Book book) {
        //?????????
        book.setSource(LocalBookSource.shiguang.toString());
        //??????url
        String imgUrl = doc.select("meta[property=og:image]").attr("content");
        book.setImgUrl(imgUrl);

        //??????
        String title = doc.select("meta[property=og:novel:book_name]").attr("content");
        book.setName(title);

        //??????
        String author = doc.select("meta[property=og:novel:author]").attr("content");
        book.setAuthor(author);

        //????????????
        String updateDate = doc.select("meta[property=og:novel:update_time]").attr("content");
        book.setUpdateDate(updateDate);

        //????????????
        String newestChapterTitle = doc.select("meta[property=og:novel:latest_chapter_name]").attr("content");
        book.setNewestChapterTitle(newestChapterTitle);

        //??????
        String type = doc.select("meta[property=og:novel:category]").attr("content");
        book.setType(type);

        //??????
        String desc = doc.select("meta[property=og:description]").attr("content");
        book.setDesc(desc);
        return book;

    }
}
