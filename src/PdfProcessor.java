import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static java.nio.file.StandardWatchEventKinds.*;

public class PdfProcessor {
    /**
     * Opening class to setup File Watcher
     * @throws IOException
     * @throws InterruptedException
     */
    public static void run(String watchDir, String viewerExec) throws IOException, InterruptedException {

        // Get string from args if set
        watchDir = watchDir == null ? "E:\\Downloads" : watchDir;
        viewerExec =  viewerExec == null ? "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe" : viewerExec;

        // Create watch service to monitor download folder
        WatchService watchService = FileSystems.getDefault().newWatchService();

        // Set download folder e:downloads, this may need to be changed
        Path path = Paths.get(watchDir);

        path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        System.out.println("Monitoring download folder ["+watchDir+"] for pdfs");

        boolean poll = true;
        while (poll) {

            WatchKey key = watchService.take();

            for (WatchEvent<?> event : key.pollEvents()) {

                if (event.kind() == ENTRY_CREATE && event.context().toString().endsWith(".pdf")) {
                    System.out.println("Processing event kind : " + event.kind() + " - File : " + event.context());

                    //bingo we have a pdf try and process it
                    processPDF(watchDir + "\\" + event.context(), viewerExec);
                } else {
                    System.out.println("Ignoring event kind : " + event.kind() + " - File : " + event.context());
                }
            }
            poll = key.reset();
        }
    }

    /**
     * Uses pdfBox to open a Royal Mail PDF and extract text from the shipping label.
     * Once this is done it will then open the Royal Mail proof of postage pdf and
     * populate the boxes with the text stripped from the shipping labels.
     * @param filename is the file that it will attempt to process
     * @param viewerExec is the location of the pdf viewer
     */
    public static void processPDF(String filename, String viewerExec) {
        try {
            System.out.println("Attempting to process " + filename);
            System.out.println("Working folder " + System.getProperty("user.dir"));

            // using openPdf will load the pdf that is hopefully a Royal Mail shipping label.
            PDDocument docLoad = PDDocument.load(new File(filename));
            String text = new PDFTextStripper().getText(docLoad);;

            String output = "";

            //will loop though the text hoping to find the text 'Postage Paid GB'
            String[] lines = text.split("\r\n");

            for (int i = 0; i <= lines.length - 1; i++) {
                if (lines[i].contains("Postage Paid GB")) {
                    // found postage page text, will now get the name , address and tracking number
                    System.out.printf("Processing [%s]", lines[i+4]);

                    output  = output +
                            lines[i + 4] + "¬" +
                            lines[i+5] + " " + lines[i+6] +" " + lines[i+7] +" " + lines[i+8] + " " + lines[i+9] + "¬" +
                            lines[i-2] + " " + lines[i+3].replace(" ","").replace("-","") + System.lineSeparator();
                }
            }

            // output file is empty to will end early as there is nothing to add to proof of postage
            if (output.isEmpty()) {
                System.out.println("Output file is empty, exiting early");
                return;
            }

            // Will now attempt to add the output file to 1 or more proof of postage

            // If there is over 30 items it will loop thought creating the proof of
            // postage pdf a number of times as it will only hold 30 items
            String[] outputs = output.split(System.lineSeparator());
            for (int p = 0; p <= outputs.length - 1; p = p + 30) {

                // Load Royal Mail proof of postage pdf ready to repopulate!
                File file = new File(System.getProperty("user.dir") + "\\Royal Mail Proof Of Postage.pdf");
                PDDocument pdfTemplate = PDDocument.load(file);
                PDDocumentCatalog docCatalog = pdfTemplate.getDocumentCatalog();
                PDAcroForm acroForm = docCatalog.getAcroForm();

                // populate the forms from the pdf text stored in the outputs string array
                int countForNumberofItems = 0;
                for (int i = p; i <= outputs.length - 1; i++) {

                    if (i > p + 29) {
                        System.out.println("Over 30 items exiting early!");
                        break;
                    }

                    String[] line = outputs[i].split("¬");

                    if ((i-p) == 0) {
                        acroForm.getField("1").setValue(line[0]);
                        acroForm.getField("my text here").setValue(line[1]);
                        acroForm.getField("service used 1").setValue(line[2]);
                    } else {
                        acroForm.getField("" + ((i-p) + 1)).setValue(line[0]);
                        acroForm.getField("address and postcode " + ((i-p) + 1)).setValue(line[1]);
                        acroForm.getField("service used " + ((i-p) + 1)).setValue(line[2]);
                    }

                    System.out.println("Content added [" + line[0] + "," + line[1] + "," + line[2] + "]");
                    countForNumberofItems++;
                }

                // add date and number of items
                acroForm.getField("Text57").setValue(countForNumberofItems + " items");
                acroForm.getField("Text58").setValue(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

                // Save populated pdf file with unique guid filename
                String uniqueFilename = UUID.randomUUID() + ".pdf";
                pdfTemplate.save(System.getProperty("user.dir") + "\\" + uniqueFilename);
                pdfTemplate.close();

                //open file in chrome

                System.out.println("Opening pdf with exec [" + viewerExec + " " + System.getProperty("user.dir") + "\\" + uniqueFilename + "]");
                Runtime.getRuntime().exec(viewerExec + " " + System.getProperty("user.dir") + "\\" + uniqueFilename);

            } // will loop again if over 30 items
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
}