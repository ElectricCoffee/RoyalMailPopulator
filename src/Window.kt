import javax.swing.JButton
import javax.swing.JFrame

class Window : JFrame("Royal Mail Populator") {
    init {
        setSize(600, 400)
        isResizable = false
        setLocationRelativeTo(null)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val button = JButton("Export Certificate")
        add(button)
    }
}