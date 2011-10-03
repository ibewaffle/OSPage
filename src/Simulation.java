/*
 * Simulation.java
 * 
 * Runner for the simulation.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Runner for the simulation.
 *
 * @author Guy K. Kloss
 */
public class Simulation {
    private int numberPages;
    private int pageSize;
    private int processesToDo;
    private double averageProcessCycles;
    private double averageProcessCycleStdDev;
    private int pagesMemoryToStart;
    private int averageTimeBetweenProcessStarts;
    private int quantum;
    private Computer myComputer;
    private Random randomiser;
    private double memoryPointerRelocationSpread;
    private static Logger logger = Logger.getLogger("simulation");
    private int cpuCyclesPerDiskRequest;
    private int waitCyclesPerDiskRequest;
    private int waitCyclesPerDiskRequestSpread;
    private double probabilityMemoryJump;
    private int cpuCyclesProcessing;
    private double probabilityFreePage;
    private double tlbHitRate;
    private double waitCyclesPerPageTableLookup;
    private double waitCyclesPerPageTableSpread;
    
    /**
     * Constructor.
     */
    public Simulation() {
        Properties configuration = new Properties();
        try {
            configuration.load(new FileInputStream("computer.properties"));
        } catch (FileNotFoundException e) {
            logger.severe("Could not find configuration file 'computer.properties'.");
            System.exit(1);
        } catch (IOException e) {
            logger.severe("Could not read configuration file 'computer.properties'.");
            System.exit(1);
        }
        this.randomiser = new Random();
        this.numberPages = Integer.parseInt(configuration.getProperty("numberPages"));
        this.pageSize = Integer.parseInt(configuration.getProperty("pageSize"));
        this.processesToDo = Integer.parseInt(configuration.getProperty("processesToDo"));
        this.averageProcessCycles = Double.parseDouble(configuration.getProperty("averageProcessCycles"));
        this.averageProcessCycleStdDev = Double.parseDouble(configuration.getProperty("averageProcessCycleStdDev"));
        this.pagesMemoryToStart = Integer.parseInt(configuration.getProperty("pagesMemoryToStart"));
        this.averageTimeBetweenProcessStarts = Integer.parseInt(configuration.getProperty("averageTimeBetweenProcessStarts"));
        this.quantum = Integer.parseInt(configuration.getProperty("quantum"));
        this.memoryPointerRelocationSpread = Double.parseDouble(configuration.getProperty("memoryPointerRelocationSpread"));
        this.cpuCyclesPerDiskRequest = Integer.parseInt(configuration.getProperty("cpuCyclesPerDiskRequest"));
        this.waitCyclesPerDiskRequest = Integer.parseInt(configuration.getProperty("waitCyclesPerDiskRequest"));
        this.waitCyclesPerDiskRequestSpread = Integer.parseInt(configuration.getProperty("waitCyclesPerDiskRequestSpread"));
        this.probabilityMemoryJump = Double.parseDouble(configuration.getProperty("probabilityMemoryJump"));
        this.cpuCyclesProcessing = Integer.parseInt(configuration.getProperty("cpuCyclesProcessing"));
        this.probabilityFreePage = Double.parseDouble(configuration.getProperty("probabilityFreePage"));
        this.tlbHitRate = Double.parseDouble(configuration.getProperty("tlbHitRate"));
        this.waitCyclesPerPageTableLookup = Double.parseDouble(configuration.getProperty("waitCyclesPerPageTableLookup"));
        this.waitCyclesPerPageTableSpread = Double.parseDouble(configuration.getProperty("waitCyclesPerPageTableSpread"));
    }

    /**
     * Create our Simulation.
     * 
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        // Make a simulation.
        Simulation mySimulation = new Simulation();
        
        // With that, make our computer.
        mySimulation.myComputer = new Computer(mySimulation);
        
        // Run simulation.
        mySimulation.run();
    }

    /**
     * Run the simulation.
     */
    private void run() {
        // Simulation loop.
        while (this.myComputer.getProcessesDone() != this.processesToDo) {
            this.myComputer.step();
        }
        
        this._collectResults();
    }
    

    /**
     * Utility method to return a new instance of an implementation of
     * our page replacement algorithm.
     * 
     * @param pageTable Page table used for address translation.
     * @param memoryFrames Usage status of "physical" memory frames
     *      (true = used, false = free).
     * @return Instance of a page replacement algorithm.
     */
    public PageReplacement getPageReplacementImpl(List<PageTableEntry> pageTable,
                                                  boolean[] memoryFrames) {
        return new GuysRandomReplacement(pageTable, memoryFrames);
    }
    
    /**
     * @return Process cycles to go for new process.
     */
    public int getProcessCyclesToGo() {
        int processCyclesToGo = (int)(this.randomiser.nextGaussian()
                * this.averageProcessCycleStdDev + this.averageProcessCycles);
        if (processCyclesToGo < 0.2 * this.averageProcessCycles) {
            processCyclesToGo = (int)this.averageProcessCycles;
        }
        return processCyclesToGo;
    }
    
    /**
     * @return Number of pages of memory to start process.
     */
    public int getPagesMemoryToStart() {
        return this.randomiser.nextInt(this.pagesMemoryToStart);
    }
    
    /**
     * @return Returns the number of pages.
     */
    public int getNumberPages() {
        return numberPages;
    }

    /**
     * @return Returns the page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @return Returns the processes to do.
     */
    public int getProcessesToDo() {
        return processesToDo;
    }
    
    /**
     * @return Returns number of cycles before next process is to start.
     */
    public int getCyclesTillNextProcess() {
        if (this.myComputer.getProcessesCreated() < this.processesToDo) {
            return this.randomiser.nextInt(this.averageTimeBetweenProcessStarts);
        }
        return -1;
    }

