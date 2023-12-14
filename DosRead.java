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
        StdDraw.setYscale(min(sig) * yPadding, max(sig) * yPadding);
        StdDraw.setXscale(-(sig.length * xPadding - sig.length), sig.length * xPadding);

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
        System.out.println(Arrays.toString(dosRead.audio));

        displaySig(dosRead.audio, 0, dosRead.audio.length - 1, "line", "Signal audio");


        // reverse the negative values
        dosRead.audioRectifier();
        //System.out.println(Arrays.toString(dosRead.audio));

        // apply a low pass filter
        dosRead.audioLPFilter(44);
        //System.out.println(Arrays.toString(dosRead.audio));

        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate / BAUDS, 12000);
        //System.out.println(Arrays.toString(dosRead.audio));

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null) {
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

//        displaySig(dosRead.audio, 0, dosRead.audio.length - 1, "line", "Signal audio");

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
     * Fc = (1/2n)*FECH
     *
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
        final double fc = (1.0 / (2.0 * n)) * sampleRate;
        final double alpha = 2.0 * Math.PI * fc / sampleRate;
        final double coeff = 1.0 / (1.0 + Math.sin(alpha));

        for (int i = 0; i < audio.length; i++) {
            audio[i] = (i == 0)
                    ? audio[i]
                    : coeff * (audio[i - 1] + audio[i] - audio[i - 1]);
        }
    }



    /**
     * Resample the audio array and apply a threshold
     *
     * @param period    the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold) {
        final int newLength = audio.length / period;
        final double[] resampledAudio = new double[newLength];

        for (int i = 0; i < newLength; i++) {
            final int start = i * period;
            final int end = Math.min((i + 1) * period, audio.length);

            // Calcul de la moyenne sur la période actuelle
            double sum = 0;
            for (int j = start; j < end; j++)
                sum += audio[j];


            final double average = sum / (end - start);

            // Appliquer le seuil et convertir en 0 ou 1
            resampledAudio[i] = (average >= threshold) ? 1 : 0;
        }

        audio = resampledAudio;
    }

    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar() {
        int decal = -1;

        for(int i=0; i<audio.length; i++) {
            for(int j=0; j<START_SEQ.length; j++) {
                if(audio[i] != START_SEQ[j]) break;

                if(j+1 == START_SEQ.length) decal = i;
                i++;
            }
        }

        if (decal == -1) {
            System.out.println("START_SEQ NOT FOUND");
            return;
        }

        decodedChars = new char[audio.length - decal];
        for(int i=decal; i<audio.length; i++) {
            decodedChars[i] = (char)audio[i];
        }
    }

    public static double min(double[] arr) {
        double min = arr[0];

        for(int i=1; i<arr.length; i++) {
            final double x = arr[i];
            if(min > x) min = x;
        }

        return min;
    }

    public static double max(double[] arr) {
        double max = arr[0];

        for(int i=1; i<arr.length; i++) {
            final double x = arr[i];
            if(max < x) max = x;
        }

        return max;
    }
}