import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

println 'running ehr-tool...'

cwd =  new File('.')

def getParam(name) {
    def param = System.getenv(name)
    if (param == null) {
        throw new Exception('required env var ' + name + ' input parameter not set')
    }
    param
}

def messagesDir = getParam('MESSAGES_DIR')
def templateMessageId = getParam('TEMPLATE_MESSAGE_ID')
def targetMessageId = getParam('TARGET_MESSAGE_ID')

println 'current dir: ' + cwd.absolutePath
println 'messages dir: ' + messagesDir
println 'template message id: ' + templateMessageId
println 'target message id: ' + targetMessageId

def messagesDirFile = new File((String) messagesDir)

def templateMessageDirFile = new File(messagesDirFile, templateMessageId)

def loadDocument(xmlFile) {
    def fileIS = new FileInputStream(xmlFile)
    def builderFactory = DocumentBuilderFactory.newInstance()
    builderFactory.setNamespaceAware(true)
    def builder = builderFactory.newDocumentBuilder()
    builder.parse(fileIS)
}

org.w3c.dom.NodeList query(org.w3c.dom.Document xmlDocument, String xpathExpression) {
    def xpath = XPathFactory.newInstance().newXPath()
    def namespaceResolver = new NamespaceContext() {
        static final namespaces = [
            'eb' : 'http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd',
            'hl7': 'urn:hl7-org:v3'
        ]

        String getNamespaceURI(String prefix) {
            namespaces[prefix]
        }

        String getPrefix(String namespaceURI) {
            return null
        }

        @Override
        Iterator<String> getPrefixes(String namespaceURI) {
            return null
        }
    }
    xpath.setNamespaceContext(namespaceResolver)
    return xpath.compile(xpathExpression).evaluate(xmlDocument, XPathConstants.NODESET)
}


def queryPrint(org.w3c.dom.Document xmlDocument, String xpathExpression) {
    println xpathExpression + ':'
    def nodes = query(xmlDocument, xpathExpression)
    nodes.each {
        println it
    }
}

def ebxmlFile = new File(templateMessageDirFile, 'ebxml.xml')

println 'ebxml: ' + ebxmlFile

def ebxml = loadDocument(ebxmlFile)

queryPrint(ebxml, '//eb:ConversationId/text()')
queryPrint(ebxml, '//eb:MessageData/eb:MessageId/text()')

def payloadFile = new File(templateMessageDirFile, 'payload.xml')

println 'payload: ' + payloadFile

def payload = loadDocument(payloadFile)

def firstNarrativeStatementComponent = '(//hl7:EhrExtract/hl7:component/hl7:ehrFolder/hl7:component[.//hl7:NarrativeStatement])[1]'
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:id/@root')
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:component/hl7:CompoundStatement/hl7:id/@root')
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:component/hl7:CompoundStatement//hl7:NarrativeStatement/hl7:id/@root')
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:component/hl7:CompoundStatement//hl7:NarrativeStatement/hl7:text/text()')
