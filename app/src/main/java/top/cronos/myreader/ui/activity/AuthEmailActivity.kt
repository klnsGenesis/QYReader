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
import android.text.Editable
import android.view.View
import androidx.appcompat.widget.Toolbar
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import top.cronos.myreader.R
import top.cronos.myreader.application.App
import top.cronos.myreader.base.BaseActivity
import top.cronos.myreader.base.BitIntentDataManager
import top.cronos.myreader.base.MyTextWatcher
import top.cronos.myreader.base.observer.MySingleObserver
import top.cronos.myreader.common.APPCONST
import top.cronos.myreader.databinding.ActivityAuthEmailBinding
import top.cronos.myreader.model.user.Result
import top.cronos.myreader.model.user.User
import top.cronos.myreader.model.user.UserService.bindEmail
import top.cronos.myreader.model.user.UserService.resetPwd
import top.cronos.myreader.model.user.UserService.sendEmail
import top.cronos.myreader.ui.dialog.DialogCreator
import top.cronos.myreader.ui.dialog.LoadingDialog
import top.cronos.myreader.util.CyptoUtils
import top.cronos.myreader.util.ToastUtils
import java.util.*

/**
 * @author fengyue
 * @date 2021/12/9 15:20
 */
class AuthEmailActivity : BaseActivity<ActivityAuthEmailBinding>(), SingleObserver<Result> {

    private var email = ""
    private var password = ""
    private var emailCode = ""
    private var keyc = ""
    private lateinit var dialog: LoadingDialog
    private var disp: Disposable? = null
    private lateinit var operator: String
    private var user: User? = null
    private var isBind: Boolean = false
    override fun bindView() {
        binding = ActivityAuthEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initData(savedInstanceState: Bundle?) {
        user = BitIntentDataManager.getInstance().getData(intent) as User?
        isBind = user != null
        operator = if (isBind) "????????????" else "????????????"
        dialog = LoadingDialog(this, "????????????") {
            disp?.dispose()
        }
    }

    override fun setUpToolbar(toolbar: Toolbar?) {
        super.setUpToolbar(toolbar)
        setStatusBarColor(R.color.colorPrimary, true)
        supportActionBar?.title = operator
    }

    override fun initWidget() {
        binding.tvTitle.text = operator
        binding.btSubmit.text = operator
        binding.etEmail.editText?.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                email = s.toString()
                if (!email.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$".toRegex())) {
                    showTip("??????????????????")
                } else {
                    binding.tvRegisterTip.visibility = View.GONE
                }
                checkNotNone()
            }
        })

        binding.etEmailCode.editText?.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                emailCode = s.toString().trim()
                checkNotNone()
            }
        })
        binding.etPassword.editText!!.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                password = s.toString()
                if (password.length < 8 || password.length > 16) {
                    showTip("???????????????8-16?????????")
                } else if (password.matches("^\\d+$".toRegex())) {
                    showTip("????????????????????????")
                } else {
                    binding.tvRegisterTip.visibility = View.GONE
                }
                checkNotNone()
            }
        })
        if (!isBind) {
            binding.etPassword.visibility = View.VISIBLE
        }
    }

    override fun initClick() {
        binding.tvGetEmailCode.setOnClickListener {
            if (!email.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$".toRegex())) {
                ToastUtils.showWarring("?????????????????????")
                return@setOnClickListener
            }
            dialog.show()
            dialog.setmMessage("????????????")
            sendEmail(email, if (isBind) "bind" else "auth", keyc)
                .subscribe(object : MySingleObserver<Result?>() {
                    override fun onSubscribe(d: Disposable) {
                        addDisposable(d)
                        disp = d
                    }

                    override fun onSuccess(result: Result) {
                        if (result.code == 106) {
                            ToastUtils.showSuccess("?????????????????????")
                            keyc = result.result.toString()
                            timeDown(60)
                        } else {
                            ToastUtils.showWarring(result.result.toString())
                        }
                        dialog.dismiss()
                    }

                    override fun onError(e: Throwable) {
                        ToastUtils.showError("????????????????????????${e.localizedMessage}")
                        dialog.dismiss()
                    }
                })
        }

        binding.btSubmit.setOnClickListener {
            if (!isBind && (password.matches("^\\d+$".toRegex()) || !password.matches("^.{8,16}$".toRegex()))) {
                DialogCreator.createTipDialog(
                    this, "??????????????????",
                    "???????????????8-16?????????\n????????????????????????"
                )
            } else if (!email.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$".toRegex())) {
                DialogCreator.createTipDialog(
                    this, "??????????????????",
                    "??????????????????????????????????????????@????????????.com(.cn???)"
                )
            } else if ("" == keyc) {
                DialogCreator.createTipDialog(this, "???????????????????????????")
            } else if (emailCode.length < 6) {
                DialogCreator.createTipDialog(this, "?????????6??????????????????")
            } else {
                dialog.show()
                dialog.setmMessage("????????????")
                if (isBind) {
                    val user = User().apply {
                        email = this@AuthEmailActivity.email
                        userName = this@AuthEmailActivity.user?.userName
                    }
                    bindEmail(user, emailCode, keyc)
                        .subscribe(this)
                } else {
                    val user = User().apply {
                        email = this@AuthEmailActivity.email
                        password = CyptoUtils.encode(APPCONST.KEY, this@AuthEmailActivity.password)
                    }
                    resetPwd(user, emailCode, keyc)
                        .subscribe(this)
                }

            }
        }
    }

    fun showTip(tip: String?) {
        binding.tvRegisterTip.visibility = View.VISIBLE
        binding.tvRegisterTip.text = tip
    }

    private fun timeDown(time: Int) {
        if (time == 0) {
            binding.tvGetEmailCode.text = getString(R.string.re_get_email_code, "")
            binding.tvGetEmailCode.isEnabled = true
        } else {
            binding.tvGetEmailCode.isEnabled = false
            val timeStr = "($time)"
            binding.tvGetEmailCode.text = getString(R.string.re_get_email_code, timeStr)
            App.getHandler().postDelayed({ timeDown(time - 1) }, 1000)
        }
    }

    fun checkNotNone() {
        binding.btSubmit.isEnabled = "" != email && "" != emailCode && (isBind || "" != password)
    }

    override fun onDestroy() {
        dialog.dismiss()
        super.onDestroy()
    }

    override fun onSubscribe(d: Disposable) {
        addDisposable(d)
        disp = d
    }

    override fun onSuccess(result: Result) {
        if (result.code < 200) {
            ToastUtils.showSuccess(result.result.toString())
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            ToastUtils.showWarring(result.result.toString())
        }
        dialog.dismiss()
    }

    override fun onError(e: Throwable) {
        ToastUtils.showError("?????????${e.localizedMessage}")
        dialog.dismiss()
    }
}