package nrlssc.gradle.tasks.helpers

import com.qkyrie.markdown2pdf.Markdown2PdfConverter
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream

import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.internal.FileUtils
import org.gradle.internal.file.PathToFileResolver


class MD2PDFCopyAction implements CopyAction {
    private final PathToFileResolver fileResolver

    MD2PDFCopyAction(PathToFileResolver fileResolver) {
        this.fileResolver = fileResolver
    }

    WorkResult execute(CopyActionProcessingStream stream) {
        MD2PDFGenerationInternalAction action = new MD2PDFGenerationInternalAction()
        stream.process(action)
        return WorkResults.didWork(action.didWork)
    }

    private class MD2PDFGenerationInternalAction implements CopyActionProcessingStreamAction {
        private boolean didWork

        private MD2PDFGenerationInternalAction() {
        }

        void processFile(FileCopyDetailsInternal details) {
            File target = fileResolver.resolve(details.getRelativePath().getPathString())
            renameIfCaseChanged(target)
            
            
            if(!details.file.name.endsWith(".md")) 
            {
                boolean copied = details.copyTo(target);
                if (copied) {
                    this.didWork = true;
                }
            }
            else //if(details.file.name.endsWith(".md"))
            {
                if(target.name.endsWith(".md")) target = new File(target.path.replaceAll(/(?i)\.md$/, ".pdf"))
                else if(!target.name.endsWith(".pdf")) target = new File(target.path + ".pdf")
                    
                Markdown2PdfConverter.newConverter()
                        .readFrom({
                            ByteArrayOutputStream bos = new ByteArrayOutputStream()
                            details.copyTo(bos)
                            return bos.toString()
                        })
                        .writeTo({
                            FileOutputStream fos = new FileOutputStream(target) 
                            fos.write(it)
                            fos.close()
                        })
                        .doIt()
                    
                this.didWork = true
            }

        }

        private void renameIfCaseChanged(File target) {
            if (target.exists()) {
                File canonicalizedTarget = FileUtils.canonicalize(target)
                if (!Objects.equals(target.getName(), canonicalizedTarget.getName())) {
                    canonicalizedTarget.renameTo(target)
                }
            }

        }
    }
}

