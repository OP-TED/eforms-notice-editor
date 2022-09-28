package eu.europa.ted.eforms.noticeeditor;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import eu.europa.ted.eforms.sdk.SdkVersion;
import eu.europa.ted.eforms.sdk.resource.SdkDownloader;

/**
 * The entry point, a Spring boot application.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class EformsNoticeEditorApp {

  private static final Logger logger = LoggerFactory.getLogger(EformsNoticeEditorApp.class);

  public static final String APP_VERSION = "1.0.0";

  public static void main(final String[] args) {
    logger.info("STARTING eForms Notice Editor Demo Application");
    // See README.md on how to run server.
    // https://spring.io/guides/gs/serving-web-content/

    // Here you have access to command line args.
    // logger.debug("args={}", Arrays.toString(args));

    for (SdkVersion sdkVersion : NoticeEditorConstants.SUPPORTED_SDKS) {
      try {
        SdkDownloader.downloadSdk(sdkVersion, NoticeEditorConstants.EFORMS_SDKS_DIR);
      } catch (IOException e) {
        throw new RuntimeException("Failed to download SDK artifacts", e);
      }
    }

    SpringApplication.run(EformsNoticeEditorApp.class, args);
  }
}
