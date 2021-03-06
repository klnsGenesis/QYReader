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

package top.cronos.myreader.ui.adapter.holder

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.cronos.myreader.R
import top.cronos.myreader.base.adapter.BaseListAdapter
import top.cronos.myreader.base.adapter.IViewHolder
import top.cronos.myreader.base.adapter.ViewHolderImpl
import top.cronos.myreader.greendao.entity.search.SearchWord1
import top.cronos.myreader.greendao.entity.search.SearchWord2

/**
 * @author fengyue
 * @date 2021/12/5 20:28
 */
class SearchWord1Holder(var activity: AppCompatActivity) : ViewHolderImpl<SearchWord1>() {

    private lateinit var tvChapterTitle: TextView
    private lateinit var rvWordList: RecyclerView
    private lateinit var adapter: BaseListAdapter<SearchWord2>

    override fun getItemLayoutId(): Int {
        return R.layout.item_search_word1
    }

    override fun initView() {
        tvChapterTitle = findById(R.id.tv_chapter_title)
        rvWordList = findById(R.id.rv_search_word2)
    }

    override fun onBind(holder: RecyclerView.ViewHolder?, data: SearchWord1?, pos: Int) {
        tvChapterTitle.text = String.format("%s(%d)", data?.chapterTitle, data?.searchWord2List?.size)
        adapter = object : BaseListAdapter<SearchWord2>() {
            override fun createViewHolder(viewType: Int): IViewHolder<SearchWord2> {
                return SearchWord2Holder()
            }
        }
        rvWordList.layoutManager = LinearLayoutManager(context)
        rvWordList.adapter = adapter
        adapter.refreshItems(data?.searchWord2List)
        adapter.setOnItemClickListener { _, pos1 ->
            val searchWord2 = adapter.getItem(pos1)
            val intent = Intent()
            intent.putExtra("chapterNum", searchWord2.chapterNum)
            intent.putExtra("countInChapter", searchWord2.count)
            intent.putExtra("keyword", searchWord2.keyword)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        }
    }
}