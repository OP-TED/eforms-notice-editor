package eu.europa.ted.eforms.noticeeditor.helper.notice;

import static eu.europa.ted.eforms.noticeeditor.util.JsonUtils.getTextStrict;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.helper.SafeDocumentBuilder;
import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.noticeeditor.sorting.NoticeXmlTagSorter;
import eu.europa.ted.eforms.noticeeditor.util.EditorXmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;
import eu.europa.ted.eforms.noticeeditor.util.XmlUtils;
import eu.europa.ted.eforms.noticeeditor.util.XpathUtils;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * <h1>The Physical Model (PM)</h1>
 *
 * <p>
 * Holds the XML document representation. This class also provides static methods to build the
 * physical model from the conceptual model and a method to serialize it to XML text. The SDK
 * version is taken into account.
 * </p>
 *
 * <p>
 * Note that the XML elements are sorted, reordered, according to information found in the SDK.
 * </p>
 */
public class PhysicalModel {

  private static final Logger logger = LoggerFactory.getLogger(PhysicalModel.class);

  public static final String CBC_CUSTOMIZATION_ID = "cbc:CustomizationID";
  private static final String CBC_ID = "cbc:ID"; // Notice id, related to BT-701-notice.
  private static final String XMLNS = "xmlns";

  private static final String NODE_XPATH_RELATIVE = "xpathRelative";

  public static final String FIELD_CODE_LIST = "codeList";
  private static final String FIELD_XPATH_RELATIVE = "xpathRelative";

  private static final String XML_ATTR_EDITOR_COUNTER_SELF = "editorCounterSelf";
  private static final String XML_ATTR_EDITOR_FIELD_ID = "editorFieldId";
  private static final String XML_ATTR_EDITOR_NODE_ID = "editorNodeId";

  public static final String XML_ATTR_LIST_NAME = "listName";
  public static final String XML_ATTR_SCHEME_NAME = "schemeName";

  /**
   * This character should not be part of the reserved xpath technical characters. It should also no
   * be part of any xpath that is used in the SDK.
   */
  private static final char XPATH_TEMP_REPLACEMENT = '~'; // ONE CHAR ONLY!

  /**
   * W3C Document Object Model (DOM), holds the XML representation. This can be queried using xpath
   * and is also easy to serialize.
   */
  private final Document domDocument;

  private final FieldsAndNodes fieldsAndNodes;
  private final XPath xpathInst;
  private final Optional<Path> mainXsdPathOpt;

  /**
   * @param document W3C DOM document representing the notice XML
   * @param xpathInst Used for xpath evaluation
   * @param fieldsAndNodes Holds SDK field and node metadata
   * @param mainXsdPathOpt Path to the main XSD file to use, may be empty if the feature is not
   *        supported in an older SDK. This can be used later for XSD validation of the physical
   *        model
   */
  public PhysicalModel(final Document document, final XPath xpathInst,
      final FieldsAndNodes fieldsAndNodes, final Optional<Path> mainXsdPathOpt) {
    this.domDocument = document;

    // You may have the patch until this point, which could help debug the application.
    // But before the XML is written the patch version is removed.
    this.setSdkVersionWithoutPatch(getSdkVersion());

    this.fieldsAndNodes = fieldsAndNodes;
    this.xpathInst = xpathInst;
    this.mainXsdPathOpt = mainXsdPathOpt;
  }

  public Document getDomDocument() {
    return domDocument;
  }

  public FieldsAndNodes getFieldsAndNodes() {
    return fieldsAndNodes;
  }

  public Optional<Path> getMainXsdPathOpt() {
    return mainXsdPathOpt;
  }

  public UUID getNoticeId() {
    final String tagName = CBC_ID;
    // Get the direct child as we know it is directly under the root.
    final Element child = XmlUtils.getDirectChild(this.domDocument.getDocumentElement(), tagName);
    Validate.notNull(child, "The physical model notice id cannot be found by tagName=%s", tagName);
    final String text = child.getTextContent();
    Validate.notBlank(text, "The physical model notice id is blank via tagName=%s", tagName);
    return UUID.fromString(text);
  }

  public final void setSdkVersionWithoutPatch(final SdkVersion sdkVersion) {
    final Node xmlElem = getSdkVersionElement();
    xmlElem.setTextContent(VersionHelper.prefixSdkVersionWithoutPatch(sdkVersion));
  }

  public SdkVersion getSdkVersion() {
    final Node jsonNode = getSdkVersionElement();
    Validate.notNull(jsonNode, "The physical model SDK version element cannot be found!");
    final String text = jsonNode.getTextContent();
    return VersionHelper.parsePrefixedSdkVersion(text);
  }

  private Node getSdkVersionElement() {
    return this.domDocument.getElementsByTagName(CBC_CUSTOMIZATION_ID).item(0);
  }

