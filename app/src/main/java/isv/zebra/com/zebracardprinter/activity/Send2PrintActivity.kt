package isv.zebra.com.zebracardprinter.activity

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import com.zebra.sdk.common.card.containers.JobStatusInfo
import com.zebra.sdk.common.card.enumerations.CardSource
import com.zebra.sdk.common.card.enumerations.PrintType
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.zebraui.ZebraButton
import com.zebra.zebraui.ZebraEditText
import com.zebra.zebraui.ZebraSpinnerView
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.model.PrintOptions
import isv.zebra.com.zebracardprinter.zebra.discovery.SelectedPrinterManager
import isv.zebra.com.zebracardprinter.zebra.discovery.SendPrintJobTask
import isv.zebra.com.zebracardprinter.zebra.jobStatus.JobInfo
import isv.zebra.com.zebracardprinter.zebra.jobStatus.PollJobStatusTask
import isv.zebra.com.zebracardprinter.zebra.util.*
import isv.zebra.com.zebracardprinter.zebra.util.StorageHelper.handleRequestStoragePermissionsResult
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

@InternalCoroutinesApi
class Send2PrintActivity : AppCompatActivity(), CoroutineScope
{
	companion object
	{
		const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2001
		const val REQUEST_SELECT_FRONT_SIDE_IMAGE_FILE = 3001
		const val REQUEST_SELECT_FRONT_SIDE_OVERLAY_IMAGE_FILE = 3002
		const val REQUEST_SELECT_BACK_SIDE_IMAGE_FILE = 3003
	}

	private var jobSendPrintJob: Job  = Job().apply { this.cancel() }
	private var jobPollJobStatus: Job = Job().apply { this.cancel() }
	override val coroutineContext: CoroutineContext = Job() + Dispatchers.IO
	private var sendPrintJobTask: SendPrintJobTask? = null
	private var pollJobStatusTask: PollJobStatusTask? = null

	private var printer: DiscoveredPrinter? = null
	private var frontSideImageUri: Uri? = null
	private var frontSideOverlayImageUri: Uri? = null
	private var backSideImageUri: Uri? = null

	private var insertCardDialog: AlertDialog? = null
	private var printFrontSideSwitch: Switch? = null
	private var frontSideImageFileContainer: RelativeLayout? = null
	private var frontSideImageFileEditText: ZebraEditText? = null
	private var frontSideTypesSpinner: ZebraSpinnerView? = null
	private var printFrontSideOverlaySwitch: Switch? = null
	private var frontSideOverlayImageFileContainer: RelativeLayout? = null
	private var frontSideOverlayImageFile: ZebraEditText? = null
	private var backSideImageFileContainer: RelativeLayout? = null
	private var backSideImageFileEditText: ZebraEditText? = null
	private var printBackSideSwitch: Switch? = null
	private var printQuantities: ZebraSpinnerView? = null
	private var printButton: ZebraButton? = null
	private var progressOverlay: ConstraintLayout? = null
	private var progressMessage: TextView? = null
	private var jobStatusText: TextView? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demo_print)
