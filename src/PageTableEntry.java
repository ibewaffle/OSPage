import java.util.Random;

/*
 * PageTableEntry.java
 * 
 * Page Table Element (PTE).
 * 
 * Created: 28/09/2011 Guy K. Kloss <guy.kloss@aut.ac.nz>
 * Changed:
 * 
 * Copyright (C) 2011 Auckland University of Technology, New Zealand
 * 
 * Some rights reserved
 * 
 * http://www.aut.ac.nz/
 */

/**
 * Page Table Element (PTE).
 *
 * @author Guy K. Kloss
 */
public class PageTableEntry {
    private boolean valid = false;
    private boolean referenced = false;
    private boolean modified = false;
    private int pageFrameNumber = 0;
    private Random randomiser = new Random();
        
    /**
     * Constructor.
     * 
     * @param pageFrameNumber Frame number this PTE points to.
     */
    public PageTableEntry(int pageFrameNumber) {
        this.pageFrameNumber = pageFrameNumber;
    }

    /**
     * PTE "valid" bit.
     * @return Returns the valid bit.
     */
    public boolean isValid() {
        return valid;
    }

    
    /**
     * PTE "valid" bit.
     * @param valid The valid bit to set.
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    
    /**
     * PTE "referenced" bit.
     * @return Returns the referenced bit.
     */
    public boolean isReferenced() {
        return referenced;
    }

    
    /**
     * PTE "referenced" bit.
     * @param referenced The referenced bit to set.
     */
    public void setReferenced(boolean referenced) {
        this.referenced = referenced;
    }

    
    /**
     * PTE "modified" bit.
     * @return Returns the modified bit.
     */
    public boolean isModified() {
        return modified;
    }

    
    /**
     * PTE "modified" bit.
     * @param modified The modified bit to set.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    
    /**
     * Page frame number in physical memory.
     * @return Returns the pageFrame.
     */
    public int getPageFrameNumber() {
        return pageFrameNumber;
    }

    
    /**
     * Page frame number in physical memory.
     * @param pageFrameNumber The pageFrame to set.
     */
    public void setPageFrameNumber(int pageFrameNumber) {
        this.pageFrameNumber = pageFrameNumber;
    }

    
    /**
     * "Access" the page table element. This may also flip the modified bit.
     */
    public void access() {
        this.referenced = true;
        if (this.randomiser.nextFloat() < 0.2) {
            // 20/80 chance of modifying it.
            this.modified = true;
        }
    }
}
