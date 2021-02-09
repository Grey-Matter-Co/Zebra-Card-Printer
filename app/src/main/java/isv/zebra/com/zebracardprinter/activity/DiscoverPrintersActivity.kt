package isv.zebra.com.zebracardprinter.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import isv.zebra.com.zebracardprinter.R
import isv.zebra.com.zebracardprinter.adapter.ZPrinterAdapter
import isv.zebra.com.zebracardprinter.model.ZPrinter

class DiscoverPrintersActivity : AppCompatActivity()
{
    lateinit var zPrinters: ArrayList<ZPrinter>
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover_printers)
        setSupportActionBar(findViewById(R.id.toolbar))
        initComponents()
    }

    private fun initComponents()
    {
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // Setting RecyclerView w/ its [Printer]s
        zPrinters = ZPrinter.createPrintersList(5, "ZPXT12")
        val recyclerView = findViewById<RecyclerView>(R.id.recView_zprinters)
        recyclerView.adapter = ZPrinterAdapter(zPrinters)
            .apply {
                this.setOnClickZPrinterListener { pos ->
                    Snackbar.make( findViewById<RecyclerView>(R.id.recView_zprinters).getChildAt(pos), "U clicked the ${pos+1} one", Snackbar.LENGTH_LONG )
                        .setAction("Action", null).show()
                }
        }
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }
}