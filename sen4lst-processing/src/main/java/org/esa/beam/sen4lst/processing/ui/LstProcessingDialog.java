package org.esa.beam.sen4lst.processing.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

/**
 * todo: activate this dialog
 * To change this template use File | Settings | File Templates.
 * Date: 21.10.13
 * Time: 13:13
 *
 * @author olafd
 */
public class LstProcessingDialog extends DefaultSingleTargetProductDialog {

    public LstProcessingDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(operatorName, appContext, title, helpID);
        setDirectoryProperties();
    }

    private void setDirectoryProperties() {
        Property[] properties = getBindingContext().getPropertySet().getProperties();
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
            if (descriptor.getName().toLowerCase().endsWith("directory")) {
                descriptor.setAttribute("directory", Boolean.TRUE);
            }
        }
    }
}
