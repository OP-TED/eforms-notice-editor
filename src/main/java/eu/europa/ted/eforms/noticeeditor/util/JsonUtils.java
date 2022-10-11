package eu.europa.ted.eforms.noticeeditor.util;

import java.util.Optional;
import org.apache.commons.lang3.Validate;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtils {

  private JsonUtils() {
    throw new UnsupportedOperationException("The class is a utility class!");
  }

  public static ObjectMapper getStandardJacksonObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    // https://fasterxml.github.io/jackson-annotations/javadoc/2.7/com/fasterxml/jackson/annotation/JsonInclude.Include.html

    // Value that indicates that only properties with non-null values are to be included.
    objectMapper.setSerializationInclusion(Include.NON_NULL);

    // Value that indicates that only properties with null value, or what is considered empty, are
    // not to be included.
    objectMapper.setSerializationInclusion(Include.NON_EMPTY);

    return objectMapper;
  }

  public static String marshall(final Object obj) throws JsonProcessingException {
    final ObjectMapper objectMapper = getStandardJacksonObjectMapper();
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  public static String getTextStrict(final JsonNode json, final String key) {
    Validate.notNull(json, "Elem is null for key=%s", key);

    final JsonNode jsonElem = json.get(key);
    Validate.notNull(jsonElem, "Not found for key=%s", key);

    final String text = jsonElem.asText(null);
    Validate.notBlank(text, "Text is blank for key=%s", key);

    return text;
  }

  public static Optional<String> getTextOpt(final JsonNode json, final String key) {
    Validate.notNull(json, "Elem is null for key=%s", key);
    final JsonNode jsonElem = json.get(key);
    if (jsonElem == null) {
      return Optional.empty();
    }
    final String text = jsonElem.asText(null);
    if (text == null) {
      return Optional.empty();
    }
    return Optional.of(text);
  }
}
