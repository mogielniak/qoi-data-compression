package cs107;

public final class ArrayUtils {

    private ArrayUtils(){}

    // ==================================================================================
    // =========================== ARRAY EQUALITY METHODS ===============================
    // ==================================================================================
    public static boolean equals(byte[] a1, byte[] a2){
        if (a1 == null && a2 == null) return true;
        if (a1 == null || a2 == null)
            throw new AssertionError("one of the parameters is null ");

        if (a1.length != a2.length) return false;

        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) return false;
        }
        return true;
    }

    public static boolean equals(byte[][] a1, byte[][] a2){
        if (a1 == null && a2 == null) return true;

        if (a1 == null || a2 == null)
            throw new AssertionError("one of the parameters is null");

        if (a1.length != a2.length) return false;


        for (int i = 0; i < a1.length; i++) {
            if (a1[i].length != a2[i].length) return false;

            for (int j = 0; j < a1[i].length; j++) {
                if (a1[i][j] != a2[i][j]) return false;
            }
        }
        return true;
    }

    // ==================================================================================
    // ============================ ARRAY WRAPPING METHODS ==============================
    // ==================================================================================

    public static byte[] wrap(byte value){
        return new byte[]{value};
    }

    // ==================================================================================
    // ========================== INTEGER MANIPULATION METHODS ==========================
    // ==================================================================================

    public static int toInt(byte[] bytes){
        assert bytes != null && bytes.length == 4;

        int res = 0xFF & bytes[0];
        res <<= 8;
        res += 0xFF & bytes[1];
        res <<= 8;
        res += 0xFF & bytes[2];
        res <<= 8;
        res += 0xFF & bytes[3];
        return res;
    }

    public static byte[] fromInt(int value){
        return new byte[] {
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    // ==================================================================================
    // ========================== ARRAY CONCATENATION METHODS ===========================
    // ==================================================================================

    public static byte[] concat(byte ... bytes){
        assert bytes != null;
        byte[] concat = new byte[bytes.length];
        System.arraycopy(bytes, 0, concat, 0, bytes.length);
        return concat;
    }

    public static byte[] concat(byte[] ... tabs){
        assert tabs != null;
        int byteLength = 0;
        for (byte[] tab : tabs) {
            byteLength += tab.length;
        }
        byte[] concat = new byte[byteLength];
        int l = 0;
        for (byte[] tab : tabs) {
            System.arraycopy(tab, 0, concat, l, tab.length);
            l += tab.length;
        }
        return concat;
    }

    // ==================================================================================
    // =========================== ARRAY EXTRACTION METHODS =============================
    // ==================================================================================

    public static byte[] extract(byte[] input, int start, int length){
        assert input != null;
        assert start >= 0 && start < input.length;
        assert length >= 0;
        assert (start + length) <= input.length;
        byte[] extract = new byte[length];
        System.arraycopy(input, start, extract, 0, length);
        return extract;
    }

    public static byte[][] partition(byte[] input, int ... sizes) {
        assert input != null;
        assert sizes != null;
        byte[][] partition = new byte[sizes.length][];
        int start = 0;
        for (int i = 0; i < sizes.length; i++) {
            partition[i] = extract(input, start, sizes[i]);
            start += sizes[i];
        }
        return partition;
    }

    // ==================================================================================
    // ============================== ARRAY FORMATTING METHODS ==========================
    // ==================================================================================

    public static byte[][] imageToChannels(int[][] input){
        assert input != null;
        int len = input[0].length;
        for (int[] ints : input) {
            assert len == ints.length;
        }
        byte[][] channels = new byte[input.length * len][4];
        int i = 0;
        for (int[] ints : input) {
            for (int pixel : ints) {
                //image pixel is argb not rgba
                byte[] channel = fromInt(pixel);
                channels[i][QOISpecification.r] = channel[1];
                channels[i][QOISpecification.g] = channel[2];
                channels[i][QOISpecification.b] = channel[3];
                channels[i][QOISpecification.a] = channel[0];
                i++;
            }
        }
        return channels;
    }


    public static int[][] channelsToImage(byte[][] input, int height, int width){
        assert input != null;
        assert input[0].length == 4;
        assert input.length == height * width;
        int[][] pixels = new int[height][width];
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                byte[] bytes = input[w + h * width];
                pixels[h][w] = ((bytes[QOISpecification.a] & 0xFF) << 24)
                        | ((bytes[QOISpecification.r] & 0xFF) << 16)
                        | ((bytes[QOISpecification.g] & 0xFF) << 8)
                        | (bytes[QOISpecification.b] & 0xFF);
            }
        }
        return pixels;
    }
    }