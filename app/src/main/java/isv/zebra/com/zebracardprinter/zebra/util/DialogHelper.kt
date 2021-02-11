package isv.zebra.com.zebracardprinter.zebra.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import isv.zebra.com.zebracardprinter.R

class DialogHelper
{
	companion object
	{
		fun showErrorDialog(activity: Activity, message: String?)
			= showErrorDialog(activity, activity.getString(R.string.error), message)
		private fun showErrorDialog(activity: Activity, title: String?, message: String?)
		{
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

		fun createManuallyConnectDialog(
			context: Context?,
			onPositiveButtonClickListener: DialogInterface.OnClickListener?
		): AlertDialog
		{
			return Builder(context!!).setTitle(R.string.dialog_title_manually_connect)
				.setView(R.layout.dialog_manually_connect)
				.setPositiveButton(R.string.connect, onPositiveButtonClickListener)
				.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
				.create()
		}

		fun createDisconnectDialog(
			context: Context?,
			onPositiveButtonClickListener: DialogInterface.OnClickListener?
		): AlertDialog? {
			return Builder(context!!).setTitle(R.string.dialog_title_disconnect_printer)
				.setMessage(R.string.dialog_message_disconnect_printer)
				.setPositiveButton(R.string.disconnect, onPositiveButtonClickListener)
				.setNegativeButton(android.R.string.cancel,
					DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
				.create()
		}
	}
}
