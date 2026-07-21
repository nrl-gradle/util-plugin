package nrlssc.gradle.tasks


import nrlssc.gradle.UtilPlugin
import nrlssc.gradle.tasks.helpers.AddLegalHeader
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by scraft on 3/13/2017.
 */
class AddLegalHeaderTask extends DefaultTask{
    private static Logger logger = LoggerFactory.getLogger(AddLegalHeaderTask.class)

    static AddLegalHeaderTask createFor(Project project)
    {
        AddLegalHeaderTask acTask = project.tasks.create("addLegalHeader", AddLegalHeaderTask.class)
        acTask.group = UtilPlugin.NRL_GROUP
        acTask.description = 'Adds the Legal header text comment-block to the head of every java source in your project.'
        return acTask
    }



    private String[] files

    @Input
    String legalVersion = "1.0"
    @Input
    String poc = "Author"
    @Input
    @Optional
    String sectionCode = '1'
    @Input
    String legalText = '/******************************** -- [POC] [LEGAL_VERSION] -- ***********************************/'

    void poc(String poc){
        this.poc = poc
    }

    void legalVersion(String legalVersion){
        this.legalVersion = legalVersion
    }

    void sectionCode(String sectionCode){
        this.sectionCode = sectionCode
    }

    void legalText(String legalText){
        this.legalText = legalText
    }

    @InputFiles
    String[] getPaths()
    {
        if(files == null){
            files = getProject().sourceSets.main.allJava.files
        }
        return files
    }

    void setPaths(String[] files)
    {
        this.files = files
    }


    @TaskAction
    void AddLegalHeaderText()
    {
        logger.lifecycle("Adding legal header to all java source files")
        for(dir in getPaths())
        {
            logger.debug(dir)
        }
        AddLegalHeader al = new AddLegalHeader(legalVersion, poc, sectionCode, legalText)
        al.addHeader(getPaths())
    }
}
