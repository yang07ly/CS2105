/*
 * CS2105 Assignment 1
 * Name: Lu Yang
 * Matriculation: A0130684H
 */

import java.net.*;
import java.io.*;

class FileReceiver {
	public DatagramSocket socket; 
	public DatagramPacket pkt;

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
		int port = Integer.parseInt(localPort);
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void process() {
		byte[] buffer = new byte[1000];
		String dest=null;
		double sum=0;

		try {
			pkt = new DatagramPacket(buffer, buffer.length);
			socket.receive(pkt);
			String header = new String(pkt.getData(), 0, pkt.getLength());
			dest = extractDestFileName(header);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));

			while (pkt.getLength()!=0) {
				pkt = new DatagramPacket(buffer, buffer.length);
				socket.receive(pkt);
				bos.write(buffer, 0, pkt.getLength());
				sum += pkt.getLength();
			}
			
			bos.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public String extractDestFileName(String string) {
		String[] strings = string.split("/");
		String destFileName = strings[1];
		destFileName = destFileName.substring(9);
		return destFileName;
	}
}
