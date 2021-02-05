package isv.zebra.com.zebracardprinter.zebra.discovery

import android.content.Context
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.common.card.printer.ZebraCardPrinter
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.zebra.util.ConnectionHelper
import isv.zebra.com.zebracardprinter.zebra.util.ZebraPrinterViewer
import java.lang.ref.WeakReference

class PrinterStatusUpdateTask(context: Context, private val printer: DiscoveredPrinter)
{
	private var weakContext = WeakReference(context)
	private var onUpdatePrinterStatusListener: OnUpdatePrinterStatusListener? = null
	private var exception: java.lang.Exception? = null

	suspend fun printerStatusUpdate()
	{
		onUpdatePrinterStatusListener?.onUpdatePrinterStatusStarted()

		var connection: Connection? = null
		var zebraCardPrinter: ZebraCardPrinter? = null
		var zebraViewerStatus :ZebraPrinterViewer.PrinterStatus? = ZebraPrinterViewer.PrinterStatus.UNKNOWN

		try
		{
			connection = printer.connection
			connection.open()
			zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection)
			val printerStatus = zebraCardPrinter.printerStatus
			zebraViewerStatus = if (printerStatus != null)
									if (printerStatus.errorInfo.value != 0 || printerStatus.alarmInfo.value != 0)
										ZebraPrinterViewer.PrinterStatus.ERROR
									else
										ZebraPrinterViewer.PrinterStatus.ONLINE
								else
									ZebraPrinterViewer.PrinterStatus.ERROR
		} catch (e: ConnectionException) {
			exception = ConnectionException(weakContext.get()!!.getString(R.string.unable_to_communicate_with_printer_message))
			zebraViewerStatus = ZebraPrinterViewer.PrinterStatus.ERROR
		} catch (e: Exception) {
			exception = e
			zebraViewerStatus = ZebraPrinterViewer.PrinterStatus.ERROR
		} finally {
			ConnectionHelper.cleanUpQuietly(zebraCardPrinter, connection)
			onUpdatePrinterStatusListener?.onUpdatePrinterStatusFinished(exception, zebraViewerStatus)
		}

	}

	fun setOnUpdatePrinterStatusListener(onUpdatePrinterStatusListener: OnUpdatePrinterStatusListener?) {
		this.onUpdatePrinterStatusListener = onUpdatePrinterStatusListener
	}

	interface OnUpdatePrinterStatusListener
	{
		fun onUpdatePrinterStatusStarted()
		fun onUpdatePrinterStatusFinished(exception: java.lang.Exception?, printerStatus: ZebraPrinterViewer.PrinterStatus?)
	}
}
