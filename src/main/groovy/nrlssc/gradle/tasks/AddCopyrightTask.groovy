package nrlssc.gradle.tasks

import nrlssc.copyright.AddCopyright
import nrlssc.gradle.UtilPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by scraft on 3/13/2017.
 */
class AddCopyrightTask extends DefaultTask{
    private static Logger logger = LoggerFactory.getLogger(AddCopyrightTask.class)

    static AddCopyrightTask createFor(Project project)
    {
        AddCopyrightTask acTask = project.tasks.create("addCopyrightText", AddCopyrightTask.class)
        acTask.group = UtilPlugin.NRL_GROUP
        acTask.description = 'Adds the NRL Copyright text comment-block to the head of every java source in your project.'
        return acTask
    }



    private String[] files

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
    void AddCopyrightText()
    {
        logger.lifecycle("Adding copyright to all java files under: ")
        for(dir in getPaths())
        {
            logger.lifecycle(dir)
        }
        AddCopyright.main(getPaths())
    }
}
