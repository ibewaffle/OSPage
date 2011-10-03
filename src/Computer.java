/*
 * Computer.java
 * 
 * The virtual computer for the purpose of the simulation.
 * 
 * Created: 23/09/2011 Guy K. Kloss <guy.kloss@aut.ac.nz>
 * Changed:
 * 
 * Copyright (C) 2011 Auckland University of Technology, New Zealand
 * 
 * Some rights reserved
 * 
 * http://www.aut.ac.nz/
 */

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The virtual computer for the purpose of the simulation.
 *
 * @author Guy K. Kloss
 */
public class Computer {
    private List<Process> currentProcesses;
    private int cyclesTillNextProcess;
    private int currentCycle = 0;
    private int processesCreated = 0;
    private int processesDone = 0;
    private int totalWaits = 0;
    private int totalInstructions = 0;
    private Simulation mySimulation;
    private int nextPid = 0;
    private int lastCycleCount = 0;
    private int freePagesReturned = 0;
    private int cleanPagesReturned = 0;
    private int dirtyPagesReturned = 0;
    private Logger logger = Logger.getLogger("simulation");;
    
    /**
     * Constructor.
     * 
     * @param aSimulation A simulation object containing the parameters.
     */
    public Computer(Simulation aSimulation) {
        this.mySimulation = aSimulation;
        this.currentProcesses = new ArrayList<Process>();
    }
    
    
    /**
     * Advance simulation by one step.
     */
    public void step() {
        // In the beginning, make our first process.
        if (this.currentCycle == 0) {
            this.currentProcesses.add(_makeNewProcess());
        }
        
        int cyclesElapsed = this.currentCycle - this.lastCycleCount;
        this.lastCycleCount = this.currentCycle;
        
        // Do the timing for launching new processes.
        if (this.mySimulation.getProcessesToDo() > this.nextPid) {
            if (cyclesElapsed > this.cyclesTillNextProcess) {
                this.cyclesTillNextProcess = this.mySimulation.getCyclesTillNextProcess();
                this.currentProcesses.add(_makeNewProcess());
            } else {
                this.cyclesTillNextProcess -= cyclesElapsed;
            }
        }
        
        // Advance each process.
        for (Process aProcess : this.currentProcesses) {
            this.currentCycle += aProcess.step(this.currentCycle);
        }
        // Get rid of the ones done.
        this._purgeDoneProcesses();
        
        // The OS is working a bit as well..
        this.currentCycle += 50;
    }
    
    /**
     * @return Returns the processes created.
     */
    public int getProcessesCreated() {
        return processesCreated;
    }

    /**
     * @return Returns the processes done.
     */
    public int getProcessesDone() {
        return processesDone;
    }

    /**
     * @return Returns the total waits.
     */
    public int getTotalWaits() {
        return totalWaits;
    }

    /**
     * @return Returns the current cycle clock.
     */
    public int getCurrentCycle() {
        return this.currentCycle;
    }

    /**
     * @return Returns the total instructions.
     */
    public int getTotalInstructions() {
        return totalInstructions;
    }

    /**
     * @return Returns the number of free pages returned.
     */
    public int getFreePagesReturned() {
        return freePagesReturned;
    }
    
    /**
     * @return Returns the number of clean pages returned.
     */
    public int getCleanPagesReturned() {
        return cleanPagesReturned;
    }
    
    /**
     * @return Returns the number of dirty pages returned.
     */
    public int getDirtyPagesReturned() {
        return dirtyPagesReturned;
    }

    /**
     * Create new process.
     * 
     * @return Object for new process.
     */
    private Process _makeNewProcess() {
        this.nextPid++;
        int cyclesToGo= this.mySimulation.getProcessCyclesToGo();
        Process newProcess =  new Process(this.mySimulation,
                                          this.nextPid,
                                          cyclesToGo,
                                          this.mySimulation.getPagesMemoryToStart(),
                                          this.mySimulation.getQuantum());
        this.processesCreated++;
        logger.info("Process with PID " + this.nextPid + " created for "
                    + cyclesToGo + " cycles.");
        return newProcess;
    }
    
    /**
     * Purge finished processes from the process list and do some accounting
     * for the simulation.
     */
    private void _purgeDoneProcesses() {
        for (int i = this.currentProcesses.size() - 1; i >= 0; i--) {
            Process aProcess = this.currentProcesses.get(i);
            if (aProcess.isDone()) {
                this.logger.info("Process with PID " + aProcess.getPid() + " is done.");
                this.processesDone++;
                this.totalWaits += aProcess.getTotalWaits();
                this.totalInstructions += aProcess.getTotalInstructions();
                this.freePagesReturned += aProcess.getFreePagesReturned();
                this.cleanPagesReturned += aProcess.getCleanPagesReturned();
                this.dirtyPagesReturned += aProcess.getDirtyPagesReturned();
                this.currentProcesses.remove(i);
            }
        }
    }

}
