package com.github.kr328.clash

class ApkBrokenActivity : BaseActivity() {

    override fun onBackPressed() {
        super.onBackPressed()

        finishAffinity()
        finish()
    }

    override fun shouldDisplayHomeAsUpEnabled(): Boolean {
        return false
    }
}