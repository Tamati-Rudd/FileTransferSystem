package RMI;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 * Remote interface containing methods specific to a File Transfer Message (which handles the transmission of File byte data)
 * This is intended to be expandable to include more functionality in the future (e.g. rename, delete, sending of directories)
 * @author Tamati Rudd 18045626
 */
public interface FileTransfer extends Remote {
    public File getFile() throws RemoteException;
    public byte[] getFileData() throws RemoteException;
    public void sendMessage() throws RemoteException;
}
