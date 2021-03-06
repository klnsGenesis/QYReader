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

package top.cronos.myreader.ui.activity;


import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.cronos.myreader.R;
import top.cronos.myreader.base.BaseTabActivity;
import top.cronos.myreader.databinding.ActivityFileSystemBinding;
import top.cronos.myreader.enums.LocalBookSource;
import top.cronos.myreader.greendao.entity.Book;
import top.cronos.myreader.greendao.service.BookService;
import top.cronos.myreader.ui.dialog.DialogCreator;
import top.cronos.myreader.ui.fragment.BaseFileFragment;
import top.cronos.myreader.ui.fragment.FileCategoryFragment;
import top.cronos.myreader.ui.fragment.LocalBookFragment;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.utils.FileUtils;


/**
 * @author fengyue
 * @date 2020/8/12 20:02
 */

public class FileSystemActivity extends BaseTabActivity<ActivityFileSystemBinding> {
    private static final String TAG = "FileSystemActivity";

    private LocalBookFragment mLocalFragment;
    private FileCategoryFragment mCategoryFragment;
    private BaseFileFragment mCurFragment;

    private BaseFileFragment.OnFileCheckedListener mListener = new BaseFileFragment.OnFileCheckedListener() {
        @Override
        public void onItemCheckedChange(boolean isChecked) {
            changeMenuStatus();
        }

        @Override
        public void onCategoryChanged() {
            //????????????
            mCurFragment.setCheckedAll(false);
            //????????????
            changeMenuStatus();
            //????????????????????????
            changeCheckedAllStatus();
        }
    };

    @Override
    protected void bindView() {
        binding = ActivityFileSystemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        super.bindView();
    }

    @Override
    protected List<Fragment> createTabFragments() {
        mLocalFragment = new LocalBookFragment();
        mCategoryFragment = new FileCategoryFragment();
        return Arrays.asList(mLocalFragment, mCategoryFragment);
    }

    @Override
    protected List<String> createTabTitles() {
        return Arrays.asList("????????????", "????????????");
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar);
        setStatusBarColor(R.color.colorPrimary, true);
        getSupportActionBar().setTitle("????????????");
    }

    @Override
    protected void initClick() {
        super.initClick();
        binding.fileSystemCbSelectedAll.setOnClickListener(
                (view) -> {
                    //??????????????????
                    boolean isChecked = binding.fileSystemCbSelectedAll.isChecked();
                    mCurFragment.setCheckedAll(isChecked);
                    //??????????????????
                    changeMenuStatus();
                }
        );

        mVp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mCurFragment = mLocalFragment;
                } else {
                    mCurFragment = mCategoryFragment;
                }
                //??????????????????
                changeMenuStatus();
                //????????????????????????
                changeCheckedAllStatus();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        binding.fileSystemBtnAddBook.setOnClickListener(
                (v) -> {
                    //?????????????????????
                    List<File> files = mCurFragment.getCheckedFiles();
                    //?????????Book,?????????
                    List<Book> books = convertBook(files);
                    BookService.getInstance()
                            .addBooks(books);
                    //??????HashMap???false
                    mCurFragment.setCheckedAll(false);
                    //??????????????????
                    changeMenuStatus();
                    //????????????????????????
                    changeCheckedAllStatus();
                    //????????????????????????
                    ToastUtils.showSuccess(getResources().getString(R.string.file_add_succeed, books.size()));

                }
        );

        binding.fileSystemBtnDelete.setOnClickListener(
                (v) -> {
                    //?????????????????????????????????
                    DialogCreator.createCommonDialog(this, "????????????", "??????????????????????",
                            true, (dialog, which) -> {
                                //?????????????????????
                                mCurFragment.deleteCheckedFiles();
                                //??????????????????
                                changeMenuStatus();
                                //????????????????????????
                                changeCheckedAllStatus();
                                //????????????????????????
                                ToastUtils.showSuccess("??????????????????");
                            }, null);
                }
        );

        mLocalFragment.setOnFileCheckedListener(mListener);
        mCategoryFragment.setOnFileCheckedListener(mListener);
    }

    @Override
    protected void processLogic() {
        super.processLogic();
        mCurFragment = mLocalFragment;
    }


    @Override
    public void onBackPressed() {
        if (mCurFragment == mCategoryFragment) {
            if (mCategoryFragment.backLast()) return;
        }
        super.onBackPressed();
    }

    /**
     * ??????????????????CollBook
     *
     * @param files:???????????????????????????
     * @return
     */
    private List<Book> convertBook(List<File> files) {
        List<Book> books = new ArrayList<>(files.size());
        for (File file : files) {
            //????????????????????????
            if (!file.exists()) continue;

            Book book = new Book();
            book.setName(file.getName().replace(FileUtils.SUFFIX_EPUB, "").replace(FileUtils.SUFFIX_TXT, ""));
            book.setChapterUrl(file.getAbsolutePath());
            book.setType("????????????");
            book.setHistoryChapterId("???????????????");
            book.setNewestChapterTitle("???????????????");
            book.setAuthor("????????????");
            book.setSource(LocalBookSource.local.toString());
            book.setDesc("???");
            book.setIsCloseUpdate(true);
            books.add(book);
        }
        return books;
    }

    /**
     * ??????????????????????????????
     */
    private void changeMenuStatus() {

        //??????????????????????????????
        if (mCurFragment.getCheckedCount() == 0) {
            binding.fileSystemBtnAddBook.setText(getString(R.string.file_add_shelf));
            //????????????????????????????????????
            setMenuClickable(false);

            if (binding.fileSystemCbSelectedAll.isChecked()) {
                mCurFragment.setChecked(false);
                binding.fileSystemCbSelectedAll.setChecked(mCurFragment.isCheckedAll());
            }

        } else {
            binding.fileSystemBtnAddBook.setText(getString(R.string.file_add_shelves, mCurFragment.getCheckedCount()));
            setMenuClickable(true);

            //?????????????????????

            //???????????????????????????????????????????????????
            if (mCurFragment.getCheckedCount() == mCurFragment.getCheckableCount()) {
                //???????????????
                mCurFragment.setChecked(true);
                binding.fileSystemCbSelectedAll.setChecked(mCurFragment.isCheckedAll());
            }
            //??????????????????????????????
            else if (mCurFragment.isCheckedAll()) {
                mCurFragment.setChecked(false);
                binding.fileSystemCbSelectedAll.setChecked(mCurFragment.isCheckedAll());
            }
        }

        //?????????????????????
        if (mCurFragment.isCheckedAll()) {
            binding.fileSystemCbSelectedAll.setText("??????");
        } else {
            binding.fileSystemCbSelectedAll.setText("??????");
        }

    }

    private void setMenuClickable(boolean isClickable) {

        //?????????????????????
        binding.fileSystemBtnDelete.setEnabled(isClickable);
        binding.fileSystemBtnDelete.setClickable(isClickable);

        //???????????????????????????
        binding.fileSystemBtnAddBook.setEnabled(isClickable);
        binding.fileSystemBtnAddBook.setClickable(isClickable);
    }

    /**
     * ???????????????????????????
     */
    private void changeCheckedAllStatus() {
        //??????????????????????????????
        int count = mCurFragment.getCheckableCount();

        //????????????????????????
        if (count > 0) {
            binding.fileSystemCbSelectedAll.setClickable(true);
            binding.fileSystemCbSelectedAll.setEnabled(true);
        } else {
            binding.fileSystemCbSelectedAll.setClickable(false);
            binding.fileSystemCbSelectedAll.setEnabled(false);
        }
    }
}
