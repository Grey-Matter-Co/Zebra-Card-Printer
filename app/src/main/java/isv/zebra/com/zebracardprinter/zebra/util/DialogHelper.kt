package isv.zebra.com.zebracardprinter.zebra.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import isv.zebra.com.zebracardprinter.R

object DialogHelper {
	fun showAlarmEncounteredDialog(activity: Activity, title: String?, message: String?, positiveButtonText: String?, negativeButtonText: String?, onPositiveButtonClickListener: DialogInterface.OnClickListener?, onNegativeButtonClickListener: DialogInterface.OnClickListener?)
		= activity.runOnUiThread {
			val builder = Builder(activity)
			val dialog = builder.setTitle(title)
				.setMessage(message)
				.setPositiveButton(positiveButtonText, onPositiveButtonClickListener)
				.setNegativeButton(negativeButtonText, onNegativeButtonClickListener)
				.setCancelable(false)
				.create()
			dialog.show()
		}

	fun showStorageErrorDialog(activity: Activity, onPositiveButtonClickListener: DialogInterface.OnClickListener?)
		= activity.runOnUiThread {
			val builder = Builder(activity)
			val dialog = builder.setTitle(R.string.storage_error)
				.setMessage(activity.getString(R.string.storage_error_message))
				.setPositiveButton(android.R.string.ok, onPositiveButtonClickListener)
				.setCancelable(false)
				.create()
			dialog.show()
		}

//	fun showConvertedGraphicFileInfoDialog(activity: Activity) {
//		activity.runOnUiThread {
//			val builder = Builder(activity)
//			val dialog: AlertDialog = builder.setTitle(R.string.note)
//				.setMessage(
//					activity.getString(
//						R.string.converted_graphic_file_location_message,
//						DEFAULT_DIRECTORY_CONVERTED_GRAPHIC_FILE.getAbsolutePath()
//					)
//				)
//				.setPositiveButton(android.R.string.ok,
//					DialogInterface.OnClickListener { dialogInterface, _ -> dialogInterface.dismiss() })
//				.setCancelable(false)
//				.create()
//			dialog.show()
//		}
//	}

	fun createInsertCardDialog(context: Context): AlertDialog
	= Builder(context).setTitle(R.string.insert_card)
		.setMessage(context.getString(R.string.insert_card_into_atm_slot_message))
		.setPositiveButton(android.R.string.ok)
			{ dialogInterface, _ -> dialogInterface.dismiss() }
		.setCancelable(false)
		.create()

	fun createManuallyConnectDialog(context: Context?, onPositiveButtonClickListener: DialogInterface.OnClickListener?): AlertDialog
		= Builder(context!!).setTitle(R.string.dialog_title_manually_connect)
			.setView(R.layout.dialog_manually_connect)
			.setPositiveButton(R.string.connect, onPositiveButtonClickListener)
			.setNegativeButton(android.R.string.cancel)
				{ dialog, _ -> dialog.dismiss() }
			.create()

	fun createDisconnectDialog(context: Context?, onPositiveButtonClickListener: DialogInterface.OnClickListener?): AlertDialog
		= Builder(context!!).setTitle(R.string.dialog_title_disconnect_printer)
			.setMessage(R.string.dialog_message_disconnect_printer)
			.setPositiveButton(R.string.disconnect, onPositiveButtonClickListener)
			.setNegativeButton(android.R.string.cancel)
				{ dialog, _ -> dialog.dismiss() }
			.create()

//	fun createContinuePrinterResetDialog(
//		context: Context,
//		resetRequiredSettings: List<String?>?,
//		resetPrinter: Boolean,
//		onPositiveButtonClickListener: DialogInterface.OnClickListener?,
//		onNegativeButtonClickListener: DialogInterface.OnClickListener?
//	): Builder {
//		return Builder(context).setTitle(if (resetPrinter) R.string.printer_reset_required else R.string.network_reset_required)
//			.setMessage(
//				context.getString(
//					if (resetPrinter) R.string.continue_printer_reset_message else R.string.continue_network_reset_message,
//					TextUtils.join(
//						context.getString(R.string.list_delimiter),
//						resetRequiredSettings!!
//					)
//				)
//			)
//			.setPositiveButton(android.R.string.ok, onPositiveButtonClickListener)
//			.setNegativeButton(android.R.string.cancel, onNegativeButtonClickListener)
//	}

//	fun createPrinterResetDialog(
//		context: Context,
//		resetRequiredSettings: List<String?>?,
//		resetPrinter: Boolean,
//		onPositiveButtonClickListener: DialogInterface.OnClickListener?
//	): Builder {
//		return Builder(context).setTitle(if (resetPrinter) R.string.printer_reset_required else R.string.network_reset_required)
//			.setMessage(
//				context.getString(
//					if (resetPrinter) R.string.printer_reset_required_message else R.string.network_reset_required_message,
//					TextUtils.join(
//						context.getString(R.string.list_delimiter),
//						resetRequiredSettings!!
//					),
//					context.getString(R.string.reset)
//				)
//			)
//			.setPositiveButton(R.string.reset, onPositiveButtonClickListener)
//			.setCancelable(false)
//	}

	fun showErrorDialog(activity: Activity, message: String?)
		= showErrorDialog(activity, activity.getString(R.string.error), message)

	private fun showErrorDialog(activity: Activity, title: String?, message: String?)
		= activity.runOnUiThread {
			val builder = Builder(activity)
			val dialog = builder.setTitle(title)
				.setMessage(message)
				.setPositiveButton( android.R.string.ok )
					{dialog, _ -> dialog.dismiss()}
				.setCancelable(false)
				.create()
			dialog.show()
		}
}
