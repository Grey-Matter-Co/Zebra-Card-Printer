package isv.zebra.com.zebracardprinter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import isv.zebra.com.zebracardprinter.adapter.CardAdapter
import isv.zebra.com.zebracardprinter.model.Card


class MainActivity: AppCompatActivity()
{
    lateinit var cards: ArrayList<Card>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initomponents()
    }

    fun initomponents()
    {
        cards = Card.createCardsList(10, R.drawable.image_12)
        val recyclerView = findViewById<RecyclerView>(R.id.recView_cards)
        val adapter = CardAdapter(cards)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

    }
}
