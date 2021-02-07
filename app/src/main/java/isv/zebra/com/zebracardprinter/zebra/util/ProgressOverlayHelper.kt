package isv.zebra.com.zebracardprinter.zebra.util

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class ProgressOverlayHelper {
	companion object
	{
		fun showProgressOverlay(progressMessage: TextView, progressOverlay: ConstraintLayout, message: String?)
		{
			progressMessage.text = message
			progressOverlay.visibility = View.VISIBLE
		}

		fun hideProgressOverlay(progressMessage: TextView, progressOverlay: ConstraintLayout)
		{
			progressMessage.text = null
			progressOverlay.visibility = View.GONE
		}
	}
}
