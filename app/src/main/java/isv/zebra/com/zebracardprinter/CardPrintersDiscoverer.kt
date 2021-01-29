package isv.zebra.com.zebracardprinter

import com.zebra.sdk.common.card.printer.discovery.NetworkCardDiscoverer
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveryException
import com.zebra.sdk.printer.discovery.DiscoveryHandler


object NetworkCardDiscovererExample
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        val discoveryHandler: DiscoveryHandler = object : DiscoveryHandler
        {
            var printers: MutableList<DiscoveredPrinter> = ArrayList()
            override fun foundPrinter(printer: DiscoveredPrinter)
            {
                printers.add(printer)
            }

            override fun discoveryFinished()
            {
                for (printer in printers)
                    println(printer)
                println("Discovered " + printers.size + " printers.")
            }

            override fun discoveryError(message: String)
            {
                println("An error occurred during discovery : $message")
            }
        }
        try
        {
            println("Starting printer discovery.")
            NetworkCardDiscoverer.findPrinters(discoveryHandler)
        }
        catch (e: DiscoveryException)
        {
            e.printStackTrace()
        }
    }
}