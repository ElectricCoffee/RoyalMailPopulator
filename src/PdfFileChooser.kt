import java.io.File
import javax.swing.JFileChooser

class PdfFileChooser : JFileChooser() {
    init {
        val ff = PdfFileFilter()
        addChoosableFileFilter(ff)
        fileFilter = ff
        currentDirectory = File(System.getProperty("user.home"))
    }
}