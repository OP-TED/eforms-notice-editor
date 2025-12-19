package eu.europa.ted.eforms.noticeeditor.helper.notice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.noticeeditor.service.SdkService;
import eu.europa.ted.eforms.sdk.SdkVersion;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putFieldDef;
import static eu.europa.ted.eforms.noticeeditor.helper.notice.VisualModel.putGroupDef;

/**
 * A test with focus on save to xml "repeatability", including nested repeatable groups.
 */
@SpringBootTest
public class SaveNoticeRepeatableTest extends SaveNoticeTest {

	private static final Logger logger = LoggerFactory.getLogger(SaveNoticeRepeatableTest.class);

	/**
	 * It could be anything for this test dummy, but we will use BRIN.
	 */
	private static final String NOTICE_DOCUMENT_TYPE = "BRIN";

	private static final String ND_A = "ND-A";
	private static final String ND_B = "ND-B";

	private static final String BT_FIELD_DUMMY_C_REP = "BT-field-c";

	@Autowired
	protected SdkService sdkService;

	@Override
	protected VisualModel setupVisualModel(final ObjectMapper mapper, final SdkVersion sdkVersion, final String noticeSubTypeForTest) {

		final VisualModel visualModel = super.setupVisualModel(mapper, sdkVersion, noticeSubTypeForTest);
		final VisualTreeElement visRoot = visualModel.getVisualRoot();
		final List<VisualTreeElement> visRootChildren = visualModel.getVisRootChildren();

		//
		// NOTICE CONTENT.
		//

		// For the test data we want nested repeatability (repeating groups inside a repeating group).
		// On the left is the content
		// On the right is the node id (without the GR- prefix to simplify):
		// X -> Y "means X has child Y and Y is child of X"

		// THE_CONTENT -> A1
		// A1 -> A1-B1
		// A1 -> A1-B2

		// THE_CONTENT -> A2
		// A2 -> A2-B1

		// ---------------------------
		// A1.
		// ---------------------------
		{
			final VisualTreeElement visGroupA1 = new VisualTreeElement();
			visRootChildren.add(visGroupA1);
			putGroupDef(visGroupA1);
			visGroupA1.setContentId("GR-A1");
			visGroupA1.setVisNodeId(ND_A);
			visGroupA1.setRepeatable(true);
			final List<VisualTreeElement> a1Children = new ArrayList<>();
			visGroupA1.setChildren(a1Children);

			final VisualTreeElement visGroupB1 = new VisualTreeElement();
			a1Children.add(visGroupB1);
			putGroupDef(visGroupB1);
			visGroupB1.setContentId("GR-A1-B1");
			visGroupB1.setVisNodeId(ND_B);
			List<VisualTreeElement> b1Children = new ArrayList<>();
			visGroupB1.setChildren(b1Children);
			visGroupB1.setRepeatable(true);

			// Add dummy field in B1.
			final VisualTreeElement visFieldC1 = new VisualTreeElement();
			b1Children.add(visFieldC1);
			putFieldDef(visFieldC1, BT_FIELD_DUMMY_C_REP);
			visFieldC1.setValue("dummy_value");
			visFieldC1.setRepeatable(true);

			final VisualTreeElement visGroupB2 = new VisualTreeElement();
			a1Children.add(visGroupB2);
			putGroupDef(visGroupB2);
			visGroupB2.setContentId("GR-A1-B2");
			visGroupB2.setVisNodeId(ND_B);
			List<VisualTreeElement> b2Children = new ArrayList<>();
			visGroupB2.setChildren(b2Children);
			visGroupB2.setRepeatable(true);
			visGroupB2.setContentCount(2); // Override default.

			// Add dummy field in B2.
			final VisualTreeElement visFieldC2 = new VisualTreeElement();
			b2Children.add(visFieldC2);
			putFieldDef(visFieldC2, BT_FIELD_DUMMY_C_REP);
			visFieldC2.setValue("dummy_value");
			visFieldC2.setRepeatable(true);
		}

		// ---------------------------
		// A2.
		// ---------------------------
		{
			final VisualTreeElement visGroupA2 = new VisualTreeElement();
			visRootChildren.add(visGroupA2);
			putGroupDef(visGroupA2);
			visGroupA2.setContentId("GR-A2");
			visGroupA2.setVisNodeId(ND_A);
			visGroupA2.setContentCount(2); // Override default.
			final List<VisualTreeElement> a2Children = new ArrayList<>();
			visGroupA2.setChildren(a2Children);
			visGroupA2.setRepeatable(true);

			// Add B1 in A2.
			final VisualTreeElement visGroupB1 = new VisualTreeElement();
			a2Children.add(visGroupB1);
			putGroupDef(visGroupB1);
			visGroupB1.setContentId("GR-A2-B1");
			visGroupB1.setVisNodeId(ND_B);
			final List<VisualTreeElement> b1Children = new ArrayList<>();
			visGroupB1.setChildren(b1Children);
			visGroupB1.setRepeatable(true);

			// Add C in A2 B1.
			final VisualTreeElement visGroupC = new VisualTreeElement();
			b1Children.add(visGroupC);
			putGroupDef(visGroupC);
			visGroupC.setContentId("GR-A2-B1-C1");
			final List<VisualTreeElement> groupcChildren = new ArrayList<>();
			visGroupC.setChildren(groupcChildren);

			// Add dummy field in C.
			final VisualTreeElement visFieldC1 = new VisualTreeElement();
			groupcChildren.add(visFieldC1);
			putFieldDef(visFieldC1, BT_FIELD_DUMMY_C_REP);
			visFieldC1.setValue("value-of-field-c1");
			visFieldC1.setRepeatable(true);

			// Add another dummy field in C.
			final VisualTreeElement visFieldC2 = new VisualTreeElement();
			groupcChildren.add(visFieldC2);
			putFieldDef(visFieldC2, BT_FIELD_DUMMY_C_REP, 2);
			visFieldC2.setValue("value-of-field-c2");
			visFieldC2.setRepeatable(true);
		}

		final Map<String, JsonNode> nodeById = setupFieldsJsonXmlStructureNodes(mapper);
		final Map<String, JsonNode> fieldById = setupFieldsJsonFields(mapper);
		final FieldsAndNodes fieldsAndNodes = new FieldsAndNodes(fieldById, nodeById, sdkVersion);

		return new VisualModel(visRoot, fieldsAndNodes);
	}

