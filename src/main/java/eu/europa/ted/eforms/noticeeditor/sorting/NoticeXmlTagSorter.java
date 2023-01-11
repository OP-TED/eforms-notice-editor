package eu.europa.ted.eforms.noticeeditor.sorting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeNamespace;
import eu.europa.ted.eforms.noticeeditor.service.XmlWriteService;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Sorts notice XML tags (elements) in the order defined by the corresponding SDK XSD sequences.
 *
 * <p>
 * NOTE: For a sample usage there is a unit test which uses this.
 * </p>
 */
public class NoticeXmlTagSorter {

  private static final Logger logger = LoggerFactory.getLogger(NoticeXmlTagSorter.class);

  private static final String CBC_CUSTOMIZATION_ID = "cbc:CustomizationID";
  private static final String XSD_COMPLEX_TYPE = "xsd:complexType";

  /**
   * Contains the tag order.
   */
  private static final String XSD_SEQUENCE = "xsd:sequence";

  private static final String XSD_ELEMENT = "xsd:element";

  private final DocumentBuilder docBuilder;
  private final DocumentTypeInfo docTypeInfo;
  private final Path sdkRootFolder;
  private final Map<String, DocumentTypeNamespace> xsdMetaByPrefix;
  private final XPath xpathInst;

  /**
   * This is not technically required, but avoids parsing the XSD XML multiple times.
   */
  private final Map<String, Element> xsdRootByPrefixCache;

  /**
   * The instance is reusable but specific to a given SDK version.
   *
   * @param docBuilder The document builder is passed as it can be reused
   * @param xpathInst Reusable xpath preconfigured instance
   * @param docTypeInfo SDK document type info
   * @param sdkRootFolder The root folder of the downloaded SDK(s)
   */
  public NoticeXmlTagSorter(final DocumentBuilder docBuilder, final XPath xpathInst,
      final DocumentTypeInfo docTypeInfo, final Path sdkRootFolder) {

    Validate.notNull(docBuilder);
    Validate.notNull(xpathInst);
    Validate.notNull(sdkRootFolder);
    Validate.notNull(docTypeInfo);

    this.docBuilder = docBuilder;

    // SDK specific.
    this.docTypeInfo = docTypeInfo;
    this.xpathInst = xpathInst;
    this.sdkRootFolder = sdkRootFolder;
    this.xsdRootByPrefixCache = new HashMap<>();
    this.xsdMetaByPrefix = docTypeInfo.buildAdditionalNamespacesByPrefix();
  }

  /**
   * Sorts the passed notice XML document starting from the root. This depends on the SDK version of
   * the notice.
   *
   * @param doc A notice XML as a W3C DOM.
   *
   * @throws SAXException If any parse error occurs.
   * @throws IOException If any IO error occurs.
   */
  public void sortXml(final Document doc) throws SAXException, IOException {
    sortXml(doc.getDocumentElement());
  }

