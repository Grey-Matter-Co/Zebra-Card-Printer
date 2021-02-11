package isv.zebra.com.zebracardprinter.zebra.util

import android.R
import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

object UIHelper {
	fun setLogoOnActionBar(activity: AppCompatActivity) {
		val actionBar: androidx.appcompat.app.ActionBar? = activity.supportActionBar
		if (actionBar != null) {
			actionBar.setDisplayShowHomeEnabled(true)
			actionBar.setLogo(R.drawable.ic_lock_idle_lock)
			actionBar.setDisplayUseLogoEnabled(true)
		}
	}

	fun showSnackbar(activity: Activity, message: String?) {
		activity.runOnUiThread {
			val snackbar: Snackbar = Snackbar.make(
				activity.findViewById(R.id.content),
				message!!,
				Snackbar.LENGTH_LONG
			)
			val textView: TextView =
				snackbar.view.findViewById(com.google.android.material.R.id.snackbar_text)
			textView.setTextColor(Color.WHITE)
			textView.maxLines = 5
			snackbar.show()
		}
	}

	fun hideSoftKeyboard(activity: Activity) {
		val currentFocus = activity.currentFocus
		if (currentFocus != null) {
			val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
		}
	}
}
