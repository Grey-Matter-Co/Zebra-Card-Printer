package isv.zebra.com.zebracardprinter

import android.content.*
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
import isv.zebra.com.zebracardprinter.activity.Send2PrintActivity
import isv.zebra.com.zebracardprinter.adapter.ZCardAdapter
import isv.zebra.com.zebracardprinter.model.ZCard
import isv.zebra.com.zebracardprinter.zebra.discovery.PrinterStatusUpdateTask
import isv.zebra.com.zebracardprinter.zebra.discovery.PrinterStatusUpdateTask.OnUpdatePrinterStatusListener
import isv.zebra.com.zebracardprinter.zebra.discovery.ReconnectPrinterTask
import isv.zebra.com.zebracardprinter.zebra.discovery.ReconnectPrinterTask.OnReconnectPrinterListener
import isv.zebra.com.zebracardprinter.zebra.discovery.SelectedPrinterManager
import isv.zebra.com.zebracardprinter.zebra.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@InternalCoroutinesApi
class MainActivity: AppCompatActivity(), CoroutineScope
{
	companion object {
		private const val REQUEST_START_ACTIVITY = 3001
	}
	private lateinit var zCards: ArrayList<ZCard>

	//For Coroutines
	private var jobPrinterStatusUpdate : Job = Job()
	private var jobReconnectPrinter : Job = Job()
	override val coroutineContext: CoroutineContext = Job()+Dispatchers.IO
	private var reconnectPrinterTask    : ReconnectPrinterTask? = null
	private var printerStatusUpdateTask : PrinterStatusUpdateTask? = null

	private lateinit var bannerNoPrnSel	: ConstraintLayout
	private lateinit var bannerPrnSel	: ConstraintLayout
	private lateinit var bannerPrg		: ConstraintLayout
	private lateinit var prgMsg			: TextView
	private lateinit var printerSelIcon	: ZebraPrinterView

