
public class PanValidatorTest {
	
	static void check(String input,boolean expected) {
		boolean actual = PanValidator.isValidPan(input);
        System.out.println((actual == expected ? "PASS" : "FAIL")
            + " | " + input + " | expected " + expected + ", got " + actual);
	}
	
	public static void main(String args[]) {
		check("ABCDE1234F", true);   // valid
        check("abcde1234f", false);  // lowercase
        check("ABCDE12345", false);  // last character is a digit
        check("aBCDE1234F", false);
        check("44444ABCD5", false);
        check(null, false);
        check("", false);
        check("ABCDE123F", false);     // too short
        check("ABCDE12345F", false);   // too long
        check(" ABCDE1234F", false);   // space at the start
        check("ZZZZZ9999Z", true);     // another valid one
        check("ABCD#1234F", false);    // special character
        check("ABCDE1234F ", false);   // space at the end
	}
}
