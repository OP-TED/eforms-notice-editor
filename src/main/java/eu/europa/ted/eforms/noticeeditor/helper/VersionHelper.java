package eu.europa.ted.eforms.noticeeditor.helper;

import org.apache.commons.lang3.Validate;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

/**
 * Common SDK Version handling code. Some of this could be moved to SdkVersion.java later.
 */
public class VersionHelper {

  public static String buildSdkVersionWithoutPatch(final SdkVersion sdkVersion) {
    return sdkVersion.toStringWithoutPatch();
  }

  /**
   * @param sdkVersion The SDK version which should be prefixed by "eforms-sdk-"
   */
  public static String prefixSdkVersionWithoutPatch(final SdkVersion sdkVersion) {
    final String prefixedSdkVersionNoPatch =
        SdkConstants.NOTICE_CUSTOMIZATION_ID_VERSION_PREFIX
            + buildSdkVersionWithoutPatch(sdkVersion);
    return prefixedSdkVersionNoPatch;
  }

  /**
   * Example: If we have "eforms-sdk-1.6" we want "1.6"
   *
   * @param eformsSdkVersion A prefixed SDK version like "eforms-sdk-1.6"
   */
  public static SdkVersion parsePrefixedSdkVersion(final String eformsSdkVersion) {
    final String eformsSdkPrefix = SdkConstants.NOTICE_CUSTOMIZATION_ID_VERSION_PREFIX;
    Validate.isTrue(eformsSdkVersion.startsWith(eformsSdkPrefix),
        "Expecting SDK version to start with prefix=%s", eformsSdkPrefix);
    final String sdkVersionStr = eformsSdkVersion.substring(eformsSdkPrefix.length());
    return new SdkVersion(sdkVersionStr);
  }

  public static final boolean equalsVersionWithoutPatch(final SdkVersion sdkVersionA,
      final SdkVersion sdkVersionB) {
    final boolean majorEq = sdkVersionA.getMajor().equals(sdkVersionB.getMajor());
    final boolean minorEq = sdkVersionA.getMinor().equals(sdkVersionB.getMinor());
    return majorEq && minorEq;
  }
}