    /**
     * @return Returns the process execution quantum.
     */
    public int getQuantum() {
        return quantum;
    }

    /**
     * Returns a new relative location for the memory pointer by a certain
     * probability. If not, then the passed location is kept.
     * 
     * @param currentLocation Current relative location.
     * @return Relative position in virtual address space.
     */
    public double newMPlocation(double currentLocation) {
        if ((this.randomiser.nextDouble() < this.probabilityMemoryJump)
                || (currentLocation == 0)) {
            return this._newMPlocation();
        } else {
            currentLocation += this._newMPrelocation();
            if (currentLocation < 0) {
                currentLocation = this._newMPlocation();
            }
        }
        return currentLocation;
    }

    /**
     * Returns a new relative location for the process counter by a certain
     * probability. If not, then the passed location is kept.
     * 
     * @param currentLocation Current relative location.
     * @return Relative position in virtual address space.
     */
    public double newPClocation(double currentLocation) {
        if ((this.randomiser.nextDouble() < this.probabilityMemoryJump)
                || (currentLocation == 0)) {
            return this._newPClocation();
        } else {
            currentLocation += this._newPCrelocation();
            if (currentLocation < 0) {
                currentLocation = this._newPClocation();
            }
        }
        return currentLocation;
    }

    /**
     * Returns CPU cycles per disk request.
     * 
     * @return Number of cycles.
     */
    public int getCpuCyclesPerDiskRequest() {
        return this.cpuCyclesPerDiskRequest;
    }

    /**
     * Returns CPU cycles for processing.
     * 
     * @return Number of cycles.
     */
    public int getCpuCyclesProcessing() {
        return 2 + this.randomiser.nextInt(this.cpuCyclesProcessing);
    }

    /**
     * Used after a jump. Determines whether we should free the page that we've
     * jumped away from.
     * 
     * @return True if the page is to be freed.
     */
    public boolean askFreePage() {
        return (this.probabilityFreePage < this.randomiser.nextFloat());
    }

    /**
     * Are we successful on the TLB page resolution?
     * 
     * @return True for a successful TLB lookup.
     */
    public boolean isTlbHit() {
        return (this.randomiser.nextFloat() < this.tlbHitRate);
    }

    /**
     * Cycles used for page table lookup.
     * 
     * @return Number of cycles.
     */
    public int getPageTableCycles() {
        int waitCycles = (int)(this.waitCyclesPerPageTableLookup
                               + this.randomiser.nextGaussian()
                               * this.waitCyclesPerPageTableSpread);
        if (waitCycles < 5) {
            waitCycles = 5;
        }
        return waitCycles;
    }

    /**
     * Cycles used for loading page from disk into page table.
     * 
     * @return Number of cycles.
     */
    public int getPageFromDiskCycles() {
        int waitCycles = (int)(this.waitCyclesPerDiskRequest
                + this.randomiser.nextGaussian()
                * this.waitCyclesPerDiskRequestSpread);
        if (waitCycles < 500) {
            waitCycles = 500;
        }
        return waitCycles;
    }

    /**
     * Returns a new relative location for the process counter.
     * 
     * @return Relative position in virtual address space.
     */
    private double _newPClocation() {
        return 0.75 * this.randomiser.nextFloat();
    }

    /**
     * Returns byte relocation for current process counter.
     * 
     * @return Number of bytes to relocate PC for.
     */
    private int _newPCrelocation() {
        return (int)(this.memoryPointerRelocationSpread / this.pageSize
                     * (1 + this.randomiser.nextGaussian()));
    }

    /**
     * Returns a new relative location for the memory pointer.
     * 
     * @return Relative position in virtual address space.
     */
    private double _newMPlocation() {
        return 0.75 + 0.25 * this.randomiser.nextFloat();
    }

    /**
     * Returns byte relocation for memory pointer.
     * 
     * @return Number of bytes to relocate MP for.
     */
    private int _newMPrelocation() {
        return (int)(this.memoryPointerRelocationSpread / this.pageSize
                     * this.randomiser.nextGaussian());
    }

    /**
     * Inventorise, and write results to a file.
     */
    private void _collectResults() {
        String pageReplacementClassName = this.getPageReplacementImpl(null, null).getClass().getName();
        String retultFileName = "results_" + pageReplacementClassName + ".txt";
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(retultFileName));
            
            // Write the page replacement algorithm implementation to the file.
            out.write("[PageReplacement]\n");
            out.write(pageReplacementClassName + "\n\n");
            
            // Write the configuration to the output file as well.
            out.write("[Configuration]\n");
            BufferedReader config;
            try {
                config = new BufferedReader(new FileReader("computer.properties"));
                String line = config.readLine();
                while (line != null) {
                    out.write(line + "\n");
                    line = config.readLine();
                }
                config.close();
                out.write("\n");
            } catch (IOException e) {
                logger.severe("Could not read configuratio file computer.properties");
            }

            // Show us how we've done.
            out.write("[Results]\n");
            out.write("Total instructions: " + this.myComputer.getTotalInstructions() + "\n");
            out.write("Total waits: " + this.myComputer.getTotalWaits() + "\n");
            out.write("Total cycles: " + this.myComputer.getCurrentCycle() + "\n");
            out.write("Total free pages returned: " + this.myComputer.getFreePagesReturned() + "\n");
            out.write("Total clean pages returned: " + this.myComputer.getCleanPagesReturned() + "\n");
            out.write("Total dirty pages returned: " + this.myComputer.getDirtyPagesReturned() + "\n");
            out.write("Processes done: " + this.getProcessesToDo() + "\n");
            out.close();
        } catch (IOException e) {
            logger.severe("Could not write to file " + retultFileName);
        }
    }
}
