package quclient.Config;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class PasswordUtils {

    /**
     * Returns true is the input password is a valid access/super password.
     * @param password
     * @return is valid password
     */
    public static boolean isValidPassword(String password) {
        // Passwords must be 4 characters.
        if(password.length() != 4) {
            return false;
        }
        // Check that every character is either a letter or digit.
        for(int i = 0; i < 4; i++) {
            if(!(Character.isLetter(password.charAt(i)) || Character.isDigit(password.charAt(i)))) {
                return false;
            }
        }
        // Password is ok.
        return true;
    }
}
