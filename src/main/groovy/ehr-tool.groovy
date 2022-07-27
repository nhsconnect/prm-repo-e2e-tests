import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

println 'running ehr-tool...'

cwd =  new File('.')

println 'current dir: ' + cwd.absolutePath

messagesDir = new File('tools/large-ehr-inspector/samples')
mhsJsonInDir = new File(messagesDir, 'mhs-json')
def mhsJsons = mhsJsonInDir.list()
mhsJsons.sort().each {
    println it
}

def recursiveFiles(File dirPath, allFiles = []) {
    def filesList = dirPath.listFiles()
    filesList.each {
        if (it.isFile()) {
            println "File path: " + it.name
            allFiles << it
        } else {
            return recursiveFiles(it, allFiles)
        }
    }
    allFiles
}
def ebxmls = recursiveFiles(messagesDir).findAll {
    println it
    it =~ /ebxml.xml$/
}

ebxmls.each {ebxml ->
    println ebxml
    def fileIS = new FileInputStream(ebxml)
    def builderFactory = DocumentBuilderFactory.newInstance()
    builderFactory.setNamespaceAware(true)
    def builder = builderFactory.newDocumentBuilder()
    def xmlDocument = builder.parse(fileIS)
    def xpath = XPathFactory.newInstance().newXPath()

    // something like
    xpath.setNamespaceContext(new NamespaceContext() {
        static final namespaces = [
            'eb':  'http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd',
            'hl7':  'urn:hl7-org:v3'
        ]

        String getNamespaceURI(String prefix) {
            println 'prefix: ' + prefix

            def specified = namespaces[prefix]
            specified ? specified : namespaces['hl7']
        }

        String getPrefix(String namespaceURI) {
            return null
        }

        @Override
        Iterator<String> getPrefixes(String namespaceURI) {
            return null
        }
    })

    String expression = '//eb:Reference'

    nodeList = (org.w3c.dom.NodeList) xpath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET)

    nodeList.each {
        println it
    }
}