  /**
   * This is provided for convenience for the unit tests.
   *
   * <p>
   * Evaluates xpath and returns a nodelist. Note: this works when the required notice values are
   * present as some xpath may rely on their presence (codes, indicators, ...).
   * </p>
   *
   * @param contextElem The XML context element in which the xpath is evaluated
   * @param xpathExpr The XPath expression relative to the passed context
   * @param idForError An identifier which is shown in case of errors
   * @return The result of evaluating the XPath expression as a list of elements
   */
  List<Element> evaluateXpathForTests(final String xpathExpr, String idForError) {
    return XmlUtils.evaluateXpathAsElemList(this.xpathInst, this.getDomDocument(), xpathExpr,
        idForError);
  }

  /**
   * @param indented True if the XML text should be indented, false otherwise.
   *
   * @return The XML as text.
   */
  public String toXmlText(final boolean indented) {
    return EditorXmlUtils.asText(domDocument, indented);
  }

  @Override
  public String toString() {
    return toXmlText(true);
  }

  /**
   * Builds the physical model from the conceptual model and SDK field and nodes information.
   *
   * @param conceptModel The conceptual model from the previous step
   * @param fieldsAndNodes Information about SDK fields and nodes
   * @param noticeInfoBySubtype Map with info about notice metadata by notice sub type
   * @param documentInfoByType Map with info about document metadata by document type
   * @param debug Adds special debug info to the XML, useful for humans and unit tests. Not for
   *        production
   * @param buildFields Allows to disable field building, for debugging purposes. Note that if xpath
   *        relies on the presence of fields or attribute of fields this could be problematic
   * @param sortXml Sorts the XML according to the SDK xsdSequenceOrder if true, else keeps the raw
   *        order
   *
   * @return The physical model as an object containing the XML with a few extras
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
      justification = "Checked to Runtime OK here")
  public static PhysicalModel buildPhysicalModel(final ConceptualModel conceptModel,
      final FieldsAndNodes fieldsAndNodes, final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final boolean debug,
      final boolean buildFields,
      final Path sdkRootFolder,
      final boolean sortXml)
      throws ParserConfigurationException, SAXException, IOException {

    logger.info("Attempting to build physical model.");

    final DocumentBuilder safeDocBuilder =
        SafeDocumentBuilder.buildSafeDocumentBuilderAllowDoctype(true);

    logger.info("XML DOM namespaceAware={}", safeDocBuilder.isNamespaceAware());
    logger.info("XML DOM validating={}", safeDocBuilder.isValidating());

    final Document xmlDoc = safeDocBuilder.newDocument();

    final DocumentTypeInfo docTypeInfo =
        getDocumentTypeInfo(noticeInfoBySubtype, documentInfoByType, conceptModel);
    final String rootElementType = docTypeInfo.getRootElementTagName();

    // Create the root element, top level element.
    final StringBuilder sb = new StringBuilder(512);
    final Element xmlDocRoot = createElemXml(xmlDoc, rootElementType, debug, "", sb, "node");
    xmlDoc.appendChild(xmlDocRoot);

    // TEDEFO-1426
    // For the moment do as if it was there.
    final XPath xpathInst = setXmlNamespaces(docTypeInfo, xmlDocRoot);

    if (debug) {
      // Write dot file about conceptual model.
      conceptModel.writeDotFile(fieldsAndNodes);
      JavaTools.writeTextFile(Path.of("target", "debug", "conceptual-model.json"),
          conceptModel.toString());
    }

    // Recursion: start with the concept root.
    final ConceptTreeNode conceptualModelTreeRootNode = conceptModel.getTreeRootNode();
    final int depth = 0;
    buildPhysicalModelRec(xmlDoc, fieldsAndNodes, conceptualModelTreeRootNode, xmlDocRoot, debug,
        buildFields, depth, sb, xpathInst);
    logger.info("Done building unsorted physical model.");

    // Reorder / sort the physical model.
    // The location of the XSDs is given in the SDK and could vary by SDK version.
    final SdkVersion sdkVersion = fieldsAndNodes.getSdkVersion();
    final Path pathToSpecificSdk = sdkRootFolder.resolve(sdkVersion.toStringWithoutPatch());
    final NoticeXmlTagSorter sorter =
        new NoticeXmlTagSorter(xpathInst, docTypeInfo, pathToSpecificSdk,
            fieldsAndNodes);
    try {
      if (sortXml) {
        logger.info("Attempting to sort physical model.");
        sorter.sortXml(xmlDocRoot);
      }
      final Optional<Path> mainXsdPathOpt = sorter.getMainXsdPathOpt();
      if (mainXsdPathOpt.isPresent()) {
        Validate.isTrue(mainXsdPathOpt.get().toFile().exists(),
            "File does not exist: mainXsdPath=%s",
            mainXsdPathOpt);
      }
      if (debug) {
        final Path path = Path.of("target", "debug");
        Files.createDirectories(path);
        JavaTools.writeTextFile(path.resolve("physical-model.txt"), sb.toString());
      }

      return new PhysicalModel(xmlDoc, xpathInst, fieldsAndNodes, mainXsdPathOpt);
    } catch (final Exception e) {
      final String xmlAsText = EditorXmlUtils.asText(xmlDoc, true);
      logger.error("Problem sorting XML: {}", xmlAsText); // Log the entire XML.
      throw e;
    }
  }

  /**
   * Recursive function used to build the physical model.
   *
   * @param doc The XML document, modified as a SIDE-EFFECT!
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param conceptElem The current conceptual element
   * @param xmlNodeElem The current xml node element
   * @param debug Adds extra debugging info in the XML if true
   * @param buildFields True if fields have to be built, false otherwise
   * @param depth Passed for debugging and logging purposes
   */
  private static void buildPhysicalModelRec(final Document doc, final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeNode conceptElem, final Element xmlNodeElem, final boolean debug,
      final boolean buildFields, final int depth,
      final StringBuilder sb, final XPath xpathInst) {
    Validate.notNull(conceptElem, "conceptElem is null");
    Validate.notNull(xmlNodeElem, "xmlElem is null, conceptElem=%s", conceptElem.getIdUnique());

    final String depthStr = StringUtils.leftPad(" ", depth * 4);
    System.out.println(depthStr + " -----------------------");
    System.out.println(depthStr + " BUILD PHYSICAL " + depth);
    System.out.println(depthStr + " -----------------------");
    System.out
        .println(depthStr + " " + xmlNodeElem.getTagName() + ", id=" + conceptElem.getIdUnique());

    // FIELDS.
    // Put fields first because some nodes xpath predicates may depend on the presence of a field,
    // like ID fields, for example something like:
    // <cbc:ID schemeName="Lot">LOT-0001</cbc:ID>
    if (buildFields) {
      buildFields(doc, fieldsAndNodes, conceptElem, xmlNodeElem, debug, depth, sb);
    }

    //
    // NODES.
    //
    for (final ConceptTreeNode conceptNode : conceptElem.getConceptNodes()) {
      // The nodes may contain fields ...
      buildNodesAndFields(doc, fieldsAndNodes, conceptNode, xpathInst, xmlNodeElem, debug, depth,
          sb, buildFields);
    }
  }

