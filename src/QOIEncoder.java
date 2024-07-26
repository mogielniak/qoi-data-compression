package cs107;

public final class QOIEncoder {

    private QOIEncoder() {
    }

    // ==================================================================================
    // ============================ QUITE OK IMAGE HEADER ===============================
    // ==================================================================================

    public static byte[] qoiHeader(Helper.Image image) {
        assert image != null;
        assert image.channels() == QOISpecification.RGB || image.channels() == QOISpecification.RGBA;
        assert image.color_space() == QOISpecification.sRGB || image.color_space() == QOISpecification.ALL;

        return ArrayUtils.concat(
                QOISpecification.QOI_MAGIC,
                // width
                ArrayUtils.fromInt(image.data()[0].length),
                // height
                ArrayUtils.fromInt(image.data().length),
                // channels
                ArrayUtils.wrap(image.channels()),
                // colorspace
                ArrayUtils.wrap(image.color_space())
        );
    }

    // ==================================================================================
    // ============================ ATOMIC ENCODING METHODS =============================
    // ==================================================================================

    public static byte[] qoiOpRGB(byte[] pixel) {
        assert pixel.length == 4;

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGB_TAG,
                pixel[0],
                pixel[1],
                pixel[2]
        );
    }

    public static byte[] qoiOpRGBA(byte[] pixel) {
        assert pixel.length == 4;

        return ArrayUtils.concat(
                ArrayUtils.wrap(QOISpecification.QOI_OP_RGBA_TAG),
                pixel
        );
    }
    public static byte[] qoiOpIndex(byte index) {
        assert index <= 63 && index >= 0;

        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_INDEX_TAG | index));
    }

    public static byte[] qoiOpDiff(byte[] diff) {

        assert diff != null;
        assert diff.length >= 3;
        assert diff[0] > -3 && diff[0] < 2;
        assert diff[1] > -3 && diff[1] < 2;
        assert diff[2] > -3 && diff[2] < 2;

        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_DIFF_TAG
                | (diff[0] + 2) << 4
                | (diff[1] + 2) << 2
                | (diff[2] + 2)
        ));
    }
    public static byte[] qoiOpLuma(byte[] diff) {

        assert diff != null;
        assert diff.length >= 3;

        byte dr = diff[0];
        byte dg = diff[1];
        byte db = diff[2];
        byte dr_dg = (byte) (dr - dg);
        byte db_dg = (byte) (db - dg);

        assert smallDiff(dg);
        assert smallerDiff(dr_dg);
        assert smallerDiff(db_dg);

        byte[] byte0 = ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_LUMA_TAG | (dg + 32)));
        byte[] byte1 = ArrayUtils.wrap((byte) ((dr_dg + 8) << 4 | (db_dg + 8)));
        return ArrayUtils.concat(byte0, byte1);
    }
    public static byte[] qoiOpRun(byte count) {

        return ArrayUtils.wrap(
                (byte) (QOISpecification.QOI_OP_RUN_TAG | (count - 1))
        );
    }

    // ==================================================================================
    // ============================== GLOBAL ENCODING METHODS  ==========================
    // ==================================================================================
    public static byte[] encodeData(byte[][] image) {
        // step 1 initialization
        byte[] previousPixel = QOISpecification.START_PIXEL;
        byte[][] indexHashTable = new byte[64][4];
        byte counter = 0;

        // step 2 pixel process
        byte[] result = new byte[0];
        for (int i = 0; i < image.length; i++) {
            byte[] pixel = image[i];
            // 1.
            if (ArrayUtils.equals(pixel, previousPixel)) {
                counter++;
                if (counter >= 62 || i == image.length - 1) {
                    result = ArrayUtils.concat(result, qoiOpRun(counter));
                    counter = 0;
                }
                previousPixel = pixel;
                continue;
            }
            if (counter != 0) {
                result = ArrayUtils.concat(result, qoiOpRun(counter));
                counter = 0;
            }
            // 2.
            byte index = QOISpecification.hash(pixel);
            if (ArrayUtils.equals(indexHashTable[index], pixel)) {
                result = ArrayUtils.concat(result, qoiOpIndex(index));
                previousPixel = pixel;
                continue;
            }
            indexHashTable[index] = pixel;
            // 3.
            if (pixel[3] == previousPixel[3]) {
                byte dr = (byte) (pixel[0] - previousPixel[0]);
                byte dg = (byte) (pixel[1] - previousPixel[1]);
                byte db = (byte) (pixel[2] - previousPixel[2]);
                byte dr_dg = (byte) (dr - dg);
                byte db_dg = (byte) (db - dg);
                if (smallestDiff(dr) && smallestDiff(dg) && smallestDiff(db)) {
                    // 3
                    result = ArrayUtils.concat(result, qoiOpDiff(new byte[]{dr, dg, db}));
                } else if (smallDiff(dg) && smallerDiff(dr_dg) && smallerDiff(db_dg)) {
                    // 4
                    result = ArrayUtils.concat(result, qoiOpLuma(new byte[]{dr, dg, db}));
                } else {
                    // 5
                    result = ArrayUtils.concat(result, qoiOpRGB(pixel));
                }
            } else {
                // 6
                result = ArrayUtils.concat(result, qoiOpRGBA(pixel));
            }
            previousPixel = pixel;
        }

        return result;
    }

    private static boolean smallDiff(int i) {
        return i > -33 && i < 32;
    }

    private static boolean smallerDiff(int i) {
        return i > -9 && i < 8;
    }

    private static boolean smallestDiff(int i) {
        return i > -3 && i < 2;
    }

    public static byte[] qoiFile(Helper.Image image) {

        byte[] header = qoiHeader(image);
        byte[] content = encodeData(ArrayUtils.imageToChannels(image.data()));

        return ArrayUtils.concat(header, content, QOISpecification.QOI_EOF);
    }

}
