package isv.zebra.com.zebracardprinter.activity

import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb
import com.zebra.sdk.printer.discovery.UsbDiscoverer
import com.zebra.zebraui.ZebraEditText
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.adapter.DiscoveredPrinterAdapter
import isv.zebra.com.zebracardprinter.zebra.discovery.ManualConnectionTask
import isv.zebra.com.zebracardprinter.zebra.discovery.NetworkAndUsbDiscoveryTask
import isv.zebra.com.zebracardprinter.zebra.discovery.SelectedPrinterManager
import isv.zebra.com.zebracardprinter.zebra.util.DialogHelper
import isv.zebra.com.zebracardprinter.zebra.util.ProgressOverlayHelper
import isv.zebra.com.zebracardprinter.zebra.util.UsbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class DiscoverPrintersActivity : AppCompatActivity(), NetworkAndUsbDiscoveryTask.OnPrinterDiscoveryListener, ManualConnectionTask.OnManualConnectionListener, CoroutineScope
{
	private var isApplicationBusy = false

	//For Coroutines
	private var jobManualConnection: Job       = Job()
	private var jobNetworkAndUsbDiscovery: Job = Job()
	override val coroutineContext: CoroutineContext = Job()+Dispatchers.Main

	private lateinit var manualConnectionTask: ManualConnectionTask
	private lateinit var networkAndUsbDiscoveryTask: NetworkAndUsbDiscoveryTask

	private lateinit var discoveredPrinters: ArrayList<DiscoveredPrinter>
	private lateinit var progressOverlay: ConstraintLayout
	private lateinit var progressMessage: TextView
	private lateinit var refreshPrintersButton: ImageView
	private lateinit var rotation: Animation
	private lateinit var discoveredPrinterAdapter: DiscoveredPrinterAdapter
	private lateinit var pullToRefresh: SwipeRefreshLayout
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyListView: LinearLayout

	private val usbDisconnectReceiver: BroadcastReceiver = object : BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action)
			{
				val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
				if (device != null && UsbDiscoverer.isZebraUsbDevice(device))
				{
					val disconnectedPrinter = DiscoveredPrinterUsb(device.deviceName, UsbHelper.getUsbManager(this@DiscoverPrintersActivity), device)
					val address = disconnectedPrinter.discoveryDataMap["ADDRESS"]
					val idx = discoveredPrinterAdapter.getIndexWithAddress(address!!)
					if (idx != -1)
					{
						discoveredPrinters.removeAt(idx)
						discoveredPrinterAdapter.notifyItemChanged(idx)
						if (discoveredPrinters.isEmpty())
						{
							emptyListView.visibility = View.VISIBLE
							recyclerView.visibility = View.GONE
						}
					}
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
						{
							SelectedPrinterManager.setSelectedPrinter(DiscoveredPrinterUsb(device.deviceName, UsbHelper.getUsbManager(this@DiscoverPrintersActivity), device))
							finish()
						}
						else
							DialogHelper.showErrorDialog(this@DiscoverPrintersActivity, getString(R.string.msg_warning_usb_permissions_denied))
					isApplicationBusy = false
					ProgressOverlayHelper.hideProgressOverlay(progressMessage, progressOverlay)
				}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_discover_printers)
		setSupportActionBar(findViewById(R.id.toolbar))

		registerReceivers()
		initComponents()
		findPrinters()
	}

	override fun onResume() { super.onResume(); registerReceivers() }
	override fun onPause()  { super.onPause(); unregisterReceivers() }
	override fun onDestroy() { super.onDestroy(); jobNetworkAndUsbDiscovery.cancel(); jobManualConnection.cancel() }

	private fun initComponents()
	{
//		refreshPrintersButton = findViewById(R.id.refreshPrintersButton)
		progressOverlay = findViewById(R.id.progressOverlay)
		progressMessage = findViewById(R.id.progressMessage)
		emptyListView	= findViewById(R.id.noPrintersFoundContainer)

		rotation = AnimationUtils.loadAnimation(this, R.anim.anim_rotate)
		findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { addPrinter() }
//		refreshPrintersButton.setOnClickListener { findPrinters() }

		//		 Setting function to SwipeToRefresh
		pullToRefresh = findViewById(R.id.pullToRefresh_printers)
		pullToRefresh.setOnRefreshListener { findPrinters() }

		//		Setting RecyclerView w/ its [Printer]s
		discoveredPrinters = ArrayList()
		discoveredPrinterAdapter = DiscoveredPrinterAdapter(discoveredPrinters).apply {
			this.context = this@DiscoverPrintersActivity
			this.setOnClickDiscoveredPrinterListener { pos ->
				if (!isApplicationBusy)
				{
					isApplicationBusy = true
					ProgressOverlayHelper.showProgressOverlay(progressMessage, progressOverlay, getString(R.string.connecting_to_printer))
					val printer = discoveredPrinters[pos]
					if (printer is DiscoveredPrinterUsb)
					{
						val usbManager = UsbHelper.getUsbManager(this@DiscoverPrintersActivity)
						val device = printer.device
						if (!usbManager.hasPermission(device))
						{
							ProgressOverlayHelper.showProgressOverlay(progressMessage, progressOverlay, getString(R.string.msg_waiting_requesting_usb_permission))
							UsbHelper.requestUsbPermission(this@DiscoverPrintersActivity, usbManager, device)
							return@setOnClickDiscoveredPrinterListener
						}
					}
					SelectedPrinterManager.setSelectedPrinter(printer)
					finish()
				}
			}
		}
		recyclerView = findViewById(R.id.recView_discovered_printers)
		recyclerView.adapter = discoveredPrinterAdapter
		recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
	}

	override fun onManualConnectionStarted()
	{
		isApplicationBusy = true
		ProgressOverlayHelper.showProgressOverlay(progressMessage, progressOverlay, getString(R.string.connecting_to_printer))
	}

	override fun onManualConnectionFinished(exception: Exception?, printer: DiscoveredPrinter?)
	{
		isApplicationBusy = false
		ProgressOverlayHelper.hideProgressOverlay(progressMessage, progressOverlay)
		if (exception != null)
			DialogHelper.showErrorDialog(this, getString(R.string.error_manually_connecting_message, exception.message))
		else if (printer != null)
		{
			SelectedPrinterManager.setSelectedPrinter(printer)
			finish()
		}
	}

	override fun onPrinterDiscoveryStarted() {}

	override fun onPrinterDiscovered(printer: DiscoveredPrinter?)
	{
		runOnUiThread {
			discoveredPrinters.add(printer!!)
			discoveredPrinterAdapter.notifyItemChanged(discoveredPrinters.size-1)
			if (discoveredPrinters.size==1)
			{
				emptyListView.visibility = View.GONE
				recyclerView.visibility = View.VISIBLE
			}
		}
	}

	override fun onPrinterDiscoveryFinished(exception: Exception?)
	{
//		finishRefreshAnimation()
		pullToRefresh.isRefreshing = false
		if (exception != null)
			DialogHelper.showErrorDialog(this, getString(R.string.error_discovering_printers_message, exception.message))
	}

	private fun addPrinter()
	{
		if (!isApplicationBusy)
		{
			DialogHelper.createManuallyConnectDialog(this@DiscoverPrintersActivity,
				DialogInterface.OnClickListener { dialog, _ ->
					val printerDnsIpAddressInput: ZebraEditText? = (dialog as AlertDialog).findViewById(R.id.printerDnsIpAddressInput)
					val ipAddress = printerDnsIpAddressInput!!.text

					jobManualConnection.cancel()
					manualConnectionTask =
						ManualConnectionTask(this@DiscoverPrintersActivity, ipAddress)
					manualConnectionTask.setOnManualConnectionListener(this@DiscoverPrintersActivity)
					jobManualConnection = launch { manualConnectionTask.execute() }
				}).show()
		}
	}

	private fun findPrinters()
	{
		pullToRefresh.isRefreshing = true
		discoveredPrinters.clear()
		discoveredPrinterAdapter.notifyDataSetChanged()
		emptyListView.visibility = View.VISIBLE
		recyclerView.visibility = View.GONE
//		startRefreshAnimation()
		jobNetworkAndUsbDiscovery.cancel()
		networkAndUsbDiscoveryTask = NetworkAndUsbDiscoveryTask(UsbHelper.getUsbManager(this@DiscoverPrintersActivity))
		networkAndUsbDiscoveryTask.setOnPrinterDiscoveryListener(this@DiscoverPrintersActivity)
		jobNetworkAndUsbDiscovery = launch { networkAndUsbDiscoveryTask.execute() }
	}

	private fun startRefreshAnimation()
	{
		refreshPrintersButton.isEnabled = false
		refreshPrintersButton.alpha = 0.5f
		refreshPrintersButton.startAnimation(rotation)
	}

	private fun finishRefreshAnimation()
	{
		refreshPrintersButton.isEnabled = true
		refreshPrintersButton.alpha = 1.0f
		refreshPrintersButton.clearAnimation()
	}

	private fun registerReceivers() {
		var filter = IntentFilter()
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		registerReceiver(usbDisconnectReceiver, filter)
		filter = IntentFilter()
		filter.addAction(UsbHelper.ACTION_USB_PERMISSION_GRANTED)
		registerReceiver(usbPermissionReceiver, filter)
	}

	private fun unregisterReceivers() {
		unregisterReceiver(usbDisconnectReceiver)
		unregisterReceiver(usbPermissionReceiver)
	}
}