  private static void buildFields(final Document doc, final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeNode conceptElem, final Element xmlNodeElem, final boolean debug,
      final int depth, final StringBuilder sb) {
    // The fields are terminal (tree leaves) and cannot contain nodes.
    // But we have to deal with element attributes that must be added to some elements.
    // We want the attribute information available once we reach the element that has them.
    // This is done in a separator for loop as the attribute fields could appear after the fields
    // that have the attributes.
    final Map<String, ConceptTreeField> attributeFieldById = new HashMap<>();
    final List<ConceptTreeField> conceptFieldsHavingAttributes = new ArrayList<>();
    final List<ConceptTreeField> conceptFieldsWithoutAttributes = new ArrayList<>();
    for (final ConceptTreeField conceptField : conceptElem.getConceptFields()) {
      final String fieldId = conceptField.getFieldId();
      final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
      // An attribute is an attribute of another field.
      final Optional<String> attributeOf =
          JsonUtils.getTextOpt(fieldMeta, FieldsAndNodes.ATTRIBUTE_OF);
      if (attributeOf.isPresent()) {
        // This is an attribute field.
        attributeFieldById.put(fieldId, conceptField);
      } else {
        if (fieldMeta.has(FieldsAndNodes.ATTRIBUTES)) {
          conceptFieldsHavingAttributes.add(conceptField);
        } else {
          conceptFieldsWithoutAttributes.add(conceptField);
        }
      }
    }

    //
    // Build field elements and set the attributes.
    //

    // First: add fields having attributes as other fields may have an xpath referring to the
    // attribute (sibling fields). For example one field could be some kind of category marker.
    for (final ConceptTreeField conceptField : conceptFieldsHavingAttributes) {
      buildFieldElementsAndAttributes(doc, fieldsAndNodes, conceptField, xmlNodeElem, debug,
          depth, sb, attributeFieldById);
    }

    // Second: add fields that have no attribute.
    for (final ConceptTreeField conceptField : conceptFieldsWithoutAttributes) {
      buildFieldElementsAndAttributes(doc, fieldsAndNodes, conceptField, xmlNodeElem, debug,
          depth, sb, attributeFieldById);
    }

    // if (debug) {
    // Display the XML building steps:
    // System out is used here because it is more readable than the logger lines in the console.
    // This is not a replacement for logger.debug(...)
    // System.out.println("");
    // System.out.println(EditorXmlUtils.asText(doc, true));
    // }
  }

