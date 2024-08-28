import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
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
        exportBtn.setEnabled(false); // initially this has to be disabled until the listener re-enables it

        DefaultTableModel dtm = new DefaultTableModel(null, new Object[]{"Name", "Address & Postcode", "Service"});
        pdfTable.setModel(dtm);

        /* Export */
        exportBtn.addActionListener(e -> {
            int rowCount = pdfTable.getRowCount();

            Vector<UserData> userData = new Vector<>();

            for (int i = 0; i < rowCount; i++) {
                String name = (String) dtm.getValueAt(i, 0);
                String addr = (String) dtm.getValueAt(i, 1);
                String serv = (String) dtm.getValueAt(i, 2);
                userData.add(new UserData(name, addr, serv));
            }

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

            for (UserData datum : result) {
                dtm.addRow(datum.toArray());
            }
        });

        dtm.addTableModelListener(e -> {
            System.out.printf("pdf table updated, row count %d\n", dtm.getRowCount());
            exportBtn.setEnabled(dtm.getRowCount() > 0);
        });
    }
}
