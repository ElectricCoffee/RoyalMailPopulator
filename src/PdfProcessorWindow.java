import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Vector;

public class PdfProcessorWindow extends JFrame {
    private JButton removeBtn;
    private JButton addBtn;
    private JButton exportBtn;
    private JTable pdfTable;
    private JPanel MainPanel;
    private JScrollPane scrollPane;

    public PdfProcessorWindow() {
        super("Royal Mail PDF Processor");
        setContentPane(MainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        DefaultTableModel dtm = new DefaultTableModel(null, new Object[]{"Name", "Address & Postcode", "Service"});
        pdfTable.setModel(dtm);

        /* Export */
        exportBtn.addActionListener(e -> {

        });

        /* Remove */
        removeBtn.addActionListener(e -> {
            int[] rows = pdfTable.getSelectedRows();

            // iterate in reverse to delete due to a quirk in Swing.
            for (int i = rows.length - 1; i >= 0; i--) {
                dtm.removeRow(rows[i]);
            }
        });

        /* Add */
        addBtn.addActionListener(e -> {
            PdfFileChooser chooser = new PdfFileChooser();
            int dialogResult = chooser.showOpenDialog(this);

            if (dialogResult != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFile = chooser.getSelectedFile();
            String path = selectedFile.getPath();

            System.out.println(path);

            String[] lines = PdfProcessor.INSTANCE.stripLines(path);
            Vector<UserData> result = PdfProcessor.INSTANCE.genOutput(lines);

//            DefaultTableModel model = (DefaultTableModel) pdfTable.getModel();

            for (UserData datum : result) {
                dtm.addRow(datum.toArray());
            }
        });
    }
}
