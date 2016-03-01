import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class FileSender {
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
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToOpen));
			byte[] buffer = new byte[1000];
			int buffersize = 0;

			InetAddress serverAddress = InetAddress.getByName(host);
			port = Integer.parseInt(portString);

			// header packet
			// rdt2.0
			String header = serverAddress.toString() + rcvFileName;
			byte[] headerData = header.getBytes();
			int checksum = calculateChecksum(headerData);
			System.out.println("[DEBUG] checksum: " + checksum);
			byte[] pktBytes = combineBytes(convertInttoBytes(checksum), headerData);
			DatagramPacket headerPkt = new DatagramPacket(pktBytes, pktBytes.length, serverAddress, port);
			clientSocket.send(headerPkt);

			rdt2_0(headerPkt);


			/*while ((buffersize=bis.read(buffer))>0) {
				DatagramPacket packet = new DatagramPacket(buffer, buffersize, serverAddress, portNumber);
				clientSocket.send(packet);
				Thread.sleep(1);
			}

			// create empty packet
			buffer = new byte[0];
			DatagramPacket emptyPkt = new DatagramPacket(buffer, buffer.length, serverAddress, portNumber);
			clientSocket.send(emptyPkt);*/

			clientSocket.close();
			bis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void rdt2_0(DatagramPacket pkt) {
		try {
			String reply = "";
			byte[] buffer = new byte[1000];
			DatagramPacket receivedPkt = new DatagramPacket(buffer, buffer.length);
				clientSocket.receive(receivedPkt);

				reply = new String(receivedPkt.getData(), 0, receivedPkt.getLength());
				System.out.println("[DEBUG] reply: " + reply);

				if (reply.equals("NAK")) {
					clientSocket.send(pkt);
				}
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	public int calculateChecksum(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return (int) crc.getValue();
	}

	public byte[] convertInttoBytes(int integer) {
		byte[] checksumBytes = ByteBuffer.allocate(4).putInt(integer).array();
		return checksumBytes;
	}

	public byte[] combineBytes(byte[] arr1, byte[] arr2) {
		byte[] combined = new byte[arr1.length + arr2.length];

		System.arraycopy(arr1, 0, combined, 0, arr1.length);
		System.arraycopy(arr2, 0, combined, arr1.length, arr2.length);
		return combined;
	}
}
