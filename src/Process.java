/*
 * Process.java
 * 
 * Process abstraction for simulation.
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

import java.util.logging.Logger;

/**
 * Process abstraction for simulation.
 *
 * @author Guy K. Kloss
 */
public class Process {
    private Logger logger = Logger.getLogger("simulation");
    private int waitCyclesToGo = 0;
    private int processCyclesToGo;
    private int totalWaits = 0;
    private int totalInstructions = 0;
    private int lastCycleCount = 0;
    private AddressTranslator virtualMemory;
    private int pagesMemoryToStart;
    private int loadCount = 0;
    private int pid;
    private int quantum;
    private boolean loaded;
    private Simulation mySimulation;
    private double currentPC;
    private double currentMP;
    private boolean stoppedWaiting = false;
    
    /**
     * Constructor.
     * 
     * @param simulation Reference to simulation.
     * @param pid Process ID.
     * @param processCyclesToGo Number of process cycles to do before done.
     * @param pagesMemoryToStart How many pages are needed to start this process.
     * @param quantum Process execution quantum.
     */
    public Process(Simulation simulation,
                   int pid,
                   int processCyclesToGo,
                   int pagesMemoryToStart,
                   int quantum) {
        this.mySimulation = simulation;
        this.pid = pid;
        this.processCyclesToGo = processCyclesToGo;
        this.pagesMemoryToStart = pagesMemoryToStart;
        this.quantum = quantum;
        this.virtualMemory = new AddressTranslator(this.mySimulation,
                                                   this.pagesMemoryToStart);
    }
    
    /**
     * @return True if process is waiting for memory or I/O.
     */
    public boolean isWaiting() {
        if (this.waitCyclesToGo  > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Decrement the wait counter.
     *
     * @param cycles Number of cycles to count off.
     */
    public void stepDownWait(int cycles) {
    }

    /**
     * Advance process by one step;
     * 
     * @param currentCycle Current global cycle count.
     * @return Cycles spent in task.
     */
    public int step(int currentCycle) {
        this.logger.fine("PID " + this.pid + ": Cycle " + currentCycle + " of " + this.processCyclesToGo);
        int cyclesElapsed = currentCycle - this.lastCycleCount;
        this.lastCycleCount = currentCycle;
        
        // If we're waiting, do our wait, and return.
        if (this.waitCyclesToGo > cyclesElapsed) {
            this.waitCyclesToGo -= cyclesElapsed;
            return 1;
        } else {
            this.waitCyclesToGo = 0;
            this.stoppedWaiting = true;
        }
        
        cyclesElapsed = 0;
        // Do some work.
        while ((cyclesElapsed < this.quantum)
                && (this.waitCyclesToGo < 500)) {
            if (!this.loaded) {
                cyclesElapsed = this._loadProcess();
            } else {
                cyclesElapsed = this._advanceProcess();
            }
        }
        this.totalInstructions += cyclesElapsed;
        return cyclesElapsed;
    }

    /**
     * @return True if process is done.
     */
    public boolean isDone() {
        if (this.totalInstructions > this.processCyclesToGo) {
            return true;
        }
        return false;
    }

    /**
     * @return Returns the total waits.
     */
    public int getTotalWaits() {
        return totalWaits;
    }

    /**
     * @return Returns the total instructions.
     */
    public int getTotalInstructions() {
        return totalInstructions;
    }

    /**
     * @return Returns the PID.
     */
    public int getPid() {
        return pid;
    }

    /**
     * @return Returns the number of free pages returned.
     */
    public int getFreePagesReturned() {
        return this.virtualMemory.getFreePagesReturned();
    }
    
    /**
     * @return Returns the number of clean pages returned.
     */
    public int getCleanPagesReturned() {
        return this.virtualMemory.getCleanPagesReturned();
    }
    
    /**
     * @return Returns the number of dirty pages returned.
     */
    public int getDirtyPagesReturned() {
        return this.virtualMemory.getDirtyPagesReturned();
    }

    /**
     * Advances the process for every step.
     * 
     * @return Number of CPU cycles elapsed for the operation.
     */
    private int _advanceProcess() {
        int loadCycles = 0;
        int freePageCycles = 0;
        if (!this.stoppedWaiting) {
            // Get next mem references of process counter (PC)
            // and memory pointer (MP).
            double oldMP = this.currentMP;
            this.currentPC = this.mySimulation.newPClocation(this.currentPC);
            this.currentMP = this.mySimulation.newMPlocation(this.currentMP);
        
            // Free the old page after move of more than 10%?
            if ((Math.abs(oldMP) - this.currentMP > 0.1)
                    && (this.mySimulation.askFreePage())) {
                this.virtualMemory.freePage(this._relativeToPage(oldMP));
                freePageCycles = 5;
            }
            
            // Load memory addresses.
            loadCycles = this.virtualMemory.accessPage(this._relativeToPage(this.currentPC));
            loadCycles += this.virtualMemory.accessPage(this._relativeToPage(this.currentMP));
            
            if (loadCycles >= 500) {
                // Too bad, need to wait now. Yield to other processes.
                this.waitCyclesToGo += loadCycles;
                this.totalWaits += loadCycles;
                return this.mySimulation.getCpuCyclesPerDiskRequest();
            }
        }
        
        this.stoppedWaiting = false;
        return loadCycles + freePageCycles
                + this.mySimulation.getCpuCyclesProcessing();
    }

    /**
     * Loads the initial process when started.
     * 
     * @return Number of CPU cycles elapsed for the operation.
     */
    private int _loadProcess() {
        if (this.loadCount < this.pagesMemoryToStart) {
            this.logger.fine("PID " + this.pid + ": Loaded page " + this.loadCount);
            this.waitCyclesToGo += this.virtualMemory.accessPage(this.loadCount);
            this.loadCount++;
        } else {
            this.currentPC = this.mySimulation.newPClocation(0);
            this.currentMP = this.mySimulation.newMPlocation(0);
            this.loaded = true;
            this.logger.fine("PID " + this.pid + ": Finished loading");
        }
        return this.mySimulation.getCpuCyclesPerDiskRequest();
    }

    /**
     * Converts the relative address within the virtual address space to the
     * page number.
     * 
     * @param relativeReference Relative address within VM.
     * @return Page number.
     */
    private int _relativeToPage(double relativeReference) {
        if (relativeReference < 1.0) {
            return (int)(this.virtualMemory.getVirtualSize() * relativeReference
                    / this.virtualMemory.getPageSize());
        } else {
            return (int)(this.virtualMemory.getVirtualSize()
                    / this.virtualMemory.getPageSize());
        }
    }
}
