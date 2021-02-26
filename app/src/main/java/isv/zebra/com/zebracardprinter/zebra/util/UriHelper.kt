package isv.zebra.com.zebracardprinter.zebra.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.util.*

object UriHelper
{
	fun getFilename(context: Context, uri: Uri?): String?
	{
		var result: String? = null
		if (uri != null) {
			if (uri.scheme == "content")
			{
				val cursor = context.contentResolver.query(uri, null, null, null, null)
				cursor.use { cursor ->
					if (cursor != null && cursor.moveToFirst())
						result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
				}
			}
			if (result == null)
			{
				result = uri.path
				val cut = result!!.lastIndexOf('/')
				if (cut != -1)
					result = result!!.substring(cut + 1)
			}
		}
		return result
	}

	private fun getFileExtension(context: Context, uri: Uri): String
	{
		val filename = getFilename(context, uri)
		return FilenameUtils.getExtension(filename)
	}

	fun isXmlFile(context: Context, uri: Uri): Boolean
	{
		val extension = getFileExtension(context, uri)
		return extension.toLowerCase(Locale.ROOT) == "xml"
	}

	@Throws(IOException::class)
	fun getByteArrayFromUri(context: Context, uri: Uri?): ByteArray?
	{
		val contentResolver = context.contentResolver
		val inputStream = contentResolver.openInputStream(uri!!)
		return if (inputStream != null)
		{
			try
				{ IOUtils.toByteArray(inputStream) }
			finally
				{ IOUtils.closeQuietly(inputStream) }
		}
		else
			null
	}
}
