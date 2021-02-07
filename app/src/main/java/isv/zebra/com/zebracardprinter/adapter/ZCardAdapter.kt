package isv.zebra.com.zebracardprinter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.adapter.ZCardAdapter.ZCardViewHolder
import isv.zebra.com.zebracardprinter.model.ZCard

class ZCardAdapter(private val zcards: List<ZCard>): RecyclerView.Adapter<ZCardViewHolder>()
{
    // get params from card layout
    class ZCardViewHolder(itemView: View) : ViewHolder(itemView)
    {
        val card: ImageView = itemView.findViewById(R.id.card_bg)
        //val nameTextView = itemView.findViewById<TextView>(R.id.__)
        // @TODO("Add missing params")
    }

    // Find card layout to get its params
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZCardViewHolder
    {
        val ctx = parent.context
        val inflater = LayoutInflater.from(ctx)
        val cardView = inflater.inflate(R.layout.item_zcard, parent, false)
        return ZCardViewHolder(cardView)
    }

    override fun getItemCount(): Int =
        zcards.size

    // Configures card view
    override fun onBindViewHolder(holder: ZCardViewHolder, position: Int)
    {
        val card = zcards[position]
        val cardBg: ImageView = holder.card
        cardBg.setBackgroundResource(card.idBackground)
        //println("cardView.width"+cardView.width)
        //cardView.layoutParams = ConstraintLayout.LayoutParams( cardView.width, (cardView.width/1.58577251).toInt())

        /* onBindViewHolder SAMPLE:
        val textView = viewHolder.nameTextView
        textView.setText(contact.name)
        val button = viewHolder.messageButton
        button.text = if (contact.isOnline) "Message" else "Offline"
        button.isEnabled = contact.isOnline
         */
    }
}