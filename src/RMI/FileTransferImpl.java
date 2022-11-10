package RMI;

import System.MessageType;
import System.VectorTimestamp;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class functions as a special type of Message, allowing a sender to send a file in bytes to the receiver(s)
 * @author Tamati Rudd 18045626
 */
public class FileTransferImpl implements FileTransfer, Message, Serializable {
    private MessageType messageType = MessageType.FILE_TRANSFER;
    private File fileSent;
    private byte[] fileData;
    private Peer sender;
    private ArrayList<Peer> recipients; //File Transfer Messages can have multiple receivers, unlike other types
    private VectorTimestamp msgTimestamp;
    
    //Construct a new FileTransferImpl message
    public FileTransferImpl(File fileSent, Peer sender, ArrayList<Peer> recipients, VectorTimestamp timestamp) {
        this.fileSent = fileSent;
        this.sender = sender;
        this.recipients = recipients;
        this.msgTimestamp = timestamp;
        
        try {
            fileData = Files.readAllBytes(Paths.get(fileSent.getPath()));
        } catch (IOException ex) {
            Logger.getLogger(FileTransferImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Send the message - call the receiveMessage method in all receiving Peers
    @Override
    public void sendMessage() throws RemoteException {
        for (Peer p : recipients) {
            p.receiveMessage(this);
        }
    }
    
    //Getter methods
    @Override
    public File getFile() throws RemoteException {
        return fileSent;
    }
    
    @Override
    public Peer getSender() throws RemoteException {
        return sender;
    }

    @Override
    public byte[] getFileData() throws RemoteException {
        return fileData;
    }
    
    @Override
    public VectorTimestamp getMsgTimestamp() throws RemoteException {
        return msgTimestamp;
    }  

    @Override
    public MessageType getMessageType() throws RemoteException {
        return messageType;
    }

    //These methods are unsupported by File Transfer messages, as they are only needed for Distributed Algorithms
    @Override
    public Peer getIdentifier() throws RemoteException {
        throw new UnsupportedOperationException("Not supported by File Transfer messages."); 
    }

    @Override
    public ArrayList<File> getSnapshot() throws RemoteException {
        throw new UnsupportedOperationException("Not supported by File Transfer messages."); 
    }

    @Override
    public ArrayList<Message> getPrevMessages() throws RemoteException {
        throw new UnsupportedOperationException("Not supported by File Transfer messages."); 
    }

    @Override
    public ArrayList<Message> getNextMessages() throws RemoteException {
        throw new UnsupportedOperationException("Not supported by File Transfer messages."); 
    }
}
