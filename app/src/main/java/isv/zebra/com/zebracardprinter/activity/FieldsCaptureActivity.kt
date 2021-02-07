package isv.zebra.com.zebracardprinter.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import isv.zebra.com.zebracardprinter.R

class FieldsCaptureActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        // No se pone entrecomillado porque no es un texto o una ruta. La clase R indexa todos los elementos con numeros que son sus identificadores
        setContentView(R.layout.activity_fields_capture)
    }
}