  /**
   * Sorts the passed notice XML elements and sub-elements. This depends on the SDK version of the
   * notice.
   *
   * @param xmlRoot The xml root element is the entry point
   * @throws SAXException If any parse error occurs.
   * @throws IOException If any IO error occurs.
   */
  public void sortXml(final Element xmlRoot) throws SAXException, IOException {

    // Compare sdkVersion of the element to the SDK version of this instance.
    final String sdkVersionOfNoticeStr =
        XmlUtils.getDirectChild(xmlRoot, CBC_CUSTOMIZATION_ID).getTextContent();

    final SdkVersion sdkVersionOfNotice =
        new SdkVersion(XmlWriteService.parseEformsSdkVersionText(sdkVersionOfNoticeStr));
    final SdkVersion sdkVersionOfSorter = getSorterSdkVersion();
    if (!sdkVersionOfSorter.equals(sdkVersionOfNotice)) {
      throw new RuntimeException(
          String.format("Incompatible version: sorterInstance=%s, noticeToSort=%s",
              sdkVersionOfSorter, sdkVersionOfNotice));
    }

    //
    // Example:
    // <ext:UBLExtensions>
    // Get the "ext" prefix.
    // final DocumentTypeNamespace documentTypeNamespace = infoByPrefix.get("ext");
    // final String xsdSchemaLocation = documentTypeNamespace.getSchemaLocation();
    //

    //
    // Setup main XSD path.
    //
    final Path mainXsdPath = getMainXsdPath();
    if (mainXsdPath == null) {
      return;
    }

    logger.info("Attempting to sort tags in the XML, starting from root element={}",
        xmlRoot.getTagName());
    logger.info("XML uri={}", xmlRoot.getOwnerDocument().getBaseURI());

    final Document xsdRootDoc = buildDoc(mainXsdPath);
    final Element xsdRootElem = xsdRootDoc.getDocumentElement();

    // The used map does not need to be a LinkedHashMap but it helps to see in which order the
    // entries have been added during when debugging (root first, ...).
    final Map<String, List<String>> elemOrderByXsdElemType = new LinkedHashMap<>(128);
    final Map<String, String> xsdElemTypeByXsdElemName = new LinkedHashMap<>(256);

    //
    // Example:
    // <xsd:element name="BusinessRegistrationInformationNotice"
    // type="BusinessRegistrationInformationNoticeType"/>
    //
    final Element xsdElem = XmlUtils.getDirectChild(xsdRootElem, XSD_ELEMENT);
    this.parseSequence(elemOrderByXsdElemType, xsdElemTypeByXsdElemName, xsdElem, Optional.empty(),
        xsdRootElem.getNodeName(), xsdRootElem);

    // Recursion on child elements.
    sortChildTagsRec(elemOrderByXsdElemType, xsdElemTypeByXsdElemName, xmlRoot);
    logger.trace("elemOrderByXsdElemType size={}", elemOrderByXsdElemType.size());
    logger.trace("elemOrderByXsdElemType keys={}", elemOrderByXsdElemType.keySet());

    if (logger.isTraceEnabled()) {
      elemOrderByXsdElemType.entrySet()
          .forEach(entry -> logger.trace("{} = {}", entry.getKey(), entry.getValue()));
    }
  }

  public Path getMainXsdPath() {
    final Optional<String> sdkXsdPathOpt = this.docTypeInfo.getSdkXsdPathOpt();
    if (sdkXsdPathOpt.isEmpty()) {
      logger.info("Sorting not supported for version={}", getSorterSdkVersion());
      return null;
    }
    return sdkRootFolder.resolve(sdkXsdPathOpt.get());
  }

  private SdkVersion getSorterSdkVersion() {
    return docTypeInfo.getSdkVersion();
  }

  private void sortChildTagsRec(final Map<String, List<String>> elemOrderByXsdElemType,
      final Map<String, String> xsdElemTypeByXsdElemName, final Element noticeElem)
      throws SAXException, IOException {

    final String xsdElemType = xsdElemTypeByXsdElemName.get(noticeElem.getTagName());
    Validate.notNull(xsdElemType, "XSD element type is null for key %s", noticeElem.getTagName());

    final List<String> tagOrder = elemOrderByXsdElemType.get(xsdElemType);
    if (tagOrder == null) {
      return; // It can be null, there is nothing to sort in that case.
    }

    // Go through tags in order.
    for (final String tagName : tagOrder) {

      // Find elements by tag name in the notice.
      final String xpathExpr = tagName; // Search for the tags.
      final String idForError = tagName;
      final List<Element> elemsFoundByTag =
          XmlUtils.evaluateXpathAsElemList(xpathInst, noticeElem, xpathExpr, idForError);

      // Modify physical model: sort, reorder XML elements.
      for (final Element elemFound : elemsFoundByTag) {

        // Find comments above the element.
        final List<Node> commentsAbove = new ArrayList<>();
        Node previousSibling = elemFound.getPreviousSibling();
        while (previousSibling != null) {
          if (previousSibling.getNodeType() == Node.TEXT_NODE) {
            commentsAbove.add(previousSibling);
          } else if (previousSibling.getNodeType() == Node.COMMENT_NODE) {
            commentsAbove.add(previousSibling);
          } else {
            break;
          }
          previousSibling = previousSibling.getPreviousSibling();
        }
        Collections.reverse(commentsAbove); // To keep the order.

        // THIS SORTS THE TAGS:
        for (final Node commentAbove : commentsAbove) {
          removeAndAppend(noticeElem, commentAbove);
        }
        removeAndAppend(noticeElem, elemFound);
      }

      // Continue recursively on the child elements of the notice.
      for (final Element elemFound : elemsFoundByTag) {
        parseXsdSequencesByPrefix(elemOrderByXsdElemType, xsdElemTypeByXsdElemName, elemFound);

        logger.trace("sortChildTagsRec: XML tagName={}", elemFound.getTagName());
        sortChildTagsRec(elemOrderByXsdElemType, xsdElemTypeByXsdElemName, elemFound);
      }
    }
  }

