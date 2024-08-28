import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.Vector;

public class PdfProcessorWindow extends JFrame {
    private JButton removeBtn;
    private JButton addBtn;
    private JButton exportBtn;
    private JTable pdfTable;
    private JPanel MainPanel;
    private JScrollPane scrollPane;
    private JLabel label;

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
            if (userData.isEmpty()) {
                return;
            }

            DirChooser dc = new DirChooser();

            if (dc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedDir = dc.getSelectedFile();
            String path = selectedDir.getPath();

            try {
                PdfProcessor.INSTANCE.processPdf(userData, path);
                // delete all rows after saving the pdf
                for (int i = rowCount - 1; i >= 0; i--) {
                    dtm.removeRow(i);
                }
                label.setText("Document saved to " + path);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        ex.getMessage(),
                        ex.getClass().getCanonicalName(),
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        /*
         * Remove Button Listener
         *
         * Removes the selected items.
         * */
        removeBtn.addActionListener(e -> {
            int[] rows = pdfTable.getSelectedRows();

            // iterate in reverse to delete due to a quirk in Swing.
            for (int i = rows.length - 1; i >= 0; i--) {
                dtm.removeRow(rows[i]);
            }
        });

        /*
         * Add Button Listener
         *
         * Opens a dialogue window which lets you select a pdf for import.
         * If the pdf contains anything usable, it will populate the table with the data.
         * */
        addBtn.addActionListener(e -> {
            PdfFileChooser chooser = new PdfFileChooser();
            int dialogResult = chooser.showOpenDialog(this);

            if (dialogResult != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFile = chooser.getSelectedFile();
            String path = selectedFile.getPath();

            populateTableFromPath(path, dtm);
        });

        /* Listens for Table Changes */
        dtm.addTableModelListener(e -> {
            int rowCount = dtm.getRowCount();
            System.out.printf("pdf table updated, row count %d\n", rowCount);
            exportBtn.setEnabled(rowCount > 0);
            label.setText(rowCount + " Items Loaded");
        });

        /* Listens for drag-and-drop events */
        new DropTarget(this, new DropTargetListener() {
            @Override public void dragEnter(DropTargetDragEvent dtde) {}

            @Override public void dragOver(DropTargetDragEvent dtde) {}

            @Override public void dropActionChanged(DropTargetDragEvent dtde) {}

            @Override public void dragExit(DropTargetEvent dte) {}

            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                try {
                    // Retrieve the dropped data
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        // Get the list of files dropped
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                        for (File file : droppedFiles) {
                            // Only process PDF files
                            if (file.getName().toLowerCase().endsWith(".pdf")) {
                                // Get the file path and use it
                                String filePath = file.getAbsolutePath();
                                populateTableFromPath(filePath, dtm);
                            } else {
                                JOptionPane.showMessageDialog(
                                        null,
                                        "Only PDF files are accepted.",
                                        "Incorrect file type",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            ex.getMessage(),
                            ex.getClass().getCanonicalName(),
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    dtde.dropComplete(true);
                }
            }
        });
    }

    /**
     * Populates the table with data loaded from the certificate
     * @param path The path to the input pdf
     * @param dtm The table to be populated
     */
    void populateTableFromPath(String path, DefaultTableModel dtm) {
        System.out.println(path);

        String[] lines = PdfProcessor.INSTANCE.stripLines(path);
        Vector<UserData> result = PdfProcessor.INSTANCE.genOutput(lines);

        for (UserData datum : result) {
            dtm.addRow(datum.toArray());
        }

        if (dtm.getRowCount() < 1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Did not find any address information.\n" +
                            "Are you sure you got the right file?",
                    "Data Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
