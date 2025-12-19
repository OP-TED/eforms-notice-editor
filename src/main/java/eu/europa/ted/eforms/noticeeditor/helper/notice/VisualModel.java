package eu.europa.ted.eforms.noticeeditor.helper.notice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import eu.europa.ted.eforms.noticeeditor.helper.VersionHelper;
import eu.europa.ted.eforms.sdk.SdkVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.noticeeditor.util.GraphvizDotTool;
import eu.europa.ted.eforms.noticeeditor.util.JavaTools;
import eu.europa.ted.eforms.noticeeditor.util.JsonUtils;

/**
 * Visual model (VM).
 *
 * <p>
 * Wrapper around the JSON representation of the form data. Vis is be used as a shorthand for
 * visual.
 * </p>
 * <p>
 * NOTE: the form data hierarchy is supposed to follow the SDK notice-types definitions (.json)
 * hierarchy for the given SDK and notice sub type.
 * </p>
 * <p>
 * NOTE: the Jackson xyzNode objects are not related to the SDK node concept, it is just that the
 * term "node" is commonly used for items of a tree (tree nodes) and that JSON data is hierarchical.
 * </p>
 */
public class VisualModel {

	private static final Logger logger = LoggerFactory.getLogger(VisualModel.class);

	private static final String VIS_TYPE_FIELD = "field";
	private static final String VIS_TYPE_NON_FIELD = "non-field";

	public static final String PRESET_VALUE = "presetValue";

	/**
	 * As we use a web UI the data is received as JSON. We work directly on this JSON tree model.
	 */
	private final VisualTreeElement visualRoot;
	private final FieldsAndNodes fieldsAndNodes;
	private final SdkVersion sdkVersion;

