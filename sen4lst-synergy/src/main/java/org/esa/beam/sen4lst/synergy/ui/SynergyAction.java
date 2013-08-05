package org.esa.beam.sen4lst.synergy.ui;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.sen4lst.synergy.SynergyConstants;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * Action class for Synergy Processing GUI
 *
 * @author olafd
 */
public class SynergyAction extends AbstractVisatAction {
    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
            dialog = new DefaultSingleTargetProductDialog("Sen4LST.Sdr",
                                                          getAppContext(),
                                                          "Sen4LST Synergy Processing - " + SynergyConstants.SYNERGY_PROCESSING_VERSION,
                                                          "synergyprocessing");
        }
        dialog.show();
    }
}
