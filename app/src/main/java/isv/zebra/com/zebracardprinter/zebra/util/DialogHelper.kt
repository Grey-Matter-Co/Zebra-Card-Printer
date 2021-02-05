package isv.zebra.com.zebracardprinter.zebra.util

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import isv.zebra.com.zebracardprinter.R

class DialogHelper {
	companion object
	{
		fun showErrorDialog(activity: Activity, message: String?)
			= showErrorDialog(activity, activity.getString(R.string.error), message)
		private fun showErrorDialog(activity: Activity, title: String?, message: String?) {
			activity.runOnUiThread {
				val builder = Builder(activity)
				val dialog: AlertDialog = builder.setTitle(title)
						.setMessage(message)
						.setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
						.setCancelable(false)
						.create()
				dialog.show()
			}
		}
	}
}
