/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os;

import java.util.HashMap;
import java.util.Map;

/**
 * Simplified Completely Fair Scheduler (CFS).
 *
 * Every ready process has a "virtual runtime" (vruntime): an accumulated,
 * weight-scaled measure of how much CPU it has already received. getNext
 * always hands the CPU to the process with the LOWEST vruntime (the
 * "leftmost" process on CFS's fairness timeline), so no process can be
 * starved: the more a process runs, the faster its vruntime grows, and the
 * less likely it is to be picked again until the others catch up.
 *
 * The existing `priority` field (0 = highest, reused from PriorityQueue) is
 * repurposed here as a CFS "weight/niceness" hint instead of a strict
 * precedence rule: a higher-priority process accumulates vruntime more
 * slowly, so it tends to be scheduled more often, but it never permanently
 * blocks lower-priority processes the way PriorityQueue does.
 *
 * @author Equipo UR-OS
 */
public class FAIR extends Scheduler {

    // WEIGHT[priority] -- higher weight = vruntime grows more slowly = more CPU share.
    // Index 0 corresponds to priority 0 (highest), same convention as PriorityQueue.
    private static final double[] WEIGHT = {4.0, 3.0, 2.0, 1.0};
    private static final double DEFAULT_WEIGHT = 1.0;

    private final Map<Process, Double> vruntime;

    public FAIR(OS os) {
        super(os);
        vruntime = new HashMap<>();
    }

    private double weightOf(Process p) {
        int nivel = p.getPriority();
        if (nivel >= 0 && nivel < WEIGHT.length) {
            return WEIGHT[nivel];
        }
        return DEFAULT_WEIGHT;
    }

    // The vruntime a process should be given the moment it starts competing
    // for the CPU: the minimum vruntime currently in play (ready queue + the
    // process in the CPU, if any). This is what keeps a brand-new process
    // from being unfairly favored (starting at 0 while others have already
    // accumulated vruntime) or unfairly punished.
    private double minVruntime() {
        double min = Double.MAX_VALUE;
        for (Process p : processes) {
            min = Math.min(min, vruntime.getOrDefault(p, 0.0));
        }
        if (!os.isCPUEmpty()) {
            Process running = os.getProcessInCPU();
            min = Math.min(min, vruntime.getOrDefault(running, 0.0));
        }
        return (min == Double.MAX_VALUE) ? 0.0 : min;
    }

    private Process pickMinVruntime() {
        Process chosen = null;
        double best = Double.MAX_VALUE;
        for (Process p : processes) {
            double v = vruntime.getOrDefault(p, 0.0);
            if (v < best) {
                best = v;
                chosen = p;
            } else if (v == best && chosen != null) {
                chosen = tieBreaker(chosen, p);
            }
        }
        return chosen;
    }

    @Override
    public void addProcess(Process p) {
        ProcessState estadoOriginal = p.getState();

        if (estadoOriginal == ProcessState.NEW) {
            // Brand-new process: placed on the fair timeline at the current minimum.
            vruntime.put(p, minVruntime());
        } else if (estadoOriginal == ProcessState.IO) {
            // Returning from I/O: keeps ITS OWN accumulated vruntime (rewards
            // interactive / I/O-bound processes, exactly like real CFS).
            vruntime.putIfAbsent(p, minVruntime());
        }
        // If estadoOriginal == CPU (evicted before finishing its burst), its
        // vruntime is already up to date from getNext(false) below -- nothing
        // to initialize here, it just re-joins the ready list.

        p.setState(ProcessState.READY);
        processes.add(p);

        if (estadoOriginal == ProcessState.NEW) {
            newProcess(os.isCPUEmpty());
        } else if (estadoOriginal == ProcessState.IO) {
            IOReturningProcess(os.isCPUEmpty());
        }
    }

    @Override
    public void getNext(boolean cpuEmpty) {
        if (cpuEmpty) {
            Process next = pickMinVruntime();
            if (next != null) {
                processes.remove(next);
                os.interrupt(InterruptType.SCHEDULER_RQ_TO_CPU, next);
            }
        } else {
            // CPU busy: charge the running process for the cycle it just used.
            Process running = os.getProcessInCPU();
            double current = vruntime.getOrDefault(running, 0.0);
            vruntime.put(running, current + (1.0 / weightOf(running)));
        }
    }

    private void preemptIfLessDeserving(boolean cpuEmpty) {
        if (cpuEmpty) return;
        Process running = os.getProcessInCPU();
        double runningV = vruntime.getOrDefault(running, 0.0);
        for (Process p : processes) {
            if (vruntime.getOrDefault(p, 0.0) < runningV) {
                os.interrupt(InterruptType.SCHEDULER_CPU_TO_RQ, null);
                return;
            }
        }
    } // Only re-evaluated at arrival events (new process / I/O return), same
      // non-preemptive-mid-burst convention used by PriorityQueue and MFQ.

    @Override
    public void newProcess(boolean cpuEmpty) {
        preemptIfLessDeserving(cpuEmpty);
    }

    @Override
    public void IOReturningProcess(boolean cpuEmpty) {
        preemptIfLessDeserving(cpuEmpty);
    }
}
