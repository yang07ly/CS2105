import java.util.*;

public class IPAddress {

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine();
		String output = "";
		
		for (int i=0; i<4; i++) {
			int decimal = Integer.parseInt(input.substring(0,8), 2);
			output += decimal;
			if (i!=3) output+=".";
			input = input.substring(8);
		}
		System.out.println(output);
	}

}
