import java.util.Arrays;

public class LPFilter2 {

    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        final int n = (int) sampleFreq / DosRead.FP;
        final double[] filteredAudio = new double[inputSignal.length];

        for (int i = 0; i < inputSignal.length; i++) {
            // Calculate the start and end indices for the current sample
            final int start = Math.max(0, i - n / 2);
            final int end = Math.min(inputSignal.length, i + n / 2);
            final int count = end - start;

            // Calculate the sum of audio samples within the specified range
            final double sum = Arrays.stream(Arrays.copyOfRange(inputSignal, start, end)).sum();

            // Calculate the average value and store it in the filteredAudio array
            filteredAudio[i] = sum / count;
        }

        return filteredAudio;
    }

}
