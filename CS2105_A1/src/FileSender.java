/* 
 * CS2105 Assignment 1
 * Name: Lu Yang
 * Matriculation: A0130684H
 */

import java.io.*;
import java.net.*;


class FileSender {
    
    public DatagramSocket socket; 
    public DatagramPacket pkt;
    
    public static void main(String[] args) throws IOException {
        
        // check if the number of command line argument is 4
        if (args.length != 4) {
            System.out.println("Usage: java FileSender <path/filename> "
                                   + "<rcvHostName> <rcvPort> <rcvFileName>");
            System.exit(1);
        }
        
        new FileSender(args[0], args[1], args[2], args[3]);
    }
    
    public FileSender(String fileToOpen, String host, String port, String rcvFileName) throws IOException {
        
        // Refer to Assignment 0 Ex #4 on how to open a file with BufferedInputStream
    	byte[] buffer = new byte[1000];
    	File file = new File(fileToOpen);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
    	int buffersize = 0;
    	
        InetAddress serverAddress = InetAddress.getByName(host);
        int portNumber = Integer.parseInt(port);
    	
        // UDP transmission is unreliable. Sender may overrun
        // receiver if sending too fast, giving packet lost as a result.
        // In that case, sender may need to pause sending once in a while.
        // E.g., Thread.sleep(1); // pause for 1 millisecond
    	
    	DatagramSocket clientSocket = new DatagramSocket();
    	
    	try {
			// header packet
			String header = serverAddress.toString() + rcvFileName;
			byte[] headerFile = header.getBytes();
			DatagramPacket  headerPkt = new DatagramPacket(headerFile, headerFile.length, serverAddress, portNumber);
			clientSocket.send(headerPkt);
			Thread.sleep(1);
			
			while ((buffersize=bis.read(buffer))>0) {
				DatagramPacket packet = new DatagramPacket(buffer, buffersize, serverAddress, portNumber);
				clientSocket.send(packet);
				Thread.sleep(1);
			}
			
			buffer = new byte[0];
			DatagramPacket emptyPkt = new DatagramPacket(buffer, buffer.length, serverAddress, portNumber);
			clientSocket.send(emptyPkt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  
    	clientSocket.close();
    	bis.close();
    }
}
