package eu.europa.ted.eforms.noticeeditor.helper.notice;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Element of the visual model. Used to serialize the JSON notice from the front-end
 */
public class VisualTreeElement {

	private String contentId;

	private String contentType;

	private int contentCount;

	private String visType;

	private String visNodeId;

	private String value;

	private boolean repeatable;

	private String sdkVersion;

	private String noticeSubType;

	private String noticeUuid;

	private String idScheme;

	private String languageId;

	private List<VisualTreeElement> children;

	/**
	 * Recursive method that removes the value of any instance id fields where the instance is empty
	 * @return True is the visual tree element is empty, otherwise False
	 */
	public boolean clearEmptyInstances() {
		if (visType.equals("field")) {
			return  StringUtils.isEmpty(value) || StringUtils.isNotEmpty(idScheme);
		}

		var isEmpty = true;
		for (VisualTreeElement child : children) {
			var isChildEmpty = child.clearEmptyInstances();
			isEmpty = isEmpty && isChildEmpty;
		}

		if (visType.equals("non-field") && StringUtils.isNotEmpty(idScheme) && isEmpty) {
			clearInstanceId(idScheme);
		}

		return isEmpty;
	}

	private void clearInstanceId(String idScheme) {
		for (VisualTreeElement child : children) {
			if (child.visType.equals("field")) {
				if (child.idScheme.equals(idScheme)) {
					child.value = "";
					return;
				}
			} else {
				child.clearInstanceId(idScheme);
			}
		}

	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public int getContentCount() {
		return contentCount;
	}

	public void setContentCount(int contentCount) {
		this.contentCount = contentCount;
	}

	public String getVisType() {
		return visType;
	}

	public void setVisType(String visType) {
		this.visType = visType;
	}

	public String getVisNodeId() {
		return visNodeId;
	}

	public void setVisNodeId(String visNodeId) {
		this.visNodeId = visNodeId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<VisualTreeElement> getChildren() {
		return children;
	}

	public void setChildren(List<VisualTreeElement> children) {
		this.children = children;
	}

	public boolean isRepeatable() {
		return repeatable;
	}

	public void setRepeatable(boolean repeatable) {
		this.repeatable = repeatable;
	}

	public String getSdkVersion() {
		return sdkVersion;
	}

	public void setSdkVersion(String sdkVersion) {
		this.sdkVersion = sdkVersion;
	}

	public String getNoticeSubType() {
		return noticeSubType;
	}

	public void setNoticeSubType(String noticeSubType) {
		this.noticeSubType = noticeSubType;
	}

	public String getNoticeUuid() {
		return noticeUuid;
	}

	public void setNoticeUuid(String noticeUuid) {
		this.noticeUuid = noticeUuid;
	}

	public String getIdScheme() {
		return idScheme;
	}

	public void setIdScheme(String idScheme) {
		this.idScheme = idScheme;
	}

	public String getLanguageId() {
		return languageId;
	}

	public void setLanguageId(String languageId) {
		this.languageId = languageId;
	}
}
