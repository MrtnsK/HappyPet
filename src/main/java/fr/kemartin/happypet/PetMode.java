package fr.kemartin.happypet;

public enum PetMode {
    FOLLOWING,
    SITTING,
    PATROL;

    /**
     * Cycle : FOLLOWING -> SITTING -> PATROL -> FOLLOWING
     */
    public PetMode next() {
        PetMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
