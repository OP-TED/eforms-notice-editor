package eu.europa.ted.eforms.noticeeditor.sorting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeNamespace;
import eu.europa.ted.eforms.noticeeditor.helper.notice.PhysicalModel;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;

/**
 * Sorts notice XML tags (elements) in the order defined by the corresponding SDK schema sequences.
 */
public class NoticeXmlTagSorter {

  private static final Logger logger = LoggerFactory.getLogger(NoticeXmlTagSorter.class);

  private NoticeXmlTagSorter() {
    throw new AssertionError("Utility class.");
  }

  /**
   * Sort the notice XML.
   *
   * @param docBuilder The document builder is passed as it can be reused
   * @param docTypeInfo SDK document type info
   * @param sdkRootFolder The root folder of the downloaded SDK(s)
   * @param noticeXmlRoot The notice XML root element
   */
  public static void sortXmlTags(final DocumentBuilder docBuilder,
      final DocumentTypeInfo docTypeInfo, final Path sdkRootFolder, final Element noticeXmlRoot)
      throws SAXException, IOException {
    logger.info("Attempting to sort tags for {}", noticeXmlRoot.getTagName());

    //
    // Setup XSD by namespace prefix map.
    //
    final Map<String, DocumentTypeNamespace> xsdMetaByPrefix =
        docTypeInfo.buildAdditionalNamespacesByPrefix();

    //
    // Example:
    // <ext:UBLExtensions>
    // Get the "ext" prefix.
    // final DocumentTypeNamespace documentTypeNamespace = infoByPrefix.get("ext");
    // final String xsdSchemaLocation = documentTypeNamespace.getSchemaLocation();
    //
    logger.info("xsdMetaByPrefix prefixes={}", xsdMetaByPrefix.keySet());
    final SortContext ctx =
        new SortContext(docBuilder, sdkRootFolder, xsdMetaByPrefix, docTypeInfo);

    //
    // Setup main XSD path.
    //
    final Optional<String> sdkXsdPathOpt = docTypeInfo.getSdkXsdPathOpt();
    if (sdkXsdPathOpt.isEmpty()) {
      logger.info("Sorting not supported for version={}", docTypeInfo.getSdkVersion());
      return;
    }
    final Path mainXsdPath = sdkRootFolder.resolve(sdkXsdPathOpt.get());

    //
    // Sort the XML.
    //
    ctx.sortXml(mainXsdPath, noticeXmlRoot);
  }

  private static class SortContext {

    private static final String XSD_COMPLEX_TYPE = "xsd:complexType";

    /**
     * Contains the tag order.
     */
    private static final String XSD_SEQUENCE = "xsd:sequence";

    private static final String XSD_ELEMENT = "xsd:element";

    private final DocumentBuilder docBuilder;
    private final Path sdkRootFolder;
    private final Map<String, DocumentTypeNamespace> xsdMetaByPrefix;
    private final XPath xpathInst;

    /**
     * @param docBuilder Reusable document builder
     * @param sdkRootFolder The SDK root folder
     * @param xsdMetaByPrefix SDK meta information by namespace prefix
     * @param docTypeInfo SDK document type meta information
     */
    public SortContext(final DocumentBuilder docBuilder, final Path sdkRootFolder,
        final Map<String, DocumentTypeNamespace> xsdMetaByPrefix,
        final DocumentTypeInfo docTypeInfo) {
      this.sdkRootFolder = sdkRootFolder;
      this.xpathInst = PhysicalModel.setupXpathInst(docTypeInfo, Optional.empty());
      this.xsdMetaByPrefix = xsdMetaByPrefix;
      this.docBuilder = docBuilder;
    }

    /**
     * @param xsdPath Path to the xsd matching the notice root element
     * @param xmlRoot The xml root element is the entry point
     * @throws SAXException If any parse error occurs.
     * @throws IOException If any IO error occurs.
     */
    public void sortXml(final Path xsdPath, final Element xmlRoot)
        throws SAXException, IOException {

      final Document xsdRootDoc = buildDoc(xsdPath);
      final Element xsdRootElem = xsdRootDoc.getDocumentElement();

      //
      // Example:
      // <xsd:element name="BusinessRegistrationInformationNotice"
      // type="BusinessRegistrationInformationNoticeType"/>
      //
      final Element xsdElem = XmlUtils.getDirectChild(xsdRootElem, XSD_ELEMENT);
      final String xsdElemType = XmlUtils.getAttrText(xsdElem, "type");
      final Element xsdComplexType = XmlUtils.getDirectChild(xsdRootElem, XSD_COMPLEX_TYPE);
      final String complexType = XmlUtils.getAttrText(xsdComplexType, "name");
      Validate.isTrue(xsdElemType.equals(complexType));

      // Collect tags in order.
      final Node xsdSequence = XmlUtils.getDirectChild(xsdComplexType, XSD_SEQUENCE);
      final List<String> rootTagOrder = extractSequenceElemOrder((Element) xsdSequence, xpathInst);

      final Map<String, List<String>> elemOrderByXsdElemType = new HashMap<>(128);
      elemOrderByXsdElemType.put(xsdElemType, rootTagOrder);

      sortChildTagsRec(elemOrderByXsdElemType, xmlRoot, rootTagOrder);
      logger.info("elemOrderByXsdElemType size={}", elemOrderByXsdElemType.size());
      logger.info("elemOrderByXsdElemType keys={}", elemOrderByXsdElemType.keySet());

      if (logger.isDebugEnabled()) {
        for (final Entry<String, List<String>> entry : elemOrderByXsdElemType.entrySet()) {
          logger.debug(entry.getKey() + " = " + entry.getValue());
        }
      }
    }

