package eu.europa.ted.eforms.noticeeditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class EformsNoticeEditorApp {

  private static final Logger logger = LoggerFactory.getLogger(EformsNoticeEditorApp.class);

  public static final String APP_VERSION = "1.0.0";

  public static void main(final String[] args) {
    logger.info("STARTING eForms Notice Editor Demo Application");
    // See README.md on how to run server.
    // https://spring.io/guides/gs/serving-web-content/
    SpringApplication.run(EformsNoticeEditorApp.class, args);
  }

}
