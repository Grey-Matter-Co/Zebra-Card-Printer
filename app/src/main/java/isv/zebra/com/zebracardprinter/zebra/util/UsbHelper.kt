package isv.zebra.com.zebracardprinter.zebra.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbHelper {
	companion object
	{
		const val ACTION_USB_PERMISSION_GRANTED = "isv.zebra.com.zebracardprinter.USB_PERMISSION_GRANTED"

		fun getUsbManager(context: Context): UsbManager {
			return context.getSystemService(Context.USB_SERVICE) as UsbManager
		}

		fun requestUsbPermission(context: Context?, manager: UsbManager, device: UsbDevice?)
		{
			val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION_GRANTED), 0)
			manager.requestPermission(device, permissionIntent)
		}
	}
}
