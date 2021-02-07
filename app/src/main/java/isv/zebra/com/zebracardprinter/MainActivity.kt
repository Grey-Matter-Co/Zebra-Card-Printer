package isv.zebra.com.zebracardprinter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb
import com.zebra.sdk.printer.discovery.UsbDiscoverer
import com.zebra.zebraui.ZebraPrinterView
import isv.zebra.com.zebracardprinter.activity.DiscoverPrintersActivity
import isv.zebra.com.zebracardprinter.activity.FieldsCaptureActivity
import isv.zebra.com.zebracardprinter.adapter.ZCardAdapter
import isv.zebra.com.zebracardprinter.adapter.ZCardAdapter.OnZCardListener
import isv.zebra.com.zebracardprinter.model.ZCard
import isv.zebra.com.zebracardprinter.zebra.discovery.PrinterStatusUpdateTask
import isv.zebra.com.zebracardprinter.zebra.discovery.PrinterStatusUpdateTask.OnUpdatePrinterStatusListener
import isv.zebra.com.zebracardprinter.zebra.discovery.ReconnectPrinterTask
import isv.zebra.com.zebracardprinter.zebra.discovery.ReconnectPrinterTask.OnReconnectPrinterListener
import isv.zebra.com.zebracardprinter.zebra.discovery.SelectedPrinterManager
import isv.zebra.com.zebracardprinter.zebra.util.DialogHelper
import isv.zebra.com.zebracardprinter.zebra.util.ProgressOverlayHelper
import isv.zebra.com.zebracardprinter.zebra.util.UsbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity: AppCompatActivity(), CoroutineScope, OnUpdatePrinterStatusListener, OnReconnectPrinterListener, OnZCardListener
{
	private lateinit var zCards: ArrayList<ZCard>

	private lateinit var bannNoPrinterSel	: ConstraintLayout
	private lateinit var bannPrinterSel		: ConstraintLayout
	private lateinit var bannProg			: ConstraintLayout
	private lateinit var progMsg			: TextView
	private lateinit var printerIcon		: ZebraPrinterView
	
	//For Coroutines
	private lateinit var job: Job
	override val coroutineContext: CoroutineContext
		get() = job+Dispatchers.Main

	// For Zebra Card Printer
	private var printerSelected         : DiscoveredPrinter? = null
	private val reconnectPrinterTask    : ReconnectPrinterTask? = null
	private var printerStatusUpdateTask : PrinterStatusUpdateTask? = null

	private val usbDisconnectReceiver: BroadcastReceiver = object : BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action)
			{
				val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
				val printer = SelectedPrinterManager.getSelectedPrinter()
				if (printer is DiscoveredPrinterUsb && UsbDiscoverer.isZebraUsbDevice(device) && printer.device == device)
				{
					SelectedPrinterManager.setSelectedPrinter(null)
					refreshSelectedPrinterBanner()
				}
			}
		}
	}
	private val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			if (UsbHelper.ACTION_USB_PERMISSION_GRANTED == intent.action)
				synchronized(this)
				{
					val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
					val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
					if (device != null && UsbDiscoverer.isZebraUsbDevice(device))
						if (permissionGranted)
							SelectedPrinterManager
								.setSelectedPrinter(DiscoveredPrinterUsb(device.deviceName, UsbHelper.getUsbManager(this@MainActivity), device))
						else
							DialogHelper.showErrorDialog(this@MainActivity, getString(R.string.msg_warning_usb_permissions_denied))
					ProgressOverlayHelper.hideProgressOverlay(progMsg, bannProg)
					refreshSelectedPrinterBanner()
				}
		}
	}
	private val usbDeviceAttachedReceiver: BroadcastReceiver = object : BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action)
			{
				val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
				if (device != null && UsbDiscoverer.isZebraUsbDevice(device))
				{
					SelectedPrinterManager.setSelectedPrinter(null)
					val usbManager: UsbManager = UsbHelper.getUsbManager(this@MainActivity)
					if (!usbManager.hasPermission(device))
					{
						ProgressOverlayHelper.showProgressOverlay(progMsg, bannProg, getString(R.string.msg_waiting_requesting_usb_permission))
						UsbHelper.requestUsbPermission(this@MainActivity, usbManager, device)
					}
					else
					{
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
		// Setting banners and its elements
		bannProg         = findViewById(R.id.bann_prog)
		bannNoPrinterSel = findViewById(R.id.bann_no_printer_sel)
		bannPrinterSel   = findViewById(R.id.bann_printer_sel)
		progMsg          = findViewById(R.id.bann_prog_txt)
		printerIcon      = findViewById(R.id.bann_printer_sel_icon)

		// Setting recuclerview w/ its cards
		zCards = ZCard.createCardsList(10, R.drawable.card_mamalona)
		val recyclerView = findViewById<RecyclerView>(R.id.recView_zcards)
		recyclerView.adapter = ZCardAdapter(zCards, this)
		recyclerView.layoutManager = LinearLayoutManager(this)

		// Config button to select printer
		bannNoPrinterSel.setOnClickListener {
			startActivity(Intent(this@MainActivity, DiscoverPrintersActivity::class.java)
					.putExtra("key", "Kotlin"))
		}
	}

	private fun refreshSelectedPrinterBanner()
	{
		val printer: DiscoveredPrinter? = printerSelected
		val isPrinterSelected = printer!=null
		if (isPrinterSelected)
		{
			val printerAddress 	: TextView 	= findViewById(R.id.bann_printer_sel_addR)
			val printerModel 	: TextView = findViewById(R.id.bann_printer_sel_model)
			//val printerIcon		: ZebraPrinterView = findViewById(R.id.bann_printer_sel_icon)

			val address = printer!!.discoveryDataMap["ADDRESS"]
			val model   = printer.discoveryDataMap["MODEL"]

			printerAddress.visibility = if (address != null && address.isNotEmpty()) View.VISIBLE else View.GONE
			printerAddress.text = address
			printerModel.visibility = if (model != null && model.isNotEmpty()) View.VISIBLE else View.GONE
			printerModel.text = model
			printerIcon.setPrinterModel(model)
            if (reconnectPrinterTask == null || reconnectPrinterTask.isBackgroundTaskFinished())
            {
				job.cancel()
				printerStatusUpdateTask = PrinterStatusUpdateTask(this@MainActivity, printer)
                printerStatusUpdateTask!!.setOnUpdatePrinterStatusListener(this)
				launch{ printerStatusUpdateTask?.printerStatusUpdate() }
            }
		}

		bannPrinterSel.visibility = if (isPrinterSelected) View.VISIBLE else View.GONE
		bannNoPrinterSel.visibility = if (isPrinterSelected) View.GONE else View.VISIBLE
		bannProg.visibility = View.GONE
		//updateDemoButtons()
		//invalidateOptionsMenu()
	}

	override fun onUpdatePrinterStatusStarted()
	{
		printerIcon.printerStatus = ZebraPrinterView.PrinterStatus.REFRESHING
	}

	override fun onUpdatePrinterStatusFinished(exception: java.lang.Exception?, printerStatus: ZebraPrinterView.PrinterStatus?)
	{
		if (exception != null)
		{
			DialogHelper.showErrorDialog(this, getString(R.string.msg_error_updating_printer_status, exception.message))
			SelectedPrinterManager.setSelectedPrinter(null)
			refreshSelectedPrinterBanner()
		}
		else
		{
			printerIcon.printerStatus = printerStatus
//			updateDemoButtons()
		}
	}

	override fun onReconnectPrinterStarted() {
		refreshSelectedPrinterBanner()
		ProgressOverlayHelper.showProgressOverlay(progMsg, bannProg, getString(R.string.msg_waiting_reconnecting_to_printer))
	}

	override fun onReconnectPrinterFinished(exception: Exception?) {
		ProgressOverlayHelper.hideProgressOverlay(progMsg, bannProg)
		if (exception != null)
		{
			DialogHelper.showErrorDialog(this, getString(R.string.msg_error_reconnecting_to_printer, exception.message))
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

	override fun onCardClick(position: Int)
	{
		startActivity(Intent(this@MainActivity, FieldsCaptureActivity::class.java)
			.putExtra("zcardSelected", -1))
	}

	private fun unregisterReceivers() {
		unregisterReceiver(usbDisconnectReceiver)
		unregisterReceiver(usbPermissionReceiver)
		unregisterReceiver(usbDeviceAttachedReceiver)
	}
}
