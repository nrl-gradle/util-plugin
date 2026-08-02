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
    int filesModified = 0;
    int filesWithCorrectCopyrightVersion = 0;
    int filesWithIncorrectCopyrightVersion = 0;
    int unmodifiableFiles = 0;

    private String legalVersion
    private String poc
    private String sectionCode
    private String legalText

    private String fullText;
    private String fullTextLine0;
    private Pattern line0Pattern

    public boolean force;

    HeaderFileVisitor(String legalVersion, String poc, String sectionCode, String legalText, boolean force = false) throws IOException {
        this.legalVersion = legalVersion
        this.poc = poc
        this.sectionCode = sectionCode
        this.legalText = legalText;
        this.force = force;
        fullText = startMarker.replaceAll("\\[LEGAL_VERSION\\]", legalVersion) + "\n" + legalText.replaceAll("\\[LEGAL_VERSION\\]", legalVersion).replaceAll("\\[SECTION_CODE\\]", sectionCode).replaceAll("\\[POC\\]", poc) + "\n" + endMarker;

        this.fullTextLine0 = fullText.split("[\\r?\\n]")[0]

        this.line0Pattern = Pattern.compile(
                Pattern.quote('/******************************** -- LEGAL ') +
                        '.*' +
                        Pattern.quote(' -- ***********************************') +
                        '.*'
        )
    }

    private String startMarker = "/******************************** -- LEGAL [LEGAL_VERSION] -- ***********************************";
    private String endMarker = "******************************** -- END LEGAL -- ***********************************/"
    @Override
    FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        String extension = com.google.common.io.Files.getFileExtension(path.toString());
        if ("java".equals(extension) && path.toFile().isFile()) {
            logger.debug("Considering file {}", path);
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);

            String line0 = readLine(path.toAbsolutePath().toString());
            if (fullTextLine0.equals(line0)) {
                logger.info("Legal header was present in file {}", path);
                filesWithCorrectCopyrightVersion++;
            } else if (line0.matches(line0Pattern)) {
                // Have a copyright header, but wrong verion. Replace.
                List<String> javaFilesLines = Files.readAllLines(path);
                int indexOfEndComment = -1;
                boolean foundEnd = false;
                int indexOfOldEndComment = -1;
                for (int i = 0; i < javaFilesLines.size(); i++) {
                    String curLine = javaFilesLines.get(i);
                    if (curLine.equals(endMarker)) {
                        indexOfEndComment = i;
                        foundEnd = true;
                        break;
                    }
                    if(force) {
                        if (curLine.contains("*/")) {
                            indexOfOldEndComment = i;
                            break;
                        }
                    }
                }

                if(force && indexOfEndComment == -1 && indexOfOldEndComment > 0){
                    indexOfEndComment = indexOfOldEndComment;
                    foundEnd = true;
                    logger.info("Found original ending, forcing overwrite");
                }

                if(!foundEnd){
                    logger.info("Found possible header, but was missing end indicator.  No modifications will be made " + path.toString())
                    unmodifiableFiles++
                }
                else
                {
                    String tempFilename = path.toString() + ".zzz";
                    Path tempFile = Paths.get(tempFilename);

                    Files.write(tempFile, List.of(fullText), StandardOpenOption.CREATE);
                    Files.write(tempFile, javaFilesLines.subList(indexOfEndComment + 1, javaFilesLines.size()), Charset.defaultCharset(), StandardOpenOption.APPEND);

                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                    Files.setLastModifiedTime(path, lastModifiedTime);
                    logger.info("Incorrect version of copyright was present in file {}", path);
                    filesWithIncorrectCopyrightVersion++;

                }


            } else {
                String tempFilename = path.toString() + ".zzz";
                Path tempFile = Paths.get(tempFilename);
                List<String> javaFilesLines = Files.readAllLines(path);

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


    static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String firstLine = br.readLine(); // Reads only the first line
            return firstLine
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}