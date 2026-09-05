/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os;

import java.util.ArrayList;
import java.util.Arrays;


/**
 *
 * @author prestamour
 */
public class PriorityQueue extends Scheduler{

    int currentScheduler;
    
    private ArrayList<Scheduler> schedulers;
    
    PriorityQueue(OS os){
        super(os);
        currentScheduler = -1;
        schedulers = new ArrayList();
    }
    
    PriorityQueue(OS os, Scheduler... s){ //Received multiple arrays
        this(os);
        schedulers.addAll(Arrays.asList(s));
        if(s.length > 0)
            currentScheduler = 0;
    }
    
    
    @Override
    public void addProcess(Process p){
        ProcessState estadoOriginal = p.getState();  // hay que guardarlo ANTES de delegar
        int nivel = p.getPriority();
        
        schedulers.get(nivel).addProcess(p);  // inserta en la sub-cola correcta (RR interno, no preemptivo)
        
        if (estadoOriginal == ProcessState.NEW) {
            newProcess(os.isCPUEmpty());
        } else if (estadoOriginal == ProcessState.IO) {
            IOReturningProcess(os.isCPUEmpty());
        }
        // Si estadoOriginal == CPU (viene de un RR interno que le acabó el quantum a alguien del MISMO nivel),
        // no hace falta hacer nada más aquí — ya quedó bien encolado en su propio nivel.


       //Overwriting the parent's addProcess(Process p) method may be necessary in order to decide what to do with process coming from the CPU.
       //On which queue should the process go?
        
    }
    
    void defineCurrentScheduler(){
        currentScheduler = -1;
        for (int i = 0; i < schedulers.size(); i++) {
            if (!schedulers.get(i).isEmpty()) {
                currentScheduler = i;
                break;
            }
        }
        //This methos is suggested to help you find the scheduler that should be the next in line to provide processes... perhaps the one with process in the queue?
    }
    
   
    @Override
    public void getNext(boolean cpuEmpty) {
         if (cpuEmpty) {
            defineCurrentScheduler();
            if (currentScheduler != -1) {
                schedulers.get(currentScheduler).getNext(true);
            }
        } else {
            int nivelActual = os.getProcessInCPU().getPriority();
            schedulers.get(nivelActual).getNext(false);  // deja que su propio RR siga contando su quantum
        }

        //Suggestion: now that you know on which scheduler a process is, you need to keep advancing that scheduler. If it a preemptive one, you need to notice the changes
        //that it may have caused and verify if the change is coherent with the priority policy for the queues.
        //Suggestion: if the CPU is empty, just find the next scheduler based on the order and the existence of processes
        //if the CPU is not empty, you need to define that will happen with the process... if it fully preemptive, and there are process pending in higher queue, does the
        //scheduler removes a process from the CPU or does it let it finish its quantum? Make this decision and justify it.
  
    }
    
    @Override
    public void newProcess(boolean cpuEmpty) {
        if (cpuEmpty) return;
        int nivelActual = os.getProcessInCPU().getPriority();
        for (int nivel = 0; nivel < nivelActual; nivel++) {
            if (!schedulers.get(nivel).isEmpty()) {
                os.interrupt(InterruptType.SCHEDULER_CPU_TO_RQ, null);
                return;
            }
        }
    } //Non-preemtive in this event


    @Override
    public void IOReturningProcess(boolean cpuEmpty) {
        if (cpuEmpty) return;
        int nivelActual = os.getProcessInCPU().getPriority();
        for (int nivel = 0; nivel < nivelActual; nivel++) {
            if (!schedulers.get(nivel).isEmpty()) {
                os.interrupt(InterruptType.SCHEDULER_CPU_TO_RQ, null);
                return;
            }
        }
    } //Non-preemtive in this event
    
}
