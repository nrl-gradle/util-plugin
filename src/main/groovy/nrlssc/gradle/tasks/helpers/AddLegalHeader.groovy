package nrlssc.gradle.tasks.helpers


import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.stream.Collectors
import com.google.common.io.Resources
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Adds an initial bit of text (the NRL source code disclosure text) to all Java source files in the given folder(s). The copyright text is versioned and marked by a special
 * sequence of characters in the first line. If the version number is updated, this code will replace the existing text with the new text. If the first lines are the same, then no
 * action is taken.
 *
 * The use of the word copyright is a misnomer, as Scott Bell informed me (Richard Owens). Use of this term in this project is not meant to imply an actual copyright.
 *
 */
class AddLegalHeader {

    private static final Logger logger = LoggerFactory.getLogger(AddLegalHeader.class);

    private String legalVersion = "1.1";
    private String poc = "Naval Research Laboratory";
    private String branchCode = "7340";
    private String legalText = '/******************************** -- [POC] [LEGAL_VERSION] -- ***********************************/';
    AddLegalHeader(String legalVersion, String POC, String branchCode, String legalText){
        this.legalVersion = legalVersion
        this.poc = POC
        this.branchCode = branchCode
        this.legalText = legalText
    }
    AddLegalHeader(){}

    void addHeader(String[] dirs) throws IOException {
        for (String dirName : dirs) {
            if((new File(dirName)).exists()) addHeader(dirName);
        }
    }

    void addHeader(String dirName) throws IOException {

        HeaderFileVisitor fileVisitor = new HeaderFileVisitor(legalVersion, poc, branchCode, legalText);
        Files.walkFileTree(Paths.get(dirName), fileVisitor);
        logger.debug("Files updated: {}", fileVisitor.filesModified);
        logger.debug("Files with correct copyright: {}", fileVisitor.filesWithCorrectCopyrightVersion);
        logger.debug("Files with incorrect copyright: {}", fileVisitor.filesWithIncorrectCopyrightVersion);
        logger.debug("Files with missing end indicator: {}", fileVisitor.unmodifiableFiles);
    }

}
