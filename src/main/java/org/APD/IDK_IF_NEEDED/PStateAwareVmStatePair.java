package org.APD.IDK_IF_NEEDED;

import org.cloudbus.cloudsim.examples.src.PowerModels.PerformanceState;

// Key class
// This class represents a pair of a PStateAwareVm and its corresponding PerformanceState.
public class PStateAwareVmStatePair {
    private final PStateAwareVm vm;
    private final PerformanceState state;

    public PStateAwareVmStatePair(PStateAwareVm vm, PerformanceState state) {
        this.vm = vm;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PStateAwareVmStatePair that)) return false;
        return vm.equals(that.vm) && state.equals(that.state);
    }

    @Override
    public int hashCode() {
        return 31 * vm.hashCode() + state.hashCode();
    }
}
