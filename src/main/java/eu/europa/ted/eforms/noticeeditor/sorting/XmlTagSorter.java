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
import eu.europa.ted.eforms.sdk.SdkVersion;

public class XmlTagSorter {

  private static final Logger logger = LoggerFactory.getLogger(XmlTagSorter.class);

  private XmlTagSorter() {
    throw new AssertionError("Utility class.");
  }

  public static void sortXmlTagsTopLevel(final DocumentBuilder builder, final Element noticeRoot,
      final DocumentTypeInfo docTypeInfo, final Path sdkRootFolder, final SdkVersion sdkVersion) {
    logger.info("Attempting to sort tags for {}", noticeRoot.getTagName());

    // SETUP main XSD path.
    final Optional<String> sdkXsdPathOpt = docTypeInfo.getSdkXsdPathOpt();
    if (sdkXsdPathOpt.isEmpty()) {
      logger.info("Sorting not supported for version={}", sdkVersion);
      return;
    }
    final Path mainXsdPath = sdkRootFolder.resolve(sdkXsdPathOpt.get());

    // SETUP xsd by prefix map.
    final Map<String, DocumentTypeNamespace> xsdMetaByPrefix =
        docTypeInfo.getAdditionalNamespacesByPrefix();
    logger.info("infoByPrefix prefixes={}", xsdMetaByPrefix.keySet());

    //
    // Example:
    // <ext:UBLExtensions>
    // Get the "ext" prefix.
    // final DocumentTypeNamespace documentTypeNamespace = infoByPrefix.get("ext");
    // final String xsdSchemaLocation = documentTypeNamespace.getSchemaLocation();
    //
    try {
      final SortContext ctx = new SortContext(builder, sdkRootFolder, xsdMetaByPrefix, docTypeInfo);
      ctx.sortRoot(noticeRoot, mainXsdPath);
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class SortContext {
    private final DocumentBuilder builder;
    private final Path sdkRootFolder;
    private final Map<String, DocumentTypeNamespace> xsdMetaByPrefix;
    private final XPath xpathInst;

    public SortContext(final DocumentBuilder builder, final Path sdkRootFolder,
        final Map<String, DocumentTypeNamespace> xsdMetaByPrefix,
        final DocumentTypeInfo docTypeInfo) {
      this.builder = builder;
      this.sdkRootFolder = sdkRootFolder;
      this.xsdMetaByPrefix = xsdMetaByPrefix;
      this.xpathInst = PhysicalModel.setupXpathInst(docTypeInfo, Optional.empty());
    }

    public void sortRoot(final Element noticeRoot, final Path xsdPath)
        throws SAXException, IOException {

      final Document xsdRootDoc = this.getBuilder().parse(xsdPath.toFile());
      final Element xsdRootElem = xsdRootDoc.getDocumentElement();

      //
      // Example:
      // <xsd:element name="BusinessRegistrationInformationNotice"
      // type="BusinessRegistrationInformationNoticeType"/>
      //
      final Element xsdElement = XmlUtils.getDirectChild(xsdRootElem, "xsd:element");
      final String type = XmlUtils.getAttrText(xsdElement, "type");
      final Element xsdComplexType = XmlUtils.getDirectChild(xsdRootElem, "xsd:complexType");
      final String complexType = XmlUtils.getAttrText(xsdComplexType, "name");
      Validate.isTrue(type.equals(complexType));

      // Collect tag in order.
      final Node xsdSequence = XmlUtils.getDirectChild(xsdComplexType, "xsd:sequence");
      final List<Element> seqNodes =
          XmlUtils.getDirectChildren((Element) xsdSequence, "xsd:element");
      final List<String> tagOrder = new ArrayList<>();
      for (final Element seqElem : seqNodes) {
        tagOrder.add(XmlUtils.getAttrText(seqElem, "ref"));
      }

      final Map<String, List<String>> orderByType = new HashMap<>();
      orderByType.put(type, tagOrder);

      sortChildTagsRec(orderByType, noticeRoot, tagOrder);
      for (final Entry<String, List<String>> entry : orderByType.entrySet()) {
        System.out.println(entry.getKey() + " = " + entry.getValue());
      }
    }

    private void sortChildTagsRec(final Map<String, List<String>> orderByType,
        final Element noticeElem, final List<String> tagOrder) throws SAXException, IOException {

      // Go through tags in order.
      for (final String tagName : tagOrder) {

        // Find elements by tag name in the notice.
        final String xpathExpr = tagName;
        final String idForError = tagName;
        final NodeList elemsFoundByTag =
            PhysicalModel.evaluateXpath(this.getXpathInst(), noticeElem, xpathExpr, idForError);

        // Sort elements.
        for (int i = 0; i < elemsFoundByTag.getLength(); i++) {
          final Node childElem = elemsFoundByTag.item(i);
          noticeElem.removeChild(childElem); // Removes child from old location.
          noticeElem.appendChild(childElem); // Appends child at the new location.
        }

        for (int i = 0; i < elemsFoundByTag.getLength(); i++) {
          final Element childElem = (Element) elemsFoundByTag.item(i);
          parseSequences(childElem, orderByType);
          sortChildTagsRec(orderByType, childElem, tagOrder);
        }
      }
    }

    private final void parseSequences(final Element noticeElem,
        final Map<String, List<String>> orderByType) throws SAXException, IOException {

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
      // Find prefix, get xsd, get type, ... recursive
      // <xsd:element name="BusinessPartyGroup" type="BusinessPartyGroupType"/>
      //
      final NodeList elementsFoundByXpath = PhysicalModel.evaluateXpath(this.getXpathInst(),
          xsdRoot, String.format("xsd:element[@name='%s']", tagName), tagName);

      for (int i = 0; i < elementsFoundByXpath.getLength(); i++) {
        final Element xsdElement = (Element) elementsFoundByXpath.item(i);

        // NOTE: Multiple tag names can point to the same type, so cache by type.
        // <xsd:element name="CallForTenderDocumentReference" type="DocumentReferenceType"/>
        // <xsd:element name="CallForTendersDocumentReference" type="DocumentReferenceType"/>
        final String type = XmlUtils.getAttrText(xsdElement, "type");
        if (orderByType.containsKey(type)) {
          return; // We already have this cached.
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
        final NodeList xsdSequences = PhysicalModel.evaluateXpath(this.getXpathInst(), xsdRoot,
            String.format("xsd:complexType[@name='%s']/xsd:sequence", type), type);

        // TODO tttt handle nested sequences:
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
        final List<String> tagOrder = new ArrayList<>();
        if (xsdSequences.getLength() > 0) {
          for (int j = 0; j < xsdSequences.getLength(); j++) {
            final Element xsdSequence = (Element) xsdSequences.item(j);
            final List<Element> children = XmlUtils.getDirectChildren(xsdSequence, "xsd:element");
            for (final Element child : children) {
              final String ref = XmlUtils.getAttrText(child, "ref");
              tagOrder.add(ref);
            }
          }
          if (!tagOrder.isEmpty()) {
            if (orderByType.containsKey(type)) {
              throw new RuntimeException(
                  String.format("prefixedTagName=%s already contained", prefixedTagName));
            }
            orderByType.put(type, tagOrder);
          }
        }

      }
    }

    private Element getXsdRootByPrefix(final String prefix) throws SAXException, IOException {
      // Find the SDK metadata by prefix.
      final DocumentTypeNamespace dtn = this.getXsdMetaByPrefix().get(prefix);
      if (dtn == null) {
        throw new RuntimeException(String.format("Info not found for prefix=%s", prefix));
      }
      final String schemaLocation = dtn.getSchemaLocation();

      // Parse XSD.
      final Path xsdPath = this.getSdkRootFolder().resolve(Path.of(schemaLocation));
      final Document xsdDoc = this.getBuilder().parse(xsdPath.toFile());
      return xsdDoc.getDocumentElement();
    }

    public DocumentBuilder getBuilder() {
      return builder;
    }

    public XPath getXpathInst() {
      return xpathInst;
    }

    public Path getSdkRootFolder() {
      return sdkRootFolder;
    }

    public Map<String, DocumentTypeNamespace> getXsdMetaByPrefix() {
      return xsdMetaByPrefix;
    }

  }

}