	/**
	 * Setup dummy node metadata.
	 *
	 * @param nodeById This map will be modified as a SIDE-EFFECT
	 */
	@Override
	protected Map<String, JsonNode> setupFieldsJsonXmlStructureNodes(final ObjectMapper mapper) {
		final Map<String, JsonNode> nodeById = super.setupFieldsJsonXmlStructureNodes(mapper);
		{
			final ObjectNode node = mapper.createObjectNode();
			nodeById.put(ND_A, node);
			node.put(KEY_NODE_ID, ND_A);
			node.put(KEY_NODE_PARENT_ID, ND_ROOT);
			node.put(KEY_XPATH_ABS, "/*/a");
			node.put(KEY_XPATH_REL, "a");
			SaveNoticeTest.putXsdSequenceOrder(node, Map.of("a", 1), mapper);
			SaveNoticeTest.fieldPutRepeatable(node, true);
		}
		{
			final ObjectNode node = mapper.createObjectNode();
			nodeById.put(ND_B, node);
			node.put(KEY_NODE_ID, ND_B);
			node.put(KEY_NODE_PARENT_ID, ND_A);
			node.put(KEY_XPATH_ABS, "/*/a/b");
			node.put(KEY_XPATH_REL, "b");
			SaveNoticeTest.putXsdSequenceOrder(node, Map.of("b", 1), mapper);
			SaveNoticeTest.fieldPutRepeatable(node, true);
		}
		return Collections.unmodifiableMap(nodeById);
	}

