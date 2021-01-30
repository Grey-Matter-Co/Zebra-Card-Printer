package isv.zebra.com.zebracardprinter

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.zebra.sdk.common.card.printer.discovery.NetworkCardDiscoverer
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveryException
import com.zebra.sdk.printer.discovery.DiscoveryHandler

class DataCaptureActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datacapture)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            findPrinters(view)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId)
        {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun findPrinters(view: View)
    {
        val discoveryHandler: DiscoveryHandler = object : DiscoveryHandler
        {
            val printers: MutableList<DiscoveredPrinter> = mutableListOf()
            override fun foundPrinter(printer: DiscoveredPrinter)
            {
                printers.add(printer)
            }

            override fun discoveryFinished()
            {
                for (printer in printers)
                {
                    Snackbar.make(view, printer.toString(), Snackbar.LENGTH_LONG).setAction("Action", null).show()
                    println(printer)
                }
                println("Discovered " + printers.size + " printers.")
                Snackbar.make(view, "Discovered "+printers.size+" printers.", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            }

            override fun discoveryError(message: String)
            {
                println("An error occurred during discovery : $message")
                Snackbar.make(view, "An error occurred during discovery : $message", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            }
        }
        try
        {
            println("Starting printer discovery.")
            Snackbar.make(view, "Starting printer discovery.", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            NetworkCardDiscoverer.findPrinters(discoveryHandler)
        }
        catch (e: DiscoveryException)
        {
            e.printStackTrace()
        }
    }
}