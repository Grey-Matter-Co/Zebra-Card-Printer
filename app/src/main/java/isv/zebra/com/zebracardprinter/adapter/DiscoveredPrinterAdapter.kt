package isv.zebra.com.zebracardprinter.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.cardview.widget.CardView
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView
import com.zebra.sdk.common.card.printer.discovery.DiscoveredCardPrinterNetwork
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb
import isv.zebra.com.zebracardprinter.R
import kotlin.math.roundToInt

class DiscoveredPrinterAdapter(private var zPrinters: ArrayList<DiscoveredPrinter>): RecyclerView.Adapter<DiscoveredPrinterAdapter.DiscoveredPrinterViewHolder>()
{
	private var mOnClickDiscoveredPrinterListener = OnClickDiscoveredPrinterListener()
	lateinit var context: Context

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoveredPrinterViewHolder
		= DiscoveredPrinterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_zprinter, parent, false))
				.apply{ this.onClickDiscoveredPrinterListener = mOnClickDiscoveredPrinterListener }

	override fun onBindViewHolder(holder: DiscoveredPrinterViewHolder, position: Int)
	{
		val printer = zPrinters[position]
		val model = printer.discoveryDataMap["MODEL"]
		val address = printer.discoveryDataMap["ADDRESS"]
		val cardV = holder.card
		val iconV = holder.icon
		val modelV = holder.model
		val addressV = holder.reference

		modelV.visibility   = if (model != null && model.isNotEmpty()) View.VISIBLE else View.GONE
		addressV.visibility = if (address != null && address.isNotEmpty()) View.VISIBLE else View.GONE

		modelV.text = model
		addressV.text = address

		iconV.setImageResource(when (printer)
		{
			is DiscoveredPrinterUsb ->
				R.drawable.ic_usb
			is DiscoveredPrinterBluetooth ->
				R.drawable.ic_bluetooth
			is DiscoveredCardPrinterNetwork ->
				R.drawable.ic_wifi
			else -> R.drawable.ic_no_printer
		})

		// To fix view margin with layout
		if (cardV.layoutParams is MarginLayoutParams)
		{
			val marginLateral   = cardV.marginLeft
			var marginTop       = cardV.marginTop
			var marginBottom    = cardV.marginBottom
			val toAdd: Int      = context.resources.getDimension(R.dimen.spacing_middle).roundToInt()

			// Add margin to first 2 elements and for last 2|1 elements
			if (position<2)
				marginTop += toAdd
			else if (zPrinters.size < position+ if(zPrinters.size%2==0)1 else 2)
				marginBottom += toAdd

			when
			{
				position%2==0 -> (cardV.layoutParams as MarginLayoutParams).setMargins(marginLateral*2, marginTop, cardV.marginRight, marginBottom)
				else -> (cardV.layoutParams as MarginLayoutParams).setMargins(cardV.marginLeft, marginTop, cardV.marginRight, marginBottom)
			}
			if (position%2==0)

			cardV.requestLayout()
		}
	}

	override fun getItemCount(): Int
		= zPrinters.size

	fun setOnClickDiscoveredPrinterListener(@Nullable listener: (Int) -> Unit)
		{ mOnClickDiscoveredPrinterListener.onClick = listener }

	fun getIndexWithAddress(address: String): Int
	{
		for (i in 0 until zPrinters.size)
		{
			if (address == zPrinters[i].discoveryDataMap["ADDRESS"])
				return i
		}
		return -1
	}

	class OnClickDiscoveredPrinterListener
		{ lateinit var onClick: ((post: Int) -> Unit) }

	// get params from card layout
	class DiscoveredPrinterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
	{
		lateinit var onClickDiscoveredPrinterListener: OnClickDiscoveredPrinterListener
		val card: CardView = itemView.findViewById(R.id.printer_card)
		val icon: ImageView = itemView.findViewById(R.id.printer_icon)
		val model: TextView = itemView.findViewById(R.id.printer_model)
		val reference: TextView = itemView.findViewById(R.id.printer_reference)

		init
			{  card.setOnClickListener { onClickDiscoveredPrinterListener.onClick(adapterPosition) } }
	}
}
