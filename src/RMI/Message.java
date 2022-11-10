package RMI;

import System.MessageType;
import System.VectorTimestamp;
import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * This remote interface represents a Message
 * Contains general Message methods, and methods needed for the Distributed Algorithms
 * File byte Transfer functionality is separated into interface FileTransfer.java
 * @author Tamati Rudd 18045626
 */
public interface Message extends Remote {
    public void sendMessage() throws RemoteException;
    public MessageType getMessageType() throws RemoteException;
    public VectorTimestamp getMsgTimestamp() throws RemoteException;
    public Peer getSender() throws RemoteException;
    public Peer getIdentifier() throws RemoteException;
    public ArrayList<File> getSnapshot() throws RemoteException;
    public ArrayList<Message> getPrevMessages() throws RemoteException;
    public ArrayList<Message> getNextMessages() throws RemoteException; 
}
