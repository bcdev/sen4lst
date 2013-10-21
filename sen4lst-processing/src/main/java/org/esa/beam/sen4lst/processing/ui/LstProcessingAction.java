package org.esa.beam.sen4lst.processing.ui;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.sen4lst.processing.LstConstants;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * Action class for LST Processing GUI
 *
 * @author olafd
 */
public class LstProcessingAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
//            dialog = new LstProcessingDialog("Sen4LST.Lst",
//                                                          getAppContext(),
//                                                          "LST Processing - " + LstConstants.LST_PROCESSING_VERSION,
//                                                          "LstProcessorPlugIn");
            dialog = new DefaultSingleTargetProductDialog("Sen4LST.Lst",
                                                          getAppContext(),
                                                          "LST Processing - " + LstConstants.LST_PROCESSING_VERSION,
                                                          "LstProcessorPlugIn");
        }
        dialog.show();
    }
}
