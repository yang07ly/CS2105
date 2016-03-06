import java.io.*;

public class Copier {

	public static void main(String[] args) throws IOException {
		String src = args[0];
		String dest = args[1];
		
		byte[] buffer = new byte[1000];
		FileInputStream fis = new FileInputStream(src);
		FileOutputStream fos = new FileOutputStream(dest);
		BufferedInputStream bis = new BufferedInputStream(fis);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		long t1 = System.currentTimeMillis();
		int buffersize = 0;
		int sum =0;
		while ((buffersize=bis.read(buffer))>0) {
			sum += buffersize;
			bos.write(buffer,0,buffersize);
		}
		long t2 = System.currentTimeMillis();
		System.out.println("Sum: " + sum);
		System.out.println("Time " + ((double) (t2 - t1)/1000));
		System.out.println(src + " successfully copied to " + dest);
		
		bis.close();
		bos.close();
	}
}