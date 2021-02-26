package isv.zebra.com.zebracardprinter.zebra.jobStatus

import android.content.Context
import androidx.annotation.Nullable
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.common.card.containers.JobStatusInfo
import com.zebra.sdk.common.card.enumerations.CardSource
import com.zebra.sdk.common.card.printer.ZebraCardPrinter
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.zebra.util.ConnectionHelper
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable.isCancelled
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch

class PollJobStatusTask
{
	private var weakContext: WeakReference<Context>
	private var printer: DiscoveredPrinter? = null
	private var jobInfoList: MutableList<JobInfo?> = arrayListOf()
	private var onJobStatusPollListener: OnJobStatusPollListener? = null
	private var exception: Exception? = null
	private var showAtmDialog = true
	private var startTime: Long = 0
	private var cancelledByUser = false

	interface OnJobStatusPollListener
	{
		fun onJobStatusUpdate(@Nullable jobInfo: JobInfo?, @Nullable jobStatusInfo: JobStatusInfo?, message: String?)
		fun onJobStatusUserInputRequired(title: String?, message: String?, positiveButtonText: String?, negativeButtonText: String?, onUserInputListener: OnUserInputListener?)
		fun onJobStatusAtmCardRequired()
		fun onJobStatusPollFinished(exception: Exception?)
	}

	interface OnUserInputListener
	{
		fun onPositiveButtonClicked()
		fun onNegativeButtonClicked()
	}

	constructor(context: Context, printer: DiscoveredPrinter?, jobInfo: JobInfo?)
	{
		weakContext = WeakReference(context)
		this.printer = printer
		jobInfoList.add(jobInfo)
	}

	constructor(context: Context, printer: DiscoveredPrinter?, jobInfoList: MutableList<JobInfo?>)
	{
		weakContext = WeakReference(context)
		this.printer = printer
		this.jobInfoList = jobInfoList
	}

