package isv.zebra.com.zebracardprinter.zebra.discovery

import com.zebra.sdk.comm.CardConnectionReestablisher
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.common.card.printer.ZebraCardPrinter
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory
import com.zebra.sdk.common.card.printer.ZebraPrinterZmotif
import com.zebra.sdk.common.card.printer.discovery.DiscoveredCardPrinterNetwork
import com.zebra.sdk.printer.CardPrinterReconnectionHandler
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import isv.zebra.com.zebracardprinter.zebra.util.ConnectionHelper
import kotlinx.coroutines.delay

class ReconnectPrinterTask(private val printer: DiscoveredPrinter, private val resetPrinter: Boolean = false)
{
	private val WAITTIMEPRINTERRESET: Long = 5000
	private val TIMEOUTNETWORKREESTABLISHMENT: Long = 60000

	private var onPrinterDiscoveryListener: OnReconnectPrinterListener? = null

	private var exception: java.lang.Exception? = null
	private var isBackgroundTaskFinished = false

	suspend fun printerStatusUpdate()
	{
		onPrinterDiscoveryListener?.onReconnectPrinterStarted()

		var zebraPrinterZmotif: ZebraPrinterZmotif? = null
		var connection: Connection? = null
		try {
			connection = printer.connection
			connection.open()
			zebraPrinterZmotif = ZebraCardPrinterFactory.getZmotifPrinter(connection)
			zebraPrinterZmotif.resetNetwork()
			delay(WAITTIMEPRINTERRESET)
			if (printer is DiscoveredCardPrinterNetwork)
			{
				val reconnectionHandler = ReconnectionHandler()
				val reestablisher = connection.getConnectionReestablisher(TIMEOUTNETWORKREESTABLISHMENT) as CardConnectionReestablisher
				reestablisher.reestablishConnection(reconnectionHandler)
				ConnectionHelper.cleanUpQuietly(zebraPrinterZmotif, connection)
				while (!reconnectionHandler.isPrinterOnline())
					delay(100)
				connection = reconnectionHandler.getZebraCardPrinter().connection
				connection.open()
				val discoveryDataMap: MutableMap<String, String> = printer.getDiscoveryDataMap()
				val connectionTcp: TcpConnection? = connection as TcpConnection?
				discoveryDataMap["ADDRESS"] = connectionTcp!!.address
				discoveryDataMap["PORT_NUMBER"] = connectionTcp.portNumber
				val newPrinter: DiscoveredPrinter = DiscoveredCardPrinterNetwork(discoveryDataMap)
				SelectedPrinterManager.setSelectedPrinter(newPrinter)
			}
		} catch (e: Exception) {
			exception = e
		} finally {
			ConnectionHelper.cleanUpQuietly(zebraPrinterZmotif, connection)
			isBackgroundTaskFinished = true
			onPrinterDiscoveryListener?.onReconnectPrinterFinished(exception)
		}
	}

	fun isBackgroundTaskFinished(): Boolean
			= isBackgroundTaskFinished

	fun setOnPrinterDiscoveryListener(onPrinterDiscoveryListener: OnReconnectPrinterListener?) {
		this.onPrinterDiscoveryListener = onPrinterDiscoveryListener
	}

	interface OnReconnectPrinterListener
	{
		fun onReconnectPrinterStarted()
		fun onReconnectPrinterFinished(exception: java.lang.Exception?)
	}

	class ReconnectionHandler: CardPrinterReconnectionHandler
	{
		private lateinit var zebraCardPrinter: ZebraCardPrinter
		private var isPrinterOnline: Boolean = false

		override fun printerOnline(p0: ZebraCardPrinter, p1: String?)
		{
			zebraCardPrinter = p0
			isPrinterOnline = true
		}

		// Do nothing
		override fun progressUpdate(p0: String?, p1: Int)
		{}

		fun getZebraCardPrinter(): ZebraCardPrinter
			= zebraCardPrinter

		fun isPrinterOnline(): Boolean
			= isPrinterOnline
	}
}
