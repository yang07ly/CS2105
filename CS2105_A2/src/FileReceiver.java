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
	int sequence;

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
		sequence=-1;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void process() {
		byte[] buffer = new byte[1000];

		try {
			DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
			
			socket.receive(pkt);
			byte[] headerData = rdt(pkt); // data is reliable now
			String dest = extractDest(headerData);
			System.out.println(dest);
			
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));
			System.out.println("Receiver starts receiving file data");
			double sum = 0;
			while (pkt.getLength()!=0) {
				socket.receive(pkt);
				if (pkt.getLength()==0) break;
				
				byte[] data = rdt(pkt);
				int current = extractPktNumber(data);
				if (current!=sequence) {
					sequence--;
					continue;
				}
				data = Arrays.copyOfRange(data, 8, data.length);
				bos.write(data, 0, data.length);
				sum += data.length;
				System.out.println("---------" + data.length + " bytes received. Sum is " + sum + "--------");
			}
			bos.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// this method gurantees the correctness of the packet returned
	public byte[] rdt(DatagramPacket pkt) {
		InetAddress client = pkt.getAddress();
		byte[] data = trimPktData(pkt);
		String reply = "";
		int breakCount=0; //DEBUGGING
		
		try {
			while (isCorrupted(data)) {
				System.out.println("Packet" + sequence + " corrupted. :(");
				System.out.println("Waiting for packet" + sequence + "......");
				reply = "ACK" + sequence;
				byte[] replyData = reply.getBytes();
				DatagramPacket replyPkt = new DatagramPacket(replyData, replyData.length, client, pkt.getPort());
				socket.send(replyPkt);
				socket.receive(pkt);
				data = trimPktData(pkt);
				
				breakCount++; //DEBUGGING
			}
			
			sequence++;
			System.out.println("Packet" + sequence + " received successfully. :)");
			reply = "ACK" + sequence;
			byte[] replyData = reply.getBytes();
			DatagramPacket replyPkt = new DatagramPacket(replyData, replyData.length, client, pkt.getPort());
			socket.send(replyPkt);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	private boolean isRepeated(byte[] bytes) {
		int current = extractPktNumber(bytes);
		System.out.println("isRepeated/sequence: " +  sequence);
		if (current == (sequence-1)) return true;
		else return false;
	}
	
	private boolean isCorrupted(byte[] bytes) {
		int senderChecksum = extractChecksum(bytes);
		byte[] contents = Arrays.copyOfRange(bytes, 4, bytes.length);
		int contentChecksum = calculateChecksum(contents);
		System.out.println("[DEBUG] senderChecksum: " + senderChecksum);
		System.out.println("[DEBUG] contentChecksum: " + contentChecksum);
		if (senderChecksum==contentChecksum) return false;
		else return true;
	}
	
	private int extractPktNumber(byte[] bytes) {
		ByteBuffer wrapper = ByteBuffer.wrap(bytes, 4, 8);
		return wrapper.getInt();
	}
	
	private int extractChecksum(byte[] bytes) {
		ByteBuffer wrapper = ByteBuffer.wrap(bytes, 0, 4);
		return wrapper.getInt();
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
	
	private String extractDest(byte[] headerData) {
		headerData = Arrays.copyOfRange(headerData, 8, headerData.length);
		String header = new String(headerData, 0, headerData.length);
		System.out.println("[DEBUG] header: " + header);
		String dest = extractDestFileName(header);
		System.out.println("[DEBUG] dest: " + dest);
		return dest;
	}
	
	private String extractDestFileName(String string) {
		String[] strings = string.split("/");
		String destFileName = strings[1];
		destFileName = destFileName.substring(9);
		return destFileName;
	}
}