  /**
   * Build the physical nodes (and fields).
   *
   * @param doc The XML document, modified as a SIDE-EFFECT!
   * @param fieldsAndNodes Field and node meta information (no form values)
   * @param conceptNode The current conceptual node
   * @param xpathInst Allows to evaluate xpath expressions
   * @param xmlNodeElem The current xml node element (modified as a SIDE-EFFECT!)
   * @param debug Adds extra debugging info in the XML if true, for humans or unit tests, the XML
   *        may become invalid
   * @param depth The current depth level passed for debugging and logging purposes
   * @param buildFields True if fields have to be built, false otherwise
   */
  private static boolean buildNodesAndFields(final Document doc,
      final FieldsAndNodes fieldsAndNodes, final ConceptTreeNode conceptNode, final XPath xpathInst,
      final Element xmlNodeElem, final boolean debug, final int depth, final StringBuilder sb,
      final boolean buildFields) {

    final String depthStr = StringUtils.leftPad(" ", depth * 4);

    // Get the node meta-data from the SDK.
    final String nodeId = conceptNode.getNodeId();
    final JsonNode nodeMeta = fieldsAndNodes.getNodeById(nodeId);

    // If a field or node is repeatable, then the XML element to repeat is the first XML
    // element in the xpathRelative.
    final boolean nodeMetaRepeatable = FieldsAndNodes.isNodeRepeatableStatic(nodeMeta);

    final String xpathRel = getTextStrict(nodeMeta, NODE_XPATH_RELATIVE); // "xpathRelative"
    Element previousElem = xmlNodeElem;
    Element partElem = null;

    // xpathRelative can contain many XML elements. We must build the hierarchy.
    // Split the XPATH into parts.
    final List<String> xpathParts = getXpathParts(xpathRel);
    if (debug) {
      // System out is used here because it is more readable than the logger lines.
      // This is not a replacement for logger.debug(...)
      System.out.println(depthStr + " NODE PARTS SIZE: " + xpathParts.size());
      System.out.println(depthStr + " NODE PARTS: " + listToString(xpathParts));

      sb.append(depthStr).append("nodeId=").append(nodeId).append('\n');
    }

    // In SDK 1.9:
    // "id" : "OPT-060-Lot"
    // /cac:ContractExecutionRequirement[cbc:ExecutionRequirementCode/@listName='conditions']
    // /cbc:ExecutionRequirementCode"
    // "id" : "OPT-060-Lot-List" is the attribute

    for (final String xpathPart : xpathParts) {
      Validate.notBlank(xpathPart, "partXpath is blank for nodeId=%s, xmlNodeElem=%s", nodeId,
          xmlNodeElem);
      final PhysicalXpathPart px = buildXpathPart(xpathPart);
      final String xpathExpr = px.getXpathExpr();
      final String tag = px.getTagOrAttribute();
      if (debug) {
        // System out is used here because it is more readable than the logger lines.
        // This is not a replacement for logger.debug(...)
        System.out.println(depthStr + " tag=" + tag);
        System.out.println(depthStr + " xmlTag=" + xmlNodeElem.getTagName());
      }
      if (previousElem.getTagName().equals(tag) && xpathExpr.equals(tag)) {
        // Sometimes the xpath absolute part already matches the previous element.
        // If there is no special xpath expression, just skip the part.
        // This avoids nesting of the same .../tag/tag/...
        // This may be fixed by ticket TEDEFO-1466, but the problem could come back.
        logger.warn("Same tag, skipping tag: {}", tag);
        continue; // Skip this tag.
      }

      if (nodeMetaRepeatable) {
        // Create an XML element for the node.
        if (debug) {
          final String msg = String.format("%s, xml=%s", nodeId, tag);
          System.out.println(depthStr + " " + msg);
        }
        partElem = createElemXml(doc, tag, debug, depthStr, sb, "node");

      } else {
        // Find existing elements in the context of the previous element.
        final NodeList foundElements =
            XmlUtils.evaluateXpathAsNodeList(xpathInst, previousElem, xpathExpr, nodeId);
        if (foundElements.getLength() > 0) {
          Validate.isTrue(foundElements.getLength() == 1,
              "Found more than one element: {}, nodeId={}, xpathExpr={}", foundElements, nodeId,
              xpathExpr);

          // Node is a w3c dom node, nothing to do with the SDK node.
          final Node xmlNode = foundElements.item(0);
          System.out.println(depthStr + " " + "Found elements: " + foundElements.getLength());
          if (Node.ELEMENT_NODE == xmlNode.getNodeType()) {
            // An existing element was found, reuse it.
            partElem = (Element) xmlNode;
          } else {
            throw new RuntimeException(String.format("NodeType=%s not an Element", xmlNode));
          }

        } else {
          // Create an XML element for the node.
          if (debug) {
            final String msg = String.format("%s, xml=%s", nodeId, tag);
            System.out.println(depthStr + " " + msg);
          }
          partElem = createElemXml(doc, tag, debug, depthStr, sb, "node");
        }
      }

      previousElem.appendChild(partElem); // SIDE-EFFECT! Adding item to the tree.
      previousElem = partElem;

    } // End of for loop on parts of relative xpath.

    // We arrived at the end of the relative xpath.
    // We are interested in the last xml element as this represents the current node.
    // For example:
    // "id" : "ND-RegistrarAddress"
    // "xpathRelative" : "cac:CorporateRegistrationScheme/cac:JurisdictionRegionAddress"
    // The element nodeElem is cac:JurisdictionRegionAddress, so it is the node.
    final Element lastNodeElem = partElem;
    Validate.notNull(lastNodeElem, "partElem is null, conceptElem=%s", conceptNode.getIdUnique());

    // This could make the XML invalid, this is meant to be read by humans.
    if (debug) {
      lastNodeElem.setAttribute(XML_ATTR_EDITOR_NODE_ID, nodeId); // SIDE-EFFECT!
      lastNodeElem.setAttribute(XML_ATTR_EDITOR_COUNTER_SELF,
          Integer.toString(conceptNode.getCounter())); // SIDE-EFFECT!
    }

    // Build child nodes recursively.
    buildPhysicalModelRec(doc, fieldsAndNodes, conceptNode, lastNodeElem, debug, buildFields,
        depth + 1, sb, xpathInst);

    return nodeMetaRepeatable;
  }

