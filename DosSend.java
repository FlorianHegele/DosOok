import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.io.IOException;

public class DosSend {

    static final int FECH = 44100; // fréquence d'échantillonnage
    static final int FP = 1000;    // fréquence de la porteuses
    static final int BAUDS = 100;  // débit en symboles par seconde
    static final int FMT = 16;    // format des données
    static final int MAX_AMP = (1 << (FMT - 1)) - 1; // amplitude max en entier
    static final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    static final int[] START_SEQ = {1, 0, 1, 0, 1, 0, 1, 0}; // séquence de synchro au début
    final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille;                // nombre d'octets de données à transmettre
    double duree;              // durée de l'audio
    double[] dataMod;           // données modulées
    char[] dataChar;            // données en char
    FileOutputStream outStream; // flux de sortie pour le fichier .wav

    /**
     * Constructor
     *
     * @param path the path of the wav file to create
     */
    public DosSend(String path) {
        final File file = new File(path);
        try {
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.err.println("Erreur de création du fichier");
        }
    }

    /**
     * Display a signal in a window
     *
     * @param sig   the signal to display
     * @param start the first sample to display
     * @param stop  the last sample to display
     * @param mode  "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        // Ensure start and stop values are within bounds
        start = Math.max(0, start);
        stop = Math.min(stop, sig.length);

        if(start > stop) {
            start = 0;
            stop = sig.length;
        }

        // Enable double buffering for faster graphics rendering
        StdDraw.enableDoubleBuffering();

        StdDraw.setTitle(title);

        StdDraw.setCanvasSize(1280, 700);

        // Constants for adjusting the display
        final double yPadding = 1.5;

        // Set the y-scale based on the signal's minimum and maximum values
        StdDraw.setYscale(ArrayUtil.min(sig) * yPadding, ArrayUtil.max(sig) * yPadding);

        // Set the x-scale based on the length of the signal
        StdDraw.setXscale(start, stop);

        // Handle the selected display mode
        if (mode.equals("line")) {
            // Draw a line between consecutive points for the "line" mode
            for (int i = start + 1; i < stop; i++) {
                final int beforePoint = i - 1;
                StdDraw.line(beforePoint, sig[beforePoint], i, sig[i]);
            }
        } else if (mode.equals("point")) {
            // Draw individual points for the "point" mode
            for (int i = start; i < stop; i++) {
                StdDraw.point(i, sig[i]);
            }
        } else {
            // Handle invalid display modes
            System.err.println("Mode can only be 'line' or 'point'");
        }

        StdDraw.show();
    }


    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title) {
        // Enable double buffering for faster graphics rendering
        StdDraw.enableDoubleBuffering();

        StdDraw.setTitle(title);
        StdDraw.setCanvasSize(1280, 700);

        // Constants for adjusting the display
        final double xPadding = 1.01;
        final double yPadding = 1.5;

        // Calculate maxSize once
        final int maxSize = ArrayUtil.maxSize(listOfSigs);

        // Calculate min and max values once
        final double minValue = ArrayUtil.min(listOfSigs) * yPadding;
        final double maxValue = ArrayUtil.max(listOfSigs) * yPadding;

        // Set the y-scale based on the minimum and maximum values among all signals
        StdDraw.setYscale(minValue, maxValue);

        // Set the x-scale based on the maximum size of signals
        StdDraw.setXscale(Math.min(0, -(maxSize * xPadding - maxSize)), maxSize * xPadding);

        // Ensure start value is within bounds
        if (start < 0)
            start = 0;

        // Adjust stop value if it exceeds the signal length
        stop = Math.min(stop, maxSize);

        // Handle the selected display mode
        for (double[] sig : listOfSigs) {
            int tmpStart = Math.min(start, sig.length - 1);
            int tmpStop = Math.min(stop, sig.length);

            // Reset start and stop values if they are invalid
            if (tmpStart >= tmpStop) {
                tmpStart = 0;
                tmpStop = sig.length;
            }

            if (mode.equals("line")) {
                // Draw a line between consecutive points for the "line" mode
                for (int i = tmpStart + 1; i < tmpStop; i++) {
                    final int beforePoint = i - 1;
                    StdDraw.line(beforePoint, sig[beforePoint], i, sig[i]);
                }
            } else if (mode.equals("point")) {
                // Draw individual points for the "point" mode
                for (int i = tmpStart; i < tmpStop; i++) {
                    StdDraw.point(i, sig[i]);
                }
            } else {
                // Handle invalid display modes
                System.err.println("Mode can only be 'line' or 'point'");
            }
        }

        StdDraw.show();
    }


    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");

        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (dosSend.readTextData() + START_SEQ.length / 8.0) * 8.0 / BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();

        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : " + String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : " + dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : " + dosSend.dataMod.length);
        System.out.println("\tDurée : " + dosSend.duree + " s");
        System.out.println();

        // exemple d'affichage du signal modulé dans une fenêtre graphique
        displaySig(Arrays.asList(dosSend.dataMod, dosSend.dataMod), 0, dosSend.dataMod.length, "line", "Signal modulé");
    }

    /**
     * Write a raw 4-byte integer in little endian
     *
     * @param octets     the integer to write
     * @param destStream the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream) {
        char poidsFaible;
        while (taille > 0) {
            poidsFaible = (char) (octets & 0xFF);
            try {
                destStream.write(poidsFaible);
            } catch (Exception e) {
                System.out.println("Erreur d'écriture");
            }
            octets = octets >> 8;
            taille--;
        }
    }

    /**
     * Create and write the header of a wav file
     */
    public void writeWavHeader() {
        // Verify the validity of audio parameters
        if (CHANNELS <= 0 || FECH <= 0 || FMT <= 0 || duree <= 0) {
            System.out.println("Error: Invalid parameters for WAV header.");
            return;
        }

        // Calculate the total size of audio data in bytes
        taille = (long) (FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;

        try {
            outStream.write(new byte[] { 'R', 'I', 'F', 'F' });
            writeLittleEndian((int) (nbBytes + 36), 4, outStream);
            outStream.write(new byte[] { 'W', 'A', 'V', 'E' });
            outStream.write(new byte[] { 'f', 'm', 't', ' ' });
            writeLittleEndian(16, 4, outStream);
            writeLittleEndian(1, 2, outStream);
            writeLittleEndian(CHANNELS, 2, outStream);
            writeLittleEndian(FECH, 4, outStream);
            writeLittleEndian((FECH * FMT * CHANNELS) / 8, 4, outStream);
            writeLittleEndian((FMT * CHANNELS) / 8, 2, outStream);
            writeLittleEndian(FMT, 2, outStream);
            outStream.write(new byte[] { 'd', 'a', 't', 'a' });
            writeLittleEndian((int) nbBytes, 4, outStream);
        } catch (IOException e) {
            System.err.println("Error writing WAV header: " + e);
        }
    }

    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits signed)
     */
    public void writeNormalizeWavData() {
        try {
            // Loop through each element in the dataMod array
            for (double element : dataMod) {
                // Normalize the amplitude of the element to the maximum value of the format (8 bits signed)
                int normalizedElement = (int) (MAX_AMP * element);

                // Write the normalized element to the WAV file
                writeLittleEndian(normalizedElement, FMT / 8, outStream);
            }
        } catch (Exception e) {
            // Handle any potential writing errors
            System.err.println("Error writing data");
        }
    }
    /**
     * Read the text data to encode and store them into dataChar
     *
     * @return the number of characters read
     */
    public int readTextData() {
        final StringBuilder message = new StringBuilder();

        // Check if there is another line of input
        while (input.hasNextLine()) {
            // Read the next line of input and append it to the StringBuilder
            message.append(input.nextLine());

            // Check if there is another line after the current one
            if (input.hasNextLine())
                message.append('\n'); // Add a newline character between lines

        }

        // Convert the StringBuilder content to a character array
        dataChar = message.toString().toCharArray();

        // Return the total number of characters read
        return dataChar.length;
    }


    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     *
     * @param bits the data to modulate
     */
    public void modulateData(byte[] bits) {
        // Define constants for modulation parameters
        final int samplesPerSymbol = (FECH / BAUDS); // Number of samples per symbol
        final int syncSeqSize = START_SEQ.length * samplesPerSymbol; // Size of the synchronization sequence
        final double angularFrequency = 2 * Math.PI * FP / FECH; // Angular frequency for modulation

        // Initialize the array to store the modulated data
        dataMod = new double[syncSeqSize + bits.length * samplesPerSymbol];

        // Add synchronization sequence at the beginning
        for (int i = 0; i < START_SEQ.length; i++) {
            for (int j = 0; j < samplesPerSymbol; j++)
                // Modulate each sample based on the synchronization sequence
                dataMod[i * samplesPerSymbol + j] = START_SEQ[i] == 1
                        ? Math.sin(angularFrequency * j)
                        : 0;
        }

        // Modulate the input data
        for (int i = 0; i < bits.length; i++) {
            for (int j = 0; j < samplesPerSymbol; j++)
                // Modulate each sample based on the input data
                dataMod[syncSeqSize + i * samplesPerSymbol + j] = bits[i] == 1
                        ? Math.sin(angularFrequency * (j + START_SEQ.length * samplesPerSymbol))
                        : 0;
        }
    }

