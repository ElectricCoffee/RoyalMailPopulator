import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

        exportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        removeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] lines = PdfProcessor.INSTANCE.stripLines("C:\\Users\\Electric Coffee\\Downloads\\order-1958-proof-of-postage.pdf");
                for (String line : lines) {
                    System.out.println(line);
                }
            }
        });
    }
}