  /**
   * Builds the fields XML, some fields have XML elements in their xpath, those will also be built.
   * As a side-effect the XML document and the passed XML element will be modified.
   *
   * @param doc The XML document, modified as a SIDE-EFFECT!
   * @param xmlNodeElem current XML element (modified as a SIDE-EFFECT!)
   * @param debug special debug mode for humans and unit tests (XML may be invalid)
   * @param depth The current depth level passed for debugging and logging purposes
   * @param attributeFieldById All attribute fields by id
   */
  private static void buildFieldElementsAndAttributes(final Document doc,
      final FieldsAndNodes fieldsAndNodes,
      final ConceptTreeField conceptField, final Element xmlNodeElem,
      final boolean debug, final int depth, final StringBuilder sb,
      final Map<String, ConceptTreeField> attributeFieldById) {

    final String depthStr = StringUtils.leftPad(" ", depth * 4);
    final String fieldValue = conceptField.getValue();
    final String fieldId = conceptField.getFieldId();
    logger.debug("PM fieldId={}", fieldId);

    if (debug) {
      System.out.println("");
      System.out.println(depthStr + " fieldId=" + fieldId);

      sb.append(depthStr).append("fieldId=").append(fieldId).append('\n');
    }

    // Get the field meta-data from the SDK.
    final JsonNode fieldMeta = fieldsAndNodes.getFieldById(fieldId);
    Validate.notNull(fieldMeta, "fieldMeta null for fieldId=%s", fieldId);

    final Optional<String> attributeOf =
        JsonUtils.getTextOpt(fieldMeta, FieldsAndNodes.ATTRIBUTE_OF);
    final boolean isAttribute = attributeOf.isPresent();
    if (isAttribute) {
      // Skip. Attributes are used later in this code from the field on which they are attached to.
      // Starting from "attributes" and not from "attributeOf".
      throw new RuntimeException(String
          .format("Attribute fields should not reach this part of the code, fieldId=%s", fieldId));
    }
    final Map<String, String> attributeNameAndValueMap =
        determineAttributes(fieldsAndNodes, attributeFieldById, fieldId, fieldMeta, fieldValue);

    // IMPORTANT: !!! The relative xpath of fields can contain intermediary xml elements !!!
    // Example: "cac:PayerParty/cac:PartyIdentification/cbc:ID" contains more than just the field.
    // These intermediary elements are very simple items and have no nodeId.
    final String xpathRel = getTextStrict(fieldMeta, FIELD_XPATH_RELATIVE); // "xpathRelative"

    // If a field or node is repeatable, the XML element to repeat is the first XML
    // element in the xpathRelative.
    // final boolean fieldMetaRepeatable = FieldsAndNodes.isFieldRepeatableStatic(fieldMeta);

    Element previousElem = xmlNodeElem;
    Element partElem = null;

    final List<String> parts = getXpathParts(xpathRel);
    if (debug) {
      System.out.println(depthStr + " FIELD PARTS SIZE: " + parts.size());
      System.out.println(depthStr + " FIELD PARTS: " + listToString(parts));
    }

    // final String attrTemp = "temp";
    for (final String partXpath : parts) {

      final PhysicalXpathPart px = buildXpathPart(partXpath);
      // final String xpathExpr = px.getXpathExpr();
      final String tagOrAttr = px.getTagOrAttribute();

      // Example:
      // ... efbc:OverallApproximateFrameworkContractsAmount/@currencyID", the last part.
      final boolean isAttributePart = tagOrAttr.startsWith("@") && tagOrAttr.length() > 1;
      Validate.isTrue(!isAttributePart, "An attribute should not appear here! fieldId=%s", fieldId);

      // Create an XML element.
      partElem = createElemXml(doc, tagOrAttr, debug, depthStr, sb, "field");
      // partElem.setAttribute(attrTemp, attrTemp);

      previousElem.appendChild(partElem); // SIDE-EFFECT! Adding item to the XML doc tree.
      previousElem = partElem;
    } // End of for loop on parts of relative xpath.

    // We arrived at the end of the relative xpath of the field.
    // By design of the above algorithm the last element is always a leaf: the current field element
    final Element lastFieldElem = partElem;
    Validate.notNull(lastFieldElem, "fieldElem is null for fieldId=%s, xpathRel=%s", fieldId,
        xpathRel);

    // Set the attributes of this element.
    for (final Entry<String, String> entry : attributeNameAndValueMap.entrySet()) {

      final String attributeName = entry.getKey();
      if (StringUtils.isNotBlank(lastFieldElem.getAttribute(attributeName))) {
        // If the attribute had already been set this would overwrite the existing value.
        // There is no case for which this is desirable.
        throw new RuntimeException(String.format(
            "Double set: Attribute already set, attributeName=%s, fieldId=%s", attributeName,
            fieldId));
      }

      final String attributeValue;
      if (XML_ATTR_LIST_NAME.equals("attributeName")
          && lastFieldElem.getNodeName().equals("cbc:CapabilityTypeCode")) {
        // HARDCODED: This will be fixed, probably in SDK 1.9 or SDK 1.10.
        attributeValue = "sector";
      } else {
        attributeValue = entry.getValue();
      }
      lastFieldElem.setAttribute(attributeName, attributeValue); // SIDE-EFFECT!
    }

    if (debug) {
      // This makes the XML invalid, it is meant to be read by humans to help understand the XML.
      // These attributes are also useful in unit tests for easy checking of field by id.
      // This keeps the original visual / conceptual fieldId in the physical model.
      // Another concept could be to set XML comments but this can also be a problem in unit tests.
      lastFieldElem.setAttribute(XML_ATTR_EDITOR_FIELD_ID, fieldId);

      final String counter = Integer.toString(conceptField.getCounter());
      lastFieldElem.setAttribute(XML_ATTR_EDITOR_COUNTER_SELF, counter);
    }

    // Set value of the field.
    Validate.notNull(fieldValue, "value is null for fieldId=%s", fieldId, "fieldId=" + fieldId);
    lastFieldElem.setTextContent(fieldValue);
  }

