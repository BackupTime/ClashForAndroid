package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.charleskorn.kaml.YamlException
import com.github.kr328.clash.adapter.FormAdapter
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.utils.FileUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_import_file.*
import java.io.FileOutputStream
import kotlin.concurrent.thread

class ImportFileActivity : BaseActivity() {
    private val elements: List<FormAdapter.Type> = listOf(
        FormAdapter.TextType(
            R.drawable.ic_about,
            R.string.clash_profile_name,
            R.string.clash_profile_name_hint
        ),
        FormAdapter.FilePickerType(
            R.drawable.ic_new_profile_file,
            R.string.clash_profile_file,
            R.string.clash_profile_file_hint
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_file)

        setSupportActionBar(activity_import_file_toolbar)

        activity_import_file_form.also {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = FormAdapter(this, elements)
        }

        activity_import_file_save.setOnClickListener {
            activity_import_file_save.visibility = View.GONE
            activity_import_file_saving.visibility = View.VISIBLE

            checkAndInsert()
        }

        setResult(Activity.RESULT_CANCELED)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && (activity_import_file_form.adapter as FormAdapter).onActivityResult(
                requestCode,
                resultCode,
                data
            )
        )
            return

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkAndInsert() {

        val name = elements[0] as FormAdapter.TextType
        val file = elements[1] as FormAdapter.FilePickerType

        if (name.content.isEmpty()) {
            activity_import_file_save.visibility = View.VISIBLE
            activity_import_file_saving.visibility = View.GONE
            Snackbar.make(
                activity_import_file_root,
                R.string.clash_import_file_empty_name,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        if (file.content == null || file.content == Uri.EMPTY) {
            activity_import_file_save.visibility = View.VISIBLE
            activity_import_file_saving.visibility = View.GONE
            Snackbar.make(
                activity_import_file_root,
                R.string.clash_import_file_empty_path,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val data =
            contentResolver.openInputStream(file.content!!)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: throw NullPointerException("Unable to open config file")


        runClash {
            try {
                val pipe = ParcelFileDescriptor.createPipe()

                thread {
                    FileOutputStream(pipe[1].fileDescriptor).use {
                        it.write(data.toByteArray())
                    }

                    pipe[0].close()
                    pipe[1].close()
                }

                val error = it.checkProfileValid(pipe[0])

                if (error != null)
                    throw Exception(error)

                val cache =
                    FileUtils.generateRandomFile(filesDir.resolve(Constants.PROFILES_DIR), ".yaml")

                FileOutputStream(cache).use {
                    it.write(data.toByteArray())
                }

                it.profileService.addProfile(
                    ClashProfileEntity(
                        name = name.content,
                        token = ClashProfileEntity.fileToken(file.content!!.toString()),
                        file = cache.absolutePath,
                        active = false,
                        lastUpdate = System.currentTimeMillis()
                    )
                )

                runOnUiThread {
                    setResult(Activity.RESULT_OK)

                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Snackbar.make(
                        activity_import_file_root,
                        getString(
                            R.string.clash_profile_invalid,
                            e.message?.replace(YamlException::class.java.name + ":", "")
                                ?: "Unknown"
                        ),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}