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

package top.cronos.myreader.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kongzue.dialogx.dialogs.BottomDialog;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.interfaces.OnBindView;
import com.kongzue.dialogx.interfaces.OnMenuItemSelectListener;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.base.BitIntentDataManager;
import top.cronos.myreader.base.observer.MySingleObserver;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.entity.Chapter;
import top.cronos.myreader.greendao.entity.rule.BookSource;
import top.cronos.myreader.greendao.service.BookService;
import top.cronos.myreader.greendao.service.ChapterService;
import top.cronos.myreader.model.sourceAnalyzer.BookSourceManager;
import top.cronos.myreader.ui.activity.BookDetailedActivity;
import top.cronos.myreader.ui.activity.BookInfoEditActivity;
import top.cronos.myreader.ui.activity.SourceEditActivity;
import top.cronos.myreader.ui.adapter.helper.IItemTouchHelperViewHolder;
import top.cronos.myreader.ui.adapter.helper.ItemTouchCallback;
import top.cronos.myreader.ui.dialog.DialogCreator;
import top.cronos.myreader.ui.dialog.SourceExchangeDialog;
import top.cronos.myreader.ui.presenter.BookcasePresenter;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.help.StringHelper;
import top.cronos.myreader.util.utils.FileUtils;
import top.cronos.myreader.util.utils.NetworkUtils;
import top.cronos.myreader.util.utils.RxUtils;
import top.cronos.myreader.util.utils.ShareBookUtil;
import top.cronos.myreader.util.utils.StoragePermissionUtils;
import top.cronos.myreader.webapi.crawler.ReadCrawlerUtil;
import top.cronos.myreader.webapi.crawler.base.ReadCrawler;
import top.cronos.myreader.widget.BadgeView;
import top.cronos.myreader.widget.CoverImageView;
import top.cronos.myreader.widget.SwitchButton;

/**
 * @author fengyue
 * @date 2020/4/19 11:23
 */
public abstract class BookcaseAdapter extends RecyclerView.Adapter<BookcaseAdapter.ViewHolder> {

    private final Map<String, Boolean> isLoading = new HashMap<>();
    private final Map<String, Boolean> mCheckMap = new LinkedHashMap<>();
    private int mCheckedCount = 0;
    protected OnBookCheckedListener mListener;
    protected boolean isCheckedAll;
    protected int mResourceId;
    protected ArrayList<Book> list;
    protected Context mContext;
    protected boolean mEditState;
    protected BookService mBookService;
    protected ChapterService mChapterService;
    protected BookcasePresenter mBookcasePresenter;
    protected boolean isGroup;
    protected String[] menu = {
            App.getmContext().getResources().getString(R.string.menu_book_Top),
            App.getmContext().getResources().getString(R.string.menu_book_download),
            App.getmContext().getResources().getString(R.string.menu_book_cache),
            App.getmContext().getResources().getString(R.string.menu_book_delete)
    };
    protected ItemTouchCallback.OnItemTouchListener itemTouchCallbackListener;

    public BookcaseAdapter(Context context, int textViewResourceId, ArrayList<Book> objects
            , boolean editState, BookcasePresenter bookcasePresenter, boolean isGroup) {
        mContext = context;
        mResourceId = textViewResourceId;
        list = objects;
        mEditState = editState;
        mBookService = BookService.getInstance();
        mChapterService = ChapterService.getInstance();
        mBookcasePresenter = bookcasePresenter;
        this.isGroup = isGroup;
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public Book getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return !isGroup ? list.get(position).getSortCode() : list.get(position).getGroupSort();
    }

    public void onDataMove() {
        for (int i = 0; i < list.size(); i++) {
            if (!isGroup) {
                list.get(i).setSortCode(i);
            } else {
                list.get(i).setGroupSort(i);
            }
        }
        mBookService.updateBooks(list);
    }

    public void remove(Book item) {
        list.remove(item);
        notifyDataSetChanged();
        mBookService.deleteBook(item);
    }

