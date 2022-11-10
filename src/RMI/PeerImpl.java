package RMI;

import System.MessageType;
import System.VectorTimestamp;
import static System.FileTransferGUI.REGISTRY_URL;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption ;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server implementation of the Peer remote interface
 * This remote object represents a single peer in the peer to peer system
 * Peers are created as a remote object, then added to the Peer Connections remote object
 * @author Tamati Rudd 18045626
 */
public class PeerImpl implements Peer {
    //Peer Fields
    private String name;
    private String ipAddress;
    private VectorTimestamp timestamp; //The Peer's Vector Timestamp
    private FileTransferImpl transferMsg;
    private Path peerDirectory; //The Peer's directory in the project File system
    private ArrayList<File> myPeerFiles; //List of all Files this Peer has in its folder
    private Peer leader; //Reference to Leader of the system
    //Distributed Algorithm Fields
    private MessageImpl message;
    private boolean participant = false;
    private boolean ownTaken = false;
    private boolean prevTaken = false;
    private ArrayList<Message> prevMessages;
    private boolean nextTaken = false;
    private ArrayList<Message> nextMessages;
    private ArrayList<File> snapshot; //Snapshot of a Peer's received Files
    private MessageImpl snapshotMessage;
    private Queue<Message> waitingPeers;
    private int pendingReplies = 0;
    private boolean inCriticalSection = false;
    private boolean waiting = false;
    private ArrayList<File> globalFileList; //List of ALL Files on the system - compilation of Snapshots. Updated on Leader.
    private ArrayList<Message> transitMessages; //List of In Transit Messages at the time of a Global Snapshot