  /**
   * If the passed field has attributes the attribute name and value will be put in a map and return
   * the map.
   *
   * @param fieldsAndNodes Holds SDK field and node metadata
   * @param fieldId The field ID for the field for which the attributes have to be determined
   * @param fieldMeta The SDK metadata about the field
   * @param attributeFieldById Only used for reading
   * @param fieldValue The value of the field
   * @return A map with the determined attribute name and value for the passed field
   */
  private static Map<String, String> determineAttributes(final FieldsAndNodes fieldsAndNodes,
      final Map<String, ConceptTreeField> attributeFieldById, final String fieldId,
      final JsonNode fieldMeta, final String fieldValue) {
    // Find attribute field ids of this SDK field.
    final List<String> attributeFieldIds =
        JsonUtils.getListOfStrings(fieldMeta, FieldsAndNodes.ATTRIBUTES);
    final Map<String, String> attributeNameAndValues =
        new HashMap<>(attributeFieldIds.size(), 1.0f);
    for (final String attributeFieldId : attributeFieldIds) {
      final ConceptTreeField conceptAttribute = attributeFieldById.get(attributeFieldId);
      final JsonNode sdkAttrMeta = fieldsAndNodes.getFieldById(attributeFieldId);
      final String attrName = JsonUtils.getTextStrict(sdkAttrMeta, FieldsAndNodes.ATTRIBUTE_NAME);
      if (conceptAttribute != null) {
        final String attributeValue = conceptAttribute.getValue(); // Value from the form.
        if (StringUtils.isNotBlank(attributeValue)) {
          attributeNameAndValues.put(attrName, attributeValue);
        } else {
          // It does not make sense for the value to be blank, but it can happen if there is no
          // front-end validation and we want an empty form to be processed, otherwise we would have
          // to only pass a fully filled and valid form, which is not easy in the editor demo.
          // We want to set the attributes as those are used in the xpath expressions which are used
          // to locate elements.
          inferAttribute(sdkAttrMeta, attributeNameAndValues, attrName, fieldMeta, fieldValue);
        }
      } else {
        inferAttribute(sdkAttrMeta, attributeNameAndValues, attrName, fieldMeta, fieldValue);
      }
    }
    return attributeNameAndValues;
  }

