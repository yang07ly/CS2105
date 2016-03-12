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

	public FileSender() {
		try {
			clientSocket = new DatagramSocket();
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
			
			//testing
			//Header Packet contains dest filename
			String header = serverAddress.toString() + rcvFileName;
			byte[] headerData = header.getBytes();
			headerData = addPktNumber(headerData, 0);
			headerData = addChecksum(headerData);
			DatagramPacket headerPkt = new DatagramPacket(headerData, headerData.length, serverAddress, port);
			clientSocket.send(headerPkt);
			rdt2_0(headerPkt);
			
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToOpen));
			byte[] buffer = new byte[996];
			int buffersize = 0;
			int sum =0;
			while ((buffersize=bis.read(buffer, 0, 996))>0) {
				sum += buffersize;
				byte[] pktData = trimData(buffer, buffersize);
				pktData = addChecksum(pktData);
				DatagramPacket packet = new DatagramPacket(pktData, buffersize+4, serverAddress, port);
				clientSocket.send(packet);
				rdt2_0(packet);
			}
			System.out.println("[DEBUG] size of file sent: " + sum);
			bis.close();

			// create empty packet
			buffer = new byte[0];
			DatagramPacket emptyPkt = new DatagramPacket(buffer, buffer.length, serverAddress, port);
			clientSocket.send(emptyPkt);
			
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private byte[] trimData(byte[] bytes, int size) {
		byte[] arr = new byte[size];
		arr = Arrays.copyOfRange(bytes, 0, size);
		return arr;
	}
	
	private byte[] addPktNumber(byte[] bytes, int number) {
		byte[] numberBytes = ByteBuffer.allocate(4).putInt(number).array();
		byte[] pktData = combineBytes(numberBytes, bytes);
		return pktData;
	}
	
	public byte[] addChecksum(byte[] bytes) {
		int checksum = calculateChecksum(bytes);
		System.out.println("[DEBUG] checksum: " + checksum);
		byte[] checksumBytes = ByteBuffer.allocate(4).putInt(checksum).array();
		byte[] pktData = combineBytes(checksumBytes, bytes);
		return pktData;
	}

	public void rdt2_0(DatagramPacket pkt) {
		try {
			String reply = "";
			byte[] buffer = new byte[1000];
			DatagramPacket receivedPkt = new DatagramPacket(buffer, buffer.length);
			boolean resend = false;
			
			do {
				clientSocket.receive(receivedPkt);
				receivedPkt = trimPktData(receivedPkt);
				reply = new String(receivedPkt.getData(), 0, receivedPkt.getLength());
				System.out.println("[DEBUG] reply: " + reply);
				
				int replyChecksum = calculateChecksum(receivedPkt.getData());
				System.out.println("[DEBUG] replyChecksum: " + replyChecksum);
				if (replyChecksum!=NAK && replyChecksum!=ACK) {
					System.out.println("Corrupted reply message.");
					clientSocket.send(pkt);
					resend=true;
				}
				else if (reply.equals("NAK")) {
					resend = true;
					clientSocket.send(pkt);
				}
				else resend = false;
			} while (resend);
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	public int calculateChecksum(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return (int) crc.getValue();
	}

	public byte[] combineBytes(byte[] arr1, byte[] arr2) {
		byte[] combined = new byte[arr1.length + arr2.length];
		System.arraycopy(arr1, 0, combined, 0, arr1.length);
		System.arraycopy(arr2, 0, combined, arr1.length, arr2.length);
		return combined;
	}
	
	private DatagramPacket trimPktData(DatagramPacket pkt) {
		int size = pkt.getLength();
		byte[] arr1 = pkt.getData();
		byte[] arr2 = new byte[size];
		arr2 = Arrays.copyOfRange(arr1, 0, size);
		pkt.setData(arr2);
		return pkt;
	}
}
