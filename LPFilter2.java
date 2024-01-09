public class LPFilter2 {

        public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
            final double[] filteredAudio = new double[inputSignal.length];

            // Coefficients du filtre Butterworth (ordre 1)
            final double a1 = Math.exp(-2.0 * Math.PI * cutoffFreq / sampleFreq);
            final double b0 = 1.0 - a1;

            // Initialisation de la première valeur filtrée
            filteredAudio[0] = inputSignal[0];

            // Application du filtre aux échantillons suivants
            for (int i = 1; i < inputSignal.length; i++) {
                filteredAudio[i] = b0 * inputSignal[i] + a1 * filteredAudio[i - 1];
            }

            return filteredAudio;
        }

}