	@InternalCoroutinesApi
	suspend fun execute()
	{
		var connection: Connection? = null
		var zebraCardPrinter: ZebraCardPrinter? = null
		var isFeeding = false
		try
		{
			connection = printer!!.connection
			connection.open()
			zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection)
			startTime = System.currentTimeMillis()
			for (jobInfo in jobInfoList)
				if (onJobStatusPollListener != null)
					onJobStatusPollListener!!.onJobStatusUpdate(jobInfo, null, weakContext.get()!!.getString(R.string.polling_job_status, jobInfo?.getJobId()))
			while (jobInfoList.size > 0)
			{
				for (jobInfo in ArrayList<JobInfo>(jobInfoList))
				{
					if (isCancelled)
						break
					val jobStatusInfo = zebraCardPrinter.getJobStatus(jobInfo.getJobId()!!)
					if (!isFeeding)
						startTime = System.currentTimeMillis()
					val isAlarmInfoPresent = jobStatusInfo.alarmInfo.value > 0
					val isErrorInfoPresent = jobStatusInfo.errorInfo.value > 0
					isFeeding = jobStatusInfo.cardPosition.contains("feeding")
					if (onJobStatusPollListener != null) {
						val alarmInfo = if (isAlarmInfoPresent) weakContext.get()!!
							.getString(
								R.string.alarm_error_value_and_description,
								jobStatusInfo.alarmInfo.value,
								jobStatusInfo.alarmInfo.description
							) else jobStatusInfo.alarmInfo.value.toString()
						val errorInfo = if (isErrorInfoPresent) weakContext.get()!!
							.getString(
								R.string.alarm_error_value_and_description,
								jobStatusInfo.errorInfo.value,
								jobStatusInfo.errorInfo.description
							) else jobStatusInfo.errorInfo.value.toString()
						val jobStatusMessage = weakContext.get()!!.getString(
							R.string.job_status_message,
							jobInfo.getJobId(),
							jobStatusInfo.printStatus,
							jobStatusInfo.cardPosition,
							jobStatusInfo.contactSmartCard,
							jobStatusInfo.contactlessSmartCard,
							alarmInfo,
							errorInfo
						)
						onJobStatusPollListener!!.onJobStatusUpdate(
							jobInfo,
							jobStatusInfo,
							jobStatusMessage
						)
					}
					if (jobStatusInfo.printStatus == "done_ok") {
						finishPolling(
							jobInfo, jobStatusInfo, weakContext.get()!!
								.getString(R.string.job_id_completed, jobInfo.getJobId())
						)
					} else if (jobStatusInfo.printStatus == "done_error") {
						finishPolling(
							jobInfo,
							jobStatusInfo,
							weakContext.get()!!.getString(
								R.string.job_id_completed_with_error_message,
								jobInfo.getJobId(),
								jobStatusInfo.errorInfo.description
							)
						)
					} else if (jobStatusInfo.printStatus.contains("cancelled")) {
						val message = if (isErrorInfoPresent) weakContext.get()!!.getString(
							R.string.job_id_cancelled_with_error_message,
							jobInfo.getJobId(),
							jobStatusInfo.errorInfo.description
						) else weakContext.get()!!
							.getString(R.string.job_id_cancelled, jobInfo.getJobId())
						finishPolling(jobInfo, jobStatusInfo, message)
					} else if (isAlarmInfoPresent) {
						if (onJobStatusPollListener != null)
						{
							val alarmEncounteredLatch = CountDownLatch(1)
							showAlarmEncounteredDialog(
								alarmEncounteredLatch,
								jobInfo,
								jobStatusInfo
							)
							alarmEncounteredLatch.await()
						}
						if (cancelledByUser)
						{
							cancelledByUser = false
							zebraCardPrinter.cancel(jobInfo.getJobId()!!)
						}
					}
					else if (isErrorInfoPresent)
						zebraCardPrinter.cancel(jobInfo.getJobId()!!)
					else if (jobStatusInfo.contactSmartCard.contains("at_station") || jobStatusInfo.contactlessSmartCard.contains("at_station"))
					{
						if (onJobStatusPollListener != null)
						{
							val smartCardAtStationLatch = CountDownLatch(1)
							showCardAtStationDialog(smartCardAtStationLatch)
							smartCardAtStationLatch.await()
						}
						if (cancelledByUser)
						{
							cancelledByUser = false
							zebraCardPrinter.cancel(jobInfo.getJobId()!!)
						}
						else
							zebraCardPrinter.resume()
					}
					else if (isFeeding)
						if (showAtmDialog && jobInfo.getCardSource() == CardSource.ATM)
						{
							if (onJobStatusPollListener != null)
								onJobStatusPollListener!!.onJobStatusAtmCardRequired()
							showAtmDialog = false
						}
						else if (System.currentTimeMillis() > startTime + CARD_FEED_TIMEOUT)
						{
							if (onJobStatusPollListener != null)
								onJobStatusPollListener!!.onJobStatusUpdate(jobInfo, jobStatusInfo, weakContext.get()!!.getString(R.string.job_id_timed_out_message,jobInfo.getJobId()))

							zebraCardPrinter.cancel(jobInfo.getJobId()!!)
						}
					delay(500)
				}
			}
		}
		catch (e: Exception)
			{ exception = e }
		finally
			{ ConnectionHelper.cleanUpQuietly(zebraCardPrinter, connection) }
		if (onJobStatusPollListener != null)
			onJobStatusPollListener!!.onJobStatusPollFinished(exception)
	}

	fun cancel()
	{
		if (onJobStatusPollListener != null)
			onJobStatusPollListener!!.onJobStatusUpdate( null, null, weakContext.get()!!.getString(R.string.job_status_polling_cancelled))
	}

	private fun finishPolling(jobInfo: JobInfo?, jobStatusInfo: JobStatusInfo, message: String) {
		if (onJobStatusPollListener != null) {
			onJobStatusPollListener!!.onJobStatusUpdate(jobInfo, jobStatusInfo, message)
		}
		showAtmDialog = true
		startTime = System.currentTimeMillis()
		jobInfoList.remove(jobInfo)
	}

	private fun showAlarmEncounteredDialog(
		alarmEncounteredLatch: CountDownLatch,
		jobInfo: JobInfo?,
		jobStatusInfo: JobStatusInfo
	) {
		val title = weakContext.get()!!.getString(R.string.alarm_encountered)
		val positiveButtonText = weakContext.get()!!.getString(android.R.string.ok)
		val negativeButtonText = weakContext.get()!!.getString(android.R.string.cancel)
		val message = weakContext.get()!!.getString(
			R.string.alarm_encountered_message,
			jobInfo?.getJobId(),
			jobStatusInfo.alarmInfo.description,
			positiveButtonText,
			negativeButtonText
		)
		onJobStatusPollListener!!.onJobStatusUserInputRequired(
			title,
			message,
			positiveButtonText,
			negativeButtonText,
			object : OnUserInputListener {
				override fun onPositiveButtonClicked() {
					cancelledByUser = false
					alarmEncounteredLatch.countDown()
				}

				override fun onNegativeButtonClicked() {
					cancelledByUser = true
					alarmEncounteredLatch.countDown()
				}
			})
	}

	private fun showCardAtStationDialog(smartCardAtStationLatch: CountDownLatch)
	{
		val title = weakContext.get()!!.getString(R.string.card_at_station)
		val positiveButtonText = weakContext.get()!!.getString(R.string.resume)
		val negativeButtonText = weakContext.get()!!.getString(android.R.string.cancel)
		val message = weakContext.get()!!
			.getString(R.string.card_at_station_message, positiveButtonText, negativeButtonText)
		onJobStatusPollListener!!.onJobStatusUserInputRequired(
			title,
			message,
			positiveButtonText,
			negativeButtonText,
			object : OnUserInputListener {
				override fun onPositiveButtonClicked() {
					cancelledByUser = false
					smartCardAtStationLatch.countDown()
				}

				override fun onNegativeButtonClicked() {
					cancelledByUser = true
					smartCardAtStationLatch.countDown()
				}
			})
	}

//	fun setOnJobStatusPollListener(onJobStatusPollListener: OnJobStatusPollListener?) {
//		this.onJobStatusPollListener = onJobStatusPollListener
//	}

	fun setOnJobStatusPollListener(jobStatusPollListener: OnJobStatusPollListener)
		{ onJobStatusPollListener = jobStatusPollListener }

	companion object {
		private const val CARD_FEED_TIMEOUT = 60 * 1000
	}
}