  /**
   * @param elemParent The element in which to append
   * @param elemToSort The element to remove and append
   */
  private static void removeAndAppend(final Element elemParent, final Node elemToSort) {
    elemParent.removeChild(elemToSort); // Removes child from old location.
    elemParent.appendChild(elemToSort); // Appends child at the new location.
  }

  /**
   * Starts from the prefix found inside of the element node name, finds the XSD and parses the
   * sequences to populate the element order by XSD element type.
   *
   * @param elemOrderByXsdElemType Is modified as a SIDE-EFFECT
   * @param xsdElemTypeByXsdElemName Is modified as a SIDE-EFFECT
   */
  private final void parseXsdSequencesByPrefix(
      final Map<String, List<String>> elemOrderByXsdElemType,
      final Map<String, String> xsdElemTypeByXsdElemName, final Element noticeElem)
      throws SAXException, IOException {

    final String prefixedTagName = noticeElem.getNodeName();
    final int indexOfColon = prefixedTagName.indexOf(':');
    if (indexOfColon <= 0) {
      return;
    }

    // efac:BusinessPartyGroup -> efx
    final String prefix = prefixedTagName.substring(0, indexOfColon);
    logger.trace("Namespace prefix={}", prefix);

    final Element xsdRootElem = loadXsdRootByPrefix(prefix);

    // efac:BusinessPartyGroup -> BusinessPartyGroup
    final String tagName = prefixedTagName.substring(indexOfColon + 1);

    //
    // Find prefix, get xsd, get type, ... recursively
    // <xsd:element name="BusinessPartyGroup" type="BusinessPartyGroupType"/>
    //
    final List<Element> elemsFound = XmlUtils.evaluateXpathAsElemList(xpathInst, xsdRootElem,
        String.format("%s[@name='%s']", XSD_ELEMENT, tagName), tagName);

    for (final Element xsdElem : elemsFound) {
      parseSequence(elemOrderByXsdElemType, xsdElemTypeByXsdElemName, xsdElem, Optional.of(prefix),
          prefixedTagName, xsdRootElem);
    }
  }

