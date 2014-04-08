package org.esa.beam.sen4lst.processing.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

/**
 * Specific dialog for LST processing
 *
 * @author olafd
 */
public class LstProcessingDialog extends DefaultSingleTargetProductDialog {

    public LstProcessingDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(operatorName, appContext, title, helpID);
        setDirectoryProperties();
    }

    private void setDirectoryProperties() {
        // implements a workaround to provide a proper dialog to select a directory
        Property[] properties = getBindingContext().getPropertySet().getProperties();
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
            if (descriptor.getName().toLowerCase().endsWith("directory")) {
                descriptor.setAttribute("directory", Boolean.TRUE);
            }
        }
    }
}
