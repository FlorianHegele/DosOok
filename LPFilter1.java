import java.util.Arrays;

public class LPFilter1 {

    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        final int n = (int) sampleFreq / DosRead.FP;
        double[] filteredAudio = new double[inputSignal.length];

        for (int i = 0; i < filteredAudio.length; i++) {
            int start = Math.max(0, i - n / 2);
            int end = Math.min(inputSignal.length, i + n / 2);
            int count = end - start;

            double sum = Arrays.stream(Arrays.copyOfRange(inputSignal, start, end)).sum();
            filteredAudio[i] = sum / count;
        }

        return filteredAudio;
    }

}
