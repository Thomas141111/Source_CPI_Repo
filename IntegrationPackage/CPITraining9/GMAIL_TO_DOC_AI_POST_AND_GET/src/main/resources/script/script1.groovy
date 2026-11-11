import com.sap.gateway.ip.core.customdev.util.Message
// import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil

def Message processData(Message message) {
    def xml = new XmlSlurper().parseText(message.getBody(String))
    
    def confidences = []
    xml.extraction.headerFields.confidence.each { confidences << it.toBigDecimal() }
    xml.extraction.lineItems.confidence.each { confidences << it.toBigDecimal() }
    
    def avgConfidence = (confidences.sum() / confidences.size() * 100).setScale(0, BigDecimal.ROUND_HALF_UP)
    
    def root = new groovy.xml.StreamingMarkupBuilder().bind {
        root{
            definitionId("ap21.4e9c4553trial.cpiapprovalprocess.myProcess")
            context {
                doc_name(xml.fileName.text())
                doc_extraction_complete(true)
                confidence(avgConfidence)
            }
        }

    }
    
    message.setBody(XmlUtil.serialize(root))
    return message
}