    public void add(Book item) {
        list.add(item);
        notifyDataSetChanged();
        mBookService.addBook(item);
    }

    protected void showDeleteBookDialog(final Book book) {
        if (!isGroup) {
            DialogCreator.createCommonDialog(mContext, "????????????", "???????????????" + book.getName() + "???????????????????????????",
                    true, (dialogInterface, i) -> {
                        remove(book);
                        ToastUtils.showSuccess("?????????????????????");
                        mBookcasePresenter.init();
                    }, null);
        } else {
            DialogCreator.createCommonDialog(mContext, "??????/????????????", "?????????????????????" + book.getName() + "??????????????????????????????????????????????????????(??????????????????)??????",
                    true, "????????????", "??????????????????", (dialogInterface, i) -> {
                        remove(book);
                        ToastUtils.showSuccess("?????????????????????");
                        mBookcasePresenter.init();
                    }, (dialog, which) -> {
                        book.setGroupId("");
                        mBookService.updateEntity(book);
                        ToastUtils.showSuccess("??????????????????????????????");
                        mBookcasePresenter.init();
                    });
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param mEditState
     */
    public void setmEditState(boolean mEditState) {
        if (mEditState) {
            mCheckMap.clear();
            for (Book book : list) {
                mCheckMap.put(book.getId(), false);
            }
            mCheckedCount = 0;
        } /*else {
            onDataMove();
        }*/
        this.mEditState = mEditState;
        notifyDataSetChanged();
    }

    public boolean ismEditState() {
        return mEditState;
    }

    /**
     * getter??????
     *
     * @return
     */
    public Map<String, Boolean> getIsLoading() {
        return isLoading;
    }

    public boolean isBookLoading(String bookID) {
        return isLoading.get(bookID);
    }

    public ItemTouchCallback.OnItemTouchListener getItemTouchCallbackListener() {
        return itemTouchCallbackListener;
    }

    public boolean unionChapterCathe(Book book) throws IOException {
        ArrayList<Chapter> chapters = (ArrayList<Chapter>) mChapterService.findBookAllChapterByBookId(book.getId());
        if (chapters.size() == 0) {
            return false;
        }
        File bookFile = new File(APPCONST.BOOK_CACHE_PATH + book.getId());
        if (!bookFile.exists()) {
            return false;
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        String filePath = APPCONST.TXT_BOOK_DIR + book.getName() + (TextUtils.isEmpty(book.getAuthor())
                ? "" : " ?????????" + book.getAuthor()) + ".txt";
        bw = new BufferedWriter(new FileWriter(FileUtils.getFile(filePath)));
        bw.write("???" + book.getName() + "???\n");
        if (!TextUtils.isEmpty(book.getAuthor())) bw.write("?????????" + book.getAuthor() + "\n");
        if (!TextUtils.isEmpty(book.getDesc())) bw.write("?????????\n" + book.getDesc() + "\n");
        bw.newLine();
        for (Chapter chapter : chapters) {
            if (ChapterService.isChapterCached(chapter)) {
                bw.write("\t" + chapter.getTitle());
                bw.newLine();
                br = new BufferedReader(new FileReader(ChapterService.getChapterFile(chapter)));
                String line = null;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
                br.close();
            }
        }
        bw.flush();
        bw.close();
        return true;
    }


    //??????????????????
    public void setCheckedBook(String bookId) {
        boolean isSelected = mCheckMap.get(bookId);
        if (isSelected) {
            mCheckMap.put(bookId, false);
            --mCheckedCount;
        } else {
            mCheckMap.put(bookId, true);
            ++mCheckedCount;
        }
        notifyDataSetChanged();
    }

    //??????
    public void setCheckedAll(boolean isChecked) {
        mCheckedCount = isChecked ? mCheckMap.size() : 0;
        for (String bookId : mCheckMap.keySet()) {
            mCheckMap.put(bookId, isChecked);
        }
        mListener.onItemCheckedChange(true);
        notifyDataSetChanged();
    }

    public boolean getBookIsChecked(String bookId) {
        return mCheckMap.get(bookId);
    }

    public int getmCheckedCount() {
        return mCheckedCount;
    }

    public int getmCheckableCount() {
        return mCheckMap.size();
    }

    public boolean isCheckedAll() {
        return isCheckedAll;
    }

    public void setIsCheckedAll(boolean isCheckedAll) {
        this.isCheckedAll = isCheckedAll;
    }

    public List<Book> getSelectBooks() {
        List<Book> mSelectBooks = new ArrayList<>();
        for (String bookId : mCheckMap.keySet()) {
            if (mCheckMap.get(bookId)) {
                mSelectBooks.add(mBookService.getBookById(bookId));
            }
        }
        return mSelectBooks;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    /*******************************************????????????*********************************************************/
    private int selectedIndex;//?????????????????????

    protected void downloadBook(final Book book) {
        if (!NetworkUtils.isNetWorkAvailable()) {
            ToastUtils.showWarring("??????????????????");
            return;
        }
        if ("????????????".equals(book.getType())) {
            ToastUtils.showWarring("???" + book.getName() + "?????????????????????????????????");
            return;
        }
        final int[] begin = new int[1];
        final int[] end = new int[1];
        /*MyAlertDialog.build(mContext)
                .setTitle("????????????")
                .setSingleChoiceItems(mContext.getResources().getStringArray(R.array.download), selectedIndex,
                        (dialog, which) -> selectedIndex = which).setNegativeButton("??????", ((dialog, which) -> dialog.dismiss())).setPositiveButton("??????",
                (dialog, which) -> {
                    switch (selectedIndex) {
                        case 0:
                            begin[0] = book.getHisttoryChapterNum();
                            end[0] = book.getHisttoryChapterNum() + 50;
                            break;
                        case 1:
                            begin[0] = book.getHisttoryChapterNum() - 50;
                            end[0] = book.getHisttoryChapterNum() + 50;
                            break;
                        case 2:
                            begin[0] = book.getHisttoryChapterNum();
                            end[0] = 99999;
                            break;
                        case 3:
                            begin[0] = 0;
                            end[0] = 99999;
                            break;
                    }
                    Thread downloadThread = new Thread(() -> {
                        ArrayList<Chapter> chapters = (ArrayList<Chapter>) mChapterService.findBookAllChapterByBookId(book.getId());
                        mBookcasePresenter.addDownload(book, chapters, begin[0], end[0], false);
                    });
                    mBookcasePresenter.getEs().submit(downloadThread);
                }).show();*/
        BottomMenu.show("????????????", mContext.getResources().getStringArray(R.array.download))
                .setSelection(selectedIndex)
                .setOnMenuItemClickListener(new OnMenuItemSelectListener<BottomMenu>() {
                    @Override
                    public void onOneItemSelect(BottomMenu dialog, CharSequence text, int which) {
                        selectedIndex = which;
                    }
                }).setCancelButton("??????", (baseDialog, v) -> {
            switch (selectedIndex) {
                case 0:
                    begin[0] = book.getHisttoryChapterNum();
                    end[0] = book.getHisttoryChapterNum() + 50;
                    break;
                case 1:
                    begin[0] = book.getHisttoryChapterNum() - 50;
                    end[0] = book.getHisttoryChapterNum() + 50;
                    break;
                case 2:
                    begin[0] = book.getHisttoryChapterNum();
                    end[0] = 99999;
                    break;
                case 3:
                    begin[0] = 0;
                    end[0] = 99999;
                    break;
            }
            Thread downloadThread = new Thread(() -> {
                ArrayList<Chapter> chapters = (ArrayList<Chapter>) mChapterService.findBookAllChapterByBookId(book.getId());
                mBookcasePresenter.addDownload(book, chapters, begin[0], end[0], false);
            });
            mBookcasePresenter.getEs().submit(downloadThread);
            return false;
        });
    }

    public void refreshBook(String chapterUrl) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getChapterUrl(), chapterUrl)) {
                notifyItemChanged(i);
            }
        }
    }

