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
public class MFQ extends Scheduler{

    int currentScheduler;
    
    private ArrayList<Scheduler> schedulers;
    //This may be a suggestion... you may use the current sschedulers to create the Multilevel Feedback Queue, or you may go with a more tradicional way
    //based on implementing all the queues in this class... it is your choice. Change all you need in this class.
    
    MFQ(OS os){
        super(os);
        currentScheduler = -1;
        schedulers = new ArrayList();
    }
    
    MFQ(OS os, Scheduler... s){ //Received multiple arrays
        this(os);
        schedulers.addAll(Arrays.asList(s));
        if(s.length > 0)
            currentScheduler = 0;
    }
        
    @Override
    public void addProcess(Process p){

        ProcessState estadoOriginal = p.getState();
        
        if (estadoOriginal == ProcessState.NEW) {
            p.setCurrentScheduler(0);
            schedulers.get(0).addProcess(p);
            newProcess(os.isCPUEmpty());
            
        } else if (estadoOriginal == ProcessState.CPU) {
            int nivelActual = p.getCurrentScheduler();
            int nuevoNivel = Math.min(nivelActual + 1, schedulers.size() - 1);
            p.setCurrentScheduler(nuevoNivel);
            schedulers.get(nuevoNivel).addProcess(p);
            
        } else if (estadoOriginal == ProcessState.IO) {
            int nivelActual = p.getCurrentScheduler();
            int nivelAnterior = Math.max(nivelActual - 1, 0);
            
            if (nivelAnterior == nivelActual) {
                schedulers.get(nivelActual).addProcess(p);
            } else {
                int ocupacionActual   = schedulers.get(nivelActual).processes.size();
                int ocupacionAnterior = schedulers.get(nivelAnterior).processes.size();
                
                if (ocupacionAnterior < ocupacionActual) {
                    p.setCurrentScheduler(nivelAnterior);
                    schedulers.get(nivelAnterior).addProcess(p);
                } else {
                    schedulers.get(nivelActual).addProcess(p);
                }
            }
            IOReturningProcess(os.isCPUEmpty());
        }
    
       //Overwriting the parent's addProcess(Process p) method may be necessary in order to decide what to do with process coming from the CPU.
        
    }
    
    void defineCurrentScheduler(){
        currentScheduler = -1;
        for (int i = 0; i < schedulers.size(); i++) {
            if (!schedulers.get(i).isEmpty()) {
                currentScheduler = i;
                break;
            }
        }


        //This methos is siggested to help you find the scheduler that should be the next in line to provide processes... perhaps the one with process in the queue?
    }
    
   
    @Override
    public void getNext(boolean cpuEmpty) {

        if (cpuEmpty) {
            defineCurrentScheduler();
            if (currentScheduler != -1) {
                schedulers.get(currentScheduler).getNext(true);
            }
        } else {
            int nivelActual = os.getProcessInCPU().getCurrentScheduler();
            schedulers.get(nivelActual).getNext(false);
        }
        //Suggestion: now that you know on which scheduler a process is, you need to keep advancing that scheduler. If it a preemptive one, you need to notice the changes
        //that it may have caused and verify if the change is coherent with the priority policy for the queues.
  
    }
    
    @Override
    public void newProcess(boolean cpuEmpty) {

        if (cpuEmpty) return;
        int nivelActual = os.getProcessInCPU().getCurrentScheduler();
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
        int nivelActual = os.getProcessInCPU().getCurrentScheduler();
        for (int nivel = 0; nivel < nivelActual; nivel++) {
            if (!schedulers.get(nivel).isEmpty()) {
                os.interrupt(InterruptType.SCHEDULER_CPU_TO_RQ, null);
                return;
            }
        }


    } //Non-preemtive in this event
    
}
