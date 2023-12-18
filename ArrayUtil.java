public class ArrayUtil {

    private ArrayUtil() {
        throw new IllegalStateException("Utility class");
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

    public static int getSequenceIndex() {
        return -1;
    }

}