    public void showBookMenu(Book book, int pos) {
        BottomDialog.show(new BookMenuDialog(book, pos));
    }


    class BookMenuDialog extends OnBindView<BottomDialog> {
        private BottomDialog dialog;
        private Book mBook;
        private int pos;

        private RelativeLayout rlBookDetail;
        private CoverImageView ivBookImg;
        private TextView tvBookName;
        private TextView tvBookAuthor;
        private TextView tvTop;
        private SwitchButton sbIsUpdate;
        private TextView tvDownload;
        private TextView tvExport;
        private TextView tvChangeSource;
        private TextView tvSetGroup;
        private TextView tvShare;
        private TextView tvRemove;
        private TextView tvEdit;
        private TextView tvRefresh;
        private TextView tvLink;
        private TextView tvEditSource;

        public BookMenuDialog(Book book, int pos) {
            super("????????????".equals(book.getType()) ? R.layout.menu_book_local : R.layout.menu_book);
            mBook = book;
            this.pos = pos;
        }

        @Override
        public void onBind(BottomDialog dialog, View v) {
            this.dialog = dialog;
            bindLocalView(v);
            if (!"????????????".equals(mBook.getType())) {
                bindView(v);
            }
        }

        private void bindView(View v) {
            sbIsUpdate = v.findViewById(R.id.sb_is_update);
            tvDownload = v.findViewById(R.id.tv_download);
            tvExport = v.findViewById(R.id.tv_export_cathe);
            tvChangeSource = v.findViewById(R.id.tv_change_source);
            tvShare = v.findViewById(R.id.tv_share);
            tvRefresh = v.findViewById(R.id.tv_refresh);
            tvLink = v.findViewById(R.id.tv_link);
            tvEditSource = v.findViewById(R.id.tv_edit_source);
            bindEvent();
        }

