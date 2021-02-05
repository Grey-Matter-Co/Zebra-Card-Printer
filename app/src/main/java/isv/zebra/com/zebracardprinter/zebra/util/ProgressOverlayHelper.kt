package isv.zebra.com.zebracardprinter.zebra.util

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class ProgressOverlayHelper {
	companion object
	{
		fun showProgressOverlay(progressMessage: TextView, progressOverlay: LinearLayout, message: String?)
		{
			progressMessage.text = message
			progressOverlay.visibility = View.VISIBLE
		}

		fun hideProgressOverlay(progressMessage: TextView, progressOverlay: LinearLayout)
		{
			progressMessage.text = null
			progressOverlay.visibility = View.GONE
		}
	}
}
