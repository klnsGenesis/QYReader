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

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import top.cronos.myreader.entity.SearchBookBean;
import top.cronos.myreader.enums.LocalBookSource;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.model.mulvalmap.ConMVMap;
import top.cronos.myreader.util.utils.OkHttpUtils;
import top.cronos.myreader.util.utils.StringUtils;
import top.cronos.myreader.webapi.crawler.base.BaseReadCrawler;

@Deprecated
public class YanQingLouReadCrawler extends BaseReadCrawler {
    public static final String NAME_SPACE = "http://www.yanqinglou.com";
    public static final String NOVEL_SEARCH = "http://www.yanqinglou.com/Home/Search,action=search&q={key}";
    public static final String AJAX_CONTENT = "http://www.yanqinglou.com/home/index/ajaxchapter";
    public static final String CHARSET = "UTF-8";
    public static final String SEARCH_CHARSET = "UTF-8";

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
        return true;
    }

    @Override
    public String getSearchCharset() {
        return SEARCH_CHARSET;
    }

    /*
        var preview_page = "/lishi/220987/60.html";
        var next_page = "/lishi/220987/62.html";
        var index_page = "/lishi/220987/";
        var article_id = "220987";
        var chapter_id = "61";
        var nextcid = "62";
        var prevcid = "60";
        var articlename = "????????????";
        var chaptername = "???????????? ??????";
        var hash = "38e338d183600e17";
        var localpre = "www.yanqinglou.com";
     */
    public String getContentFormHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element divContent = doc.getElementById("content");
        String content = "";
        content = Html.fromHtml(divContent.html()).toString();
        try {
            if (content.contains("????????????") || content.contains("??????????????????????????????????????????")) {
                content = getAjaxContent(html);
            }
            /*if (content.contains("??????????????????????????????????????????")){
                String nextUrl = doc.select(".to_nextpage")
                        .first().select("a").first()
                        .attr("href");
                content = content.replace("??????????????????????????????????????????", "")
                        .replace("-->>", "");
                content += getContentFormHtml(OkHttpUtils.getHtml(nextUrl, CHARSET));
            }*/
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        char c = 160;
        String spaec = "" + c;
        content = content.replace(spaec, "  ")
                .replaceAll("?????????.*???????????????", "")
                .replaceAll("?????????.*com/", "");
        return content;
    }


    /*
    id: 220987
    eKey: fe9535a08b53e929
    cid: 62
    basecid: 62
     */
    public String getAjaxContent(String html) throws IOException, JSONException {
        String id = StringUtils.getSubString(html, "var article_id = \"", "\";");
        String eKey = StringUtils.getSubString(html, "var hash = \"", "\";");
        String cid = StringUtils.getSubString(html, "var chapter_id = \"", "\";");
        String body = "id=" + id + "&eKey=" + eKey + "&cid=" + cid + "&basecid=" + cid;
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody requestBody = RequestBody.create(mediaType, body);
        String jsonStr = OkHttpUtils.getHtml(AJAX_CONTENT, requestBody, CHARSET);
        JSONObject json = new JSONObject(jsonStr);
        String content = json.getJSONObject("info").getString("content");
        content = Html.fromHtml(content).toString();
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
        Element divList = doc.getElementsByClass("fulllistall").first();
        Elements elementsByTag = divList.getElementsByTag("a");
        for (int i = 0; i < elementsByTag.size(); i++) {
            Element a = elementsByTag.get(i);
            String title = a.text();
            String url = a.attr("href");
            Chapter chapter = new Chapter();
            chapter.setNumber(i);
            chapter.setTitle(title);
            chapter.setUrl(url);
            chapters.add(chapter);
        }
        return chapters;
    }

    /**
     * ?????????html??????????????????
     * <div class="bookbox">
     * <div class="p10"><span class="num"> <a title="??????????????????" href="/hunlian/459337/"><img layout="fixed" width="90" height="120" src="https://www.biquduo.com/files/article/image/64/64075/64075s.jpg" alt="??????????????????" /></a> </span>
     * <div class="bookinfo">
     * <h4 class="bookname"><a title="??????????????????" href="/hunlian/459337/">??????????????????</a></h4>
     * <div class="author">?????????<a href="/writter/%E4%B8%8A%E5%BC%A6666.html" target="_blank" title="??????666???????????????">??????666</a></div>
     * <div class="author">???????????????</div>
     * <div class="cat"><span>????????????</span><a href="/hunlian/459337/">????????????>></a></div>
     * <div class="update"><span>?????????</span>??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????</div>
     * </div>
     * <div class="delbutton"> <a class="del_but" title="??????????????????" href="/hunlian/459337/">??????</a></div>
     * </div>
     * </div>
     */
    public ConMVMap<SearchBookBean, Book> getBooksFromSearchHtml(String html) {
        ConMVMap<SearchBookBean, Book> books = new ConMVMap<>();
        Document doc = Jsoup.parse(html);
        String bookName = doc.select("meta[property=og:novel:book_name]").attr("content");
        if ("".equals(bookName)) {
            Element div = doc.getElementsByClass("keywords").first();
            Elements divs = div.getElementsByClass("bookbox");
            for (Element divBook : divs) {
                Elements as = divBook.getElementsByTag("a");
                Book book = new Book();
                book.setName(as.get(1).text());
                book.setAuthor(as.get(2).text());
                book.setType(divBook.getElementsByClass("author").get(1).text().replace("?????????", ""));
                book.setNewestChapterTitle(as.get(3).text().replace("????????????>>", ""));
                book.setDesc(divBook.getElementsByClass("update").first().text().replace("?????????", ""));
                book.setImgUrl(NAME_SPACE + divBook.getElementsByTag("img").attr("src"));
                book.setChapterUrl(as.get(0).attr("href"));
                book.setSource(LocalBookSource.yanqinglou.toString());
                SearchBookBean sbb = new SearchBookBean(book.getName(), book.getAuthor());
                books.add(sbb, book);
            }
        } else {
            Book book = new Book();
            getBookInfo(doc, book);
            SearchBookBean sbb = new SearchBookBean(book.getName(), book.getAuthor());
            books.add(sbb, book);
        }
        return books;
    }

    private void getBookInfo(Document doc, Book book) {
        String name = doc.select("meta[property=og:novel:book_name]").attr("content");
        book.setName(name);

        String author = doc.select("meta[property=og:novel:author]").attr("content");
        book.setAuthor(author);

        String type = doc.select("meta[property=og:novel:category]").attr("content");
        book.setType(type);

        String newestTitle = doc.select("meta[property=og:novel:latest_chapter_name]").attr("content");
        book.setNewestChapterTitle(newestTitle);

        String desc = doc.select("meta[property=og:description]").attr("content");
        book.setDesc(desc);

        String img = doc.select("meta[property=og:image]").attr("content");
        book.setImgUrl(NAME_SPACE + img);

        String url = doc.select("meta[property=og:novel:read_url]").attr("content");
        book.setChapterUrl(url);
        book.setSource(LocalBookSource.yanqinglou.toString());
    }


}
