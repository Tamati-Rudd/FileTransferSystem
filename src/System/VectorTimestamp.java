package System;

import java.io.Serializable;

/**
 * This class represents & manipulates a Vector Timestamp
 * @author Tamati Rudd 18045626
 */
public class VectorTimestamp implements Serializable {
    private int[] timestamps;
    private int ownIndex; //Index indicating which index is a peer's own timestamp
    
    //Construct a new VectorTimestamp object
    public VectorTimestamp(int peerCount, int ownIndex) {
        timestamps = new int[peerCount];
        this.ownIndex = ownIndex;
    }
    
    //Increment a peer's vector timestamp, and adjust other timestamps if applicable
    public void incrementTimestamp(VectorTimestamp msgTimestamp) {
        timestamps[ownIndex]++; //Increment own timestamp
        
        //If a message had a timestamp, take the larger of the known timestamps (for each counter except self)
        if (msgTimestamp != null) { 
            for (int i = 0; i < timestamps.length; i++) {
                if (i != ownIndex && i < msgTimestamp.getTimestamps()[i]) {
                    timestamps[i] = msgTimestamp.getTimestamps()[i]; 
                }
            }
        }
    }
    
    //Expand the vector timestamp array by 1 (run when a new peer joins)
    public void expandArray() {
        //Save existing values and expand the array
        int[] temp = timestamps; 
        timestamps = new int[timestamps.length+1];
        
        //Copy existing values back into the timestamps array
        for (int i = 0; i < timestamps.length; i++) { 
            if (i != timestamps.length-1) { 
                timestamps[i] = temp[i];
            }
        }
        
        incrementTimestamp(null); //Increment timestamp as received a message to expand
    }
    
    //Shrink the vector timestamp array by 1 (run when a peer leaves)
    public void shrinkArray(int indexRemoved) {
        //Save existing values and shrink the array
        int[] temp = timestamps; 
        timestamps = new int[timestamps.length-1];
        if (ownIndex > indexRemoved)
            ownIndex--;
        
        //Copy values back into the timestamps array, except for the index to be removed
        for (int i = 0, j = 0; i < temp.length; i++) {  //i = temp array index, j = timestamps array index
            if (i != indexRemoved) {
                timestamps[j] = temp[i];
                j++;
            }
        }
        
        incrementTimestamp(null); //Increment timestamp as received a message to shrink
    }

    //Getters & Setters
    public int[] getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(int[] timestamps) {
        this.timestamps = timestamps;
    }

    public int getOwnIndex() {
        return ownIndex;
    }

    public void setOwnIndex(int ownIndex) {
        this.ownIndex = ownIndex;
    }
    
    //Build a string representation of the timestamps array
    @Override
    public String toString() {
        String timestampArray = "(";
        for (int i = 0; i < timestamps.length; i++) {
            timestampArray+=timestamps[i];
            if (i < timestamps.length-1)
                timestampArray+=",";
        }
        timestampArray+=")";
        return timestampArray;
    }
}