  /**
   * @param elemOrderByXsdElemType Is modified as a SIDE-EFFECT
   * @param xsdElemTypeByXsdElemName Is modified as a SIDE-EFFECT
   */
  private void parseSequence(final Map<String, List<String>> elemOrderByXsdElemType,
      final Map<String, String> xsdElemTypeByXsdElemName, final Element xsdElem,
      final Optional<String> prefixOpt, final String prefixedTagName, final Element xsdRootElem) {
    //
    // NOTE: Multiple tag names can point to the same type:
    // <xsd:element name="CallForTenderDocumentReference" type="DocumentReferenceType"/>
    // <xsd:element name="CallForTendersDocumentReference" type="DocumentReferenceType"/>
    // Thus the key used for caching is the type, here the key would be "DocumentReferenceType"
    //
    final String xsdElemType = XmlUtils.getAttrText(xsdElem, "type");
    final String xsdElemName = XmlUtils.getAttrText(xsdElem, "name");

    // Because it can be pointed to by several names, always put the entry into the map!
    final String xsdElemNamePrefixed =
        prefixOpt.isPresent() ? prefixOpt.get() + ":" + xsdElemName : xsdElemName;
    xsdElemTypeByXsdElemName.put(xsdElemNamePrefixed, xsdElemType);

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
    final List<Element> xsdSequences = XmlUtils.evaluateXpathAsElemList(xpathInst, xsdRootElem,
        String.format("%s[@name='%s']/%s", XSD_COMPLEX_TYPE, xsdElemType, XSD_SEQUENCE),
        xsdElemType);
    if (xsdSequences.isEmpty()) {
      return;
    }

    // Populate order list from sequence.
    final List<String> xmlTagOrder = new ArrayList<>();
    for (Element xsdSequence : xsdSequences) {
      xmlTagOrder.addAll(parseSequenceElementOrder(xsdSequence, xpathInst));
    }

    // Populate order by type map from sequence.
    if (!xmlTagOrder.isEmpty()) {
      if (elemOrderByXsdElemType.containsKey(xsdElemType)) {
        // It is not expected to find a type more than one time.
        throw new RuntimeException(
            String.format("prefixedTagName=%s is already contained", prefixedTagName));
      }
      elemOrderByXsdElemType.put(xsdElemType, xmlTagOrder);
    }
  }

  /**
   * @param xsdSequence The XSD sequence element
   * @return The list of the XSD elements ref found in the XSD sequence
   */
  private static List<String> parseSequenceElementOrder(final Element xsdSequence,
      final XPath xpathInst) {

    //
    // There are nested sequences:
    //
    // <xsd:complexType name="OrganizationType">
    // <xsd:sequence>
    // <xsd:choice minOccurs="0" maxOccurs="1">
    // <xsd:sequence>
    // ...
    // </xsd:sequence>
    //

    // The expression is good enough to work with the current state of the XSDs.
    // If the expression should evolve we could use the SDK version the code logic.
    final String xpathExpr = String.format(".//%s", XSD_ELEMENT);
    final List<Element> seqElements = XmlUtils.evaluateXpathAsElemList(xpathInst, xsdSequence,
        xpathExpr, "Looking for " + XSD_ELEMENT);

    final List<String> xmlTagsInOrder = new ArrayList<>(seqElements.size());
    for (final Element seqElem : seqElements) {
      // Get the reference.
      // Example: <xsd:element ref="ext:UBLExtensions" .../>
      final String ref = XmlUtils.getAttrText(seqElem, "ref");
      Validate.notBlank(ref, "ref is blank");
      if (!xmlTagsInOrder.contains(ref)) {
        xmlTagsInOrder.add(ref);
      } else {
        // There is a duplicate which in terms of order is problematic.
        throw new RuntimeException(String.format("Already contains order ref=%s", ref));
      }
    }

    Validate.notEmpty(xmlTagsInOrder, "xmlTagOrder is empty");
    return xmlTagsInOrder;
  }

  /**
   * @param namespacePrefix A namespace prefix like cac, cbc, ...
   * @return The XML root element of the XSD for the passed prefix
   */
  private Element loadXsdRootByPrefix(final String namespacePrefix)
      throws SAXException, IOException {

    // Find the SDK metadata by prefix.
    final DocumentTypeNamespace dtn = xsdMetaByPrefix.get(namespacePrefix);
    if (dtn == null) {
      throw new RuntimeException(String.format("Info not found for prefix=%s", namespacePrefix));
    }

    final Element cached = this.xsdRootByPrefixCache.get(namespacePrefix);
    if (cached != null) {
      return cached; // Return from cache.
    }

    // Parse the XSD XML and return the root element of the XML.
    final String xsdLocation = dtn.getSchemaLocation();
    final Path xsdPath = sdkRootFolder.resolve(Path.of(xsdLocation));
    final Document xsdDoc = buildDoc(xsdPath);
    final Element rootElem = xsdDoc.getDocumentElement();
    this.xsdRootByPrefixCache.put(namespacePrefix, rootElem); // Cache it.

    return rootElem;
  }

  private Document buildDoc(final Path xsdPath) throws SAXException, IOException {
    return docBuilder.parse(xsdPath.toFile());
  }
}
