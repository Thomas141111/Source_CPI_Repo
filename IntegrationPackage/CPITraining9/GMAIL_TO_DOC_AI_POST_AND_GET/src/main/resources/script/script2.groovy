import com.sap.gateway.ip.core.customdev.util.Message
import java.util.Base64

def Message processData(Message message) {
    
    def attachments = message.getAttachments()
    
    def sb = new StringBuilder()
    sb.append("<pdfs>")
    
    attachments.each { name, attachment ->
        if (name.toLowerCase().endsWith('.pdf')) {
            
            // Read stream into bytes first
            def inputStream = attachment.getContent() as InputStream
            def pdfBytes = inputStream.bytes
            
            def encoded = Base64.getEncoder().encodeToString(pdfBytes)
            
            sb.append("<pdf>")
            sb.append("<fileName>${name}</fileName>")
            sb.append("<content>${encoded}</content>")
            sb.append("</pdf>")
        }
    }
    
    sb.append("</pdfs>")
    
    message.setBody(sb.toString())
    return message
}