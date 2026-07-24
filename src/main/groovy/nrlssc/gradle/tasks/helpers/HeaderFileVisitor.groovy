package nrlssc.gradle.tasks.helpers;

import com.google.common.io.Resources
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class HeaderFileVisitor extends SimpleFileVisitor<Path> {
    private static Logger logger = LoggerFactory.getLogger(HeaderFileVisitor.class);
    private int filesModified = 0;
    private int filesWithCorrectCopyrightVersion = 0;
    private int filesWithIncorrectCopyrightVersion = 0;

    private String legalVersion
    private String poc
    private String sectionCode
    private String legalText

    private String fullText;
    private String fullTextLine0;
    private Pattern line0Pattern

    HeaderFileVisitor(String legalVersion, String poc, String sectionCode, String legalText) throws IOException {
        this.legalVersion = legalVersion
        this.poc = poc
        this.sectionCode = sectionCode
        this.legalText = legalText
        fullText = legalText.replaceAll("\\[LEGAL_VERSION\\]", legalVersion).replaceAll("\\[SECTION_CODE\\]", sectionCode).replaceAll("\\[POC\\]", poc) + "\n" + endMarker;

        this.fullTextLine0 = fullText.split("[\\r?\\n]")[0]
        String line0 = legalText.split("[\\r?\\n]")[0]
        this.line0Pattern = Pattern.compile(
                Pattern.quote(
                        line0.replaceAll("\\[LEGAL_VERSION\\]", "0000LV0000").replaceAll("\\[SECTION_CODE\\]", "0000SC0000").replaceAll("\\[POC\\]", "0000POC0000")
                )
                        .replaceAll("0000LV0000", ".*").replaceAll("0000SC0000", ".*").replaceAll("0000POC0000", ".*") + ".*"
        )
    }

    private String endMarker = "/* === LEGAL HEADER END NON-MODIFIABLE CONTENT == */"
    @Override
    FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        String extension = com.google.common.io.Files.getFileExtension(path.toString());
        if ("java".equals(extension) && path.toFile().isFile()) {
            logger.debug("Considering file {}", path);
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);

            List<String> javaFilesLines = Files.readAllLines(path);
            if (fullTextLine0.equals(javaFilesLines.get(0))) {
                logger.info("Copyright was present in file {}", path);
                filesWithCorrectCopyrightVersion++;
            } else if (javaFilesLines.get(0).matches(line0Pattern)) {
                // Have a copyright header, but wrong verion. Replace.
                String tempFilename = path.toString() + ".zzz";
                Path tempFile = Paths.get(tempFilename);

                Files.write(tempFile, List.of(fullText), StandardOpenOption.CREATE);
                int indexOfEndComment = -1;
                for (int i = 0; i < javaFilesLines.size(); i++) {
                    if (javaFilesLines.get(i).equals(endMarker)) {
                        indexOfEndComment = i;
                        break;
                    }
                }
                Files.write(tempFile, javaFilesLines.subList(indexOfEndComment + 1, javaFilesLines.size()), Charset.defaultCharset(), StandardOpenOption.APPEND);

                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(path, lastModifiedTime);
                logger.info("Incorrect version of copyright was present in file {}", path);
                filesWithIncorrectCopyrightVersion++;
            } else {
                String tempFilename = path.toString() + ".zzz";
                Path tempFile = Paths.get(tempFilename);

                Files.write(tempFile, List.of(fullText), StandardOpenOption.CREATE);
                Files.write(tempFile, javaFilesLines, Charset.defaultCharset(), StandardOpenOption.APPEND);

                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(path, lastModifiedTime);
                filesModified++;
                logger.info("File {} updated", path);
            }
        }

        return super.visitFile(path, attrs);
    }

    int getFilesModified() {
        return filesModified;
    }

    int getFilesWithCorrectCopyrightVersion() {
        return filesWithCorrectCopyrightVersion;
    }

    int getFilesWithIncorrectCopyrightVersion() {
        return filesWithIncorrectCopyrightVersion;
    }
}