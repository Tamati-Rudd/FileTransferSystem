package RMI;

import System.MessageType; 
import System.VectorTimestamp;
import java.io.File;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * This class represents a non-file transfer message, used to run the Distributed Algorithms
 * @author Tamati Rudd 18045626
 */
public class MessageImpl implements Message, Serializable {
    private MessageType messageType;
    private Peer sender;
    private Peer receiver; //Standard messages can only have ONE receiver
    private VectorTimestamp msgTimestamp;
    //Optional Fields (for specific Message Types)
    private Peer identifier; //The candidate of a Leader Election
    private ArrayList<File> snapshot; //Snapshot of a Peer's Files
    private ArrayList<Message> prevMessages; //List of messages received from the previous Peer during a snapshot
    private ArrayList<Message> nextMessages; //List of messages received from the next Peer during a snapshot
    
    //Construct a Default Message
    public MessageImpl(MessageType type, VectorTimestamp timestamp, Peer sender, Peer receiver) {
        this.messageType = type;
        this.msgTimestamp = timestamp;
        this.sender = sender;
        this.receiver = receiver;
    }
    
    //Construct a Message with an identifier - used for the ELECTION and LEADER Message Types
    public MessageImpl(MessageType type, VectorTimestamp timestamp, Peer sender, Peer receiver, Peer identifier) {
        this(type, timestamp, sender, receiver);
        this.identifier = identifier;
    }
    
    //Construct a Message with a Snapshot & Message Lists - used for the SNAPSHOT Message Type
    public MessageImpl(MessageType type, VectorTimestamp timestamp, Peer sender, Peer receiver, ArrayList<File> snapshot, ArrayList<Message> prev, ArrayList<Message> next) {
        this(type, timestamp, sender, receiver);
        this.snapshot = snapshot;
        this.prevMessages = prev;
        this.nextMessages = next;
    }
    
    //Send the message to the receiver
    @Override
    public void sendMessage() throws RemoteException {
        receiver.receiveMessage(this);
    }
    
    //Getter Methods
    @Override
    public MessageType getMessageType() throws RemoteException {
        return messageType;
    }

    @Override
    public VectorTimestamp getMsgTimestamp() throws RemoteException  {
        return msgTimestamp;
    }

    @Override
    public Peer getSender() throws RemoteException {
        return sender;
    }
            
    @Override
    public Peer getIdentifier() throws RemoteException  {
        return identifier;
    }

    @Override
    public ArrayList<File> getSnapshot() throws RemoteException {
        return snapshot;
    }

    @Override
    public ArrayList<Message> getPrevMessages() throws RemoteException {
        return prevMessages;
    }

    @Override
    public ArrayList<Message> getNextMessages() throws RemoteException {
        return nextMessages;
    }
}