  /**
   * Some attributes can be determined automatically. They do not need to be put in the forms (or
   * they could but would need to be hidden).
   *
   * @param sdkAttrMeta SDK metadata about the attribute field
   * @param fieldMeta SDK metadata about the field having the attribute
   * @param fieldValue The value of the field having the attributes
   */
  private static void inferAttribute(final JsonNode sdkAttrMeta,
      final Map<String, String> attributeNameAndValues, final String attributeName,
      final JsonNode fieldMeta, final String fieldValue) {
    final Optional<String> presetValueOpt =
        JsonUtils.getTextOpt(sdkAttrMeta, FieldsAndNodes.PRESET_VALUE);
    if (presetValueOpt.isPresent()) {
      // For example:
      // "id" : "OPP-070-notice-List",
      // "presetValue" : "notice-subtype",
      final String presetValue = presetValueOpt.get();
      attributeNameAndValues.put(attributeName, presetValue);
    } else {
      logger.info("No presetValue: infer attribute? fieldId={}, fieldValue={}, attrName={}",
          JsonUtils.getTextStrict(sdkAttrMeta, FieldsAndNodes.ID), fieldValue,
          attributeName);
      if ("id-ref".equals(JsonUtils.getTextStrict(fieldMeta, FieldsAndNodes.FIELD_TYPE))
          && StringUtils.isNotBlank(fieldValue)
          && XML_ATTR_SCHEME_NAME.equals(attributeName)) {
        // Example:
        // In some cases for id ref there are two choices, for example ORG or TPO,
        // the user selects one either ORG or TPO.
        // Assuming the value in the field is "ORG-0001" or "TPO-0001", we want "ORG" or "TPO".
        final int indexOfDash = fieldValue.indexOf('-');
        if (indexOfDash > 0) {
          // In the database we have the "identifier_scheme".
          final String idPrefix = fieldValue.substring(0, indexOfDash);
          logger.info("idPrefix={}", idPrefix);
          final String attrValue;
          // HARDCODED until we have that mapping in the SDK.
          switch (idPrefix) {
            case "CON":
              attrValue = "contract";
              break;
            case "GLO":
              attrValue = "LotsGroup";
              break;
            case "LOT":
              attrValue = "Lot";
              break;
            case "ORG":
              attrValue = "organization";
              break;
            case "PAR":
              attrValue = "Part";
              break;
            case "RES":
              attrValue = "result";
              break;
            case "TEN":
              attrValue = "tender";
              break;
            case "TPA":
              attrValue = "tendering-party";
              break;
            case "TPO":
              attrValue = "touchpoint";
              break;
            case "UBO":
              attrValue = "ubo";
              break;
            default:
              throw new RuntimeException(String.format("Unknown id pattern: %s", idPrefix));
          }
          attributeNameAndValues.put(attributeName, attrValue);
        }
      }
    }
  }

  /**
   * Gets information about the document type from the SDK.
   *
   * @param noticeInfoBySubtype Map with info about notice metadata by notice sub type
   * @param documentInfoByType Map with info about document metadata by document type
   */
  public static DocumentTypeInfo getDocumentTypeInfo(
      final Map<String, JsonNode> noticeInfoBySubtype,
      final Map<String, JsonNode> documentInfoByType, final ConceptualModel concept) {

    logger.info("Attempting to read document type info");
    final JsonNode noticeInfo = noticeInfoBySubtype.get(concept.getNoticeSubType());

    // Get the document type info from the SDK data.
    final String documentType =
        JsonUtils.getTextStrict(noticeInfo, SdkConstants.NOTICE_TYPES_JSON_DOCUMENT_TYPE_KEY);

    final JsonNode documentTypeInfo = documentInfoByType.get(documentType);
    return new DocumentTypeInfo(documentTypeInfo, concept.getSdkVersion());
  }

  /**
   * Set XML namespaces.
   *
   * @param docTypeInfo SDK document type info
   * @param rootElement The root element of the XML
   * @return XPath instance with prefix to namespace awareness
   */
  public static XPath setXmlNamespaces(final DocumentTypeInfo docTypeInfo,
      final Element rootElement) {
    Validate.notNull(rootElement);

    final String namespaceUriRoot = docTypeInfo.getNamespaceUri();
    Validate.notBlank(namespaceUriRoot);

    //
    // NAMESPACES FOR THE XML DOCUMENT.
    //
    rootElement.setAttribute(XMLNS, namespaceUriRoot);

    // Since SDK 1.6.0 the SDK provides this information (TEDEFO-1744).
    // If these namespaces evolve they could start to differ by SDK version.
    // This is why they have been moved to the SDK metadata.
    final Map<String, String> map = docTypeInfo.buildAdditionalNamespaceUriByPrefix();

    final String xmlnsUri = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    for (final Entry<String, String> entry : map.entrySet()) {
      rootElement.setAttributeNS(xmlnsUri, XMLNS + ":" + entry.getKey(), entry.getValue());
    }
    return XpathUtils.setupXpathInst(docTypeInfo, Optional.of(map));
  }

