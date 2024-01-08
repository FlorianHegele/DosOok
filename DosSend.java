import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
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
        File file = new File(path);
        try {
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
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
     * Display signals in a window
     *
     * @param listOfSigs a list of the signals to display
     * @param start      the first sample to display
     * @param stop       the last sample to display
     * @param mode       "line" or "point"
     * @param title      the title of the window
     */
    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title) {
      /*
          À compléter
      */
    }

    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");

        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (double) (dosSend.readTextData() + dosSend.START_SEQ.length / 8) * 8.0 / dosSend.BAUDS;

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
        displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");
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
        taille = (long) (FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;

        try {
            outStream.write(new byte[]{'R', 'I', 'F', 'F'});

            // Taille totale du fichier (en octets)
            writeLittleEndian((int) (nbBytes + 36), 4, outStream);

            // Format du fichier (WAVE)
            outStream.write(new byte[]{'W', 'A', 'V', 'E'});

            // Sous-entête Format
            outStream.write(new byte[]{'f', 'm', 't', ' '});

            // Taille du sous-entête Format (16 pour PCM)
            writeLittleEndian(16, 4, outStream);

            // Format audio (1 pour PCM)
            writeLittleEndian(1, 2, outStream);

            // Nombre de canaux audio (1 pour mono)
            writeLittleEndian(CHANNELS, 2, outStream);

            // Fréquence d'échantillonnage (en Hz)
            writeLittleEndian(FECH, 4, outStream);

            // Débit d'octets par seconde
            writeLittleEndian(FECH * CHANNELS * FMT / 8, 4, outStream);

            // Bloc d'octets par échantillon
            writeLittleEndian(CHANNELS * FMT / 8, 2, outStream);

            // Bits par échantillon
            writeLittleEndian(FMT, 2, outStream);

            // Sous-entête Data
            outStream.write(new byte[]{'d', 'a', 't', 'a'});

            // Taille des données (en octets)
            writeLittleEndian((int) nbBytes, 4, outStream);
            /*
                À compléter
            */
        } catch (Exception e) {
            System.out.printf(e.toString());
        }
    }

    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits signed)
     */
    public void writeNormalizeWavData() {
        try {
            for (double elements : dataMod) {
                int normalizedElements = (int) (MAX_AMP * elements);
                writeLittleEndian(normalizedElements, FMT / 8, outStream);
            }
        } catch (Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }

    /**
     * Read the text data to encode and store them into dataChar
     *
     * @return the number of characters read
     */
    public int readTextData() {
        while (input.hasNextLine()){
            dataChar = input.nextLine().toCharArray();
        }

        return dataChar.length;
    }




    /**
     * convert a char array to a bit array
     *
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars) {
            if (chars == null) {
                System.out.println("Erreur : Le tableau de caractères est null.");
                return new byte[0]; // Ou lancez une exception appropriée selon vos besoins.
            }

            byte[] by = new byte[chars.length * 8];

            for (int i = 0; i < chars.length; i++) {
                byte[] data = BitConversion.charToBits(chars[i]);
                System.arraycopy(data, 0, by, i * 8, data.length);
            }

            for (byte element : by) {
                System.out.print(element + " ");
            }
            System.out.println();

            return by;
    }



    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     *
     * @param bits the data to modulate
     */
    public void modulateData(byte[] bits) {
        // Nombre d'échantillons par symbole
        int samplesPerSymbol = FECH / BAUDS;

        // Nombre total d'échantillons nécessaires
        int totalSamples = bits.length * samplesPerSymbol;

        // Initialisation du tableau des données modulées
        dataMod = new double[totalSamples];

        // Fréquence de la porteuse en radian par échantillon
        double omega = 2 * Math.PI * FP / FECH;

        // Modulation d'amplitude par sauts (ASK)
        for (int i = 0; i < bits.length; i++) {
            // Valeur du bit actuel
            int bitValue = (bits[i] == 0) ? 0 : 1;

            // Modulation de l'amplitude pour chaque échantillon du symbole
            for (int j = 0; j < samplesPerSymbol; j++) {
                // Indice de l'échantillon actuel
                int index = i * samplesPerSymbol + j;

                // Modulation d'amplitude en multipliant par la valeur du bit
                dataMod[index] = bitValue * Math.sin(omega * index);
            }
        }
    }






}