package isv.zebra.com.zebracardprinter

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import isv.zebra.com.zebracardprinter.adapter.CardAdapter
import isv.zebra.com.zebracardprinter.model.Card

class MainActivity: AppCompatActivity()
{
    lateinit var cards: ArrayList<Card>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComponents()
        // Checking if an printer is selected
        if (isPrinterSelected())
        {
            //Set banner for Printer selected
            //findViewById(R.id.banner_loading)
        }
        else
        {
            findViewById<View>(R.id.banner_loading).visibility = View.GONE
            findViewById<View>(R.id.banner_no_printer).visibility = View.VISIBLE
        }
    }

    private fun initComponents()
    {
        // Setting recuclerview w/ its cards
        cards = Card.createCardsList(10, R.drawable.image_12)
        val recyclerView = findViewById<RecyclerView>(R.id.recView_cards)
        val adapter = CardAdapter(cards)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Config button to select printer
        val btnNoPrinter = findViewById<View>(R.id.banner_no_printer)
        btnNoPrinter.setOnClickListener {
            val intent = Intent(this@MainActivity, PrinterSelecterActivity::class.java)
            intent.putExtra("key", "Kotlin")
            startActivity(intent)
//            startActivity(Intent(this,  PrinterSelecterActivity::class.java))
        }
    }

    private fun isPrinterSelected(): Boolean
    {
//        @TODO("finish checking printer process")
        return false
    }
/*
    private fun refreshSelectedPrinterView()
    {
        val printer: DiscoveredPrinter = SelectedPrinterManager.getSelectedPrinter()
        val isPrinterSelected = printer != null
        if (isPrinterSelected) {
            val address = printer.discoveryDataMap["ADDRESS"]
            printerAddress.setVisibility(if (address != null && !address.isEmpty()) View.VISIBLE else View.GONE)
            printerAddress.setText(address)
            val model = printer.discoveryDataMap["MODEL"]
            printerModel.setVisibility(if (model != null && !model.isEmpty()) View.VISIBLE else View.GONE)
            printerModel.setText(model)
            printerView.setPrinterModel(model)
            if (reconnectPrinterTask == null || reconnectPrinterTask.isBackgroundTaskFinished()) {
                if (printerStatusUpdateTask != null) {
                    printerStatusUpdateTask.cancel(true)
                }
                printerStatusUpdateTask = PrinterStatusUpdateTask(this, printer)
                printerStatusUpdateTask.setOnUpdatePrinterStatusListener(this)
                printerStatusUpdateTask.execute()
            }
        }
        printerSelectedContainer.setVisibility(if (isPrinterSelected) View.VISIBLE else View.GONE)
        noPrinterSelectedContainer.setVisibility(if (isPrinterSelected) View.GONE else View.VISIBLE)
        updateDemoButtons()
        invalidateOptionsMenu()
    }
 */
}
