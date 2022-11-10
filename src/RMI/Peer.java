package RMI;

import System.VectorTimestamp;
import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
/**
 * Remote interface representing a Peer object
 * @author Tamati Rudd 18045626
 */
public interface Peer extends Remote {
    //Handle Messages & Files
    public void getExistingFiles() throws RemoteException;
    public void receiveMessage(Message messageReceived) throws RemoteException;
    public void sendFile(File file, List<String> recipientNames) throws RemoteException;
    public void receiveFile(FileTransfer message) throws RemoteException;
    public void ProcessSnapshot(Message messageReceived) throws RemoteException; //This method is the Critical Section
    public void AddToGlobalList(File fileToAdd) throws RemoteException;
    public void AddToTransitMessages(Message messageToAdd) throws RemoteException;
    
    //Distributed Algorithm Methods
    public void ChangRobertsStartElection() throws RemoteException;
    public void ChangRobertsReceiveMessage(Message messageReceived) throws RemoteException;
    public void ChandyLamportInitiateSnapshot() throws RemoteException;
    public void ChandyLamportReceiveMessage(Message messageReceived) throws RemoteException; 
    public void RicartAgrawalaReqCritSection() throws RemoteException; //Request Critical Section
    public void RicartAgrawalkaExitCritSection() throws RemoteException; //Exit Critical Section
    public void RicartAgrawalaReceiveMessage(Message messageReceived) throws RemoteException;
    
    //Manipulate Vector Timestamp
    public void updateTimestamp(VectorTimestamp msgTimestamp) throws RemoteException;
    public void expandTimestamp() throws RemoteException;
    public void shrinkTimestamp(int indexRemoved) throws RemoteException;
    
    //Field Getters & Setters
    public String getName() throws RemoteException;
    public String getIP() throws RemoteException;
    public VectorTimestamp getTimestamp() throws RemoteException;
    public FileTransferImpl getTransferMsg() throws RemoteException;
    public ArrayList<File> getMyPeerFiles() throws RemoteException;
    public Peer getLeader() throws RemoteException;
    public void setLeader(Peer leader) throws RemoteException;
    public ArrayList<File> getSnapshot() throws RemoteException;
    public Message getSnapshotMessage() throws RemoteException;
    public void setSnapshotMessage(MessageImpl snapshotMsg) throws RemoteException;
    public ArrayList<File> getGlobalFileList() throws RemoteException;
    public ArrayList<Message> getTransitMessages() throws RemoteException;
}
