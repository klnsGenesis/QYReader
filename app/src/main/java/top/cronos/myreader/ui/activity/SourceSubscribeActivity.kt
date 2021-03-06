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

package top.cronos.myreader.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.kongzue.dialogx.dialogs.BottomMenu
import io.reactivex.disposables.Disposable
import top.cronos.myreader.R
import top.cronos.myreader.base.BaseActivity
import top.cronos.myreader.base.adapter.BaseListAdapter
import top.cronos.myreader.base.adapter.IViewHolder
import top.cronos.myreader.base.observer.MyObserver
import top.cronos.myreader.base.observer.MySingleObserver
import top.cronos.myreader.common.URLCONST
import top.cronos.myreader.databinding.ActivitySourceSubscribeBinding
import top.cronos.myreader.entity.lanzou.LanZouFile
import top.cronos.myreader.greendao.DbManager
import top.cronos.myreader.greendao.entity.SubscribeFile
import top.cronos.myreader.greendao.entity.rule.BookSource
import top.cronos.myreader.model.sourceAnalyzer.BookSourceManager
import top.cronos.myreader.ui.adapter.holder.SourceFileHolder
import top.cronos.myreader.ui.dialog.DialogCreator
import top.cronos.myreader.ui.dialog.LoadingDialog
import top.cronos.myreader.util.SharedPreUtils
import top.cronos.myreader.util.ToastUtils
import top.cronos.myreader.util.utils.AdUtils
import top.cronos.myreader.util.utils.AdUtils.FlowAd
import top.cronos.myreader.util.utils.RxUtils
import top.cronos.myreader.webapi.LanZouApi

/**
 * @author fengyue
 * @date 2022/3/3 9:56
 */
class SourceSubscribeActivity : BaseActivity<ActivitySourceSubscribeBinding>() {
    private lateinit var fileAdapter: BaseListAdapter<SubscribeFile>
    private var page = 1
    private var subscribeDis: Disposable? = null

