import com.sap.gateway.ip.core.customdev.util.Message
import java.util.Base64
// for only deocde base64
def Message processData(Message message) {
    
    def reader = message.getBody(java.io.Reader)
    def xml = new XmlSlurper().parse(reader)
    
    def fileName
    def encoded
    
    if (xml.name() == "pdfs") {
        fileName = xml.pdf.fileName.text()
        encoded = xml.pdf.content.text()
    } else if (xml.name() == "pdf") {
        fileName = xml.fileName.text()
        encoded = xml.content.text()
    }
    
    // Decode Base64 → PDF bytes and set as body
    def pdfBytes = Base64.getDecoder().decode(encoded.trim())
    message.setBody(new ByteArrayInputStream(pdfBytes))
    
    // Set fileName as header
    message.setHeader("CamelFileName", fileName)
    
    return message
}
