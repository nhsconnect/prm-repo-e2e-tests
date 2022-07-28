import org.w3c.dom.Document
import org.w3c.dom.Node

import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import java.nio.file.Files
import java.nio.file.Paths

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

def templateDirFile = new File(messagesDirFile, templateMessageId)
def targetDirFile = new File(messagesDirFile, targetMessageId)

def loadDocument(xmlFile) {
    def fileIS = new FileInputStream(xmlFile)
    def builderFactory = DocumentBuilderFactory.newInstance()
    builderFactory.setNamespaceAware(true)
    def builder = builderFactory.newDocumentBuilder()
    builder.parse(fileIS)
}

def writeDocToFile(Document doc, File outputFile) {
    Files.createDirectories(Paths.get(outputFile.getParent()))
    def transformerFactory = TransformerFactory.newDefaultInstance()
    Transformer transformer = transformerFactory.newTransformer()
    DOMSource source = new DOMSource(doc)
    FileWriter writer = new FileWriter(outputFile)
    StreamResult result = new StreamResult(writer)
    transformer.transform(source, result)
}

static void trimWhitespace(Node node)
{
    NodeList children = node.getChildNodes();
    for(int i = 0; i < children.getLength(); ++i) {
        Node child = children.item(i);
        if(child.getNodeType() == Node.TEXT_NODE) {
            child.setTextContent(child.getTextContent().trim());
        }
        trimWhitespace(child);
    }
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

def queryNode(org.w3c.dom.Document xmlDocument, String xpathExpression) {
    def nodes = query(xmlDocument, xpathExpression)
    nodes.item(0)
}

def updateText(Node node, String newText) {
    println 'current value: ' + node.textContent
    node.setTextContent(newText)
    println 'new value: ' + node.textContent
}

def updateText(Document doc, String xpath, String newText) {
    println 'updating: ' + xpath
    def node = queryNode(doc, xpath)
    updateText(node, newText)
}

def ebxmlFile = new File(templateDirFile, 'ebxml.xml')

println 'ebxml: ' + ebxmlFile

def ebxml = loadDocument(ebxmlFile)

updateText(ebxml, '//eb:ConversationId/text()', UUID.randomUUID().toString())
updateText(ebxml, '//eb:MessageData/eb:MessageId/text()', targetMessageId)

def outputEbxmlFile = new File(targetDirFile, 'ebxml.xml')
writeDocToFile(ebxml, outputEbxmlFile)

def payloadFile = new File(templateDirFile, 'payload.xml')

println 'payload: ' + payloadFile

def payload = loadDocument(payloadFile)

def firstNarrativeStatementComponent = '(//hl7:EhrExtract/hl7:component/hl7:ehrFolder/hl7:component[.//hl7:NarrativeStatement])[1]'
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:id/@root')
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:component/hl7:CompoundStatement/hl7:id/@root')
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:component/hl7:CompoundStatement//hl7:NarrativeStatement/hl7:id/@root')
queryPrint(payload, firstNarrativeStatementComponent + '/hl7:ehrComposition/hl7:component/hl7:CompoundStatement//hl7:NarrativeStatement/hl7:text/text()')

def outputPayloadFile = new File(targetDirFile, 'payload.xml')
writeDocToFile(payload, outputPayloadFile)
