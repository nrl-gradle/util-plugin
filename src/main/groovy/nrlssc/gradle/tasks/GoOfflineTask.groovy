package nrlssc.gradle.tasks

import nrlssc.gradle.UtilPlugin
import nrlssc.gradle.helpers.PropertyName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction

/**
 * Created by scraft on 3/10/2017.
 */
class GoOfflineTask extends DefaultTask{

    static GoOfflineTask createFor(Project project)
    {
        GoOfflineTask task = project.tasks.create("goOffline", GoOfflineTask.class)
        task.group = UtilPlugin.NRL_GROUP
        task.description = 'Modifies your properties file (gradle.properties) to take you offline and collects libs'
        
        project.gradle.projectsEvaluated {

            List<Sync> syncs = new ArrayList<>()

            project.configurations.each { conf ->
                if(conf.canBeResolved && !(conf.name.equals('updateResolver') || conf.name.equals('updateResolverRelease'))) {
                    Sync clTask = project.tasks.create("collectLibs" + conf.name.capitalize(), Sync.class)
                    clTask.configure {
                        from conf
                        into "libs/${conf.name}"
                    }
                    clTask.group = 'collection'
                    clTask.description = "Collect all ${conf.name} dependencies into the 'libs/${conf.name}' folder as jars"
                    syncs.add(clTask)
                }
            }

            Sync bsTask = project.tasks.create("collectLibsBuildscript", Sync.class)
            bsTask.configure {
                from project.buildscript.configurations.classpath
                into "libs/buildscript"
            }
            bsTask.group = 'collection'
            bsTask.description = "Collect all buildscript dependencies into the 'libs/buildscript' folder as raw jars"

            syncs.add(bsTask)

            DefaultTask ca = project.tasks.create("collectAllLibs", DefaultTask.class)
            syncs.each {
                ca.dependsOn it
            }

            ca.group = 'collection'
            ca.description = "Collects all dependencies"

            project.tasks.getByName('goOffline').dependsOn(ca)
            
        }

        project.gradle.projectsEvaluated {
            if(PropertyName.disconnected.getAsBoolean(project)) {
                project.configurations.each { conf ->
                    conf.dependencies.clear()

                    if(conf.canBeResolved)
                    {
                        project.dependencies.add(conf.name, project.fileTree("libs/${conf.name}"))
                    }
                }
            }
        }
        
        
        return task
    }

    static boolean enableDisconnected(Project project)
    {
        File propFile = project.file('gradle.properties')
        if(!propFile.exists())
        {
            propFile.createNewFile()
        }
        if(!propFile.readLines().join("\n").replace(" ", "").contains("disconnected=true")) {
            propFile.append("\ndisconnected = true")
        }
    }

    @TaskAction
    void goOffline()
    {
        enableDisconnected(getProject().rootProject)
    }
}