    private void sortChildTagsRec(final Map<String, List<String>> elemOrderByXsdElemType,
        final Element noticeElem, final List<String> tagOrder) throws SAXException, IOException {

      logger.info("sortChildTagsRec: XML tagName={}", noticeElem.getTagName());

      // Go through tags in order.
      for (final String tagName : tagOrder) {

        // Find elements by tag name in the notice.
        final String xpathExpr = tagName; // Search for the tags.
        final String idForError = tagName;
        final NodeList elemsFoundByTag =
            PhysicalModel.evaluateXpath(xpathInst, noticeElem, xpathExpr, idForError);

        // Modify physical model: sort, reorder XML elements.
        for (int i = 0; i < elemsFoundByTag.getLength(); i++) {
          final Node childElem = elemsFoundByTag.item(i);
          noticeElem.removeChild(childElem); // Removes child from old location.
          noticeElem.appendChild(childElem); // Appends child at the new location.
        }

        // Continue recursively on the child elements of the notice.
        for (int i = 0; i < elemsFoundByTag.getLength(); i++) {
          final Element childElem = (Element) elemsFoundByTag.item(i);
          parseSequences(childElem, elemOrderByXsdElemType);
          sortChildTagsRec(elemOrderByXsdElemType, childElem, tagOrder);
        }
      }
    }

    private final void parseSequences(final Element noticeElem,
        final Map<String, List<String>> elemOrderByXsdElemType) throws SAXException, IOException {

      final String prefixedTagName = noticeElem.getNodeName();
      final int indexOfColon = prefixedTagName.indexOf(':');
      if (indexOfColon <= 0) {
        return;
      }

      // efac:BusinessPartyGroup -> efx
      final String prefix = prefixedTagName.substring(0, indexOfColon);
      final Element xsdRoot = getXsdRootByPrefix(prefix);

      // efac:BusinessPartyGroup -> BusinessPartyGroup
      final String tagName = prefixedTagName.substring(indexOfColon + 1);

      //
      // Find prefix, get xsd, get type, ... recursively
      // <xsd:element name="BusinessPartyGroup" type="BusinessPartyGroupType"/>
      //
      final NodeList elementsFoundByXpath = PhysicalModel.evaluateXpath(xpathInst, xsdRoot,
          String.format("xsd:element[@name='%s']", tagName), tagName);

      for (int i = 0; i < elementsFoundByXpath.getLength(); i++) {
        final Element xsdElem = (Element) elementsFoundByXpath.item(i);

        //
        // NOTE: Multiple tag names can point to the same type:
        // <xsd:element name="CallForTenderDocumentReference" type="DocumentReferenceType"/>
        // <xsd:element name="CallForTendersDocumentReference" type="DocumentReferenceType"/>
        // Thus the key used for caching is the type, here the key would be "DocumentReferenceType"
        //
        final String xsdElemType = XmlUtils.getAttrText(xsdElem, "type");
        if (elemOrderByXsdElemType.containsKey(xsdElemType)) {
          return; // We already have this type cached.
        }

        //
        // Look for sequences inside complexType
        //
        // <xsd:complexType name="BusinessPartyGroupType">
        // <xsd:sequence>
        // <xsd:element ref="efbc:GroupTypeCode" minOccurs="0" maxOccurs="1"/>
        // <xsd:element ref="efbc:GroupType" minOccurs="0" maxOccurs="unbounded"/>
        // <xsd:element ref="cac:Party" minOccurs="1" maxOccurs="unbounded"/>
        // </xsd:sequence>
        // </xsd:complexType>
        //
        final NodeList xsdSequences = PhysicalModel.evaluateXpath(xpathInst, xsdRoot,
            String.format("%s[@name='%s']/%s", XSD_COMPLEX_TYPE, xsdElemType, XSD_SEQUENCE),
            xsdElemType);

        // Populate order by type map from sequence.
        final List<String> xmlTagOrder = new ArrayList<>();
        if (xsdSequences.getLength() > 0) {

          for (int j = 0; j < xsdSequences.getLength(); j++) {
            final Element xsdSequence = (Element) xsdSequences.item(j);
            xmlTagOrder.addAll(extractSequenceElemOrder(xsdSequence, xpathInst));
          }

          if (!xmlTagOrder.isEmpty()) {
            if (elemOrderByXsdElemType.containsKey(xsdElemType)) {
              // It is not expected to find a type more than one time.
              throw new RuntimeException(
                  String.format("prefixedTagName=%s is already contained", prefixedTagName));
            }
            elemOrderByXsdElemType.put(xsdElemType, xmlTagOrder);
          }

        }

      }
    }

