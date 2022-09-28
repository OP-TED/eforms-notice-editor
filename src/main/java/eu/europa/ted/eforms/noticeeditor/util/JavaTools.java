package eu.europa.ted.eforms.noticeeditor.util;

import java.io.IOException;
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
}

