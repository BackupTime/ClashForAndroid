package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.github.kr328.clash.view.FatItem
import kotlinx.android.synthetic.main.activity_new_profile.*

class CreateProfileActivity : BaseActivity() {
    companion object {
        val NEW_PROFILE_SOURCE = listOf(
            AdapterData(
                R.drawable.ic_new_profile_file,
                R.string.clash_new_profile_file_title,
                R.string.clash_new_profile_file_summary
            ),
            AdapterData(
                R.drawable.ic_new_profile_url,
                R.string.clash_new_profile_url_title,
                R.string.clash_new_profile_url_summary
            )
        )
        private const val IMPORT_REQUEST_CODE = 1024
    }

    class Adapter(private val context: Context) : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return ((convertView ?: FatItem(context)) as FatItem).apply {
                val current = NEW_PROFILE_SOURCE[position]

                isClickable = false

                icon = context.getDrawable(current.icon)
                title = context.getString(current.title)
                summary = context.getString(current.summary)
            }
        }

        override fun getItem(position: Int): Any {
            return NEW_PROFILE_SOURCE[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return NEW_PROFILE_SOURCE.size
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ( requestCode == IMPORT_REQUEST_CODE && resultCode == Activity.RESULT_OK ) {
            finish()
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    data class AdapterData(val icon: Int, val title: Int, val summary: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_profile)

        setSupportActionBar(activity_new_profile_toolbar)

        with(activity_new_profile_list) {
            adapter = Adapter(this@CreateProfileActivity)
            setOnItemClickListener { _, _, index, _ ->
                when (index) {
                    0 -> {
                        startActivityForResult(
                            Intent(
                                this@CreateProfileActivity,
                                ImportFileActivity::class.java
                            ), IMPORT_REQUEST_CODE
                        )
                    }
                    1 -> {
                        startActivityForResult(
                            Intent(
                                this@CreateProfileActivity,
                                ImportUrlActivity::class.java
                            ), IMPORT_REQUEST_CODE
                        )
                    }
                }
            }
        }
    }
}