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
public class HongChenReadCrawler extends BaseReadCrawler {
    public static final String NAME_SPACE = "https://www.zuxs.net";
    public static final String NOVEL_SEARCH = "https://www.zuxs.net/search.php?key={key}";
    public static final String CHARSET = "gb2312";
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
        String readUrl = doc.select("meta[property=og:novel:read_url]").attr("content");
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
            chapter.setUrl(readUrl + url);
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
		<dl><dt><a href="/zu/1140.html" target="_blank"><img class="lazyimg" data-original="https://www.zuxs.net/files/article/image/0/29/29s.jpg"></a></dt>
			<dd><a href="/zu/1140.html" class="bigpic-book-name" target="_blank">
					<font style="font-weight:bold;color:#f00">?????????</font>
				</a>
				<p><a href="/author/%CC%EC%B2%CF%CD%C1%B6%B9.html" target="_blank">????????????</a> | <a href="/top/8_1.html"
					 target="_blank">????????????</a> | ?????????</p>
				<p class="big-book-info"> ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
					????????????????????????????????????????????????
					?????????????????????????????????</p>
				<p><a href="/zu/1/1140/5284.html" target="_blank" class="red">???????????? ???1598??? ???????????????????????????</a><span>| 09-06
						05:54??????</span></p>
			</dd>
		</dl>
     */
    @Deprecated
    public ConMVMap<SearchBookBean, Book> getBooksFromSearchHtml(String html) {
        ConMVMap<SearchBookBean, Book> books = new ConMVMap<>();
        Document doc = Jsoup.parse(html);
//        try {
        Element div = doc.getElementsByClass("s-b-list").first();
        Elements dls = div.getElementsByTag("dl");
        for (Element dl : dls) {
            Elements as = dl.getElementsByTag("a");
            Book book = new Book();
            book.setName(as.get(1).text());
            book.setAuthor(as.get(2).text());
            book.setType(as.get(3).text());
            book.setNewestChapterTitle(as.get(4).text().replace("???????????? ", ""));
            book.setDesc(dl.getElementsByClass("big-book-info").first().text());
            String imgUrl = dl.getElementsByTag("img").attr("data-original");
            book.setImgUrl(imgUrl);
            //https://www.zuxs.net/zu/1140.html -> https://www.zuxs.net/zu/1/1140/
            book.setChapterUrl(as.get(1).attr("href").replace("zu/", "zu/1/").replace(".html", "/"));
            book.setSource(LocalBookSource.hongchen.toString());
            SearchBookBean sbb = new SearchBookBean(book.getName(), book.getAuthor());
            books.add(sbb, book);
        }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return books;
    }


}