    //Construct a new Peer
    public PeerImpl(String name, String ipAddress, int peerCount) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.timestamp = new VectorTimestamp(peerCount, peerCount-1);
        myPeerFiles = new ArrayList<>();
        nextMessages = new ArrayList<>();
        prevMessages = new ArrayList<>();
        snapshot = new ArrayList<>();
        waitingPeers = new LinkedList<>();
        globalFileList = new ArrayList<>();
        transitMessages = new ArrayList<>();
        try {
            Path peerDirPath = FileSystems.getDefault().getPath("./peerfiles",name);
            if (!Files.exists(peerDirPath))
                peerDirectory = Files.createDirectories(peerDirPath);
            else {
                peerDirectory = Path.of("./peerfiles/", name);
                getExistingFiles();
            }
        } catch (IOException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, "Couldnt create "+name+"'s folder", ex);
        }  
    }
    
    //This method gets any existing Files in a Peer's directory
    @Override
    public void getExistingFiles() throws RemoteException {
        File peerDirFile = peerDirectory.toFile();
        File[] files = peerDirFile.listFiles();
        for (int i = 0; i < files.length; i++)
            myPeerFiles.add(files[i]);   
    }
    
    //This method receives a Message, calling the appropriate method for processing
    @Override
    public void receiveMessage(Message messageReceived) throws RemoteException {
        updateTimestamp(messageReceived.getMsgTimestamp()); //Update the Peer's Vector Timestamp
        switch (messageReceived.getMessageType()) {
            case FILE_TRANSFER -> {
                receiveFile((FileTransfer) messageReceived);
                //Send File Transfer message to global snapshot algorithm (as it may need to be part of a snapshot)
                ChandyLamportReceiveMessage(messageReceived); 
            }
            case ELECTION, LEADER -> ChangRobertsReceiveMessage(messageReceived); //Chang Roberts Leader Election
            case MARKER -> ChandyLamportReceiveMessage(messageReceived); //Chandy-Lamport Global Snapshot
            case SNAPSHOT -> messageReceived.getSender().RicartAgrawalaReqCritSection(); //Need to process Snapshot - request to enter Critical Section
            case REQUEST, OKAY -> RicartAgrawalaReceiveMessage(messageReceived);
            default -> {
            }
        }  
    }
    
    //This method allows a Peer to create & send a File Transfer message
    @Override
    public void sendFile(File file, List<String> recipientNames) throws RemoteException {
        ArrayList<Peer> recipients = new ArrayList<>();
        try { 
            //Create an ArrayList of recipient Peers
            Registry registry = LocateRegistry.getRegistry(REGISTRY_URL);
            PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");
            for (String s : recipientNames) { 
                Peer p = connectionsProxy.getPeerByName(s);
                if (p != null) 
                    recipients.add(p);
            } 
            //Create & send a File Transfer message
            transferMsg = new FileTransferImpl(file, this, recipients, this.getTimestamp());
            FileTransfer stub = (FileTransfer) UnicastRemoteObject.exportObject(transferMsg, 0);
            stub.sendMessage();
        } catch (NotBoundException | AccessException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }
    
    //Process a File Transfer message
    @Override
    public void receiveFile(FileTransfer message) throws RemoteException {
        try {
            //Save to the Peer's local folder 
            Path receivedFile = Paths.get(peerDirectory+"/"+message.getFile().getName());
            Files.deleteIfExists(receivedFile); //If a file of the received name already exists, delete & re-create
            Files.write(receivedFile, message.getFileData(), StandardOpenOption.CREATE);
            myPeerFiles.add(receivedFile.toFile()); //Add to the ArrayList of received Files
        } catch (IOException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //Process a Snapshot received by a Peer, updating the Leader's Snapshot with Files from other Peers
    //This method is the Critical Section in the Ricart Agrawala algorithm, as all Peers need to eventually call this (but can't concurrently)
    //Note: In this system, it is assumed that if two Files have the same name, they are the same File
    @Override
    public void ProcessSnapshot(Message messageReceived) throws RemoteException {
        //Add Snapshot Files to the Global File List
        for (File snapshotFile : messageReceived.getSender().getSnapshotMessage().getSnapshot()) {
            boolean fileAlreadyRecorded = false;
            //Check if the File already exists in the Global File List
            for (File knownFile : getLeader().getGlobalFileList()) {
                if (snapshotFile.getName().equals(knownFile.getName())) 
                    fileAlreadyRecorded = true;
            }
            //If the File isn't already recorded, add it to the Global File List
            if (!fileAlreadyRecorded) 
                getLeader().AddToGlobalList(snapshotFile);
        }  
        
        //Add any in-transit Messages to the Transit Messages list
        for (Message m : messageReceived.getSender().getSnapshotMessage().getNextMessages()) {
            getLeader().AddToTransitMessages(m);
        }
        for (Message m : messageReceived.getSender().getSnapshotMessage().getPrevMessages()) {
            getLeader().AddToTransitMessages(m);
        }
    }
    
    //Add a File to the (Leader's) global File list
    @Override
    public void AddToGlobalList(File fileToAdd) throws RemoteException {
        globalFileList.add(fileToAdd);
    }
    
    //Add a Message to the (Leader's) transit Messages list
    @Override
    public void AddToTransitMessages(Message messageToAdd) throws RemoteException {
        transitMessages.add(messageToAdd);
    }
      
    /* Distributed Algorithms
    * Chang-Roberts Leader Election: Uses a Token Ring to elect a leader
    *   -The leader is the Peer with the HIGHEST FILE COUNT (tiebreaker: Peer name)
    * Chandy-Lamport Global Snapshot: Leader takes a global snapshot of ALL UNIQUE FILES in the system. 
    *   - Each Peer gives the Leader a Snapshot of their list of Files
    *   - A Peer's neighbours are considered to be the previous and next Peer in the Peers list
    * Ricart-Agrawala: Contols access to Leader's Snapshot processing Critical Section
    *   - At the end of Chandy-Lamport, every Peer is sending their Snapshot to the Leader
    *   - This information is combined into one ArrayList, however every Peer is trying to put in their data concurrently
    *   - This causes a ConcurrentModificationException, so use Ricart Agrawala to control access
    */
    
    //This method allows a Peer to start a leader election
    @Override
    public void ChangRobertsStartElection() throws RemoteException {
        participant = true;
        try { //Send Election Message to next Peer
            Registry registry = LocateRegistry.getRegistry(REGISTRY_URL);
            PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");
            Peer nextPeer = connectionsProxy.getNextPeer(this);
            message = new MessageImpl(MessageType.ELECTION, timestamp, this, nextPeer, this);
            Message stub = (Message) UnicastRemoteObject.exportObject(message, 0);
            stub.sendMessage();
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    //This method handles the receiving of an Election or Leader type message
    //Election is decided by which Peer has the MOST FILES, Ties broken by Peer name compareTo()
    //Note: This is the Token Ring version of Chang Roberts Leader Election
    @Override
    public void ChangRobertsReceiveMessage(Message messageReceived) throws RemoteException {
        try { //Get Registry & Peer Connections object
            Registry registry = LocateRegistry.getRegistry(REGISTRY_URL);
            PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");  
            if (messageReceived.getMessageType() == MessageType.ELECTION) { //Election is in progress - vote
                Peer candidate = messageReceived.getIdentifier(); //Get the current "Winning" Peer
                if (candidate.getMyPeerFiles().size() > myPeerFiles.size()) { //Case 1: Candidate has MORE Files - Vote for candidate
                    participant = true;
                    Peer nextPeer = connectionsProxy.getNextPeer(this);
                    message = new MessageImpl(MessageType.ELECTION, timestamp, this, nextPeer, candidate);
                    message.sendMessage();
                }
                else if (candidate.getMyPeerFiles().size() == myPeerFiles.size()) { //Case 2: Tie in File Count, break by comparing Peer names
                    int tiebreak = name.compareTo(candidate.getName());
                    if (tiebreak < 0) { //Case 2.1: This Peer won the tiebreak
                        if (!participant) //If not already participated, start an Election with THIS PEER as candidate
                            ChangRobertsStartElection();
                    }
                    else if (tiebreak > 0) { //Case 2.2: This Peer lost the tiebreak - vote for the candidate
                        participant = true;
                        Peer nextPeer = connectionsProxy.getNextPeer(this);
                        message = new MessageImpl(MessageType.ELECTION, timestamp, this, nextPeer, candidate);
                        message.sendMessage();
                    }
                    else if (tiebreak == 0) { //Case 2.3: The This Peer has won the election, so is Leader - send Leader message
                        connectionsProxy.setLeader(this);
                        Peer nextPeer = connectionsProxy.getNextPeer(this);
                        message = new MessageImpl(MessageType.LEADER, timestamp, this, nextPeer, candidate);
                        message.sendMessage();
                    }
                }
                else if (candidate.getMyPeerFiles().size() < myPeerFiles.size()) { //Case 3: This Peer has MORE Files 
                    if (!participant) //If not already participated, start an Election with THIS PEER as candidate
                        ChangRobertsStartElection();
                }
            }
            else if (messageReceived.getMessageType() == MessageType.LEADER) { //Leader has been elected
                setLeader(messageReceived.getIdentifier());
                participant = false;
                if (!leader.getName().equals(name)) { //If this Peer isn't leader, send the leader message on to the next Peer
                    Peer nextPeer = connectionsProxy.getNextPeer(this);
                    message = new MessageImpl(MessageType.LEADER, timestamp, this, nextPeer, messageReceived.getIdentifier());
                    message.sendMessage();
                }       
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //Initiate a Global Snapshot. Snapshots are only initiated by the Leader of the system
    //The initiator sends the first marker message to itself 
    @Override
    public void ChandyLamportInitiateSnapshot() throws RemoteException {
        globalFileList = new ArrayList<>();
        message = new MessageImpl(MessageType.MARKER, timestamp, this, this);
        message.sendMessage();   
    }
    
    //This method handles taking a Global Snapshot using the Chandy Lamport Algorithm
    //Is called upon reception of Marker & File Transfer type messages
    //A Peer's "Neighbours" is considered to be the next and previous Peer in the "Token Ring"
    @Override
    public void ChandyLamportReceiveMessage(Message messageReceived) throws RemoteException {
        if (messageReceived.getMessageType() == MessageType.MARKER) {
            try {
                Registry registry = LocateRegistry.getRegistry(REGISTRY_URL);
                PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");
                Peer nextPeer = connectionsProxy.getNextPeer(this);
                Peer prevPeer = connectionsProxy.getPrevPeer(this);
                if (!ownTaken) {
                    snapshot.addAll(myPeerFiles); //Take snapshot of Files this Peer has
                    ownTaken = true;
                    //Send marker message to neighbours
                    if (connectionsProxy.getPeers().size() == 2) { //If 2 Peers, only need to send to next (if 1 Peer, don't need to send again)
                        message = new MessageImpl(MessageType.MARKER, timestamp, this, nextPeer);
                        message.sendMessage();    
                    }
                    else if (connectionsProxy.getPeers().size() >= 3){ //If 3+ Peers, send marker to both next and previous
                        message = new MessageImpl(MessageType.MARKER, timestamp, this, nextPeer);
                        message.sendMessage(); 
                        message = new MessageImpl(MessageType.MARKER, timestamp, this, prevPeer);
                        message.sendMessage(); 
                    }   
                }
                //Check whether the "next" neighbour has taken their snapshot
                if (messageReceived.getSender().getName().equals(nextPeer.getName())) {
                    nextTaken = true;
                }
                //Check whether the "previous" neighbour has taken their snapshot
                if (messageReceived.getSender().getName().equals(nextPeer.getName())) {
                    prevTaken = true;
                }
                if (ownTaken && nextTaken && prevTaken) { //Send the Snapshot (and any in transit messages) to the Leader (snapshot initiator)
                    MessageImpl snapshotMsg = new MessageImpl(MessageType.SNAPSHOT, timestamp, this, leader, snapshot, prevMessages, nextMessages);
                    setSnapshotMessage(snapshotMsg);
                    snapshotMsg.sendMessage();
                    //Reset snapshot fields  
                    ownTaken = false;
                    prevTaken = false;
                    nextTaken = false;
                    prevMessages.clear();
                    nextMessages.clear();
                    snapshot.clear();
                }
            } catch (NotBoundException ex) {
                Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        //Check if message is part of snapshot. A File Transfer Message may need to be recorded, so check for this type
        else if (messageReceived.getMessageType() == MessageType.FILE_TRANSFER) { 
            if (ownTaken && !nextTaken)
                nextMessages.add(messageReceived);
            if (ownTaken && !prevTaken)
                prevMessages.add(messageReceived);
        }     
    }
    
    //Request access to the Process Snapshot critical section
    @Override
    public void RicartAgrawalaReqCritSection() throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(REGISTRY_URL);
            PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");
            ArrayList<Peer> peers = connectionsProxy.getPeers();     
            if (peers.size() == 1) //Only one Peer in the system - can just enter Critical Section
                ProcessSnapshot(getSnapshotMessage());
            else { //Multiple Peers - send Request messages to ALL other Peers
                waiting = true;
                pendingReplies = peers.size()-1; //Await reply from OTHER Peers (not yourself)
                for (Peer p : peers) {
                    if (!p.getName().equals(name)) { //Do not send a request to self
                        message = new MessageImpl(MessageType.REQUEST, timestamp, this, p);
                        message.sendMessage();
                    }   
                }  
            }
            
        } catch (NotBoundException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }
    
    //Exit the Process Snapshot critical section, notifying other waiting processes
    @Override
    public void RicartAgrawalkaExitCritSection() throws RemoteException {
        for (Message m : waitingPeers) { //Send an Okay Message to every waiting Peer
            message = new MessageImpl(MessageType.OKAY, m.getMsgTimestamp(), this, m.getSender());
            message.sendMessage();
        }
        waitingPeers.clear();
    }
    
    //Receive a Request or Okay type Message (for critical section access)
    @Override
    public void RicartAgrawalaReceiveMessage(Message messageReceived) throws RemoteException {
        try {
            if (messageReceived.getMessageType() == MessageType.REQUEST) {
                if (inCriticalSection)
                    waitingPeers.add(messageReceived);
                else if (!waiting) {
                    message = new MessageImpl(MessageType.OKAY, messageReceived.getMsgTimestamp(), this, messageReceived.getSender());
                    message.sendMessage();
                }
                else { //This process is waiting
                    //Get timestamps & compare Peer names
                    int[] otherTimestamp = messageReceived.getMsgTimestamp().getTimestamps();
                    int otherPeerTimestamp = otherTimestamp[messageReceived.getMsgTimestamp().getOwnIndex()];
                    int[] ownTimestamp = timestamp.getTimestamps();
                    int ownPeerTimestamp = ownTimestamp[timestamp.getOwnIndex()];
                    int tiebreak = name.compareTo(messageReceived.getSender().getName());
                    
                    //Determine which process goes first
                    if (otherPeerTimestamp < ownPeerTimestamp || (otherPeerTimestamp == ownPeerTimestamp && tiebreak < 0)) {
                        //The other should go first: send OK message
                        message = new MessageImpl(MessageType.OKAY, messageReceived.getMsgTimestamp(), this, messageReceived.getSender());
                        message.sendMessage();
                    }
                    else //This Peer should go first: add Message to Queue
                        waitingPeers.add(messageReceived);
                }
            }
            else if (messageReceived.getMessageType() == MessageType.OKAY) {
                pendingReplies--;
                if (pendingReplies == 0) { //Can enter critical section
                    inCriticalSection = true;
                    waiting = false;
                    ProcessSnapshot(getSnapshotMessage());
                    inCriticalSection = false;
                    RicartAgrawalkaExitCritSection();
                }
            }
        } catch (RemoteException ex) {
            Logger.getLogger(PeerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //Timestamp Manipulation Methods
    //Call an update to the Peer's underlying Vector Timestamp array
    @Override
    public void updateTimestamp(VectorTimestamp msgTimestamp) throws RemoteException {
        timestamp.incrementTimestamp(msgTimestamp);
    }
    
    //Call an expansion to the Peer's underlying Vector Timestamp array
    @Override
    public void expandTimestamp() throws RemoteException {
        timestamp.expandArray();
    }

    //Call a shrink to the Peer's underlying Vector Timestamp array
    @Override
    public void shrinkTimestamp(int indexRemoved) throws RemoteException {
        timestamp.shrinkArray(indexRemoved);
    }
    
    //Getter & Setter methods
    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getIP() throws RemoteException {
        return ipAddress;
    } 

    @Override
    public VectorTimestamp getTimestamp() throws RemoteException {
        return timestamp;
    } 
    
    @Override
    public FileTransferImpl getTransferMsg() throws RemoteException {
        return transferMsg;
    }
    
    @Override
    public ArrayList<File> getMyPeerFiles() throws RemoteException {
        return myPeerFiles;
    }
    
    @Override
    public Peer getLeader() throws RemoteException {
        return leader;
    }

    @Override
    public void setLeader(Peer leader) throws RemoteException {
        this.leader = leader;
    }

    @Override
    public ArrayList<File> getSnapshot() throws RemoteException {
        return snapshot;
    }
    
    @Override
    public Message getSnapshotMessage() throws RemoteException {
        return snapshotMessage;
    }
    
    @Override
    public void setSnapshotMessage(MessageImpl snapshotMsg) throws RemoteException {
        snapshotMessage = snapshotMsg;
    }
    
    @Override
    public ArrayList<File> getGlobalFileList() throws RemoteException {
        return globalFileList;
    }
    
    @Override
    public ArrayList<Message> getTransitMessages() throws RemoteException {
        return transitMessages;
    }  
}
