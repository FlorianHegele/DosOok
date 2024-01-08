public class BitConversion {

    // Fonction pour convertir un caractère en tableau de 8 bits
    public static byte[] charToBits(char character) {
        byte[] bits = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bits[i] = (byte) (character & 1);
            character >>= 1;
        }
        return bits;
    }

    // Fonction pour convertir un tableau de 8 bits en caractère
    public static char bitsToChar(byte[] bits) {
        char character = 0;
        for (int i = 0; i < 8; i++) {
            character = (char) ((character << 1) | bits[i]);
        }
        return character;
    }

    public static void main(String[] args) {
        char myChar = '!';
        byte[] bitsArray = charToBits(myChar);

        System.out.println("Caractère: " + myChar);
        System.out.print("Bits: ");
        for (int bit : bitsArray) {
            System.out.print(bit);
        }

        char convertedChar = bitsToChar(bitsArray);
        System.out.println("\nConverti en caractère: " + convertedChar);
    }
}
