package eu.europa.ted.eforms.noticeeditor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class SdkServiceTest {

  @Test
  public void testValidateSdkVersionFormat() {
    assertFalse(SdkService.securityValidateSdkVersionFormat(null));
    assertFalse(SdkService.securityValidateSdkVersionFormat("       "));
    assertFalse(SdkService.securityValidateSdkVersionFormat("1.0.0.0"));

    assertTrue(SdkService.securityValidateSdkVersionFormat("1.0.0"));
    assertTrue(SdkService.securityValidateSdkVersionFormat("42.42.42"));
  }
}
