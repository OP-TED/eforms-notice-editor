package eu.europa.ted.eforms.noticeeditor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_HAS_CHECKED",
    justification = "Checked to Runtime OK here")
public class EformsNoticeEditorApp implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(EformsNoticeEditorApp.class);

  @Value("${eforms.sdk.path}")
  private String eformsSdkDir;

  @Value("${eforms.sdk.versions}")
  private List<String> supportedSdks;

  public static void main(final String[] args) {
    logger.info("STARTING eForms Notice Editor Demo Application");
    // See README.md on how to run server.
    // https://spring.io/guides/gs/serving-web-content/

    // Here you have access to command line args.
    // logger.debug("args={}", Arrays.toString(args));

    SpringApplication.run(EformsNoticeEditorApp.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    Validate.notEmpty(eformsSdkDir, "Undefined eForms SDK path");
    Validate.notNull(supportedSdks, "Undefined supported SDK versions");

    for (final String sdkVersion : supportedSdks) {
      try {
        SdkDownloader.downloadSdk(new SdkVersion(sdkVersion), Path.of(eformsSdkDir));
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to download SDK artifacts for sdkVersion=%s", sdkVersion), e);
      }
    }
  }
}
