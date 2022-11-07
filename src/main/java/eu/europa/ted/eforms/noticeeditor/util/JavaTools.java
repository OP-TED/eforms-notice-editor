package eu.europa.ted.eforms.noticeeditor.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaTools {

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
      justification = "Known bug inside SpotBugs related to try catch.")
  public static List<String> listFiles(final Path dir) throws IOException {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName)
          .map(Path::toString).collect(Collectors.toList());
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
      justification = "Known bug inside SpotBugs related to try catch.")
  public static List<String> listFolders(final Path dir) throws IOException {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream.filter(Files::isDirectory).map(Path::getFileName).map(Path::toString)
          .collect(Collectors.toList());
    }
  }

  /**
   * Writes text to path as UTF-8. Creates new file or overwrites existing file.
   *
   * @param pathToFile the path to the file, note that this does not create the folders, use Files
   *        createDirectories for that.
   */
  public static Path writeTextFile(final Path pathToFile, final String text)
      throws IOException, FileNotFoundException {
    // There are so many ways do to this in Java, this probably the best.
    return Files.write(pathToFile, text.getBytes(StandardCharsets.UTF_8));
  }
}

