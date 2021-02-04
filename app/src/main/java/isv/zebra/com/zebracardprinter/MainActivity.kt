package isv.zebra.com.zebracardprinter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import isv.zebra.com.zebracardprinter.adapter.CardAdapter
import isv.zebra.com.zebracardprinter.model.Card

class MainActivity: AppCompatActivity()
{
	private var printerSelected: DiscoveredPrinter? = null
	private lateinit var cards: ArrayList<Card>

	private lateinit var bannerLoading          : View
	private lateinit var bannerNoPrinter        : View
	private lateinit var bannerPrinterSelected  : View


	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		initComponents()
		refreshSelectedPrinterView()
	}

	private fun initComponents()
	{
		// Setting banners
		bannerLoading         = findViewById<View>(R.id.banner_loading)
		bannerNoPrinter       = findViewById<View>(R.id.banner_no_printer)
		bannerPrinterSelected = findViewById<View>(R.id.banner_printer_selected)

		// Setting recuclerview w/ its cards
		cards = Card.createCardsList(10, R.drawable.image_12)
		val recyclerView = findViewById<RecyclerView>(R.id.recView_cards)
		recyclerView.adapter = CardAdapter(cards)
		recyclerView.layoutManager = LinearLayoutManager(this)

		// Config button to select printer
		val btnNoPrinter = findViewById<View>(R.id.banner_no_printer)
		btnNoPrinter.setOnClickListener {
			startActivity(Intent(this@MainActivity, PrinterSelecterActivity::class.java)
					.putExtra("key", "Kotlin"))
		}
	}

	private fun isPrinterSelected(): Boolean
	{
//        @TODO("finish checking printer process")
		return false
	}
	private fun refreshSelectedPrinterView()
	{
		val printer: DiscoveredPrinter? = printerSelected
		val isPrinterSelected = printer!=null
		if (isPrinterSelected)
		{
			val printerAddress : TextView = findViewById(R.id.banner_printer_address)
			val printerModel : TextView = findViewById(R.id.banner_printer_model)
			val printerView : ImageView = findViewById(R.id.banner_printer_icon)

			val address = printer!!.discoveryDataMap["ADDRESS"]
			printerAddress.visibility = if (address != null && address.isNotEmpty()) View.VISIBLE else View.GONE
			printerAddress.text = address
			val model = printer.discoveryDataMap["MODEL"]
			printerModel.visibility = if (model != null && model.isNotEmpty()) View.VISIBLE else View.GONE
			printerModel.text = model
			//printerView.setPrinterModel(model)
			// @TODO("Crear clase  reconnectPrinterTask, printerStatusUpdateTask")
//            if (reconnectPrinterTask == null || reconnectPrinterTask.isBackgroundTaskFinished())
//            {
//                if (printerStatusUpdateTask != null)
//                    printerStatusUpdateTask.cancel(true)
//                printerStatusUpdateTask = PrinterStatusUpdateTask(this, printer)
//                printerStatusUpdateTask.setOnUpdatePrinterStatusListener(this)
//                printerStatusUpdateTask.execute()
//            }
		}

		bannerPrinterSelected.visibility = if (isPrinterSelected) View.VISIBLE else View.GONE
		bannerNoPrinter.visibility = if (isPrinterSelected) View.GONE else View.VISIBLE
		bannerLoading.visibility = View.GONE
		//updateDemoButtons()
		//invalidateOptionsMenu()
	}
}
