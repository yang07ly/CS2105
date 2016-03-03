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
			DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
			socket.receive(pkt);
			pkt = trimPktData(pkt);


			// rdt2.0 packet corruption
			pkt = rdt2_0(pkt);
			String dest = extractDest(pkt);


			/*
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));

			while (pkt.getLength()!=0) {
				pkt = new DatagramPacket(buffer, buffer.length);
				socket.receive(pkt);
				bos.write(buffer, 0, pkt.getLength());
				sum += pkt.getLength();
			}

			bos.close();
			 */
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
				DatagramPacket replyPkt = new DatagramPacket(replyData, replyData.length, clientAddress, packet.getPort());
				socket.send(replyPkt);
				if (reply.equals("NAK")) socket.receive(packet);
			} while (reply.equals("NAK"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return packet;
	}

	public boolean isCorruptedPkt(byte[] bytes) {
		int senderChecksum = extractChecksum(bytes);
		byte[] contents = Arrays.copyOfRange(bytes, 4, bytes.length);
		String header = new String(contents, 0, contents.length);
		System.out.println("[DEBUG] length of contents: " + contents.length);
		System.out.println("[DEBUG] header: " + header);
		int contentChecksum = calculateChecksum(contents);
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
