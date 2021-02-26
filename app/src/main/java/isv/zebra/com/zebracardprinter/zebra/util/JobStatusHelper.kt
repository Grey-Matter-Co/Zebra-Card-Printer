package isv.zebra.com.zebracardprinter.zebra.util

import android.app.Activity
import android.util.SparseArray
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JobStatusHelper
{
	private val jobIdLastMessageMap = SparseArray<String>()
	fun updateJobStatusLog( activity: Activity?, jobStatusTextView: TextView, jobStatusMessage: String)
		= updateJobStatusLog(activity, null, jobStatusTextView, jobStatusMessage)

	fun updateJobStatusLog(activity: Activity?, jobId: Int?, jobStatusTextView: TextView, jobStatusMessage: String)
	{
		activity?.runOnUiThread {
			if (jobId != null)
			{
				val lastMessageForJobId = jobIdLastMessageMap[jobId]
				if (lastMessageForJobId == null || lastMessageForJobId != jobStatusMessage)
				{
					writeToJobStatusLog(jobStatusTextView, jobStatusMessage)
					jobIdLastMessageMap.put(jobId, jobStatusMessage)
				}
			}
			else
				writeToJobStatusLog(jobStatusTextView, jobStatusMessage)
		}
	}

	private fun writeToJobStatusLog(jobStatusTextView: TextView, jobStatusMessage: String)
	{
		var jobStatusLog = jobStatusTextView.text.toString()
		if (jobStatusLog.isNotEmpty())
			jobStatusLog += "\n"
		jobStatusLog += "$currentTimestamp $jobStatusMessage"
		jobStatusTextView.text = jobStatusLog
	}

	private val currentTimestamp: String
		get() = SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]", Locale.getDefault())
					.format(Date())
}