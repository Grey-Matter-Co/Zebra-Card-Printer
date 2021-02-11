package isv.zebra.com.zebracardprinter.zebra.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import isv.zebra.com.zebracardprinter.R

object StorageHelper {
	const val KEY_STORAGE_PERMISSIONS_RESULT = "KEY_STORAGE_PERMISSIONS_RESULT"
	const val PERMISSION_DENIED = 0
	const val PERMISSION_NEVER_ASK_AGAIN_SET = 1
	val isExternalStorageWritable: Boolean
		get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

	fun requestStoragePermissionIfNotGranted(activity: Activity?, requestCode: Int) {
		if (ContextCompat.checkSelfPermission(
				activity!!,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			) !== PackageManager.PERMISSION_GRANTED
		) {
			ActivityCompat.requestPermissions(
				activity!!,
				arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
				requestCode
			)
		}
	}

	fun handleRequestStoragePermissionsResult(
		activity: Activity,
		@NonNull permission: String,
		grantResult: Int
	) {
		if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) {
				UIHelper.showSnackbar(
					activity,
					activity.getString(R.string.storage_permissions_granted)
				)
			} else if (Build.VERSION.SDK_INT >= 23 && !activity.shouldShowRequestPermissionRationale(
					permission
				)
			) {
				val intent = Intent()
				intent.putExtra(KEY_STORAGE_PERMISSIONS_RESULT, PERMISSION_NEVER_ASK_AGAIN_SET)
				activity.setResult(Activity.RESULT_CANCELED, intent)
				activity.finish()
			} else {
				val intent = Intent()
				intent.putExtra(KEY_STORAGE_PERMISSIONS_RESULT, PERMISSION_DENIED)
				activity.setResult(Activity.RESULT_CANCELED, intent)
				activity.finish()
			}
		}
	}
}
