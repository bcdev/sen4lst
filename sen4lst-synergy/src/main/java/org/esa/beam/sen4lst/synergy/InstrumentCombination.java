package org.esa.beam.sen4lst.synergy;

/**
 * Enum for combination of instruments in Sen4LST synergy approach
 *
 * @author olafd
 */
public enum InstrumentCombination {
    MERIS_AATSR("MERIS/AATSR"),
    OLCI_SLSTR("OLCI/SLSTR");

    private final String label;

    private InstrumentCombination(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
