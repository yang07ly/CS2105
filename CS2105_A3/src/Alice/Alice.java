package Alice;
// Author: Lu Yang

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

/**********************************************************************
 * This skeleton program is prepared for weak and average students.  *
 *                                                                   *
 * If you are very strong in programming, DIY!                       *
 *                                                                   *
 * Feel free to modify this program.                                 *
 *********************************************************************/

// Alice knows Bob's public key
// Alice sends Bob session (AES) key
// Alice receives messages from Bob, decrypts and saves them to file

class Alice {  // Alice is a TCP client

	private ObjectOutputStream toBob;   // to send session key to Bob
	private ObjectInputStream fromBob;  // to read encrypted messages from Bob
	private Crypto crypto;        // object for encryption and decryption
	public static final String MESSAGE_FILE = "msgs.txt"; // file to store messages

	public static void main(String[] args) {

		// Check if the number of command line argument is 2
		if (args.length != 2) {
			System.err.println("Usage: java Alice BobIP BobPort");
			System.exit(1);
		}

		new Alice(args[0], args[1]);
	}

	// Constructor
	public Alice(String ipStr, String portStr) {

		this.crypto = new Crypto();

		// Send session key to Bob
		sendSessionKey(ipStr, portStr);

		// Receive encrypted messages from Bob,
		// decrypt and save them to file
		receiveMessages(portStr);
	}

	// Send session key to Bob
	public void sendSessionKey(String ip, String portStr) {
		int port = Integer.parseInt(portStr);
		Socket client;

		try {
			client = new Socket(ip, port);
			toBob = new ObjectOutputStream(client.getOutputStream());
			toBob.writeObject(this.crypto.getSessionKey());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Receive messages one by one from Bob, decrypt and write to file
	public void receiveMessages(String portStr) {
		int port = Integer.parseInt(portStr);
		
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(MESSAGE_FILE, true)));
			ServerSocket server = new ServerSocket(port);
			
			while (true) {
				Socket connection = server.accept();
				fromBob = new ObjectInputStream(connection.getInputStream());
				SealedObject encryptedMsgObj = (SealedObject) fromBob.readObject();
				String text = crypto.decryptMsg(encryptedMsgObj);
				pw.println(text);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// How to detect Bob has no more data to send?

	}

	/*****************/
	/** inner class **/
	/*****************/
	class Crypto {

		// Bob's public key, to be read from file
		private PublicKey pubKey;
		// Alice generates a new session key for each communication session
		private SecretKey sessionKey;
		// File that contains Bob' public key
		public static final String PUBLIC_KEY_FILE = "public.key";

		// Constructor
		public Crypto() {
			// Read Bob's public key from file
			readPublicKey();
			// Generate session key dynamically
			initSessionKey();
		}

		// Read Bob's public key from file
		public void readPublicKey() {
			// key is stored as an object and need to be read using ObjectInputStream.
			// See how Bob read his private key as an example.
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
				this.pubKey = (PublicKey)ois.readObject();
				ois.close();
			} catch (IOException oie) {
				System.out.println("Error reading public key from file");
				System.exit(1);
			} catch (ClassNotFoundException cnfe) {
				System.out.println("Error: cannot typecast to class PublicKey");
				System.exit(1);            
			}
			System.out.println("Public key read from file " + PUBLIC_KEY_FILE);
		}

		// Generate a session key
		public void initSessionKey() {
			// suggested AES key length is 128 bits
			try {
				KeyGenerator generator = KeyGenerator.getInstance("AES");
				this.sessionKey = generator.generateKey();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}

		// Seal session key with RSA public key in a SealedObject and return
		public SealedObject getSessionKey() {
			SealedObject sealed = null;
			Cipher cipher;
			// Alice must use the same RSA key/transformation as Bob specified
			try {
				cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, this.pubKey);
				sealed = new SealedObject(sessionKey.getEncoded(), cipher);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// RSA imposes size restriction on the object being encrypted (117 bytes).
			// Instead of sealing a Key object which is way over the size restriction,
			// we shall encrypt AES key in its byte format (using getEncoded() method).
			return sealed;
		}

		// Decrypt and extract a message from SealedObject
		public String decryptMsg(SealedObject encryptedMsgObj) {
			String plainText = null;
			// Alice and Bob use the same AES key/transformation
			Cipher cipher;
			try {
				cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
				cipher.init(Cipher.DECRYPT_MODE, this.pubKey);
				plainText = (String) encryptedMsgObj.getObject(cipher);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return plainText;
		}
	}
}