        private void bindEvent() {
            sbIsUpdate.setChecked(!mBook.getIsCloseUpdate());
            sbIsUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mBook.setIsCloseUpdate(!mBook.getIsCloseUpdate());
                mBookService.updateEntity(mBook);
            });
            tvDownload.setOnClickListener(v -> {
                dialog.dismiss();
                downloadBook(mBook);
            });
            tvExport.setOnClickListener(v -> {
                dialog.dismiss();
                Single.create((SingleOnSubscribe<Boolean>) emitter -> {
                    StoragePermissionUtils.request(mContext, (permissions, all) -> {
                        try {
                            emitter.onSuccess(unionChapterCathe(mBook));
                        } catch (IOException e) {
                            emitter.onError(e);
                        }
                    });
                }).compose(RxUtils::toSimpleSingle).subscribe(new MySingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(@NotNull Boolean aBoolean) {
                        if (aBoolean) {
                            DialogCreator.createTipDialog(mContext,
                                    "????????????????????????????????????"
                                            + APPCONST.TXT_BOOK_DIR);
                        } else {
                            DialogCreator.createTipDialog(mContext,
                                    "??????????????????????????????????????????????????????????????????");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        DialogCreator.createTipDialog(mContext,
                                "??????????????????????????????????????????????????????????????????");
                    }
                });
            });
            tvChangeSource.setOnClickListener(v -> {
                this.dialog.dismiss();
                SourceExchangeDialog dialog = new SourceExchangeDialog((Activity) mContext, mBook);
                dialog.setOnSourceChangeListener((bean, pos) -> {
                    Book bookTem = mBook.changeSource(bean);
                    mBookService.updateBook(mBook, bookTem);
                    mBook = bookTem;
                    list.set(this.pos, mBook);
                    mBookcasePresenter.refreshBook(mBook, true);
                });
                dialog.show();
            });
            tvShare.setOnClickListener(v -> {
                dialog.dismiss();
                ShareBookUtil.shareBook(mContext, mBook, ivBookImg);
            });
            tvRefresh.setOnClickListener(v -> {
                dialog.dismiss();
                mBookcasePresenter.refreshBook(mBook, false);
            });
            tvLink.setOnClickListener(v -> {
                ReadCrawler rc = ReadCrawlerUtil.getReadCrawler(mBook.getSource());
                Uri uri = Uri.parse(NetworkUtils.getAbsoluteURL(rc.getNameSpace(), mBook.getChapterUrl()));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                App.getHandler().postDelayed(() -> mContext.startActivity(intent), 300);
                dialog.dismiss();
            });
            tvEditSource.setOnClickListener(v -> {
                BookSource source = BookSourceManager.getBookSourceByStr(mBook.getSource());
                if (TextUtils.isEmpty(source.getSourceType())) {
                    ToastUtils.showWarring("???????????????????????????");
                } else {
                    Intent sourceIntent = new Intent(mContext, SourceEditActivity.class);
                    sourceIntent.putExtra(APPCONST.BOOK_SOURCE, source);
                    App.getHandler().postDelayed(() -> mContext.startActivity(sourceIntent), 300);
                    dialog.dismiss();
                }
            });
        }

        private void bindLocalView(View v) {
            rlBookDetail = v.findViewById(R.id.rl_book_detail);
            ivBookImg = v.findViewById(R.id.iv_book_img);
            tvBookName = v.findViewById(R.id.tv_book_name);
            tvBookAuthor = v.findViewById(R.id.tv_book_author);
            tvEdit = v.findViewById(R.id.tv_edit);
            tvTop = v.findViewById(R.id.tv_top);
            tvSetGroup = v.findViewById(R.id.tv_set_group);
            tvRemove = v.findViewById(R.id.tv_remove);
            bindLocalEvent();
        }

        private void bindLocalEvent() {
            rlBookDetail.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, BookDetailedActivity.class);
                BitIntentDataManager.getInstance().putData(intent, mBook);
                App.getHandler().postDelayed(() -> mContext.startActivity(intent), 300);
                dialog.dismiss();
            });
            if (!App.isDestroy((Activity) mContext)) {
                ReadCrawler rc = ReadCrawlerUtil.getReadCrawler(mBook.getSource());
                ivBookImg.load(NetworkUtils.getAbsoluteURL(rc.getNameSpace(), mBook.getImgUrl()), mBook.getName(), mBook.getAuthor());
            }
            tvBookName.setText(mBook.getName());
            tvBookAuthor.setText(mBook.getAuthor());
            tvEdit.setOnClickListener(v -> {
                Intent editIntent = new Intent(mContext, BookInfoEditActivity.class);
                BitIntentDataManager.getInstance().putData(editIntent, mBook);
                App.getHandler().postDelayed(() -> mContext.startActivity(editIntent), 300);
                dialog.dismiss();
            });
            tvTop.setOnClickListener(v -> {
                if (!isGroup) {
                    mBook.setSortCode(0);
                } else {
                    mBook.setGroupSort(0);
                }
                mBookService.updateEntity(mBook);
                mBookcasePresenter.init();
                ToastUtils.showSuccess("?????????" + mBook.getName() + "????????????????????????");
                dialog.dismiss();
            });
            tvSetGroup.setOnClickListener(v -> {
                dialog.dismiss();
                mBookcasePresenter.addGroup(mBook);
            });
            tvRemove.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteBookDialog(mBook);
            });
        }

    }

    static class ViewHolder extends RecyclerView.ViewHolder implements IItemTouchHelperViewHolder {
        CheckBox cbBookChecked;
        CoverImageView ivBookImg;
        TextView tvBookName;
        BadgeView tvNoReadNum;
        ProgressBar pbLoading;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
        }

        @Override
        public void onItemSelected(RecyclerView.ViewHolder viewHolder) {
            itemView.setTranslationZ(10);
        }

        @Override
        public void onItemClear(RecyclerView.ViewHolder viewHolder) {
            itemView.setTranslationZ(0);
        }
    }

    public void setOnBookCheckedListener(OnBookCheckedListener listener) {
        mListener = listener;
    }

    //?????????????????????
    public interface OnBookCheckedListener {
        void onItemCheckedChange(boolean isChecked);
    }
}
