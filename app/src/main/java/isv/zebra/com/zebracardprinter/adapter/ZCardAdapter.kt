package isv.zebra.com.zebracardprinter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.adapter.ZCardAdapter.ZCardViewHolder
import isv.zebra.com.zebracardprinter.model.ZCard


class ZCardAdapter(private var zCards: List<ZCard>): RecyclerView.Adapter<ZCardViewHolder>()
{
	private var mOnClickZCardListener = OnClickZCardListener()

	// Find card layout to get its params
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZCardViewHolder
    {
	    return ZCardViewHolder(LayoutInflater.from(parent.context)
		    .inflate(R.layout.item_zcard, parent, false))
		        .apply{ this.onClickZCardListener = mOnClickZCardListener }
    }

    // Configures card view
    override fun onBindViewHolder(holder: ZCardViewHolder, position: Int)
    {
        val card = zCards[position]
        val cardBgV: ImageView = holder.card
        cardBgV.setBackgroundResource(card.idBackground)
    }

    override fun getItemCount(): Int
        = zCards.size

	fun setOnClickZCardListener(@Nullable l: (Int) -> Unit)
		{ mOnClickZCardListener.onClick = l }

	class OnClickZCardListener
		{ lateinit var onClick: (post: Int) -> Unit }

	// get params from card layout
	class ZCardViewHolder(itemView: View) : ViewHolder(itemView)
	{
		lateinit var onClickZCardListener: OnClickZCardListener
		val card: ImageView = itemView.findViewById(R.id.card_bg)

		init
			{ card.setOnClickListener { onClickZCardListener.onClick(adapterPosition) } }
	}
}