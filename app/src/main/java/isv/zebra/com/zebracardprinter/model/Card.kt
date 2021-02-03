package isv.zebra.com.zebracardprinter.model

class Card (val idBackground: Int)
{
    companion object
    {
        fun createCardsList(numCards: Int, idBg: Int): ArrayList<Card>
        {
            val cards = ArrayList<Card>()
            for (i in 1..numCards)
                cards.add(Card(idBg))
            return cards
        }
    }
}