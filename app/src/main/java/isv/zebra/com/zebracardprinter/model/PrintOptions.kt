package isv.zebra.com.zebracardprinter.model

import android.net.Uri
import com.zebra.sdk.common.card.enumerations.PrintType

class PrintOptions(private val frontSideImageUriMap: MutableMap<PrintType, Uri?>, private val backSideImageUriMap: MutableMap<PrintType, Uri?>, private val quantity: Int)
{
	fun getFrontSideImageUriMap(): MutableMap<PrintType, Uri?>
		= frontSideImageUriMap

	fun getBackSideImageUriMap(): MutableMap<PrintType, Uri?>
		= backSideImageUriMap

	fun getQuantity(): Int
		= quantity
}