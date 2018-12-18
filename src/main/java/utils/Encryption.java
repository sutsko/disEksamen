package utils;

public final class Encryption {

  public static String encryptDecryptXOR(String rawString) {

    // If encryption is enabled in Config.
    if (Config.getEncryption()) {

      // The key is predefined and hidden in code
      // TODO: Create a more complex code and store it somewhere better FIX
      //THIS key is used for encryption.
      char[] key = {'C', 'B', 'S'};

      char[] keyyy = Config.getXorKey();

      // Stringbuilder enables you to play around with strings and make useful stuff
      StringBuilder thisIsEncrypted = new StringBuilder();

      // TODO: This is where the magic of XOR is happening. Are you able to explain what is going on? FIX
      /**
       * For the encryption, you need to first have a string to encrypt. This is our rawstring that is parsed via different
          functions in our Endpoints.
       * The stringbuilder allows to add a new value to the variable, which will then be a new char based on the XOR-operation of two other chars
          found in the rawstring and the key we have set somewhere else. Let's look at an example. Say the "rawString.charAt(i)" = a and "keyyy[i % keyyy.length]" = b.
       * The binary value of the above is 0110 0001 and 0110 0010. Doing the XOR operation on these will give the binary value 00000011 which is = 3, when converted back.
       * The 3 is now our new value stored in the thisIsEncrypted variable for the rawString.charAt(i). This is then done for all characters as iterations in rawString.
       * The use of modulo ensures that even though our "i" surpassed the amount of characters in our key, we will still get a value from it to be XOR'ed with.
      **/
      for (int i = 0; i < rawString.length(); i++) {
        thisIsEncrypted.append((char) (rawString.charAt(i) ^ keyyy[i % keyyy.length]));

      }

      // We return the encrypted string
      return thisIsEncrypted.toString();

    } else {
      // We return without having done anything
      return rawString;
    }
  }
}
