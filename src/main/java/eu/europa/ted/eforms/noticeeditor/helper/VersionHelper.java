package eu.europa.ted.eforms.noticeeditor.helper;

import org.apache.commons.lang3.Validate;
import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class VersionHelper {

  public static String buildSdkVersionWithoutPatch(final SdkVersion sdkVersion) {
    return sdkVersion.getMajor() + "." + sdkVersion.getMinor();
  }

  public static String prefixSdkVersion(final SdkVersion sdkVersion) {
    return SdkConstants.NOTICE_CUSTOMIZATION_ID_VERSION_PREFIX + sdkVersion.toString();
  }

  public static String prefixSdkVersionWithoutPatch(final SdkVersion sdkVersion) {
    final String prefixedSdkVersionNoPatch =
        SdkConstants.NOTICE_CUSTOMIZATION_ID_VERSION_PREFIX
            + buildSdkVersionWithoutPatch(sdkVersion);
    return prefixedSdkVersionNoPatch;
  }

  public static SdkVersion parsePrefixedSdkVersion(final String eformsSdkVersion) {
    // If we have "eforms-sdk-1.1.0" we want "1.1.0".
    final String eformsSdkPrefix = SdkConstants.NOTICE_CUSTOMIZATION_ID_VERSION_PREFIX;
    Validate.isTrue(eformsSdkVersion.startsWith(eformsSdkPrefix),
        "Expecting sdk version to start with prefix=%s", eformsSdkPrefix);
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