  /**
   * @param xpath A valid xpath string
   * @return The xpath string split by slash, the predicates are present in the output
   */
  public static List<String> getXpathParts(final String xpath) {
    final StringBuilder sb = new StringBuilder(xpath.length());
    int stacked = 0;
    for (int i = 0; i < xpath.length(); i++) {
      final char ch = xpath.charAt(i);
      if (ch == '[') {
        stacked++;
      } else if (ch == ']') {
        stacked--;
        Validate.isTrue(stacked >= 0, "stacked is < 0 for %s", xpath);
      }
      // If we are inside of a predicate, replace '/' by a temporary replacement.
      sb.append(ch == '/' && stacked > 0 ? XPATH_TEMP_REPLACEMENT : ch);
    }

    // The predicates are not split thanks to usage of the temporary replacement.
    final String originalRegex = "/";
    final char originalChar = '/';
    final String[] split = sb.toString().split(originalRegex);

    // Inside each part, put back the original character.
    return Arrays.stream(split)
        .map(item -> item.replace(XPATH_TEMP_REPLACEMENT, originalChar))
        .collect(Collectors.toList());
  }

  /**
   * @param xpath A valid xpath string
   * @return The xpath string split by slash, the predicates are removed before the split
   */
  public static List<String> getXpathPartsWithoutPredicates(final String xpath) {
    final StringBuilder sb = new StringBuilder(xpath.length());

    int stacked = 0;
    for (int i = 0; i < xpath.length(); i++) {
      final char ch = xpath.charAt(i);
      if (ch == XPATH_TEMP_REPLACEMENT) {
        throw new RuntimeException(String.format("Found temp replacement character: %s in %s",
            XPATH_TEMP_REPLACEMENT, xpath));
      }
      if (ch == '[') {
        stacked++;
      } else if (ch == ']') {
        stacked--;
        Validate.isTrue(stacked >= 0, "stacked is < 0 for %s", xpath);
      }
      // If we are inside of a predicate, replace '/' by a temporary replacement.
      if (stacked == 0 && ch != ']') {
        sb.append(ch);
      }
    }

    // The predicates are not split thanks to usage of the temporary replacement.
    final String originalRegex = "/";
    final char originalChar = '/';
    final String[] split = sb.toString().split(originalRegex);

    // Inside each part, put back the original character.
    return Arrays.stream(split)
        .map(item -> item.replace(XPATH_TEMP_REPLACEMENT, originalChar))
        .collect(Collectors.toList());
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "UCPM_USE_CHARACTER_PARAMETERIZED_METHOD",
      justification = "OK here, used in other places as a string")
  private static PhysicalXpathPart buildXpathPart(final String xpathPart) {
    Validate.notBlank(xpathPart, "partParam is blank");

    // NOTE: ideally we would want to avoid parsing xpath as much as possible.
    // Since SDK 1.9 the attributes are provided via fields "attributes".
    String tagName = xpathPart;

    // We want to remove the predicate as we only want the name.
    if (tagName.indexOf('[') > 0) {
      // Example: "cbc:somename[xyz]", we want to only keep "cbc:somename"
      tagName = tagName.substring(0, tagName.indexOf('['));
    }

    if (tagName.indexOf(XPATH_TEMP_REPLACEMENT) >= 0) {
      tagName = tagName.substring(0, tagName.indexOf(XPATH_TEMP_REPLACEMENT));
    }

    // For the xpath expression keep the original param, only do the replacement.
    final String xpathExpr = xpathPart.replace(XPATH_TEMP_REPLACEMENT, '/');
    Validate.notBlank(xpathExpr, "xpathExpr is blank for tag=%s, partParam=%s", tagName,
        xpathExpr);
    return new PhysicalXpathPart(xpathExpr, tagName);
  }

  /**
   * Builds a W3C DOM element.
   *
   * @param tagName The XML element tag name
   * @param type
   *
   * @return A W3C DOM element (note that it is not attached to the DOM yet)
   */
  private static final Element createElemXml(final Document doc, final String tagName,
      final boolean debug, final String depthStr, final StringBuilder sb, final String type) {
    // This removes the xmlns="" that Saxon adds.
    try {
      if (tagName.startsWith("@")) {
        throw new RuntimeException(
            String.format("Expecting a tag but this is an attribute: %s", tagName));
      }
      if (debug) {
        sb.append(depthStr).append(" creating: ")
            .append(tagName)
            .append(" with type=").append(type)
            .append('\n');
      }
      logger.debug("Creating element: {} for {}", tagName, type);
      return doc.createElementNS("", tagName);
    } catch (org.w3c.dom.DOMException ex) {
      logger.error("Problem creating element with tagName={}", tagName);
      throw ex;
    }
  }

  /**
   * @return list to string without the brackets.
   */
  public static String listToString(final List<String> list) {
    if (list.isEmpty()) {
      return "";
    }
    final String text = list.toString();
    return text.substring(1, text.length() - 1);
  }

}
