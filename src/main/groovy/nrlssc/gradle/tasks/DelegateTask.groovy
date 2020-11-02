package nrlssc.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by scraft on 3/10/2017.
 */
class DelegateTask extends DefaultTask{

    private Closure intClosure

    void closure(Closure delegate)
    {
        this.intClosure = delegate
    }

    @TaskAction
    void runDelegate()
    {
        intClosure()
    }
}