//		setSupportActionBar(findViewById(R.id.toolbar))
		setTitle(R.string.print_ymcko_mono_demo)
		UIHelper.setLogoOnActionBar(this)
		if (!StorageHelper.isExternalStorageWritable)
			DialogHelper.showStorageErrorDialog(this, DialogInterface.OnClickListener { _, _ -> finish() })
		StorageHelper.requestStoragePermissionIfNotGranted(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
		initComponents()
	}

	override fun onResume() {
		super.onResume()
		if (!jobSendPrintJob.isActive)
			if (insertCardDialog != null && insertCardDialog!!.isShowing)
				insertCardDialog!!.dismiss()
	}

	override fun onDestroy()
	{
		jobSendPrintJob.cancel()
		jobPollJobStatus.cancel()
		super.onDestroy()
	}

	override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray)
	{
		for (i in permissions.indices) {
			val permission = permissions[i]
			val grantResult = grantResults[i]
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission)
				handleRequestStoragePermissionsResult(this@Send2PrintActivity, permission, grantResult)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
		if (resultCode == RESULT_OK){
			val uri = data?.data
			val filename: String = UriHelper.getFilename(this, uri)!!
			val filenameExists = filename.trim { it <= ' ' }.isNotEmpty()
			when (requestCode) {
				REQUEST_SELECT_FRONT_SIDE_IMAGE_FILE -> {
					frontSideImageUri = if (filenameExists) uri else null
					frontSideImageFileEditText!!.text = if (filenameExists) filename else null
				}
				REQUEST_SELECT_FRONT_SIDE_OVERLAY_IMAGE_FILE -> {
					frontSideOverlayImageUri = if (filenameExists) uri else null
					frontSideOverlayImageFile!!.text = if (filenameExists) filename else null
				}
				REQUEST_SELECT_BACK_SIDE_IMAGE_FILE -> {
					backSideImageUri = if (filenameExists) uri else null
					backSideImageFileEditText!!.text = if (filenameExists) filename else null
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun initComponents()
	{
		printer = SelectedPrinterManager.getSelectedPrinter()
		createFrontSideInterface()
		createFrontSideOverlayInterface()
		createBackSideInterface()
		printQuantities = findViewById(R.id.printQuantities)
		val cancelButton: ZebraButton = findViewById(R.id.cancelButton)
		printButton = findViewById(R.id.printButton)
		progressOverlay = findViewById(R.id.progressOverlay)
		progressMessage = findViewById(R.id.progressMessage)
		jobStatusText = findViewById(R.id.jobStatusText)
		cancelButton.setOnClickListener { finish() }
		printButton?.setOnClickListener {
			Snackbar.make(findViewById<ImageView>(R.id.frontSideImageFileBrowseButton), (!jobSendPrintJob.isActive).toString(), Snackbar.LENGTH_SHORT).show()
			if (!jobSendPrintJob.isActive)
//			if (!AsyncTaskHelper.isAsyncTaskRunning(sendPrintJobTask))
			{
				UIHelper.hideSoftKeyboard(this@Send2PrintActivity)
				printButton!!.isEnabled = false
				val frontSideImageUriMap: MutableMap<PrintType, Uri?> = EnumMap(com.zebra.sdk.common.card.enumerations.PrintType::class.java)
				val backSideImageUriMap: MutableMap<PrintType, Uri?> = EnumMap(com.zebra.sdk.common.card.enumerations.PrintType::class.java)
				if (printFrontSideSwitch!!.isChecked)
				{
					val printType = PrintType.valueOf((frontSideTypesSpinner!!.selectedItem as String))
					frontSideImageUriMap[printType] = frontSideImageUri
				}
				if (printFrontSideOverlaySwitch!!.isChecked)
					frontSideImageUriMap[PrintType.Overlay] = frontSideOverlayImageUri
				if (printBackSideSwitch!!.isChecked)
					backSideImageUriMap[PrintType.MonoK] = backSideImageUri
				val quantity: Int = printQuantities?.selectedItem.toString().toInt()
				val printOptions = PrintOptions(frontSideImageUriMap, backSideImageUriMap, quantity)
				sendPrintJobTask = SendPrintJobTask(
					this@Send2PrintActivity,
					printer!!, printOptions
				)
				sendPrintJobTask!!.setOnSendPrintJobListener(
					object : SendPrintJobTask.OnSendPrintJobListener
					{
						override fun onSendPrintJobStarted()
						{
							runOnUiThread {
								ProgressOverlayHelper.showProgressOverlay(progressMessage!!, progressOverlay!!, getString(R.string.sending_print_job_to_printer))
								jobStatusText!!.text = null
							}
						}

						override fun onSendPrintJobFinished(exception: Exception?, jobId: Int?, cardSource: CardSource?)
						{
							runOnUiThread {
								ProgressOverlayHelper.hideProgressOverlay(progressMessage!!, progressOverlay!!)
								when
								{
									exception != null ->
									{
										printButton!!.isEnabled = true
										DialogHelper.showErrorDialog(this@Send2PrintActivity, getString(R.string.error_printing_card_message, exception.message))
									}
									jobId != null ->
									{
										UIHelper.showSnackbar(this@Send2PrintActivity, getString(R.string.print_job_sent_successfully))
										jobPollJobStatus.cancel()
	//									pollJobStatusTask?.cancel()
										pollJobStatusTask = PollJobStatusTask(this@Send2PrintActivity, SelectedPrinterManager.getSelectedPrinter(), JobInfo(jobId, cardSource))
										pollJobStatusTask?.setOnJobStatusPollListener(object :
											PollJobStatusTask.OnJobStatusPollListener
										{
											override fun onJobStatusUpdate(jobInfo: JobInfo?, jobStatusInfo: JobStatusInfo?, message: String?)
													= JobStatusHelper.updateJobStatusLog(this@Send2PrintActivity, jobInfo?.getJobId(), jobStatusText!!, message!!)

											override fun onJobStatusUserInputRequired(title: String?, message: String?, positiveButtonText: String?, negativeButtonText: String?, onUserInputListener: PollJobStatusTask.OnUserInputListener?)
											{
												DialogHelper.showAlarmEncounteredDialog(
													this@Send2PrintActivity,
													title,
													message,
													positiveButtonText,
													negativeButtonText,
													DialogInterface.OnClickListener { _, _ -> onUserInputListener?.onPositiveButtonClicked() },
													DialogInterface.OnClickListener { _, _ -> onUserInputListener?.onNegativeButtonClicked() })
											}

											override fun onJobStatusAtmCardRequired()
											{
												runOnUiThread {
													if (insertCardDialog != null && insertCardDialog!!.isShowing)
														insertCardDialog!!.dismiss()
													insertCardDialog = DialogHelper.createInsertCardDialog(this@Send2PrintActivity)
													insertCardDialog?.show()
												}
											}

											override fun onJobStatusPollFinished(exception: Exception?)
											{
												printButton!!.isEnabled = true
												if (insertCardDialog != null && insertCardDialog!!.isShowing)
													insertCardDialog!!.dismiss()
												if (exception != null)
												{
													val errorMessage = getString(R.string.error_printing_card_message, exception.message)
													JobStatusHelper.updateJobStatusLog(this@Send2PrintActivity, jobStatusText!!, errorMessage)
													DialogHelper.showErrorDialog(this@Send2PrintActivity, errorMessage)
												}
											}
										}
										)
										jobPollJobStatus = launch { pollJobStatusTask!!.execute() }
									}
									else ->
										printButton!!.isEnabled = true
								}
							}
						}

						override fun onPrinterReadyUpdate(message: String?, showDialog: Boolean) {
							JobStatusHelper.updateJobStatusLog(this@Send2PrintActivity, jobStatusText!!, message!!)
							if (showDialog) {
								DialogHelper.showErrorDialog(this@Send2PrintActivity, message)
							}
						}

					}
				)
//				sendPrintJobTask!!.setOnSendPrintJobListener(this@Send2PrintActivity)
				jobSendPrintJob = launch { sendPrintJobTask!!.execute() }
			}
		}
	}

	private fun createFrontSideInterface() {
		printFrontSideSwitch = findViewById(R.id.printFrontSideSwitch)
		frontSideImageFileContainer = findViewById(R.id.frontSideImageFileContainer)
		frontSideImageFileEditText = findViewById(R.id.frontSideImageFileEditText)
		val frontSideImageFileBrowseButton: ImageView =
			findViewById(R.id.frontSideImageFileBrowseButton)
		frontSideTypesSpinner = findViewById(R.id.frontSideTypesSpinner)
		frontSideTypesSpinner?.setSpinnerEntries(
			listOf(
				PrintType.Color.toString(),
				PrintType.MonoK.toString()
			)
		)
		frontSideImageFileContainer?.visibility = if (printFrontSideSwitch!!.isChecked) View.VISIBLE else View.GONE
		frontSideTypesSpinner?.visibility = if (printFrontSideSwitch!!.isChecked) View.VISIBLE else View.GONE
		printFrontSideSwitch?.setOnCheckedChangeListener { _, isChecked ->
			val visibility = if (isChecked) View.VISIBLE else View.GONE
			frontSideImageFileContainer?.visibility = visibility
			frontSideTypesSpinner?.visibility = visibility
		}
		frontSideImageFileBrowseButton.setOnClickListener {
			startActivityForResult(createImageFileSelectIntent(), REQUEST_SELECT_FRONT_SIDE_IMAGE_FILE)
		}
	}

	private fun createFrontSideOverlayInterface()
	{
		printFrontSideOverlaySwitch = findViewById(R.id.printFrontSideOverlaySwitch)
		frontSideOverlayImageFileContainer = findViewById(R.id.frontSideOverlayImageFileContainer)
		frontSideOverlayImageFile = findViewById(R.id.frontSideOverlayImageFile)
		val frontSideOverlayImageFileBrowseButton: ImageView =
			findViewById(R.id.frontSideOverlayImageFileBrowseButton)
		frontSideOverlayImageFileContainer?.visibility = if (printFrontSideOverlaySwitch!!.isChecked) View.VISIBLE else View.GONE
		printFrontSideOverlaySwitch?.setOnCheckedChangeListener { _, isChecked ->
			val visibility = if (isChecked) View.VISIBLE else View.GONE
			frontSideOverlayImageFileContainer?.visibility = visibility
		}
		frontSideOverlayImageFileBrowseButton.setOnClickListener {
			startActivityForResult(
				createImageFileSelectIntent(),
				REQUEST_SELECT_FRONT_SIDE_OVERLAY_IMAGE_FILE
			)
		}
	}

	private fun createBackSideInterface()
	{
		backSideImageFileContainer = findViewById(R.id.backSideImageFileContainer)
		backSideImageFileEditText = findViewById(R.id.backSideImageFileEditText)
		val backSideImageFileBrowseButton: ImageView =
			findViewById(R.id.backSideImageFileBrowseButton)
		printBackSideSwitch = findViewById(R.id.printBackSideSwitch)
		backSideImageFileContainer?.visibility = if (printBackSideSwitch!!.isChecked) View.VISIBLE else View.GONE
		printBackSideSwitch?.setOnCheckedChangeListener { _, isChecked ->
			val visibility = if (isChecked) View.VISIBLE else View.GONE
			backSideImageFileContainer?.visibility = visibility
		}
		backSideImageFileBrowseButton.setOnClickListener {
			startActivityForResult(
				createImageFileSelectIntent(),
				REQUEST_SELECT_BACK_SIDE_IMAGE_FILE
			)
		}
	}

	private fun createImageFileSelectIntent(): Intent? {
		val getIntent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
			.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
		return Intent.createChooser(getIntent, getString(R.string.select_graphic_file))
	}
}