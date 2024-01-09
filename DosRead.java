import java.io.FileInputStream;
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
        // Enable double buffering for faster graphics rendering
        StdDraw.enableDoubleBuffering();

        StdDraw.setTitle(title);

        StdDraw.setCanvasSize(1280, 700);

        // TODO : ADJUSTE XPADDING BY X LENGTH OF THE SIGNAL
        // Constants for adjusting the display
        final double xPadding = 1.01;
        final double yPadding = 1.5;

        // Set the y-scale based on the signal's minimum and maximum values
        StdDraw.setYscale(ArrayUtil.min(sig) * yPadding, ArrayUtil.max(sig) * yPadding);

        // Set the x-scale based on the length of the signal
        StdDraw.setXscale(Math.min(0, -(sig.length * xPadding - sig.length)), sig.length * xPadding);

        // Ensure start and stop values are within bounds
        if (start < 0) start = 0;
        if (stop > sig.length) stop = sig.length;

        if(start > stop) {
            start = 0;
            stop = sig.length;
        }

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
        final DosRead dosRead = new DosRead();
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

        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate / BAUDS, 9000);
        System.out.println(Arrays.toString(dosRead.outputBits));

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null) {
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        displaySig(dosRead.audio, 0, dosRead.audio.length - 1, "line", "Signal audio");

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
        // Create a byte array to store the header data (44 bytes)
        final byte[] header = new byte[44];

        try {
            fileInputStream = new FileInputStream(path);

            // Read the first 44 bytes (header) of the WAV file into the header array
            fileInputStream.read(header);

            // Extract sample rate information from the header (bytes 24-28)
            this.sampleRate = byteArrayToInt(header, 24, 32); // Frequence (4 octets)

            // Extract bits per sample information from the header (bytes 34-36)
            this.bitsPerSample = byteArrayToInt(header, 34, 16); // BitsPerSample (2 octets)

            // Extract data size information from the header (bytes 40-43)
            this.dataSize = byteArrayToInt(header, 40, 32); // DataSize (4 octets)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
    public void readAudioDouble() {
        // Calculate the number of bytes per sample based on bitsPerSample
        final int bytesPerSample = bitsPerSample / 8;

        // Create a new byte array to store the audio data
        final byte[] audioData = new byte[dataSize];

        // Create a new double array to store the converted audio data
        audio = new double[dataSize / bytesPerSample];

        try {
            // Read the audio data from the file into the audioData array
            fileInputStream.read(audioData);

            // Assuming 16 bits per sample
            for (int i = 0; i < audio.length; i++) {
                // Calculate the offset for the current sample in the audioData array
                final int offset = i * bytesPerSample;

                // Convert the bytes to a short (16 bits) and store it in the audio array
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
        for (int i = 0; i < audio.length; i++) {
            if (audio[i] < 0) audio[i] = -audio[i];
        }
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n) * FECH
     *
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
        final double[] filteredAudio = new double[audio.length];

        for (int i = 0; i < audio.length; i++) {
            // Calculate the start and end indices for the current sample
            final int start = Math.max(0, i - n / 2);
            final int end = Math.min(audio.length, i + n / 2);
            final int count = end - start;

            // Calculate the sum of audio samples within the specified range
            final double sum = Arrays.stream(Arrays.copyOfRange(audio, start, end)).sum();

            // Calculate the average value and store it in the filteredAudio array
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
        // Calculate the number of symbols based on the audio length and period
        final int numSymbols = audio.length / period;

        outputBits = new int[numSymbols];

        // Iterate over each symbol in the outputBits array
        for (int i = 0; i < numSymbols; i++) {
            double sum = 0;

            // Iterate over each sample within the current symbol
            for (int j = 0; j < period; j++) {
                // Accumulate the audio samples within the current symbol
                sum += audio[i * period + j];
            }

            // Calculate the average value of the audio samples within the symbol
            final double average = sum / period;

            // Apply the threshold to determine the bit value (0 or 1)
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
        final int startSeqIndex = findStartSequenceIndex();

        if (startSeqIndex == -1) {
            System.out.println("Start sequence of the message not found.");
            return;
        }

        // Calculate the remaining bits after the start sequence
        final int remainingBits = outputBits.length - startSeqIndex;

        // Calculate the number of chars needed to store the remaining bits
        final int numChars = (int) Math.ceil((double) remainingBits / 8);

        decodedChars = new char[numChars];

        for (int i = 0; i < numChars; i++) {
            // Calculate the start index for each char in the outputBits array
            final int startIndex = startSeqIndex + i * 8;

            // Extract 8 bits from the outputBits array for each char
            final char decodedChar = bitsToChar(Arrays.copyOfRange(outputBits, startIndex, startIndex + 8));

            decodedChars[i] = decodedChar;
        }
    }

    /**
     * Find the index where the START_SEQ pattern begins in the outputBits array.
     *
     * @return the index of the start sequence, or -1 if not found.
     */
    private int findStartSequenceIndex() {
        for (int i = 0; i < outputBits.length - START_SEQ.length + 1; i++) {
            if (Arrays.equals(Arrays.copyOfRange(outputBits, i, i + START_SEQ.length), START_SEQ))
                return i + START_SEQ.length;
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
        if (bits.length != 8)
            throw new IllegalArgumentException("Input array must have exactly 8 bits.");

        int result = 0;

        // Iterate over each bit in the array
        for (int i = 0; i < 8; i++) {
            // Use bitwise OR operation to build the integer value bit by bit
            result |= bits[i] << i;  // Reverse the bit sequence by swapping the shift positions
        }

        // Convert the integer value to a Unicode character
        return (char) result;
    }

}