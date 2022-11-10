package System;

import java.io.Serializable;

/**
 * This Enumeration defines the types of Messages that can be sent & received in the File Transfer System
 * @author Tamati Rudd 18045626
 */
public enum MessageType implements Serializable {
    FILE_TRANSFER,
    ELECTION, 
    LEADER,
    MARKER,
    SNAPSHOT,
    REQUEST,
    OKAY;
}
