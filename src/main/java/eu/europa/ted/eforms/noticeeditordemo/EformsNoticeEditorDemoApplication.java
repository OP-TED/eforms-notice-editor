package eu.europa.ted.eforms.noticeeditordemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
// @EnableAsync
public class EformsNoticeEditorDemoApplication {

  private static final Logger logger =
      LoggerFactory.getLogger(EformsNoticeEditorDemoApplication.class);

  public static void main(final String[] args) {
    logger.info("STARTING eForms Notice Editor Demo Application");
    // See README.md on how to run server.
    // https://spring.io/guides/gs/serving-web-content/
    SpringApplication.run(EformsNoticeEditorDemoApplication.class, args);
  }

}
