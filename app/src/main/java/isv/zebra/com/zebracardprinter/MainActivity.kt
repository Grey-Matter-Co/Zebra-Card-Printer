package isv.zebra.com.zebracardprinter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb
import com.zebra.sdk.printer.discovery.UsbDiscoverer
import isv.zebra.com.zebracardprinter.adapter.CardAdapter
import isv.zebra.com.zebracardprinter.model.Card
import isv.zebra.com.zebracardprinter.zebra.discovery.PrinterStatusUpdateTask
import isv.zebra.com.zebracardprinter.zebra.discovery.PrinterStatusUpdateTask.OnUpdatePrinterStatusListener
import isv.zebra.com.zebracardprinter.zebra.discovery.ReconnectPrinterTask
import isv.zebra.com.zebracardprinter.zebra.discovery.ReconnectPrinterTask.OnReconnectPrinterListener
import isv.zebra.com.zebracardprinter.zebra.discovery.SelectedPrinterManager
import isv.zebra.com.zebracardprinter.zebra.util.DialogHelper
import isv.zebra.com.zebracardprinter.zebra.util.ProgressOverlayHelper
import isv.zebra.com.zebracardprinter.zebra.util.UsbHelper
import isv.zebra.com.zebracardprinter.zebra.util.ZebraPrinterViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity: AppCompatActivity(), CoroutineScope, OnUpdatePrinterStatusListener, OnReconnectPrinterListener
{
	private lateinit var cards: ArrayList<Card>

	private lateinit var bannerNoPrinter        : ConstraintLayout
	private lateinit var bannerPrinterSelected  : ConstraintLayout
	private lateinit var bannerLoading          : LinearLayout
	private lateinit var progressMessage: TextView
	
	//For Coroutines
	private lateinit var job: Job
	override val coroutineContext: CoroutineContext
		get() = job+Dispatchers.Main

	// For Zebra Card Printer
	private var printerSelected: DiscoveredPrinter? = null
	private val reconnectPrinterTask: ReconnectPrinterTask? = null
	private var printerStatusUpdateTask: PrinterStatusUpdateTask? = null

	private val usbDisconnectReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
				val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
				val printer = SelectedPrinterManager.getSelectedPrinter()
				if (printer is DiscoveredPrinterUsb
						&& UsbDiscoverer.isZebraUsbDevice(device)
						&& printer.device == device) {
					SelectedPrinterManager.setSelectedPrinter(null)
					refreshSelectedPrinterBanner()
				}
			}
		}
	}
	private val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent)
		{
			if (UsbHelper.ACTION_USB_PERMISSION_GRANTED == intent.action)
				synchronized(this)
				{
					val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
					val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
					if (device != null && UsbDiscoverer.isZebraUsbDevice(device))
						if (permissionGranted)
							SelectedPrinterManager.setSelectedPrinter(DiscoveredPrinterUsb(device.deviceName, UsbHelper.getUsbManager(this@MainActivity), device))
						else
							DialogHelper.showErrorDialog(this@MainActivity, getString(R.string.usb_permissions_denied_message))
					ProgressOverlayHelper.hideProgressOverlay(progressMessage, bannerLoading)
					refreshSelectedPrinterBanner()
				}
		}
	}
	private val usbDeviceAttachedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
				val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
				if (device != null && UsbDiscoverer.isZebraUsbDevice(device)) {
					SelectedPrinterManager.setSelectedPrinter(null)
					val usbManager: UsbManager = UsbHelper.getUsbManager(this@MainActivity)
					if (!usbManager.hasPermission(device)) {
						ProgressOverlayHelper.showProgressOverlay(progressMessage, bannerLoading, getString(R.string.requesting_usb_permission))
						UsbHelper.requestUsbPermission(this@MainActivity, usbManager, device)
					} else {
						SelectedPrinterManager.setSelectedPrinter(DiscoveredPrinterUsb(device.deviceName, UsbHelper.getUsbManager(this@MainActivity), device))
						refreshSelectedPrinterBanner()
					}
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		// Request permissions for ¿@TODO("Figure out which permissions are requesting")?
		registerReceivers()

		initComponents()
		job = Job()

		// Update banner to show
		refreshSelectedPrinterBanner()
	}

	// Both for request permissions
	override fun onPause() { super.onPause(); unregisterReceivers() }
	override fun onResume() { super.onResume(); registerReceivers() }

	// Just for cancel any coroutine in process
	override fun onDestroy() { super.onDestroy(); job.cancel() }

	private fun initComponents()
	{
		// Setting banners
		bannerLoading         = findViewById(R.id.banner_loading)
		bannerNoPrinter       = findViewById(R.id.banner_no_printer)
		bannerPrinterSelected = findViewById(R.id.banner_printer_selected)
		progressMessage		  = findViewById(R.id.msg_progress)

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

	private fun refreshSelectedPrinterBanner()
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
            if (reconnectPrinterTask == null || reconnectPrinterTask.isBackgroundTaskFinished())
            {
				job.cancel()
				printerStatusUpdateTask = PrinterStatusUpdateTask(this@MainActivity, printer)
                printerStatusUpdateTask!!.setOnUpdatePrinterStatusListener(this)
				launch{ printerStatusUpdateTask?.printerStatusUpdate() }
            }
		}

		bannerPrinterSelected.visibility = if (isPrinterSelected) View.VISIBLE else View.GONE
		bannerNoPrinter.visibility = if (isPrinterSelected) View.GONE else View.VISIBLE
		bannerLoading.visibility = View.GONE
		//updateDemoButtons()
		//invalidateOptionsMenu()
	}

	override fun onUpdatePrinterStatusStarted() {
		TODO("Solve how to implement ZebraPrinterView class")
//		printerView.setPrinterStatus(ZebraPrinterViewer.PrinterStatus.REFRESHING)
	}

	override fun onUpdatePrinterStatusFinished(exception: Exception?, printerStatus: ZebraPrinterViewer.PrinterStatus?)
	{
		if (exception != null)
		{
			DialogHelper.showErrorDialog(this, getString(R.string.error_updating_printer_status_message, exception.message))
			SelectedPrinterManager.setSelectedPrinter(null)
			refreshSelectedPrinterBanner()
		}
		else
		{
			TODO("Solve how to implement ZebraPrinterView class x2")
			//printerView.setPrinterStatus(printerStatus)
			//updateDemoButtons()
		}
	}

	override fun onReconnectPrinterStarted() {
		refreshSelectedPrinterBanner()
		ProgressOverlayHelper.showProgressOverlay(progressMessage, bannerLoading, getString(R.string.reconnecting_to_printer))
	}

	override fun onReconnectPrinterFinished(exception: Exception?) {
		ProgressOverlayHelper.hideProgressOverlay(progressMessage, bannerLoading)
		if (exception != null)
		{
			DialogHelper.showErrorDialog(this, getString(R.string.error_reconnecting_to_printer_message, exception.message))
			SelectedPrinterManager.setSelectedPrinter(null)
		}

		refreshSelectedPrinterBanner()
	}

	private fun registerReceivers() {
		var filter = IntentFilter()
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		registerReceiver(usbDisconnectReceiver, filter)
		filter = IntentFilter()
		filter.addAction(UsbHelper.ACTION_USB_PERMISSION_GRANTED)
		registerReceiver(usbPermissionReceiver, filter)
		filter = IntentFilter()
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
		registerReceiver(usbDeviceAttachedReceiver, filter)
	}

	private fun unregisterReceivers() {
		unregisterReceiver(usbDisconnectReceiver)
		unregisterReceiver(usbPermissionReceiver)
		unregisterReceiver(usbDeviceAttachedReceiver)
	}
}
