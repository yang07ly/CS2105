/*
 * CS2105 Assignment 1
 * Name: Lu Yang
 * Matriculation: A0130684H
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.io.*;

class FileReceiver {
	public DatagramSocket socket; 
	int port;

	public static void main(String[] args) {

		// check if the number of command line argument is 1
		if (args.length != 1) {
			System.out.println("Usage: java FileReceiver port");
			System.exit(1);
		}

		FileReceiver fileReceiver = new FileReceiver(args[0]);
		fileReceiver.process();
	}

	public FileReceiver(String localPort) {
		port = Integer.parseInt(localPort);
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void process() {
		byte[] buffer = new byte[1000];

		try {
			int pktNumber = 0;
			
			DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
			socket.receive(pkt);
			pkt = trimPktData(pkt);
			pkt = rdt2_0(pkt);
			String dest = extractDest(pkt);
			double sum =0;

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));
			while (pkt.getLength()!=0) {
				pkt = new DatagramPacket(buffer, buffer.length);
				socket.receive(pkt);
				if (pkt.getLength()==0) break;
				
				pkt = trimPktData(pkt);

				String reply ="";
				if (!isCorruptedPkt(pkt.getData())) reply = "ACK";
				else reply = "NAK";
				System.out.println("[DEBUG] reply: " + reply);
				pktNumber = extractPktNumber(pkt.getData());
				
				byte[] replyData = reply.getBytes();
				System.out.println("Checksum of reply: " + calculateChecksum(replyData));
				DatagramPacket replyPkt = new DatagramPacket(replyData, replyData.length, pkt.getAddress(), pkt.getPort());
				socket.send(replyPkt);

				if (reply.equals("ACK")) {
					byte[] data = Arrays.copyOfRange(pkt.getData(), 4, pkt.getLength());
					bos.write(data, 0, data.length);
					sum += data.length;
					System.out.println("-------------------- " + sum + " bytes received.");
				}
			}
			bos.close();

			socket.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public DatagramPacket rdt2_0(DatagramPacket packet) {
		InetAddress clientAddress = packet.getAddress();
		String reply = "";
		try {
			do {
				if (!isCorruptedPkt(packet.getData())) {
					reply = "ACK";
				}
				else reply = "NAK";
				System.out.println("[DEBUG] reply: " + reply);
				byte[] replyData = reply.getBytes();
				System.out.println("Checksum of reply: " + calculateChecksum(replyData));
				DatagramPacket replyPkt = new DatagramPacket(replyData, replyData.length, clientAddress, packet.getPort());
				socket.send(replyPkt);
			} while (reply.equals("NAK"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return packet;
	}

	public boolean isCorruptedPkt(byte[] bytes) {
		int senderChecksum = extractChecksum(bytes);
		byte[] contents = Arrays.copyOfRange(bytes, 8, bytes.length);
		int contentChecksum = calculateChecksum(contents);
		System.out.println("[DEBUG] senderChecksum: " + senderChecksum);
		System.out.println("[DEBUG] contentChecksum: " + contentChecksum);
		if (senderChecksum==contentChecksum) return false;
		else return true;
	}

	public int calculateChecksum(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return (int) crc.getValue();
	}

	public String extractDestFileName(String string) {
		String[] strings = string.split("/");
		String destFileName = strings[1];
		destFileName = destFileName.substring(9);
		return destFileName;
	}

	public int extractChecksum(byte[] bytes) {
		ByteBuffer wrapper = ByteBuffer.wrap(bytes, 0, 4);
		return wrapper.getInt();
	}
	
	public int extractPktNumber(byte[] bytes) {
		byte[] numberData = Arrays.copyOfRange(bytes, 4, 4);
		ByteBuffer wrapper = ByteBuffer.wrap(numberData, 0, 4);
		int number = wrapper.getInt();
		System.out.println("[DEBUG] pkt number: " + number);
		return number;
	}

	public String extractDest(DatagramPacket pkt) {
		byte[] headerData = Arrays.copyOfRange(pkt.getData(), 4, pkt.getData().length);
		String header = new String(headerData, 0, headerData.length);
		System.out.println("[DEBUG] header: " + header);
		String dest = extractDestFileName(header);
		System.out.println("[DEBUG] dest: " + dest);
		return dest;
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