    /**
     * @param xsdSequence The XSD sequence element
     * @return The list of the XSD elements ref found in the XSD sequence
     */
    private static List<String> extractSequenceElemOrder(final Element xsdSequence,
        final XPath xpathInst) {

      // TODO handle nested sequences:
      //
      // <xsd:complexType name="OrganizationType">
      // <xsd:sequence>
      // --<xsd:choice minOccurs="0" maxOccurs="1">
      // ----<xsd:sequence>
      // ------<xsd:element ref="efbc:GroupLeadIndicator" minOccurs="0" maxOccurs="1"/>
      // ------<xsd:element ref="efbc:AcquiringCPBIndicator" minOccurs="0" maxOccurs="1"/>
      // ------<xsd:element ref="efbc:AwardingCPBIndicator" minOccurs="0" maxOccurs="1"/>
      // ----</xsd:sequence>
      // ----<xsd:sequence>
      // ------<xsd:element ref="efbc:ListedOnRegulatedMarketIndicator" minOccurs="0"
      // maxOccurs="1"/>
      // ------<xsd:element ref="efbc:NaturalPersonIndicator" minOccurs="0" maxOccurs="1"/>
      // ------<xsd:element ref="efac:UltimateBeneficialOwner" minOccurs="0"
      // maxOccurs="unbounded"/>
      // ----</xsd:sequence>
      // ...
      // </xsd:sequence>
      //
      // TODO For the nesting maybe use xpath instead of the direct children.
      // final List<Element> seqChildElements = XmlUtils.getDirectChildren(xsdSequence,
      // XSD_ELEMENT);

      // The expression is good enough to work with the current state of the XSDs.
      // If the expression should evolve we could use the SDK version the code logic.
      final String xpathExpr = String.format(".//%s", XSD_ELEMENT);
      final NodeList seqElements = PhysicalModel.evaluateXpath(xpathInst, xsdSequence, xpathExpr,
          "Looking for " + XSD_ELEMENT);

      final List<String> xmlTagOrder = new ArrayList<>(seqElements.getLength());
      for (int j = 0; j < seqElements.getLength(); j++) {
        final Element seqElem = (Element) seqElements.item(j);
        // Get the reference.
        // Example: <xsd:element ref="ext:UBLExtensions" .../>
        final String ref = XmlUtils.getAttrText(seqElem, "ref");
        Validate.notBlank(ref, "ref is blank");
        if (!xmlTagOrder.contains(ref)) {
          xmlTagOrder.add(ref);
        } else {
          // There is a duplicate which in terms of order is problematic.
          throw new RuntimeException(String.format("Already contains order ref=%s", ref));
        }
      }
      Validate.notEmpty(xmlTagOrder, "xmlTagOrder is empty");
      return xmlTagOrder;
    }

    /**
     * @param namespacePrefix A namespace prefix like cac, cbc, ...
     * @return The XML root element of the XSD for the passed prefix
     */
    private Element getXsdRootByPrefix(final String namespacePrefix)
        throws SAXException, IOException {
      // Find the SDK metadata by prefix.
      final DocumentTypeNamespace dtn = xsdMetaByPrefix.get(namespacePrefix);
      if (dtn == null) {
        throw new RuntimeException(String.format("Info not found for prefix=%s", namespacePrefix));
      }
      final String schemaLocation = dtn.getSchemaLocation();

      // Parse the XSD XML and return the root element of the XML.
      final Path xsdPath = sdkRootFolder.resolve(Path.of(schemaLocation));
      final Document xsdDoc = buildDoc(xsdPath);
      return xsdDoc.getDocumentElement();
    }

    private Document buildDoc(final Path xsdPath) throws SAXException, IOException {
      return docBuilder.parse(xsdPath.toFile());
    }
  }
}
