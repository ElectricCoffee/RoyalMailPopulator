import java.io.File
import javax.swing.filechooser.FileFilter
import java.util.*

class PdfFileFilter : FileFilter() {
    override fun accept(pathname: File): Boolean {
        return if (pathname.isDirectory()) {
            true;
        } else {
            pathname.name.lowercase(Locale.getDefault()).endsWith(".pdf")
        }
    }

    override fun getDescription(): String {
        return "PDF Documents (*.pdf)"
    }
}