package isv.zebra.com.zebracardprinter.zebra.util

import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.common.card.exceptions.ZebraCardException
import com.zebra.sdk.common.card.printer.ZebraCardPrinter

class ConnectionHelper
{
	companion object
	{
		fun cleanUpQuietly(zebraCardPrinter: ZebraCardPrinter?, connection: Connection?)
		{
			try {
				zebraCardPrinter?.destroy()
			} catch (e: ZebraCardException) {
				// Do nothing
			}

			try {
				connection?.close()
			} catch (e: ConnectionException) {
				// Do nothing
			}
		}
	}
}