    override fun bindView() {
        binding = ActivitySourceSubscribeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setUpToolbar(toolbar: Toolbar?) {
        super.setUpToolbar(toolbar)
        setStatusBarColor(R.color.colorPrimary, true)
        supportActionBar?.title = "????????????"
    }

    override fun initData(savedInstanceState: Bundle?) {
        super.initData(savedInstanceState)
        fileAdapter = object : BaseListAdapter<SubscribeFile>() {
            override fun createViewHolder(viewType: Int): IViewHolder<SubscribeFile> {
                return SourceFileHolder()
            }
        }
        binding.rvFiles.layoutManager = LinearLayoutManager(this)
        binding.rvFiles.adapter = fileAdapter
        loadFiles()
    }

    override fun initWidget() {
        super.initWidget()
        binding.srlFiles.setOnLoadMoreListener { loadFiles() }
        binding.srlFiles.setOnRefreshListener {
            page = 1
            loadFiles()
        }
        binding.loading.setOnReloadingListener {
            page = 1
            loadFiles()
        }
    }

    private fun loadFiles() {
        LanZouApi.getFoldFiles(URLCONST.SUB_SOURCE_URL, page, "fm9a")
            .onSuccess {
                if (it != null) {
                    if (page == 1) {
                        if (it.isEmpty()) {
                            binding.loading.showEmpty()
                        } else {
                            binding.loading.showFinish()
                            fileAdapter.refreshItems(lanZouFile2SubscribeFile(it))
                            if (it.size < 50) {
                                binding.srlFiles.finishRefreshWithNoMoreData()
                            } else {
                                binding.srlFiles.finishRefresh()
                            }
                        }
                    } else {
                        fileAdapter.addItems(lanZouFile2SubscribeFile(it))
                        if (it.size < 50) {
                            binding.srlFiles.finishLoadMoreWithNoMoreData()
                        } else {
                            binding.srlFiles.finishLoadMore()
                        }
                    }
                    page++
                } else {
                    binding.loading.showError()
                }
            }.onError {
                ToastUtils.showError("" + it.localizedMessage)
            }
    }

    override fun initClick() {
        super.initClick()
        fileAdapter.setOnItemClickListener { _, pos ->
            val file = fileAdapter.getItem(pos)
            val menu = mutableListOf<CharSequence>()
            val subscribed = DbManager.getDaoSession().subscribeFileDao.load(file.id)
            if (subscribed != null) {
                menu.add("????????????")
                menu.add("????????????")
            } else {
                menu.add("???????????????")
            }
            val checkSubscribeUpdate =
                SharedPreUtils.getInstance().getBoolean("checkSubscribeUpdate", true)
            if (checkSubscribeUpdate) {
                menu.add("????????????????????????????????????")
            } else {
                menu.add("????????????????????????????????????")
            }
            BottomMenu.show(file.name, menu)
                .setOnMenuItemClickListener { _, text, _ ->
                    when (text) {
                        "????????????", "???????????????" -> preSubscribe(file, pos)
                        "????????????" -> {
                            DbManager.getDaoSession().subscribeFileDao.deleteByKey(file.id)
                            fileAdapter.notifyItemChanged(pos)
                            DialogCreator.createCommonDialog(
                                this, "??????????????????",
                                "?????????????????????????????????????????????", false, { _, _ ->
                                    BookSourceManager.removeSourceBySubscribe(file)
                                    ToastUtils.showSuccess("??????????????????")
                                    setResult(Activity.RESULT_OK)
                                }, null
                            )
                        }
                        "????????????????????????????????????" -> {
                            SharedPreUtils.getInstance().putBoolean("checkSubscribeUpdate", false)
                            ToastUtils.showSuccess("?????????????????????????????????")
                        }
                        "????????????????????????????????????" -> {
                            SharedPreUtils.getInstance().putBoolean("checkSubscribeUpdate", true)
                            ToastUtils.showSuccess("?????????????????????????????????")
                        }
                    }
                    false
                }.setCancelButton(R.string.cancel)
        }
    }

    private fun preSubscribe(file: SubscribeFile, pos: Int) {
        AdUtils.checkHasAd().subscribe(object : MySingleObserver<Boolean?>() {
            override fun onSuccess(aBoolean: Boolean) {
                if (aBoolean && AdUtils.getAdConfig().isSubSource) {
                    DialogCreator.createCommonDialog(
                        this@SourceSubscribeActivity, "????????????",
                        "??????????????????????????????\n???????????????????????????????????????????????????", true, { _, _ ->
                            AdUtils.showRewardVideoAd(this@SourceSubscribeActivity) {
                                ToastUtils.showSuccess("?????????????????????????????????????????????")
                                subscribe(file, pos)
                            }
                        }, null
                    )
                } else {
                    DialogCreator.createCommonDialog(
                        this@SourceSubscribeActivity, "????????????",
                        "??????????????????????????????\n??????????????????????????????", true, { _, _ ->
                            subscribe(file, pos)
                        }, null
                    )
                }
            }
        })
    }

    private fun subscribe(file: SubscribeFile, pos: Int) {
        val dialog = LoadingDialog(this, "????????????") {
            subscribeDis?.dispose()
        }
        dialog.show()
        val oldSources = BookSourceManager.getSourceBySubscribe(file)
        BookSourceManager.removeBookSources(oldSources)
        BookSourceManager.importSource(file.url, file.id)
            .compose { RxUtils.toSimpleSingle(it) }
            .subscribe(object : MyObserver<List<BookSource>>() {
                override fun onSubscribe(d: Disposable) {
                    super.onSubscribe(d)
                    addDisposable(d)
                    subscribeDis = d
                }

                override fun onNext(sources: List<BookSource>) {
                    val size: Int = sources.size
                    if (sources.isNotEmpty()) {
                        DbManager.getDaoSession().subscribeFileDao.insertOrReplace(file)
                        fileAdapter.notifyItemChanged(pos)
                        ToastUtils.showSuccess(String.format("????????????????????????????????????%s?????????", size))
                        setResult(Activity.RESULT_OK)
                    } else {
                        ToastUtils.showError("????????????????????????????????????\nsources.size==0")
                        BookSourceManager.addBookSource(oldSources)
                    }
                    dialog.dismiss()
                }

                override fun onError(e: Throwable) {
                    super.onError(e)
                    e.printStackTrace()
                    BookSourceManager.addBookSource(oldSources)
                    ToastUtils.showError("????????????????????????????????????\n" + e.localizedMessage)
                    dialog.dismiss()
                }
            })
    }

    private fun lanZouFile2SubscribeFile(lanZouFile: List<LanZouFile>): MutableList<SubscribeFile> {
        val fileMap = LinkedHashMap<String, SubscribeFile>()
        lanZouFile.forEach {
            val param = it.name_all.removeSuffix(".txt").split("#")
            if (fileMap.containsKey(param[0])) {
                if (fileMap[param[0]]!!.date < param[2]) {
                    fileMap[param[0]] =
                        SubscribeFile(
                            param[0],
                            param[1].replace("nv", "???"),
                            URLCONST.LAN_ZOU_URL + "/${it.id}",
                            param[2],
                            it.size
                        )
                }
            } else {
                fileMap[param[0]] =
                    SubscribeFile(
                        param[0],
                        param[1].replace("nv", "???"),
                        URLCONST.LAN_ZOU_URL + "/${it.id}",
                        param[2],
                        it.size
                    )
            }
        }
        return fileMap.values.toMutableList()
    }
}