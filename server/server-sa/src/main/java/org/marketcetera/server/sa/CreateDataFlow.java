package org.marketcetera.server.sa;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.module.ModuleManagerMXBean;

/* $License$ */
/**
 * The create data flow command. This command creates a new
 * data flow with the sink module automatically appended to it,
 * if possible.
 *
 * @author anshul@marketcetera.com
 */
@ClassVersion("$Id: CreateDataFlow.java 16154 2012-07-14 16:34:05Z colin $") //$NON-NLS-1$
final class CreateDataFlow extends CommandRunner {
    /**
     * Creates an instance.
     */
    protected CreateDataFlow() {
        super("createDataFlow");  //$NON-NLS-1$
    }

    @Override
    final Object runCommand(ModuleManagerMXBean inManager, String inCmdString) {
        return inManager.createDataFlow(inCmdString);
    }
}
