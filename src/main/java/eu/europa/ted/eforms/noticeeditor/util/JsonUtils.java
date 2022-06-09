package eu.europa.ted.eforms.noticeeditor.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

  public static JsonNode parseString(final String json)
      throws JsonMappingException, JsonProcessingException {
    final ObjectMapper mapper = getStandardJacksonObjectMapper();
    return mapper.readTree(json);
  }

  public static JsonNode parseInputStream(final InputStream is) throws IOException {
    final ObjectMapper mapper = getStandardJacksonObjectMapper();
    return mapper.readTree(is);
  }

  public static JsonNode parsePath(final Path path) throws IOException {
    final ObjectMapper mapper = getStandardJacksonObjectMapper();
    return mapper.readTree(path.toFile());
  }

  public static Path writeFile(final Path folder, final String filename, final String text)
      throws IOException {
    final Path file = folder.resolve(filename);
    return Files.write(file, text.getBytes(StandardCharsets.UTF_8));
  }

  public static Path writeFile(final String outputfolder, final String filename, final String text)
      throws IOException {
    final Path folder = Paths.get(outputfolder);
    final Path file = folder.resolve(filename);
    return Files.write(file, text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @return the value of the boolean if present, the fallback value otherwise
   */
  public static boolean getBoolean(final JsonNode node, final String key, final boolean fallback) {
    final JsonNode jsonNode = node.get(key);
    return jsonNode != null ? jsonNode.asBoolean(fallback) : fallback;
  }

  /**
   * @return the string if present, the fallback string otherwise
   */
  public static String getText(final JsonNode node, final String key, final String fallback) {
    final JsonNode jsonNode = node.get(key);
    return jsonNode != null ? jsonNode.asText(fallback) : fallback;
  }

  /**
   * @return the string as optional if present, optional empty if key is missing
   */
  public static Optional<String> getTextOpt(final JsonNode node, final String key) {
    final JsonNode jsonNode = node.get(key);
    return jsonNode != null ? Optional.ofNullable(jsonNode.asText()) : Optional.empty();
  }

  public static List<String> getListOfStrings(final JsonNode jsonNode) {
    if (jsonNode != null && jsonNode.isArray()) {
      final ArrayNode arrayNode = (ArrayNode) jsonNode;
      final List<String> values = new ArrayList<>(arrayNode.size());
      for (final Iterator<JsonNode> elements = arrayNode.elements(); elements.hasNext();) {
        values.add(elements.next().asText());
      }
      return values;
    }
    return Collections.emptyList();
  }

  /**
   * Can be used for compatibility with our JSON formatting (easier diff).
   */
  public static void formatJsonLikeJackson(final File inputJson, final File outputJson)
      throws IOException {
    final ObjectMapper mapper = getStandardJacksonObjectMapper();
    final JsonNode node = mapper.readTree(inputJson);
    // Could some processing on node here.
    final JsonGenerator gen = mapper.getFactory().createGenerator(outputJson, JsonEncoding.UTF8);
    gen.setCodec(new ObjectMapper());
    gen.useDefaultPrettyPrinter();
    gen.writeTree(node);
  }

}
