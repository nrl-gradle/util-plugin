package nrlssc.gradle.tasks

import nrlssc.gradle.tasks.helpers.MD2PDFCopyAction
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DestinationRootCopySpec
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.internal.file.PathToFileResolver
import org.gradle.internal.reflect.Instantiator

class MD2PDFTask extends AbstractCopyTask {
    
    @Override
    protected CopyAction createCopyAction() {
        File destinationDir = this.getDestinationDir();
        if (destinationDir == null) {
            throw new InvalidUserDataException("No copy destination directory has been specified, use 'into' to specify a target directory.")
        } else {
            return new MD2PDFCopyAction(this.getFileLookup().getFileResolver(destinationDir))
        }
    }

    protected CopySpecInternal createRootSpec() {
        Instantiator instantiator = this.getInstantiator()
        PathToFileResolver fileResolver = this.getFileResolver()
        return (CopySpecInternal)instantiator.newInstance(DestinationRootCopySpec.class, fileResolver, super.createRootSpec())
    }

    DestinationRootCopySpec getRootSpec() {
        return (DestinationRootCopySpec)super.getRootSpec()
    }

    @OutputDirectory
    File getDestinationDir() {
        return this.getRootSpec().getDestinationDir()
    }

    void setDestinationDir(File destinationDir) {
        this.into(destinationDir)
    }
}
