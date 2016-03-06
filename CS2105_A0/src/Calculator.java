
public class Calculator {

	public static void main(String[] args) {
		int result = 0;
		
		for (String element: args) {
			System.out.println(element);
		}
		
		try {
			int operand1 = Integer.parseInt(args[0]);
			String operator = args[1];
			if (!operator.equals("+") && 
					!operator.equals("-") &&
					!operator.equals("'*'") &&
					!operator.equals("/") )
				throw new IllegalArgumentException();
			int operand2 = Integer.parseInt(args[2]);
			result = compute(operand1, operator, operand2);
			System.out.println(args[0] + " " + args[1] + " "
					+ args[2] + " = " + result);
		}
		catch (NumberFormatException e) {
			System.out.println(e);
			System.out.println("Error in expression");
		}
		catch (IllegalArgumentException e) {
			System.out.println(e);
			System.out.println("Error in expression");
		}
		catch (ArithmeticException e) {
			System.out.println(e);
			System.out.println("Error in expression");
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println(e);
			System.out.println("Error in expression");
		}
	}
		
	
	public static int compute(int operand1, String operator, int operand2) throws ArithmeticException {
		if (operator.equals("+")) return operand1 + operand2;
		else if (operator.equals("-")) return operand1 - operand2;
		else if (operator.equals("'*'")) return operand1 * operand2;
		else return operand1 / operand2;
	}
}