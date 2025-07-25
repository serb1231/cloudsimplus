package org.APD.PowerModels;

import org.cloudsimplus.power.PowerMeasurement;
import org.cloudsimplus.power.models.PowerModelHostAbstract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A discrete-state (DVFS-aware) power model.
 * Each {@code PerformanceState} gives the <b>static</b> power drawn when the
 * CPU is running at or below the state’s speed <i>fraction</i>.
 * Example:          util 0–5 % →  40 W
 *                   util 5–25 % → 65 W
 *                   util 25–50 % → 95 W
 *                   util 50–75 % →130 W
 *                   util 75–100 %→165 W
 * The host jumps from one level to the next – there is no interpolation.
 */
public class PowerModelPStateProcessor extends PowerModelHostAbstract {

    public double getStaticPower() {
        if (getHost() == null)
            throw new IllegalStateException("Host is not set for this PowerModelPStateProcessor");

        return states[currentStateIdx].powerConsumption;
    }

    /* ------------------------------------------------------------------ */
    public record PerformanceState(double powerConsumption, double processingFraction) { }
    /* ------------------------------------------------------------------ */

    /** Sorted ascending by {@code processingFraction}. */
    private PerformanceState[] states;
    private int currentStateIdx;

    /* ---------- ctor --------------------------------------------------- */
    public PowerModelPStateProcessor(int startingState, PerformanceState... states) {
        if (states == null || states.length == 0)
            throw new IllegalArgumentException("Must supply at least one PerformanceState");

        this.states = Arrays.copyOf(states, states.length);
        Arrays.sort(this.states,
                Comparator.comparingDouble(PerformanceState::processingFraction));

        // Sanity: processingFraction ∈]0,1]  and strictly increasing
        double prev = 0;
        for (PerformanceState s : this.states) {
            if (s.processingFraction <= prev || s.processingFraction > 1.0)
//               // also print the state that caused the error
                throw new IllegalArgumentException("processingFraction values must be in (0,1] and ascending order: " +
                        s.processingFraction + " after " + prev);
            prev = s.processingFraction;
        }
        currentStateIdx = startingState;
    }

    /**
     * Copy constructor.
     * Creates a deep copy of another PowerModelPStateProcessor.
     */
    public PowerModelPStateProcessor(PowerModelPStateProcessor other) {
        if (other.states == null || other.states.length == 0)
            throw new IllegalArgumentException("The provided model has no PerformanceStates");

        // Deep copy of PerformanceState array (immutable record, so shallow copy is safe)
        this.states = Arrays.copyOf(other.states, other.states.length);
        this.currentStateIdx = other.currentStateIdx;
    }

    public void modifyPerformanceStatesByPercentageSlower(double percentage) {
        List<PerformanceState> newStates = new ArrayList<>();
        for (var state : states) {
            PerformanceState newState = new PerformanceState(state.powerConsumption * percentage, state.processingFraction);
            newStates.add(newState);
        }
        states = newStates.toArray(new PerformanceState[0]);
        // sort the new states
        Arrays.sort(this.states,
                Comparator.comparingDouble(PerformanceState::processingFraction));

        // Sanity: processingFraction ∈]0,1]  and strictly increasing
        double prev = 0;
        for (PerformanceState s : this.states) {
            if (s.processingFraction <= prev || s.processingFraction > 1.0)
//               // also print the state that caused the error
                throw new IllegalArgumentException("processingFraction values must be in (0,1] and ascending order: " +
                        s.processingFraction + " after " + prev);
            prev = s.processingFraction;
        }
    }

    /* ---------- public helpers ---------------------------------------- */
    public PerformanceState[] getPossiblePerformanceStates() { return states; }

    public int getCurrentPerformanceState()                  { return currentStateIdx; }
    public void setCurrentPerformanceState(int idx) {
        if (idx < 0 || idx >= states.length)
            throw new IllegalArgumentException("Performance-state index out of range");
        this.currentStateIdx = idx;
    }

    /** Convenience: instantaneous wattage of the current state. */
    public double powerNow() { return states[currentStateIdx].powerConsumption; }

    /* ---------- PowerModelHostAbstract --------------------------------- */

    /**
     * Return the power (W) for a given utilisation fraction.
     * <p>We simply pick the first state whose {@code processingFraction}
     * is ≥ the requested utilisation.</p>
     */
    @Override
    public double getPowerInternal(final double util) {
        if (util < 0 || util > 1)
            throw new IllegalArgumentException("Utilisation must be in [0,1]");

        return states[currentStateIdx].powerConsumption + dynamicPower(util);
    }

    /** Called by CloudSim to obtain the split between static and dynamic power. */
    @Override
    public PowerMeasurement getPowerMeasurement() {
        var host = getHost();
        if (!host.isActive())
            return new PowerMeasurement();                   // switched off

        double usageFraction = host.getCpuMipsUtilization() / host.getTotalMipsCapacity();
        return new PowerMeasurement(states[currentStateIdx].powerConsumption, dynamicPower(usageFraction));
    }

    private double dynamicPower(final double util) {
        // current state
        PerformanceState currentState = states[currentStateIdx];
        // check if there is a higher state
        if (currentStateIdx <= states.length - 2) {
            // return (static power) + (current state power) + (next state power - current state power) * (util)
            return (states[currentStateIdx + 1].powerConsumption - currentState.powerConsumption) * util;
        }
        else {
            // return current state power
            return 0;
        }
    }
}