    /**
     * convert a char array to a bit array
     *
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars) {
        if (chars == null) {
            System.err.println("Error: The character array is null.");
            return new byte[0];
        }

        // Initialize the byte array to store the bits
        final byte[] bits = new byte[chars.length * 8];

        // Loop through each character in the array
        for (int i = 0; i < chars.length; i++) {
            // Convert each character to a byte array representing its bits
            final byte[] data = charToBits(chars[i]);

            // Copy the converted bits into the final array
            System.arraycopy(data, 0, bits, i * 8, data.length);
        }

        // Return the byte array containing the bits of the characters
        return bits;
    }

    /**
     * Converts a character to a byte array representing its bits.
     *
     * @param character the character to convert to bits
     * @return byte array representing the bits of the character
     */
    private byte[] charToBits(char character) {
        // Initialize the byte array to store the bits of the character
        final byte[] bits = new byte[8];

        // Loop through each bit of the character (from right to left)
        for (int i = 0; i < 8; i++) {
            // Copy the least significant bit of the character into the byte array
            // Example -> 00100001
            //          & 00000001
            //            --------
            //            00000001 -> 1
            bits[i] = (byte) (character & 1);

            // Shift the character one bit to the right to process the next bit
            character >>= 1;
        }

        // Return the byte array representing the bits of the character
        return bits;
    }

}