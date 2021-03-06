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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.LinkMovementMethod;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import io.reactivex.disposables.Disposable;
import top.cronos.myreader.R;
import top.cronos.myreader.application.App;
import top.cronos.myreader.base.BaseActivity;
import top.cronos.myreader.base.MyTextWatcher;
import top.cronos.myreader.base.observer.MySingleObserver;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.databinding.ActivityRegisterBinding;
import top.cronos.myreader.model.user.Result;
import top.cronos.myreader.model.user.User;
import top.cronos.myreader.model.user.UserService;
import top.cronos.myreader.ui.dialog.DialogCreator;
import top.cronos.myreader.ui.dialog.LoadingDialog;
import top.cronos.myreader.util.CodeUtil;
import top.cronos.myreader.util.CyptoUtils;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.utils.StringUtils;

/**
 * @author fengyue
 * @date 2020/9/18 22:37
 */
public class RegisterActivity extends BaseActivity<ActivityRegisterBinding> {

    private String code;
    private String username = "";
    private String password = "";
    private String email = "";
    private String emailCode = "";
    private String keyc = "";
    private String inputCode = "";
    private LoadingDialog dialog;
    private Disposable disp;

    @Override
    protected void bindView() {
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar);
        setStatusBarColor(R.color.colorPrimary, true);
        getSupportActionBar().setTitle("??????");
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        dialog = new LoadingDialog(this, "????????????", () -> {
            if (disp != null) {
                disp.dispose();
            }
        });
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        createCaptcha();
        binding.etUsername.requestFocus();
        binding.etUsername.getEditText().addTextChangedListener(new MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                StringUtils.isNotChinese(s);
                username = s.toString();
                if (username.length() < 6 || username.length() > 14) {
                    showTip("??????????????????6-14?????????");
                } else if (!username.substring(0, 1).matches("^[A-Za-z]$")) {
                    showTip("??????????????????????????????");
                } else if (!username.matches("^[A-Za-z0-9-_]+$")) {
                    showTip("????????????????????????????????????????????????????????????");
                } else {
                    binding.tvRegisterTip.setVisibility(View.GONE);
                }
                checkNotNone();
            }
        });

        binding.etPassword.getEditText().addTextChangedListener(new MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                password = s.toString();
                if (password.length() < 8 || password.length() > 16) {
                    showTip("???????????????8-16?????????");
                } else if (password.matches("^\\d+$")) {
                    showTip("????????????????????????");
                } else {
                    binding.tvRegisterTip.setVisibility(View.GONE);
                }
                checkNotNone();
            }
        });

        binding.etEmail.getEditText().addTextChangedListener(new MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                email = s.toString();
                if (!email.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$")) {
                    showTip("??????????????????");
                } else {
                    binding.tvRegisterTip.setVisibility(View.GONE);
                }
                checkNotNone();
            }
        });

        binding.etEmailCode.getEditText().addTextChangedListener(new MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                emailCode = s.toString().trim();
                checkNotNone();
            }
        });

        binding.etCaptcha.getEditText().addTextChangedListener(new MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                inputCode = s.toString().trim().toLowerCase();
                if (!inputCode.equals(code.toLowerCase())) {
                    showTip("???????????????");
                } else {
                    binding.tvRegisterTip.setVisibility(View.GONE);
                }
                checkNotNone();
            }
        });

        binding.cbAgreement.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void initClick() {
        super.initClick();
        binding.ivCaptcha.setOnClickListener(v -> createCaptcha());
        binding.tvGetEmailCode.setOnClickListener(v -> {
            if (!email.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$")) {
                ToastUtils.showWarring("?????????????????????");
                return;
            }
            dialog.show();
            dialog.setmMessage("????????????");
            UserService.INSTANCE.sendEmail(email, "reg", keyc).subscribe(new MySingleObserver<Result>() {
                @Override
                public void onSubscribe(Disposable d) {
                    addDisposable(d);
                    disp = d;
                }

                @Override
                public void onSuccess(@NonNull Result result) {
                    if (result.getCode() == 106) {
                        ToastUtils.showSuccess("?????????????????????");
                        keyc = result.getResult().toString();
                        timeDown(60);
                    } else {
                        ToastUtils.showWarring(result.getResult().toString());
                    }
                    dialog.dismiss();
                }

                @Override
                public void onError(Throwable e) {
                    ToastUtils.showError("????????????????????????\n" + e.getLocalizedMessage());
                    dialog.dismiss();
                }
            });
        });

        binding.btRegister.setOnClickListener(v -> {
            if (!username.matches("^[A-Za-z][A-Za-z0-9]{5,13}$")) {
                DialogCreator.createTipDialog(this, "?????????????????????",
                        "??????????????????6-14?????????\n??????????????????????????????\n????????????????????????????????????????????????????????????");
            } else if (password.matches("^\\d+$") || !password.matches("^.{8,16}$")) {
                DialogCreator.createTipDialog(this, "??????????????????",
                        "???????????????8-16?????????\n????????????????????????");
            } else if (!email.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$")) {
                DialogCreator.createTipDialog(this, "??????????????????",
                        "??????????????????????????????????????????@????????????.com(.cn???)");
            } else if ("".equals(keyc)) {
                DialogCreator.createTipDialog(this, "???????????????????????????");
            } else if (emailCode.length() < 6) {
                DialogCreator.createTipDialog(this, "?????????6??????????????????");
            } else if (!inputCode.trim().equalsIgnoreCase(code)) {
                DialogCreator.createTipDialog(this, "???????????????");
            } else if (!binding.cbAgreement.isChecked()) {
                DialogCreator.createTipDialog(this, "???????????????????????????????????????");
            } else {
                dialog.show();
                dialog.setmMessage("????????????");
                User user = new User(username, CyptoUtils.encode(APPCONST.KEY, password), email);
                UserService.INSTANCE.register(user, emailCode, keyc).subscribe(new MySingleObserver<Result>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                        disp = d;
                    }

                    @Override
                    public void onSuccess(@NonNull Result result) {
                        if (result.getCode() == 101) {
                            UserService.INSTANCE.writeUsername(user.getUserName());
                            ToastUtils.showSuccess(result.getResult().toString());
                            finish();
                        } else {
                            ToastUtils.showWarring(result.getResult().toString());
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.showError("???????????????\n" + e.getLocalizedMessage());
                        dialog.dismiss();
                        createCaptcha();
                    }
                });
            }
        });
    }

    public void createCaptcha() {
        code = CodeUtil.getInstance().createCode();
        Bitmap codeBitmap = CodeUtil.getInstance().createBitmap(code);
        binding.ivCaptcha.setImageBitmap(codeBitmap);
    }

    public void showTip(String tip) {
        binding.tvRegisterTip.setVisibility(View.VISIBLE);
        binding.tvRegisterTip.setText(tip);
    }

    private void timeDown(int time) {
        if (time == 0) {
            binding.tvGetEmailCode.setText(getString(R.string.re_get_email_code, ""));
            binding.tvGetEmailCode.setEnabled(true);
        } else {
            binding.tvGetEmailCode.setEnabled(false);
            String timeStr = "(" + time + ")";
            binding.tvGetEmailCode.setText(getString(R.string.re_get_email_code, timeStr));
            App.getHandler().postDelayed(() -> timeDown(time - 1), 1000);
        }
    }

    public void checkNotNone() {
        binding.btRegister.setEnabled(!"".equals(username) &&
                !"".equals(password) &&
                !"".equals(email) &&
                !"".equals(emailCode) &&
                !"".equals(inputCode));
    }

    @Override
    protected void onDestroy() {
        dialog.dismiss();
        super.onDestroy();
    }
}
