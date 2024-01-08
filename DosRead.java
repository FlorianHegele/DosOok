import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/*

   NbrCanaux -> 1      (2 octets) : Nombre de canaux (de 1 à 6, cf. ci-dessous)
   BitsPerSample -> 16 bits  (2 octets) : Nombre de bits utilisés pour le codage de chaque échantillon (8, 16, 24)
   BytePerBloc -> 1 * 16 / 8 = 2 byte/bloc  (2 octets) : Nombre d'octets par bloc d'échantillonnage (c.-à-d., tous canaux confondus : NbrCanaux * BitsPerSample/8).
   BytePerSec -> 44 100 * 2 = 88 200 byte/sec    (4 octets) : Nombre d'octets à lire par seconde (c.-à-d., Frequence * BytePerBloc).
   Frequence -> 44 100 hz = 44.1 khZ      (4 octets) : Fréquence d'échantillonnage (en hertz)

 */

public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1, 0, 1, 0, 1, 0, 1, 0};
    FileInputStream fileInputStream;
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;

    /**
     * Helper method to convert a little-endian byte array to an integer
     *
     * @param bytes  the byte array to convert
     * @param offset the offset in the byte array
     * @param fmt    the format of the integer (16 or 32 bits)
     * @return the integer value
     */
    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {
        if (fmt == 16)
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
        else if (fmt == 32)
            return ((bytes[offset + 3] & 0xFF) << 24) |
                    ((bytes[offset + 2] & 0xFF) << 16) |
                    ((bytes[offset + 1] & 0xFF) << 8) |
                    (bytes[offset] & 0xFF);
        else return (bytes[offset] & 0xFF);
    }

    /**
     * Print the elements of an array
     *
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
        for(char character : data) {
            System.out.print(character+" ");
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
        StdDraw.enableDoubleBuffering();
        StdDraw.setTitle(title);

        final double xPadding = 1.01;
        final double yPadding = 1.5;

        StdDraw.setCanvasSize(1280, 700);
        StdDraw.setYscale(ArrayUtil.min(sig) * yPadding, ArrayUtil.max(sig) * yPadding);
        StdDraw.setXscale(Math.min(0, -(sig.length * xPadding - sig.length)), sig.length * xPadding);

        if(mode.equals("line")) {
            for(int i=1; i<sig.length; i++) {
                final int beforePoint = i-1;
                StdDraw.line(beforePoint, sig[beforePoint], i, sig[i]);
            }
        }

        StdDraw.show();
    }

    /**
     * Un exemple de main qui doit pourvoir être exécuté avec les méthodes
     * que vous aurez conçues.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead <input_wav_file>");
            return;
        }

        final String wavFilePath = args[0];

        // Open the WAV file and read its header
        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);

        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        // Read the audio data
        dosRead.readAudioDouble();

        // reverse the negative values
        dosRead.audioRectifier();

        // apply a low pass filter
        dosRead.audioLPFilter(dosRead.sampleRate / FP); // 44
        //dosRead.audio = new LPFilter1().lpFilter(dosRead.audio, dosRead.sampleRate, 10000);
        displaySig(dosRead.audio, 0, dosRead.audio.length - 1, "line", "Signal audio");


        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate / BAUDS, 9000);
        System.out.println(Arrays.toString(dosRead.outputBits));


        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null) {
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor that opens the FIlEInputStream
     * and reads sampleRate, bitsPerSample and dataSize
     * from the header of the wav file
     *
     * @param path the path of the wav file to read
     */
    public void readWavHeader(String path) {
        byte[] header = new byte[44]; // The header is 44 bytes long
        try {
            fileInputStream = new FileInputStream(path);
            fileInputStream.read(header);

            this.sampleRate = byteArrayToInt(header, 24, 32); // Frequence  (4 octets) : Fréquence d'échantillonnage
            this.bitsPerSample = byteArrayToInt(header, 34, 16); // BytePerBloc  (2 octets) : Nombre d'octets par bloc d'échantillonnage
            this.dataSize = byteArrayToInt(header, 40, 32); // DataSize  (4 octets) : Nombre d'octets des données -> taille_du_fichier - taille_de_l'entête
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
    public void readAudioDouble() {
        final int bytesPerSample = bitsPerSample / 8;
        byte[] audioData = new byte[dataSize];
        audio = new double[dataSize / bytesPerSample];

        try {
            fileInputStream.read(audioData);

            // Assuming 16 bits per sample
            for (int i = 0; i < audio.length; i++) {
                final int offset = i * bytesPerSample;
                audio[i] = (short) byteArrayToInt(audioData, offset, bitsPerSample);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier() {
        for (int i = 0; i < audio.length; i++)
            if (audio[i] < 0) audio[i] = -audio[i];
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n) * FECH
     *
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
        double[] filteredAudio = new double[audio.length];

        for (int i = 0; i < audio.length; i++) {
            int start = Math.max(0, i - n / 2);
            int end = Math.min(audio.length, i + n / 2);
            int count = end - start;

            double sum = Arrays.stream(Arrays.copyOfRange(audio, start, end)).sum();
            filteredAudio[i] = sum / count;
        }

        audio = filteredAudio;
    }

    /**
     * Resample the audio array and apply a threshold
     *
     * @param period    the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold) {
        int numSymbols = audio.length / period;
        outputBits = new int[numSymbols];

        for (int i = 0; i < numSymbols; i++) {
            double sum = 0;
            for (int j = 0; j < period; j++) {
                sum += audio[i * period + j];
            }
            double average = sum / period;

            // Apply threshold to determine the bit value
            outputBits[i] = (average > threshold) ? 1 : 0;
        }
    }

    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar() {
        if (outputBits == null || outputBits.length == 0) {
            System.out.println("No bits to decode.");
            return;
        }

        // Find the index where the START_SEQ pattern begins
        int startSeqIndex = findStartSequenceIndex();

        if (startSeqIndex == -1) {
            System.out.println("Start sequence not found in the outputBits array.");
            return;
        }

        // Calculate the number of bits remaining after the start sequence
        int remainingBits = outputBits.length - startSeqIndex;

        System.out.println(outputBits.length);
        System.out.println(remainingBits);

        // Calculate the number of characters based on the remaining bits
        int numChars = Math.ceilDiv(remainingBits, 8);

        // Initialize the decodedChars array
        decodedChars = new char[numChars];

        // Fill the decodedChars array by converting each group of 8 bits to a char
        for (int i = 0; i < numChars; i++) {
            int startIndex = startSeqIndex + i * 8;

            // Extract 8 bits and convert them to a char
            char decodedChar = bitsToChar(Arrays.copyOfRange(outputBits, startIndex, startIndex + 8));

            // Store the decoded char in the array
            decodedChars[i] = decodedChar;
        }
    }

    /**
     * Find the index where the START_SEQ pattern begins in the outputBits array.
     *
     * @return the index of the start sequence, or -1 if not found.
     */
    private int findStartSequenceIndex() {
        int limit = outputBits.length - START_SEQ.length + 1;

        outerLoop:
        for (int i = 0; i < limit; i++) {
            int endIndex = i + START_SEQ.length;

            for (int j = i, k = 0; j < endIndex; j++, k++) {
                if (outputBits[j] != START_SEQ[k]) {
                    continue outerLoop;
                }
            }

            return endIndex;
        }

        return -1;
    }

    /**
     * Convert an array of 8 bits to a char.
     *
     * @param bits the array of 8 bits.
     * @return the corresponding char.
     */
    private char bitsToChar(int[] bits) {
        if (bits.length != 8) {
            throw new IllegalArgumentException("Input array must have exactly 8 bits.");
        }

        int result = 0;
        for (int i = 0; i < 8; i++) {
            result |= bits[i] << i;  // Reverse the bit sequence by swapping the shift positions
        }

        return (char) result;
    }
}