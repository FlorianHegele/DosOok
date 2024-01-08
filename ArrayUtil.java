import java.util.List;

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

    public static double min(List<double[]> arr) {
        double min = min(arr.get(0));

        for(int i=1; i<arr.size(); i++) {
            final double x = min(arr.get(i));
            if(min > x) min = x;
        }

        return min;
    }

    public static double max(List<double[]> arr) {
        double max = max(arr.get(0));

        for(int i=1; i<arr.size(); i++) {
            final double x = max(arr.get(i));
            if(max < x) max = x;
        }

        return max;
    }

    public static int maxSize(List<double[]> arr) {
        int max = arr.get(0).length;

        for(int i=1; i<arr.size(); i++) {
            final int x = arr.get(i).length;
            if(max < x) max = x;
        }

        return max;
    }

    public static int getSequenceIndex() {
        return -1;
    }

}
