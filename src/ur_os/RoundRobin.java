/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os;

/**
 *
 * @author prestamour
 */
public class RoundRobin extends Scheduler{

    int q;
    int cont;
    boolean multiqueue;
    
    RoundRobin(OS os){
        super(os);
        q = 4; // Numero RR
        cont=0;
    }
    
    RoundRobin(OS os, int q){
        this(os);
        this.q = q;
    }

    RoundRobin(OS os, int q, boolean multiqueue){
        this(os);
        this.q = q;
        this.multiqueue = multiqueue;
    }
    

    
    void resetCounter(){
        cont=0;
    }
   
    @Override
    public void getNext(boolean cpuEmpty) {
        if(cpuEmpty){

            if(!processes.isEmpty()){
                Process next = processes.poll();
                resetCounter();
                os.interrupt(InterruptType.SCHEDULER_RQ_TO_CPU, next);
            }
        } else {

            cont++;
            
            if(cont >= q){

                if(!processes.isEmpty()){
                    Process next = processes.poll();
                    resetCounter();
                    os.interrupt(InterruptType.SCHEDULER_CPU_TO_RQ, next);
                } else {
                    resetCounter();
                }
            }
        }
    }
    
    
    @Override
    public void newProcess(boolean cpuEmpty) {} //Non-preemtive in this event

    @Override
    public void IOReturningProcess(boolean cpuEmpty) {} //Non-preemtive in this event
    
}