	/**
	 * Setup dummy field metadata.
	 *
	 * @param fieldById This map will be modified as a SIDE-EFFECT
	 */
	@Override
	protected Map<String, JsonNode> setupFieldsJsonFields(final ObjectMapper mapper) {
		final Map<String, JsonNode> fieldById = super.setupFieldsJsonFields(mapper);

		// Add a repeatable field to also cover field repeatability.
		final ObjectNode field = mapper.createObjectNode();
		fieldById.put(BT_FIELD_DUMMY_C_REP, field);
		field.put(KEY_FIELD_ID, BT_FIELD_DUMMY_C_REP);
		field.put(KEY_PARENT_NODE_ID, ND_B);
		field.put(KEY_XPATH_ABS, "/*/a/b/c");
		field.put(KEY_XPATH_REL, "c");
		field.put(KEY_TYPE, TYPE_TEXT);
		SaveNoticeTest.putXsdSequenceOrder(field, Map.of("c", 1), mapper);
		SaveNoticeTest.fieldPutRepeatable(field, true);

		return fieldById;
	}

	@Test
	public final void test() throws ParserConfigurationException, IOException, SAXException {
		final ObjectMapper mapper = new ObjectMapper();
		// A dummy 1.8.0, not real 1.8.0
		final SdkVersion sdkVersion = new SdkVersion("1.8.0");
		final String prefixedSdkVersion = VersionHelper.prefixSdkVersionWithoutPatch(sdkVersion);
		final String noticeSubType = "X02"; // A dummy X02, not the real X02 of 1.8.0

		final VisualModel visualModel = setupVisualModel(mapper, sdkVersion, noticeSubType);

		final PhysicalModel physicalModel = setupPhysicalModel(mapper, noticeSubType, NOTICE_DOCUMENT_TYPE, visualModel, sdkService.getSdkRootFolder(),
				sdkVersion);

		final String xml = physicalModel.toXmlText(false); // Not indented to avoid line breaks.
		logger.info(physicalModel.toXmlText(true));

		// IDEA it would be more maintainable to use xpath to check the XML instead of pure text.
		// physicalModel.evaluateXpathForTests("/", "test2");

		checkCommon(prefixedSdkVersion, noticeSubType, xml);

		count(xml, 1, "<cbc:CustomizationID");
		count(xml, 1, "<cbc:SubTypeCode");

		// Verify repeatable nodes at top level.
		count(xml, 2, "<a");
		count(xml, 2, "editorNodeId=\"ND-A\"");

		// Verify nested repeatable nodes.
		count(xml, 3, "<b"); // 3 in total
		count(xml, 3, "editorNodeId=\"ND-B\""); // 3 in total
		count(xml, 2, "editorCounterSelf=\"1\" editorNodeId=\"ND-B\"");
		count(xml, 1, "editorCounterSelf=\"2\" editorNodeId=\"ND-B\"");

		// Verify repeatable field.

		// Ensure the field is indeed repeatable so that the test itself is not broken.
		assert physicalModel.getFieldsAndNodes().isFieldRepeatable(BT_FIELD_DUMMY_C_REP) : BT_FIELD_DUMMY_C_REP + " should be repeatable";

		// c1
		count(xml, 1, "editorFieldId=\"BT-field-c\">value-of-field-c1</c>");
		count(xml, 1, "editorCounterSelf=\"1\" editorFieldId=\"BT-field-c\">value-of-field-c1</c>");

		// c2
		count(xml, 1, "editorFieldId=\"BT-field-c\">value-of-field-c2</c>");
		count(xml, 1, "editorCounterSelf=\"2\" editorFieldId=\"BT-field-c\">value-of-field-c2</c>");
	}

}