	private val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			if (UsbHelper.ACTION_USB_PERMISSION_GRANTED == intent.action)
				synchronized(this)
				{
					val permissionGranted = intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED,
						false
					)
					val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
					if (device != null && UsbDiscoverer.isZebraUsbDevice(device))
						if (permissionGranted)
							SelectedPrinterManager.setSelectedPrinter(
								DiscoveredPrinterUsb(
									device.deviceName, UsbHelper.getUsbManager(
										this@MainActivity
									), device
								)
							)
						else
							DialogHelper.showErrorDialog(
								this@MainActivity,
								getString(R.string.msg_warning_usb_permissions_denied)
							)
					ProgressOverlayHelper.hideProgressOverlay(prgMsg, bannerPrg)
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
						ProgressOverlayHelper.showProgressOverlay(prgMsg, bannerPrg, getString(R.string.msg_waiting_requesting_usb_permission))
						UsbHelper.requestUsbPermission(this@MainActivity, usbManager, device)
					}
					else
					{
						SelectedPrinterManager.setSelectedPrinter(
							DiscoveredPrinterUsb(
								device.deviceName, UsbHelper.getUsbManager(
									this@MainActivity
								), device
							)
						)
						refreshSelectedPrinterBanner()
					}
				}
			}
		}
	}
	private val usbDeviceDetachedReceiver: BroadcastReceiver = object : BroadcastReceiver()
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

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(findViewById(R.id.toolbar))

		registerReceivers()
		initComponents()

		// Update banner to show
		refreshSelectedPrinterBanner()
	}

	// Both for request permissions
	override fun onPause()  { super.onPause();  unregisterReceivers() }
	override fun onResume() { super.onResume(); registerReceivers(); refreshSelectedPrinterBanner() }

	// Just for cancel any coroutine in process
	override fun onDestroy() { super.onDestroy(); jobPrinterStatusUpdate.cancel(); jobReconnectPrinter.cancel()}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
	{
		when (requestCode)
		{
			REQUEST_START_ACTIVITY -> if (resultCode == RESULT_OK) {
				/*
					* Save "KEY_RESET_PRINNTER" as public static constant of SettingsDemoActivity
					* to request it as "SettingsDemoActivity.KEY_RESET_PRINTER"
					* */
				val resetPrinter = data!!.getBooleanExtra("KEY_RESET_PRINNTER", false)
				if (reconnectPrinterTask != null)
					jobReconnectPrinter.cancel()
				reconnectPrinterTask = ReconnectPrinterTask(
					SelectedPrinterManager.getSelectedPrinter()!!,
					resetPrinter
				)
				reconnectPrinterTask!!.setOnPrinterDiscoveryListener(object: OnReconnectPrinterListener{
					override fun onReconnectPrinterStarted() {
						refreshSelectedPrinterBanner()
						ProgressOverlayHelper.showProgressOverlay(
							prgMsg,
							bannerPrg,
							getString(R.string.msg_waiting_reconnecting_to_printer))
					}

					override fun onReconnectPrinterFinished(exception: java.lang.Exception?) {
						ProgressOverlayHelper.hideProgressOverlay(prgMsg, bannerPrg)
						if (exception != null) {
							DialogHelper
								.showErrorDialog(this@MainActivity, getString(R.string.msg_error_reconnecting_to_printer, exception.message))
							SelectedPrinterManager.setSelectedPrinter(null)
						}
						refreshSelectedPrinterBanner()
					}

				})
				jobReconnectPrinter = launch { reconnectPrinterTask!!.execute() }
			} else if (resultCode == RESULT_CANCELED)
				if (data != null) {
					val permissionResult = data.getIntExtra(
						StorageHelper.KEY_STORAGE_PERMISSIONS_RESULT,
						-1
					)
					if (permissionResult == StorageHelper.PERMISSION_DENIED)
						UIHelper.showSnackbar(
							this,
							getString(R.string.storage_permissions_denied)
						)
					else if (permissionResult == StorageHelper.PERMISSION_NEVER_ASK_AGAIN_SET)
						UIHelper.showSnackbar(
							this,
							getString(R.string.storage_permissions_request_enable_message)
						)
				}
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun initComponents()
	{
		// Setting banners and its elements
		bannerNoPrnSel = findViewById(R.id.banner_no_prn_sel)
		bannerPrnSel   = findViewById(R.id.banner_prn_sel)
		printerSelIcon = findViewById(R.id.banner_prn_sel_icon)
		bannerPrg      = findViewById(R.id.banner_prog)
		prgMsg         = findViewById(R.id.banner_prog_txt)

		// Config buttons to printer connection
		bannerPrnSel.setOnClickListener { promptDisconnectPrinter() }
		bannerNoPrnSel.setOnClickListener {
			startActivity(
				Intent(this@MainActivity, DiscoverPrintersActivity::class.java)
					.putExtra("key", "Kotlin")
			)
		}

		// Setting RecyclerView w/ its [Card]s
		zCards = ZCard.createCardsList(10, R.drawable.card_mamalona)
		zCards.add(0, ZCard(R.drawable.card_add))

		val recyclerView = findViewById<RecyclerView>(R.id.recView_zcards)
		recyclerView.adapter = ZCardAdapter(zCards).apply {
			this.setOnClickZCardListener { pos ->
				when (pos)
				{
					0 -> startActivityForResult(Intent(this@MainActivity, Send2PrintActivity::class.java).putExtra("keySelected", -1), REQUEST_START_ACTIVITY)
					else -> startActivity(
						Intent(
							this@MainActivity,
							FieldsCaptureActivity::class.java
						)
							.putExtra("zcardSelected", -1)
					)
				}
			}
		}
		recyclerView.layoutManager = LinearLayoutManager(this)
	}

	private fun refreshSelectedPrinterBanner()
	{
		val printer: DiscoveredPrinter? = SelectedPrinterManager.getSelectedPrinter()
		val isPrinterSelected = printer!=null
		if (isPrinterSelected)
		{
			val printerAddress : TextView = findViewById(R.id.banner_prn_sel_addr)
			val printerModel   : TextView = findViewById(R.id.banner_prn_sel_model)

			val address = printer!!.discoveryDataMap["ADDRESS"]
			val model   = printer.discoveryDataMap["MODEL"]
			printerAddress.visibility = if (address!=null && address.isNotEmpty()) View.VISIBLE else View.GONE
			printerModel.visibility = if (model!=null && model.isNotEmpty()) View.VISIBLE else View.GONE
			printerAddress.text = address
			printerModel.text = model
			printerSelIcon.setPrinterModel(model)

            if (reconnectPrinterTask==null || reconnectPrinterTask!!.isBackgroundTaskFinished())
            {
				jobPrinterStatusUpdate.cancel()
				printerStatusUpdateTask = PrinterStatusUpdateTask(this@MainActivity, printer)
                printerStatusUpdateTask!!.setOnUpdatePrinterStatusListener(object: OnUpdatePrinterStatusListener {
					override fun onUpdatePrinterStatusStarted() {
						printerSelIcon.printerStatus = ZebraPrinterView.PrinterStatus.REFRESHING
					}

					override fun onUpdatePrinterStatusFinished(exception: java.lang.Exception?, printerStatus: ZebraPrinterView.PrinterStatus) {
						if (exception != null)
						{
							DialogHelper
								.showErrorDialog(this@MainActivity, getString(R.string.msg_error_updating_printer_status, exception.message))
							SelectedPrinterManager.setSelectedPrinter(null)
							refreshSelectedPrinterBanner()
						}
						else
						{
							runOnUiThread {
								printerSelIcon.printerStatus = printerStatus
							}
//			updateDemoButtons()
						}
					}
				})
				jobPrinterStatusUpdate = launch{ printerStatusUpdateTask!!.execute() }
            }
		}

		bannerPrnSel.visibility   = if (isPrinterSelected) View.VISIBLE else View.GONE
		bannerNoPrnSel.visibility = if (isPrinterSelected) View.GONE else View.VISIBLE
		bannerPrg.visibility = View.GONE
	}

	private fun promptDisconnectPrinter()
	{
		DialogHelper.createDisconnectDialog(this, DialogInterface.OnClickListener { _, _ ->
			SelectedPrinterManager.setSelectedPrinter(null)
			refreshSelectedPrinterBanner()
		}).show()
	}

	private fun registerReceivers()
	{
		var filter = IntentFilter()
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		registerReceiver(usbDeviceDetachedReceiver, filter)
		filter = IntentFilter()
		filter.addAction(UsbHelper.ACTION_USB_PERMISSION_GRANTED)
		registerReceiver(usbPermissionReceiver, filter)
		filter = IntentFilter()
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
		registerReceiver(usbDeviceAttachedReceiver, filter)
	}
	private fun unregisterReceivers() {
		unregisterReceiver(usbDeviceDetachedReceiver)
		unregisterReceiver(usbPermissionReceiver)
		unregisterReceiver(usbDeviceAttachedReceiver)
	}

//	private fun updateDemoButtons()
//	{
//		val isReconnecting =
//			reconnectPrinterTask != null && !reconnectPrinterTask!!.isBackgroundTaskFinished()
//		val enabled =
//			bannerPrnSel.visibility == View.VISIBLE && printerSelIcon.printerStatus != ZebraPrinterView.PrinterStatus.REFRESHING && !isReconnecting
////		magEncodeDemoButton.setEnabled(enabled)
////		multiJobDemoButton.setEnabled(enabled)
////		printDemoButton.setEnabled(enabled)
////		printerStatusDemoButton.setEnabled(enabled)
////		settingsDemoButton.setEnabled(enabled)
////		templateDemoButton.setEnabled(enabled)
////		smartCardDemoButton.setEnabled(enabled)
//	}
}
