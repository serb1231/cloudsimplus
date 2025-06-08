package org.APD;

import org.cloudsimplus.cloudlets.CloudletSimple;

public class DeadlineCloudlet extends CloudletSimple {
    private double deadline;

    public DeadlineCloudlet(long id, long length, int pes) {
        super(id, length, pes);
    }

    public double getDeadline() {
        return deadline;
    }

    public DeadlineCloudlet setDeadline(double deadline) {
        this.deadline = deadline;
        return this;
    }
}
