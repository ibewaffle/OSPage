/*
 * PageReplacement.java
 * 
 * Abstract class as a base to implement page replacement algorithms.
 * 
 * Created: 30/09/2011 Guy K. Kloss <guy.kloss@aut.ac.nz>
 * Changed:
 * 
 * Copyright (C) 2011 Auckland University of Technology, New Zealand
 * 
 * Some rights reserved
 * 
 * http://www.aut.ac.nz/
 */

import java.util.List;

/**
 * Abstract class as a base to implement page replacement algorithms.
 *
 * @author Guy K. Kloss
 */
public abstract class PageReplacement {
    protected List<PageTableEntry> pageTable;;
    protected boolean[] memoryFrames;
    protected int freePagesReturned = 0;
    protected int cleanPagesReturned = 0;
    protected int dirtyPagesReturned = 0;
    
    /**
     * Constructor.
     * 
     * @param pageTable Page table used for address translation.
     * @param memoryFrames Usage status of "physical" memory frames
     *      (true = used, false = free).
     */
    public PageReplacement(List<PageTableEntry> pageTable,
                           boolean[] memoryFrames) {
        this.pageTable = pageTable;
        this.memoryFrames = memoryFrames;
    }

    /**
     * This is the "core" of the page replacement algorithm. This method returns
     * the page number of the page to use. It will either identify one that is 
     * free or one that is to be evicted.
     * 
     * @return Page number of the virtual address page to evict.
     */
    public abstract int getTargetPage();

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
}
