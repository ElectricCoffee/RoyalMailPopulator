import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

enum class FetchMode {
    None, Name, Address, Service
}

object PdfProcessor {
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
    fun genOutput(lines: Array<String>): Vector<UserData> {
        val output = Vector<UserData>()
        var isCert = false
        var fetchMode = FetchMode.None

        val names = Vector<String>()
        val addresses = Vector<String>()
        var addressBuffer = Vector<String>()
        val services = Vector<String>()

        // sketchy state machine time, woo!
        for (line in lines) {
            if (line.contains("Certificate of Posting for Online Postage")) {
                isCert = true
            }

            // ignore everything unless we've hit the Certificate of Posting for Online Postage line
            if (!isCert) {
                continue;
            }

            // If the state machine is in the default state and it sees "Name & Address",
            // we know the next line will be the name
            if (fetchMode == FetchMode.None && line.contains("Name & Address")) {
                fetchMode = FetchMode.Name
                continue
            }

            // After the name has been read, the address comes next
            if (fetchMode == FetchMode.Name) {
                println("Processing [$line]")
                names.add(line)
                fetchMode = FetchMode.Address
                continue
            }

            // If we're in address mode and the line read is "Service Used" we know the address part is over
            // NOTE: this breaks if for some reason someone has "Service Used" in their address...
            if (fetchMode == FetchMode.Address && line.contains("Service Used")) {
                fetchMode = FetchMode.Service
                continue
            }

            // If the line wasn't "Service Used", then we copy that line to the address buffer
            if (fetchMode == FetchMode.Address) {
                // do stuff
                addressBuffer.add(line)
                continue
            }

            // The service is just one line of text (I assume),
            // so it should be safe to return the state machine to the default state
            if (fetchMode == FetchMode.Service) {
                addresses.add(addressBuffer.joinToString(", "))
                addressBuffer = Vector()
                isCert = false
                fetchMode = FetchMode.None
                services.add(line)
                continue
            }
        }

        if (names.size != addresses.size || addresses.size != services.size) {
            throw IOException("The fields aren't the same length...")
        }

        for (i in names.indices) {
            output.add(UserData(names[i], addresses[i], services[i]))
        }

        return output
    }

    /**
     * Uses pdfBox to open a Royal Mail PDF and extract text from the shipping label.
     * Once this is done it will then open the Royal Mail proof of postage pdf and
     * populate the boxes with the text stripped from the shipping labels.
     * @param filename is the file that it will attempt to process
     */
    fun processPdf(userData: Vector<UserData>, outputDestination: String) {
        // output file is empty to will end early as there is nothing to add to proof of postage
        if (userData.isEmpty()) {
            println("Output file is empty, exiting early")
            return
        }

        var p = 0

        while (p <= userData.size - 1) {
            // Load Royal Mail proof of postage pdf ready to repopulate!
            val file = File("resources/Royal-Mail-Bulk-Certificate-Posting-Standard.pdf")
            val pdfTemplate = PDDocument.load(file)
            val docCatalog = pdfTemplate.documentCatalog
            val acroForm = docCatalog.acroForm

            // populate the forms from the pdf text stored in the outputs string array
            var countForNumberOfItems = 0
            for (i in p..<userData.size) {
                if (i > p + 29) {
                    println("Over 30 items exiting early!")
                    break
                }

                val (name, address, service) = userData[i]

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

            val fileName = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(Date()) + "-bulk-order-form-${p + 1}.pdf"
            val path = outputDestination + File.separator + fileName
            pdfTemplate.save(path)
            pdfTemplate.close()
            p += 30
        }
    }
}