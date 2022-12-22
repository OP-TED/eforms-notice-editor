package eu.europa.ted.eforms.noticeeditor.sorting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeInfo;
import eu.europa.ted.eforms.noticeeditor.helper.notice.DocumentTypeNamespace;
import eu.europa.ted.eforms.noticeeditor.helper.notice.PhysicalModel;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class XmlTagSorting {

  private static final Logger logger = LoggerFactory.getLogger(XmlTagSorting.class);

  private XmlTagSorting() {
    throw new AssertionError("Utility class.");
  }

  public static void sortXmlTags(final DocumentBuilder builder, final Element noticeRoot,
      final DocumentTypeInfo docTypeInfo, final Path sdkRootFolder, final SdkVersion sdkVersion) {
    logger.info("Attempting to sort tags.");

    final Optional<String> sdkXsdPathOpt = docTypeInfo.getSdkXsdPathOpt();
    if (sdkXsdPathOpt.isEmpty()) {
      logger.info("Sorting not supported for version {}", sdkVersion);
      return;
    }
    final Path xsdPath = sdkRootFolder.resolve(sdkXsdPathOpt.get());

    final Map<String, DocumentTypeNamespace> infoByPrefix =
        docTypeInfo.getAdditionalNamespacesByPrefix();

    System.out.println(noticeRoot.getTagName());
    System.out.println(docTypeInfo.getSdkXsdPathOpt());

    //
    // Example:
    // <ext:UBLExtensions>
    // Get the "ext" prefix.
    // final DocumentTypeNamespace documentTypeNamespace = infoByPrefix.get("ext");
    // final String xsdSchemaLocation = documentTypeNamespace.getSchemaLocation();
    //
    try {
      final Document doc = builder.parse(xsdPath.toFile());
      final Element xsdRoot = doc.getDocumentElement();

      // <xsd:element name="BusinessRegistrationInformationNotice"
      // type="BusinessRegistrationInformationNoticeType"/>

      final Node xsdElement = XmlUtils.getDirectChild(xsdRoot, "xsd:element");
      final String type = XmlUtils.getAttrText(xsdElement.getAttributes(), "type");
      final Node xsdComplexType = XmlUtils.getDirectChild(xsdRoot, "xsd:complexType");
      final String type2 = XmlUtils.getAttrText(xsdComplexType.getAttributes(), "name");
      Validate.isTrue(type.equals(type2));

      final Node xsdSequence = XmlUtils.getDirectChild((Element) xsdComplexType, "xsd:sequence");
      final List<Element> seqNodes =
          XmlUtils.getDirectChildren((Element) xsdSequence, "xsd:element");
      final List<String> sortOrder = new ArrayList<>();
      for (Element seqElem : seqNodes) {
        sortOrder.add(XmlUtils.getAttrText(seqElem.getAttributes(), "ref"));
      }

      final XPath xpathInst = PhysicalModel.setupXpathInst(docTypeInfo, Optional.empty());
      sortChildTags(noticeRoot, sortOrder, xpathInst);

      // Find prefix, get xsd, get type, ... recursive

      // efac:BusinessPartyGroup -> xsd -> BusinessPartyGroupType ->

      // <xsd:complexType name="BusinessPartyGroupType">
      // <xsd:sequence>
      // <xsd:element ref="efbc:GroupTypeCode" minOccurs="0" maxOccurs="1"/>
      // <xsd:element ref="efbc:GroupType" minOccurs="0" maxOccurs="unbounded"/>
      // <xsd:element ref="cac:Party" minOccurs="1" maxOccurs="unbounded"/>
      // </xsd:sequence>
      // </xsd:complexType>

    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void sortChildTags(final Element parentElem, final List<String> tagSortOrder,
      final XPath xpathInst) {
    for (final String tagName : tagSortOrder) {
      final NodeList elementsFound =
          PhysicalModel.evaluateXpath(xpathInst, parentElem, tagName, tagName);
      for (int i = 0; i < elementsFound.getLength(); i++) {
        final Node elem = elementsFound.item(i);
        parentElem.removeChild(elem); // Removes child from old location.
        parentElem.appendChild(elem); // Appends child at the new location.
      }
    }
  }

}
