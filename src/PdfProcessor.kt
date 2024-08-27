import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.text.SimpleDateFormat
import java.util.*

object PdfProcessor {
    /**
     * Opening class to setup File Watcher
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    fun run(watchDir: String?, viewerExec: String?) {
        // Get string from args if set

        var watchDir = watchDir
        var viewerExec = viewerExec
        watchDir = watchDir ?: "E:\\Downloads"
        viewerExec = viewerExec ?: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"

        // Create watch service to monitor download folder
        val watchService = FileSystems.getDefault().newWatchService()

        // Set download folder e:downloads, this may need to be changed
        val path = Paths.get(watchDir)

        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        println("Monitoring download folder [$watchDir] for pdfs")

        var poll = true
        while (poll) {
            val key = watchService.take()

            for (event in key.pollEvents()) {
                if (event.kind() === StandardWatchEventKinds.ENTRY_CREATE && event.context().toString()
                        .endsWith(".pdf")
                ) {
                    println("Processing event kind : " + event.kind() + " - File : " + event.context())

                    //bingo we have a pdf try and process it
                    processPDF(watchDir + "\\" + event.context(), viewerExec)
                } else {
                    println("Ignoring event kind : " + event.kind() + " - File : " + event.context())
                }
            }
            poll = key.reset()
        }
    }

    /**
     * Uses pdfBox to open a Royal Mail PDF and extract text from the shipping label.
     * Once this is done it will then open the Royal Mail proof of postage pdf and
     * populate the boxes with the text stripped from the shipping labels.
     * @param filename is the file that it will attempt to process
     * @param viewerExec is the location of the pdf viewer
     */
    fun processPDF(filename: String, viewerExec: String) {
        try {
            println("Attempting to process $filename")
            println("Working folder " + System.getProperty("user.dir"))

            // using openPdf will load the pdf that is hopefully a Royal Mail shipping label.
            val docLoad = PDDocument.load(File(filename))
            val text = PDFTextStripper().getText(docLoad)


            var output = ""

            //will loop though the text hoping to find the text 'Postage Paid GB'
            val lines = text.split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in 0..(lines.size - 1)) {
                if (!lines[i].contains("Postage Paid GB")) {
                    continue
                }

                // found postage page text, will now get the name , address and tracking number
                System.out.printf("Processing [%s]", lines[i + 4])

                output = output +
                        lines[i + 4] + "¬" +
                        lines[i + 5] + " " + lines[i + 6] + " " + lines[i + 7] + " " + lines[i + 8] + " " + lines[i + 9] + "¬" +
                        lines[i - 2] + " " + lines[i + 3].replace(" ", "").replace("-", "") + System.lineSeparator()

            }

            // output file is empty to will end early as there is nothing to add to proof of postage
            if (output.isEmpty()) {
                println("Output file is empty, exiting early")
                return
            }

            // Will now attempt to add the output file to 1 or more proof of postage

            // If there is over 30 items it will loop thought creating the proof of
            // postage pdf a number of times as it will only hold 30 items
            val outputs = output.split(System.lineSeparator().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var p = 0
            while (p <= outputs.size - 1) {
                // Load Royal Mail proof of postage pdf ready to repopulate!
                val file = File(System.getProperty("user.dir") + "\\Royal Mail Proof Of Postage.pdf")
                val pdfTemplate = PDDocument.load(file)
                val docCatalog = pdfTemplate.documentCatalog
                val acroForm = docCatalog.acroForm

                // populate the forms from the pdf text stored in the outputs string array
                var countForNumberofItems = 0
                for (i in p..(outputs.size - 1)) {
                    if (i > p + 29) {
                        println("Over 30 items exiting early!")
                        break
                    }

                    val line = outputs[i].split("¬".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    if ((i - p) == 0) {
                        acroForm.getField("1").setValue(line[0])
                        acroForm.getField("my text here").setValue(line[1])
                        acroForm.getField("service used 1").setValue(line[2])
                    } else {
                        acroForm.getField("" + ((i - p) + 1)).setValue(line[0])
                        acroForm.getField("address and postcode " + ((i - p) + 1)).setValue(line[1])
                        acroForm.getField("service used " + ((i - p) + 1)).setValue(line[2])
                    }

                    println("Content added [" + line[0] + "," + line[1] + "," + line[2] + "]")
                    countForNumberofItems++
                }

                // add date and number of items
                acroForm.getField("Text57").setValue("$countForNumberofItems items")
                acroForm.getField("Text58").setValue(SimpleDateFormat("dd/MM/yyyy").format(Date()))

                // Save populated pdf file with unique guid filename
                val uniqueFilename = UUID.randomUUID().toString() + ".pdf"
                pdfTemplate.save(System.getProperty("user.dir") + "\\" + uniqueFilename)
                pdfTemplate.close()

                //open file in chrome
                println("Opening pdf with exec [" + viewerExec + " " + System.getProperty("user.dir") + "\\" + uniqueFilename + "]")
                Runtime.getRuntime().exec(viewerExec + " " + System.getProperty("user.dir") + "\\" + uniqueFilename)

                p += 30
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}