	@Override
	public String toString() {
		try {
			return JsonUtils.getStandardJacksonObjectMapper().writeValueAsString(visualRoot);
		} catch (JsonProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * @param visualRoot     The visual model as serialized JSON, usually set from a user interface.
	 * @param fieldsAndNodes FieldAndNodes for the specific sdk version
	 */
	public VisualModel(final VisualTreeElement visualRoot, FieldsAndNodes fieldsAndNodes) {
		Validate.isTrue(ConceptualModel.ND_ROOT.equals(visualRoot.getVisNodeId()), "Visual model root must be %s", ConceptualModel.ND_ROOT);

		//Delete from visual tree scheme ids from empty scheme instances
		//This should better be done from the UI
		visualRoot.clearEmptyInstances();

		this.visualRoot = visualRoot;
		this.fieldsAndNodes = fieldsAndNodes;

		final String eformsSdkVersion = visualRoot.getSdkVersion();
		Validate.notBlank(eformsSdkVersion, "virtual root eFormsSdkVersion is blank");
		this.sdkVersion = VersionHelper.parsePrefixedSdkVersion(eformsSdkVersion);
	}

	public VisualTreeElement getVisualRoot() {
		return this.visualRoot;
	}

	public List<VisualTreeElement> getVisRootChildren() {
		return visualRoot.getChildren();
	}

	/**
	 * Set default field info.
	 */
	static void putFieldDef(final VisualTreeElement vis, final String fieldId) {
		putFieldDef(vis, fieldId, 1);
	}

	/**
	 * Set default field info.
	 */
	static void putFieldDef(final VisualTreeElement vis, final String fieldId, final int count) {
		vis.setVisType(VIS_TYPE_FIELD);
		vis.setContentId(fieldId);
		vis.setContentCount(count);
	}

	/**
	 * Set default group info.
	 */
	static void putGroupDef(final VisualTreeElement vis) {
		vis.setVisType(VIS_TYPE_NON_FIELD);
		vis.setContentCount(1);
	}

	/**
	 * Common code which can be used in unit tests. It is placed here as it is about the visual model.
	 */
	public static void setupVisualRootForTest(final String fakeSdkForTest, final String noticeSubTypeForTest, final VisualTreeElement visRoot) {
		putGroupDef(visRoot);

		// TODO remove this ND ROOT from here.
		visRoot.setVisNodeId(ConceptualModel.ND_ROOT);

		// Put some primary info at the top level.
		visRoot.setSdkVersion(fakeSdkForTest);
		visRoot.setNoticeSubType(noticeSubTypeForTest);

		final String noticeUuid = UUID.randomUUID().toString();
		visRoot.setNoticeUuid(noticeUuid);

		final List<VisualTreeElement> visRootChildren = new ArrayList<>();
		visRoot.setChildren(visRootChildren);

		//
		// DUMMY NOTICE METADATA (as if coming from a web form before we have the XML).
		//
		{
			// SDK version.
			final VisualTreeElement visSdkVersion = new VisualTreeElement();
			visRootChildren.add(visSdkVersion);
			putFieldDef(visSdkVersion, ConceptualModel.FIELD_ID_SDK_VERSION);
			visSdkVersion.setValue(fakeSdkForTest);

			final VisualTreeElement metadata = new VisualTreeElement();
			visRootChildren.add(metadata);
			putGroupDef(metadata);
			metadata.setContentId("notice-metadata");
			final List<VisualTreeElement> metadataChildren = new ArrayList<>();
			metadata.setChildren(metadataChildren);

			// Notice sub type.
			final VisualTreeElement visNoticeSubType = new VisualTreeElement();
			metadataChildren.add(visNoticeSubType);
			putFieldDef(visNoticeSubType, ConceptualModel.FIELD_NOTICE_ID);
			visNoticeSubType.setValue(noticeUuid);
		}

		// Root extension.
		{
			final VisualTreeElement visRootExtension = new VisualTreeElement();
			visRootChildren.add(visRootExtension);
			putGroupDef(visRootExtension);
			visRootExtension.setContentId("the_root_extension");
			visRootExtension.setVisNodeId(ConceptualModel.ND_ROOT_EXTENSION);
			final List<VisualTreeElement> visRootExtChildren = new ArrayList<>();
			visRootExtension.setChildren(visRootExtChildren);

			// Notice sub type.
			final VisualTreeElement visNoticeSubType = new VisualTreeElement();
			visRootExtChildren.add(visNoticeSubType);
			putFieldDef(visNoticeSubType, ConceptualModel.FIELD_ID_NOTICE_SUB_TYPE);
			visNoticeSubType.setValue(noticeSubTypeForTest);
		}

	}

	/**
	 * Build the conceptual model from the visual model.
	 *
	 * @return The conceptual model for this visual model
	 */
	public ConceptualModel toConceptualModel() {
		logger.info("Attempting to build the conceptual model from the visual model.");

		// This is located in this class as most of the code is about reading the visual model.

		ConceptTreeNode rootNode = new ConceptTreeNode(visualRoot.getVisNodeId(), visualRoot.getVisNodeId(), visualRoot.getContentCount(),
				visualRoot.isRepeatable());

		visitElement(visualRoot, rootNode);
		clear(rootNode);
		return new ConceptualModel(rootNode, sdkVersion);
	}

	/**
	 * Recursive method that process each element of the visual model and created the conceptual model
	 *
	 * @param element                The element of the visual model
	 * @param lastRepeatableAncestor A conceptual node that is the last that was created and its visual element counterpart is a parent of the given element
	 */
	private void visitElement(VisualTreeElement element, ConceptTreeNode lastRepeatableAncestor) {
		logger.debug("Visiting element: {}", element.getContentId());

		if (element.getVisType().equals(VIS_TYPE_FIELD)) {
			createField(element, lastRepeatableAncestor);
		} else if (element.getVisType().equals(VIS_TYPE_NON_FIELD)) {
			if (element.isRepeatable() && StringUtils.isNotEmpty(element.getVisNodeId())) {
				ConceptTreeNode node = createNode(element, lastRepeatableAncestor);
				for (VisualTreeElement child : element.getChildren()) {
					visitElement(child, node);
				}
			} else {
				for (VisualTreeElement child : element.getChildren()) {
					visitElement(child, lastRepeatableAncestor);
				}
			}
		} else {
			throw new RuntimeException("Unknown VisType");
		}
	}

	/**
	 * Creates a conceptual node
	 *
	 * @param element                The element of the visual model
	 * @param lastRepeatableAncestor A conceptual node that is the last that was created and its visual element counterpart is a parent of the given element
	 * @return The created conceptual node
	 */
	private ConceptTreeNode createNode(VisualTreeElement element, ConceptTreeNode lastRepeatableAncestor) {
		JsonNode nodeMeta = fieldsAndNodes.getNodeById(element.getVisNodeId());
		ConceptTreeNode parentNode = createParents(JsonUtils.getTextStrict(nodeMeta, FieldsAndNodes.NODE_PARENT_NODE_ID), lastRepeatableAncestor);

		ConceptTreeNode node = new ConceptTreeNode(element.getContentId() + "-" + element.getContentCount(), element.getVisNodeId(), element.getContentCount(),
				element.isRepeatable());
		parentNode.addConceptNode(node, true);

		return node;
	}

	/**
	 * Created a conceptual field
	 *
	 * @param element                The element of the visual model
	 * @param lastRepeatableAncestor A conceptual node that is the last that was created and its visual element counterpart is a parent of the given element
	 * @return The created conceptual element
	 */
	private ConceptTreeField createField(VisualTreeElement element, ConceptTreeNode lastRepeatableAncestor) {
		if (StringUtils.isEmpty(element.getValue())) {
			Optional<String> presetValue = getPresetValue(element.getContentId());
			if (presetValue.isPresent()) {
				element.setValue(presetValue.get());
			} else {
				return null;
			}
		}

		JsonNode fieldMeta = fieldsAndNodes.getFieldById(element.getContentId());
		ConceptTreeNode parentNode = createParents(JsonUtils.getTextStrict(fieldMeta, FieldsAndNodes.FIELD_PARENT_NODE_ID), lastRepeatableAncestor);

		String newFieldId = element.isRepeatable() ? element.getContentId() + "-" + element.getContentCount() : element.getContentId();

		ConceptTreeField field = new ConceptTreeField(newFieldId, element.getContentId(), element.getValue(), element.getContentCount(),
				element.getLanguageId());
		parentNode.addConceptField(field);

		return field;
	}

	/**
	 * Recursive method that creates the node for the given id and any intermediate parent up to the lastRepeatableAncestor node. If a node already exists, it
	 * will not be created
	 *
	 * @param nodeId                 - The id of the node to create
	 * @param lastRepeatableAncestor A conceptual node that is the last that was created and is an ancestor of the given node.
	 * @return The created parent node
	 */
	private ConceptTreeNode createParents(String nodeId, ConceptTreeNode lastRepeatableAncestor) {
		if (nodeId.equals(lastRepeatableAncestor.getNodeId())) {
			return lastRepeatableAncestor;
		}

		JsonNode parentNodeMeta = fieldsAndNodes.getNodeById(nodeId);
		ConceptTreeNode parentNode = createParents(JsonUtils.getTextStrict(parentNodeMeta, "parentId"), lastRepeatableAncestor);

		Optional<ConceptTreeNode> existingNode = parentNode.getConceptNodes().stream().filter(conceptTreeNode -> conceptTreeNode.getNodeId().equals(nodeId))
				.findFirst();
		if (existingNode.isPresent()) {
			return existingNode.get();
		}

		ConceptTreeNode newNode = new ConceptTreeNode(nodeId, nodeId, 1, false);
		parentNode.addConceptNode(newNode, true);

		return newNode;
	}

	/**
	 * Resolve the preset value of a field
	 *
	 * @param fieldId The id of a field
	 * @return An Optional String of the resolved preset value
	 */
	private Optional<String> getPresetValue(String fieldId) {
		JsonNode meta = fieldsAndNodes.getFieldById(fieldId);
		Optional<String> presetValue = JsonUtils.getTextOpt(meta, PRESET_VALUE);

		if (presetValue.isPresent() && presetValue.get().equals("{NOW}")) {
			ZonedDateTime now = Instant.now().truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault());
			String type = JsonUtils.getTextStrict(meta, "type");
			switch (type) {
				case "date":
					return Optional.of(DateTimeFormatter.ISO_OFFSET_DATE.format(now));
				case "time":
					return Optional.of(DateTimeFormatter.ISO_OFFSET_TIME.format(now));
				default:
					throw new IllegalArgumentException("Unsupported datatype " + type + " for presetValue pattern " + presetValue.get());
			}
		}

		return presetValue;
	}

	/**
	 * Removes recursively any empty node of the conceptual model that may be created
	 *
	 * @param node A conceptual node
	 * @return true if the node is empty, otherwise false
	 */
	private boolean clear(ConceptTreeNode node) {
		node.getConceptNodes().removeIf(this::clear);
		return node.getConceptNodes().isEmpty() && (node.getConceptFields().isEmpty() || node.getConceptFields().stream()
				.allMatch(f -> JsonUtils.getTextOpt(fieldsAndNodes.getFieldById(f.idInSdkFieldsJson), PRESET_VALUE).isPresent()));

	}

	/**
	 * This can be used for visualization of the visual model tree (as a graph). The graphviz dot text itself is interesting but it makes even more sense when
	 * seen inside a tool.
	 */
	private String toDot(final FieldsAndNodes fieldsAndNodes, final boolean includeFields) {

		final StringBuilder sb = new StringBuilder(1024);
		toDotRec(fieldsAndNodes, sb, visualRoot, includeFields);

		final StringBuilder sbDot = new StringBuilder();
		final String noticeSubType = visualRoot.getNoticeSubType();
		final String title = "visual_" + noticeSubType; // - is not supported.
		GraphvizDotTool.appendDiGraph(sb.toString(), sbDot, title, "Visual model of " + noticeSubType, false, true);

		return sbDot.toString();
	}

	/**
	 * Recursively create DOT format text and append it to the string builder (sb).
	 *
	 * @param fieldsAndNodes SDK field and node metadata
	 * @param includeFields  If true include fields in the graph, otherwise do not
	 */
	private static void toDotRec(final FieldsAndNodes fieldsAndNodes, final StringBuilder sb, final VisualTreeElement item, final boolean includeFields) {
		final Optional<String> nodeIdOpt = Optional.ofNullable(item.getVisNodeId()).filter(Predicate.not(String::isEmpty));
		final String idUnique = item.getContentId() + (nodeIdOpt.map(s -> "_" + s).orElse(""));

		final String edgeLabel = "";
		// final String edgeLabel = nodeIdOpt.isPresent() ? nodeIdOpt.get() : idUnique;

		// Include children in dot file.
		if (VIS_TYPE_NON_FIELD.equals(item.getVisType())) {
			for (final VisualTreeElement child : item.getChildren()) {

				final Optional<String> childNodeIdOpt = Optional.ofNullable(child.getVisNodeId()).filter(Predicate.not(String::isEmpty));
				final boolean nodeIsRepeatable = fieldsAndNodes.isNodeRepeatable(childNodeIdOpt);
				final String color = nodeIsRepeatable ? GraphvizDotTool.COLOR_GREEN : GraphvizDotTool.COLOR_BLACK;

				final String idUniqueChild = child.getContentId() + (childNodeIdOpt.map(s -> "_" + s).orElse(""));
				GraphvizDotTool.appendEdge(edgeLabel, color, idUnique, idUniqueChild, // concept node -> child concept node
						sb);

				toDotRec(fieldsAndNodes, sb, child, includeFields);
			}
		}
	}

	/**
	 * Write dot graph file for debugging purposes.
	 */
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", justification = "Ok here")
	public void writeDotFile(final FieldsAndNodes fieldsAndNodes) {
		try {
			// Generate dot file for the conceptual model.
			// Visualizing it can help understand how it works or find problems.
			final boolean includeFields = true;
			final String dotText = this.toDot(fieldsAndNodes, includeFields);
			final Path pathToFolder = Path.of("target/dot/");
			Files.createDirectories(pathToFolder);
			final Path pathToFile = pathToFolder.resolve(visualRoot.getNoticeSubType() + "-visual.dot");
			JavaTools.writeTextFile(pathToFile, dotText);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


}
