package isv.zebra.com.zebracardprinter.zebra.discovery

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.common.card.containers.GraphicsInfo
import com.zebra.sdk.common.card.enumerations.*
import com.zebra.sdk.common.card.graphics.ZebraCardGraphics
import com.zebra.sdk.common.card.graphics.ZebraCardImageI
import com.zebra.sdk.common.card.graphics.enumerations.RotationType
import com.zebra.sdk.common.card.jobSettings.ZebraCardJobSettingNames
import com.zebra.sdk.common.card.printer.ZebraCardPrinter
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import isv.zebra.com.zebracardprinter.model.PrintOptions
import isv.zebra.com.zebracardprinter.zebra.util.ConnectionHelper
import isv.zebra.com.zebracardprinter.zebra.util.PrinterHelper
import isv.zebra.com.zebracardprinter.zebra.util.UriHelper
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

class SendPrintJobTask internal constructor(context: Context, private val printer: DiscoveredPrinter, private var printOptions: PrintOptions)
{
	private val weakContext: WeakReference<Context> = WeakReference(context)
	private var onSendPrintJobListener: OnSendPrintJobListener? = null
	private var exception: Exception? = null
	private var cardSource: CardSource? = null

	interface OnSendPrintJobListener : PrinterHelper.OnPrinterReadyListener
	{
		fun onSendPrintJobStarted()
		fun onSendPrintJobFinished(exception: Exception?, jobId: Int?, cardSource: CardSource?)
	}

	suspend fun execute()
	{
		if (onSendPrintJobListener != null)
			onSendPrintJobListener!!.onSendPrintJobStarted()

		var graphics: ZebraCardGraphics? = null
		var zebraCardPrinter: ZebraCardPrinter? = null
		var connection: Connection? = null
		var jobId: Int? = null
		try
		{
			connection = printer.connection
			connection.open()
			zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection)
			if (PrinterHelper.isPrinterReady(weakContext.get()!!, zebraCardPrinter, onSendPrintJobListener))
			{
				graphics = ZebraCardGraphics(zebraCardPrinter)
				val frontSideImageUriMap: Map<PrintType, Uri?> = printOptions.getFrontSideImageUriMap()
				val backSideImageUriMap: Map<PrintType, Uri?> = printOptions.getBackSideImageUriMap()
				var printType: PrintType? = null
				var frontSideImageUri: Uri? = null
				val frontSideOverlayImageUri =
					if (frontSideImageUriMap.containsKey(PrintType.Overlay)) frontSideImageUriMap[PrintType.Overlay]
					else
						null
				val backSideImageUri =
					if (backSideImageUriMap.containsKey(PrintType.MonoK)) backSideImageUriMap[PrintType.MonoK]
					else
						null
				if (frontSideImageUriMap.containsKey(PrintType.Color))
				{
					frontSideImageUri = frontSideImageUriMap[PrintType.Color]
					printType = PrintType.Color
				}
				else if (frontSideImageUriMap.containsKey(PrintType.MonoK))
				{
					frontSideImageUri = frontSideImageUriMap[PrintType.MonoK]
					printType = PrintType.MonoK
				}
				val frontSideImageData: ByteArray? =
					if (frontSideImageUri != null)
						UriHelper.getByteArrayFromUri(weakContext.get()!!, frontSideImageUri)
					else
						null
				val frontSideOverlayImageData: ByteArray? =
					if (frontSideOverlayImageUri != null)
						UriHelper.getByteArrayFromUri(weakContext.get()!!, frontSideOverlayImageUri)
					else
						null
				val backSideImageData: ByteArray? =
					if (backSideImageUri != null)
						UriHelper.getByteArrayFromUri(weakContext.get()!!, backSideImageUri)
					else
						null
				val graphicsData: MutableList<GraphicsInfo> = ArrayList()
				if (frontSideImageData != null)
				{
					graphics.initialize(weakContext.get()!!.applicationContext,0, 0, OrientationType.Landscape, printType, Color.WHITE)
					graphics.drawImage(frontSideImageData, 0, 0, 0, 0, RotationType.RotateNoneFlipNone)
					graphicsData.add(buildGraphicsInfo(graphics.createImage(), CardSide.Front, printType))
					graphics.clear()
				}
				if (frontSideOverlayImageData != null)
				{
					graphics.initialize(0, 0, OrientationType.Landscape, PrintType.Overlay, Color.WHITE)
					graphics.drawImage(frontSideOverlayImageData, 0, 0, 0, 0, RotationType.RotateNoneFlipNone)
					graphicsData.add(buildGraphicsInfo(graphics.createImage(), CardSide.Front, PrintType.Overlay))
					graphics.clear()
				}
				else if (frontSideImageUriMap.containsKey(PrintType.Overlay))
				{
					graphicsData.add(buildGraphicsInfo(null, CardSide.Front, PrintType.Overlay))
					graphics.clear()
				}
				if (backSideImageData != null)
				{
					graphics.initialize(0, 0, OrientationType.Landscape, PrintType.MonoK, Color.WHITE)
					graphics.drawImage(backSideImageData, 0, 0, 0, 0, RotationType.RotateNoneFlipNone)
					graphicsData.add(buildGraphicsInfo( graphics.createImage(), CardSide.Back, PrintType.MonoK))
					graphics.clear()
				}
				cardSource = CardSource.fromString(
					zebraCardPrinter.getJobSettingValue(ZebraCardJobSettingNames.CARD_SOURCE)
				)
				jobId = zebraCardPrinter.print(printOptions.getQuantity(), graphicsData)
			}
		}
		catch (e: Exception)
			{ exception = e }
		finally
		{
			graphics?.close()
			ConnectionHelper.cleanUpQuietly(zebraCardPrinter, connection)
		}
		if (onSendPrintJobListener != null)
			onSendPrintJobListener!!.onSendPrintJobFinished(exception, jobId, cardSource)
	}

	@Throws(IOException::class)
	private fun buildGraphicsInfo(zebraCardImage: ZebraCardImageI?, side: CardSide, printType: PrintType?): GraphicsInfo
	{
		val graphicsInfo = GraphicsInfo()
		if (zebraCardImage != null)
		{
			graphicsInfo.graphicData = zebraCardImage
			graphicsInfo.graphicType = GraphicType.BMP
		}
		else
			graphicsInfo.graphicType = GraphicType.NA
		graphicsInfo.side = side
		graphicsInfo.printType = printType
		return graphicsInfo
	}

	fun setOnSendPrintJobListener(onSendPrintJobListener: OnSendPrintJobListener?)
		{ this.onSendPrintJobListener = onSendPrintJobListener }
//
//	fun setOnSendPrintJobListener(onSendPrintJobListener: OnSendPrintJobListener) {
//
//	}

//	class OnSendPrintJobListener : PrinterHelper.OnPrinterReadyListener
//	{
//		public lateinit var onSendPrintJobStarted: () -> Unit
//		public lateinit var onSendPrintJobFinished: (exception: Exception?, jobId: Int?, cardSource: CardSource?) -> Unit
//		override fun onPrinterReadyUpdate(message: String?, showDialog: Boolean) {
//			TODO("Not yet implemented")
//		}
//	}
}
