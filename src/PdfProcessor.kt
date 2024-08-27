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

        val watchDir = watchDir ?: "E:\\Downloads"
        val viewerExec = viewerExec ?: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"

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
                if (event.kind() !== StandardWatchEventKinds.ENTRY_CREATE || !event.context().toString()
                        .endsWith(".pdf")
                ) {
                    println("Ignoring event kind : ${ event.kind() } - File : ${ event.context() }")
                    break
                }

                println("Processing event kind : ${event.kind()} - File : ${event.context()}")

                //bingo we have a pdf try and process it
                processPDF("$watchDir\\${event.context()}", viewerExec)
            }
            poll = key.reset()
        }
    }

    /**
     * Opens a PDF and strips out the individual lines of text
     */
    fun stripLines(fileName: String): Array<String> {
        // using openPdf will load the pdf that is hopefully a Royal Mail shipping label.
        val docLoad = PDDocument.load(File(fileName))
        val text = PDFTextStripper().getText(docLoad)

        //will loop though the text hoping to find the text 'Postage Paid GB'
        return text.split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    /**
     * Locates the relevant part of the PDF and generates an output string
     */
    fun genOutput(lines: Array<String>): String {
        var output = ""
        for (i in lines.indices) {
            if (!lines[i].contains("Postage Paid GB")) {
                continue
            }

            // found postage page text, will now get the name , address and tracking number
            System.out.printf("Processing [%s]", lines[i + 4])

            // TODO: fix this mess
            val name = lines[i + 4]
            val address = lines[i + 5] + " " + lines[i + 6] + " " + lines[i + 7] + " " + lines[i + 8] + " " + lines[i + 9]
            val trackingNo = lines[i - 2] + " " + lines[i + 3].replace(" ", "").replace("-", "")

            output += "$name¬$address¬$trackingNo" + System.lineSeparator()
        }

        return output
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
            println("Working folder ${System.getProperty("user.dir")}")

            val lines = stripLines(filename)
            val output = genOutput(lines)

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
                var countForNumberOfItems = 0
                for (i in p..<outputs.size) {
                    if (i > p + 29) {
                        println("Over 30 items exiting early!")
                        break
                    }

                    val (name, address, service) = outputs[i].split("¬".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    acroForm.run {
                        if ((i - p) == 0) {
                            getField("1").setValue(name)
                            getField("my text here").setValue(address)
                            getField("service used 1").setValue(service)
                        } else {
                            getField("" + ((i - p) + 1)).setValue(name)
                            getField("address and postcode " + ((i - p) + 1)).setValue(address)
                            getField("service used " + ((i - p) + 1)).setValue(service)
                        }
                    }

                    println("Content added [$name,$address,$service]")
                    countForNumberOfItems++
                }

                // add date and number of items
                acroForm.getField("Text57").setValue("$countForNumberOfItems items")
                acroForm.getField("Text58").setValue(SimpleDateFormat("dd/MM/yyyy").format(Date()))

                // Save populated pdf file with unique guid filename
                val uniqueFilename = UUID.randomUUID().toString() + ".pdf"
                val path = "${System.getProperty("user.dir")}\\$uniqueFilename"
                pdfTemplate.save(path)
                pdfTemplate.close()

                //open file in chrome
                println("Opening pdf with exec [$viewerExec $path]")
                Runtime.getRuntime().exec("$viewerExec $path")

                p += 30
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}