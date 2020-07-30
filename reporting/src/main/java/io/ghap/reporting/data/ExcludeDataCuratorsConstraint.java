package io.ghap.reporting.data;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ExcludeDataCuratorsConstraint extends Constraint<Boolean> {

    public ExcludeDataCuratorsConstraint() {
        setType(ConstraintType.EXCLUDE_DATA_CURATOR);
    }

    public boolean isSatisfied() {
        return true;
    }
}
