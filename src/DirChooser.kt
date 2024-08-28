import javax.swing.JFileChooser

class DirChooser : JFileChooser() {
    init {
        setFileSelectionMode(DIRECTORIES_ONLY)
        approveButtonText = "Select"
        dialogTitle = "Choose a Folder to Save To"
    }
}