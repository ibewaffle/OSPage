import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * AddressTranslator.java
 * 
 * Performs virtual to physical address translation.
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

/**
 * Performs virtual to physical address translation.
 *
 * @author Guy K. Kloss
 */
public class AddressTranslator {
    private List<PageTableEntry> pageTable;;
    private boolean[] memoryFrames;
    private Simulation mySimulation;
    private PageReplacement myPageReplacement;
    private Random randomiser = new Random();
    
    /**
     * Constructor.
     * 
     * @param aSimulation Reference to simulation.
     * @param pagesMemoryToStart How many pages are needed to start this process.
     */
    public AddressTranslator(Simulation aSimulation, int pagesMemoryToStart) {
        this.mySimulation = aSimulation;
        this.pageTable = new ArrayList<PageTableEntry>();
        // To start, we need to get our memory footprint into memory, so we're
        // grabbing that amount. For this simulation, we're keeping it at that
        // as well, so a static array.
        this.memoryFrames = new boolean[pagesMemoryToStart];
        for (int i = 0; i < pagesMemoryToStart; i++) {
            this.pageTable.add(new PageTableEntry(i));
        }
        this.myPageReplacement = this.mySimulation.getPageReplacementImpl(this.pageTable,
                                                                          this.memoryFrames);
    }
    
    /**
     * Access memory on page from a virtual address.
     * 
     * @param pageNumber Virtual page number.
     * @return Number of clock cycles for the loading.
     */
    public int accessPage(int pageNumber) {
        int waitCycles = 0;
        
        if (this.pageTable.size() <= pageNumber) {
            // Make a new page in virtual memory.
            if (this.memoryFrames.length < pageNumber) {
                // Still filling up our pre-reserved memory.
                // So page number == frame number.
                PageTableEntry newPTE = new PageTableEntry(pageNumber);
                newPTE.setValid(true);
                newPTE.setReferenced(true);
                newPTE.setModified(false);
                this.memoryFrames[pageNumber] = true;
                this.pageTable.add(newPTE);
                waitCycles = this.mySimulation.getPageFromDiskCycles();
            } else {
                // Ask the page replacement algorithm which page to use.
                int pageNumberToReplace = this.myPageReplacement.getTargetPage();
                int frameNumber = this.pageTable.get(pageNumberToReplace).getPageFrameNumber();
                PageTableEntry newPTE = new PageTableEntry(frameNumber);
                this.pageTable.add(newPTE);
                waitCycles = this._swapForNew(pageNumberToReplace, pageNumber);
                this.memoryFrames[frameNumber] = true;
            }
        } else {
            // Access existing page.
            PageTableEntry currentPTE = this.pageTable.get(pageNumber);
            
            if (!currentPTE.isValid()) {
                // Bummer, load page from disk into frame and update page table.
                int pageToReplace = this.myPageReplacement.getTargetPage();
                waitCycles += this._swapForExisting(pageToReplace,
                                                    currentPTE.getPageFrameNumber());
            } else {
                // Let's roll the dice to see whether we've had a TLB miss or hit.
                if (this.mySimulation.isTlbHit()) {
                    // One cycle for TLB resolution.
                    waitCycles += 1;
                } else {
                    waitCycles += this.mySimulation.getPageTableCycles();
                }
            }
            
            // Let's "access" it.
            currentPTE.access();
        }
        
        return waitCycles;
    }



    /**
     * Returns the current size of the virtual address space in this page table.
     * 
     * @return Number of bytes.
     */
    public double getVirtualSize() {
        return this.mySimulation.getPageSize() * this.pageTable.size();
    }

    /**
     * Returns the used page size for memory management.
     * 
     * @return Number of bytes.
     */
    public double getPageSize() {
        return this.mySimulation.getPageSize();
    }

    /**
     * Frees a memory page
     * 
     * @param pageNumber Virtual page number.
     */
    public void freePage(int pageNumber) {
        // Resolve PTE for page number and get referenced memory frame.
        PageTableEntry thePage = this.pageTable.get(pageNumber);
        int frameNumber = thePage.getPageFrameNumber();
        // Set page to invalid and mark frame as unused.
        thePage.setValid(false);
        thePage.setReferenced(false);
        thePage.setModified(false);
        this.memoryFrames[frameNumber] = false;
    }

    /**
     * Swaps out a memory frame one referenced PTE to disk, and swaps in memory
     * referred to by another.
     * 
     * @param outPageNumber Page frame number in virtual memory to swap out.
     * @param inPageNumber Page frame number in virtual memory to swap in.
     * @return Number of clock cycles for the operation.
     */
    private int _swapForExisting(int outPageNumber, int inPageNumber) {
        PageTableEntry inPTE = this.pageTable.get(inPageNumber);
        PageTableEntry outPTE = this.pageTable.get(outPageNumber);
        int frameNumber = outPTE.getPageFrameNumber();
        int waitCycles = 0;
        
        // Swap out frame from source PTE.
        if (outPTE.isModified()) {
            waitCycles = this.mySimulation.getPageFromDiskCycles();
        }
        outPTE.setValid(false);
        
        // Swap into target PTE.
        waitCycles += this.mySimulation.getPageFromDiskCycles();
        inPTE.setValid(true);
        inPTE.setModified(false);
        inPTE.setReferenced(true);
        inPTE.setPageFrameNumber(frameNumber);
        this.memoryFrames[frameNumber] = true;
        
        return waitCycles;
    }

    /**
     * Swaps out a memory frame one referenced PTE to disk to make space for
     * new memory needed..
     * 
     * @param outPageNumber Page frame number in virtual memory to swap out.
     * @param newPageNumber New page frame number in virtual memory.
     * @return Number of clock cycles for the operation.
     */
    private int _swapForNew(int outPageNumber, int newPageNumber) {
        PageTableEntry outPTE = this.pageTable.get(outPageNumber);
        PageTableEntry newPTE = this.pageTable.get(newPageNumber);
        int frameNumber = outPTE.getPageFrameNumber();
        int waitCycles = 0;
        
        // Swap out frame from source PTE.
        if (outPTE.isModified()) {
            waitCycles = this.mySimulation.getPageFromDiskCycles();
        }
        outPTE.setValid(false);
        this.memoryFrames[frameNumber] = false;
        
        // We might have some load time in case of code to load.
        if (this.randomiser.nextFloat() < 0.5) {
            // Code needs to be loaded.
            newPTE.setModified(false);
            waitCycles += this.mySimulation.getPageFromDiskCycles();
        } else {
            // Memory allocated, but therefore modified.
            newPTE.setModified(true);
        }
        newPTE.setValid(true);
        newPTE.setReferenced(true);
        return waitCycles;
    }

    /**
     * @return Returns the number of free pages returned.
     */
    public int getFreePagesReturned() {
        return this.myPageReplacement.getFreePagesReturned();
    }
    
    /**
     * @return Returns the number of clean pages returned.
     */
    public int getCleanPagesReturned() {
        return this.myPageReplacement.getCleanPagesReturned();
    }
    
    /**
     * @return Returns the number of dirty pages returned.
     */
    public int getDirtyPagesReturned() {
        return this.myPageReplacement.getDirtyPagesReturned();
    }
}
