package isv.zebra.com.zebracardprinter.zebra.util

class ZebraPrinterViewer {
	enum class PrinterStatus {
		ONLINE, WARNING, ERROR, REFRESHING, UNKNOWN;

		companion object {
			fun fromInteger(integer: Int): PrinterStatus {
				return when (integer) {
					0 -> ONLINE
					1 -> WARNING
					2 -> ERROR
					3 -> REFRESHING
					else -> UNKNOWN
				}
			}
		}
	}

}