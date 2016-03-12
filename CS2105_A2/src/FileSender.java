import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class FileSender {
	private static final int NAK = -1926652028;
	private static final int ACK = -1270629829;
	
	DatagramSocket clientSocket;
	int port;
	int ACK1;
	int sequence;

	public FileSender() {
		try {
			clientSocket = new DatagramSocket();
			sequence=0;
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		if (args.length != 4) {
			System.out.println("Usage: java FileSender <path/filename> "
					+ "<rcvHostName> <rcvPort> <rcvFileName>");
			System.exit(1);
		}

		FileSender fileSender = new FileSender();
		fileSender.process(args[0], args[1], args[2], args[3]);
	}

	public void process(String fileToOpen, String host, String portString, String rcvFileName) {
		try {
			InetAddress serverAddress = InetAddress.getByName(host);
			port = Integer.parseInt(portString);
			
			//Header Packet contains dest filename
			String header = serverAddress.toString() + rcvFileName;
			byte[] headerData = header.getBytes();
			headerData = addPktNumber(headerData, sequence);
			headerData = addChecksum(headerData);
			System.out.println("process/headerData: " + headerData.length);
			DatagramPacket headerPkt = new DatagramPacket(headerData, headerData.length, serverAddress, port);
			clientSocket.send(headerPkt);
			rdt(headerPkt);
			sequence++;
			
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToOpen));
			byte[] buffer = new byte[992];
			int buffersize=0;
			while ((buffersize=bis.read(buffer))>0) {
				// trim packet for last packet
				byte[] pktData = new byte[buffersize];
				pktData = Arrays.copyOfRange(buffer, 0, buffersize);
				
				pktData = addPktNumber(pktData, sequence);
				pktData = addChecksum(pktData);
				DatagramPacket pkt = new DatagramPacket(pktData, pktData.length, serverAddress, port);
				clientSocket.send(pkt);
				rdt(pkt);
				sequence++;
			}
			
			//send empty packet
			buffer = new byte[0];
			DatagramPacket emptyPkt = new DatagramPacket(buffer, buffer.length, serverAddress, port);
			clientSocket.send(emptyPkt);
			
			bis.close();
			clientSocket.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String rdt(DatagramPacket pkt) {
		try {
			byte[] buffer = new byte[1000];
			DatagramPacket receivedPkt = new DatagramPacket(buffer, buffer.length);
			
			while (true) {
				clientSocket.receive(receivedPkt);
				String receiverReply = new String(receivedPkt.getData(), 0, receivedPkt.getLength());
				System.out.println("Receiver replied: " + receiverReply);
				
				String expectedReply = "ACK" + sequence;
				if (receiverReply.equals(expectedReply)) {
					System.out.println("Packet" + sequence + " transmitted successfully");
					break;
				}
				System.out.println("Packet" + sequence + " corrupted.");
				System.out.println("Resending packet" + sequence + "......");
				clientSocket.send(pkt);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "Packet" + sequence + " sent";
	}
	
	private byte[] addPktNumber(byte[] data, int num) {
		byte[] numByte = ByteBuffer.allocate(4).putInt(num).array();
		byte[] pktWithNum = combine(numByte, data);
		return pktWithNum;
	}
	
	private byte[] addChecksum(byte[] data) {
		int checksum = calculateChecksum(data);
		System.out.println("Checksum: " + checksum);
		byte[] checksumByte = ByteBuffer.allocate(4).putInt(checksum).array();
		byte[] checksumedData = combine(checksumByte, data);
		return checksumedData;
		
	}
	
	private byte[] trimPktData(DatagramPacket pkt) {
		int actual = pkt.getLength();
		byte[] a = pkt.getData();
		byte[] b = new byte[actual];
		b = Arrays.copyOfRange(a, 0, actual);
		return b;
	}
	
	private int calculateChecksum(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return (int) crc.getValue();
	}
	
	private byte[] combine(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}
