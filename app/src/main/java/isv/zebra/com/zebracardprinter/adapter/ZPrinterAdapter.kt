package isv.zebra.com.zebracardprinter.adapter

import android.util.Log
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
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.model.ZPrinter

class ZPrinterAdapter(private var zPrinters: ArrayList<ZPrinter>): RecyclerView.Adapter<ZPrinterAdapter.ZPrinterViewHolder>()
{
	private var mOnClickZPrinterListener = OnClickZPrinterListener()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZPrinterViewHolder
	{
		return ZPrinterViewHolder(
			LayoutInflater.from(parent.context)
				.inflate(R.layout.item_zprinter, parent, false)
		)
				.apply{ this.onClickZPrinterListener = mOnClickZPrinterListener }
	}

	override fun onBindViewHolder(holder: ZPrinterViewHolder, position: Int)
	{
		val printer = zPrinters[position]
		val cardV = holder.card
		val modelV = holder.model
		val referenceV = holder.reference

		modelV.text = printer.model
		referenceV.text = printer.references
		if (cardV.layoutParams is MarginLayoutParams)
		{
			val margin = cardV.marginLeft
			Log.d("MindBlast->", "onBindViewHolder: ${position%2}")
			if (position%2==0)
				(cardV.layoutParams as MarginLayoutParams).setMargins((margin*1.5).toInt(), cardV.marginTop, cardV.marginRight, cardV.marginBottom)
			else
				(cardV.layoutParams as MarginLayoutParams).setMargins(cardV.marginLeft, cardV.marginTop, cardV.marginRight, cardV.marginBottom)
			cardV.requestLayout()
		}
	}

	override fun getItemCount(): Int
		= zPrinters.size

	fun setOnClickZPrinterListener(@Nullable listener: (Int) -> Unit)
		{ mOnClickZPrinterListener.onClick = listener }

	class OnClickZPrinterListener
		{ var onClick: ((post: Int) -> Unit)? = null }

	// get params from card layout
	class ZPrinterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
	{
		lateinit var onClickZPrinterListener: OnClickZPrinterListener
		val card: CardView = itemView.findViewById(R.id.printer_card)
		val icon: ImageView = itemView.findViewById(R.id.printer_icon)
		val model: TextView = itemView.findViewById(R.id.printer_model)
		val reference: TextView = itemView.findViewById(R.id.printer_reference)

		init
			{  card.setOnClickListener { onClickZPrinterListener.onClick?.let { it1 -> it1(adapterPosition) } } }
	}
}
