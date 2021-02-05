package isv.zebra.com.zebracardprinter.zebra.discovery

import com.zebra.sdk.printer.discovery.DiscoveredPrinter

class SelectedPrinterManager {

	companion object
	{
		private var selectedPrinter: DiscoveredPrinter? = null

		fun getSelectedPrinter(): DiscoveredPrinter?
			= selectedPrinter

		fun setSelectedPrinter(printer: DiscoveredPrinter?) {
			selectedPrinter = printer
		}
	}
}
