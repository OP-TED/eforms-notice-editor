package eu.europa.ted.eforms.noticeeditor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import eu.europa.ted.eforms.sdk.SdkVersion;

public class NoticeEditorConstants {
  private NoticeEditorConstants() {}

  public static final Path EFORMS_SDKS_DIR = Path.of("eforms-sdks");

  public static final List<SdkVersion> SUPPORTED_SDKS =
      Collections.unmodifiableList(Arrays.asList(new SdkVersion("0.6"), new SdkVersion("0.7"),
          new SdkVersion("1.0"), new SdkVersion("1.1")));
}
