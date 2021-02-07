package isv.zebra.com.zebracardprinter.model

class ZCard (val idBackground: Int)
{
    companion object
    {
        fun createCardsList(numCards: Int, idBg: Int): ArrayList<ZCard>
        {
            val cards = ArrayList<ZCard>()
            for (i in 1..numCards)
                cards.add(ZCard(idBg))
            return cards
        }
    }
}