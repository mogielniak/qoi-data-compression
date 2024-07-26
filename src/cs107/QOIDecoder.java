package cs107;

import static cs107.Helper.Image;


public final class QOIDecoder {

    private QOIDecoder(){}

    // ==================================================================================
    // =========================== QUITE OK IMAGE HEADER ================================
    // ==================================================================================

    public static int[] decodeHeader(byte[] header){
        assert header != null;
        assert header.length == QOISpecification.HEADER_SIZE;

        byte[][] partition = ArrayUtils.partition(header, 4, 4, 4, 1, 1);

        assert ArrayUtils.equals(partition[0], QOISpecification.QOI_MAGIC);

        int width = ArrayUtils.toInt(partition[1]);
        int height = ArrayUtils.toInt(partition[2]);
        int channels = partition[3][0];

        assert channels == QOISpecification.RGB || channels == QOISpecification.RGBA;

        int colorspace = partition[4][0];

        assert colorspace == QOISpecification.ALL || colorspace == QOISpecification.sRGB;
        return new int[]{width, height, channels, colorspace};
        }


    // ==================================================================================
    // =========================== ATOMIC DECODING METHODS ==============================
    // ==================================================================================

    public static int decodeQoiOpRGB(byte[][] buffer, byte[] input, byte alpha, int position, int idx){
        assert buffer != null;
        assert input != null;
        assert idx >= 0 && idx < input.length;
        assert input.length - idx >= 3;

        buffer[position] = new byte[]{input[idx++], input[idx++], input[idx], alpha};
        return 3;
    }
    public static int decodeQoiOpRGBA(byte[][] buffer, byte[] input, int position, int idx){
        assert buffer != null;
        assert input != null;
        assert idx >= 0 && idx < input.length;
        assert input.length - idx >= 4;

        buffer[position] = new byte[]{input[idx++], input[idx++], input[idx++], input[idx]};
        return 4;
    }

    public static byte[] decodeQoiOpDiff(byte[] previousPixel, byte chunk){
        assert previousPixel != null;
        assert previousPixel.length == 4;
        assert (chunk & QOISpecification.QOI_OP_DIFF_TAG) == QOISpecification.QOI_OP_DIFF_TAG;

        // cur - pre + 2 = chunk

        byte dr = (byte) ((chunk >> 4 & 0x3) - 2 + previousPixel[0]);
        byte dg = (byte) ((chunk >> 2 & 0x3) - 2 + previousPixel[1]);
        byte db = (byte) ((chunk & 0x3) - 2 + previousPixel[2]);

        return new byte[]{dr, dg, db, previousPixel[3]};
    }

    public static byte[] decodeQoiOpLuma(byte[] previousPixel, byte[] data){
        assert previousPixel != null;
        assert previousPixel.length == 4;
        assert ((data[0] & QOISpecification.QOI_OP_LUMA_TAG) == QOISpecification.QOI_OP_LUMA_TAG);
        byte dg = (byte) ((data[0] ^ QOISpecification.QOI_OP_LUMA_TAG) - 32);
        byte dr = (byte) (((data[1] & 0xF0) >> 4) - 8 + dg);
        byte db = (byte) ((data[1] & 0xF) - 8 + dg);

        return new byte[]{
                (byte) (dr + previousPixel[0]),
                (byte) (dg + previousPixel[1]),
                (byte) (db + previousPixel[2]),
                previousPixel[3]
        };
    }

    public static int decodeQoiOpRun(byte[][] buffer, byte[] pixel, byte chunk, int position){
        assert buffer != null;
        assert position >= 0 && position < buffer.length;
        assert pixel != null;
        assert pixel.length == 4;
        // bias -1
        int counter = (chunk ^ QOISpecification.QOI_OP_RUN_TAG) + 1;

        assert buffer.length >= counter;

        for (int i = 0; i < counter; i++) {
            buffer[position + i] = pixel;
        }
        return counter - 1;
    }

    // ==================================================================================
    // ========================= GLOBAL DECODING METHODS ================================
    // ==================================================================================
    public static byte[][] decodeData(byte[] data, int width, int height){
        assert data != null;
        assert width > 0 && height > 0;
        assert data.length >= width * height;

        byte[][] tab = new byte[width * height][4];

        // step 1 initialization
        byte[] previousPixel = QOISpecification.START_PIXEL;
        byte[][] indexHashTable = new byte[64][4];

        // step 2
        int position = 0;
        for (int idx = 0; idx < data.length; idx++) {

            if (data[idx] == QOISpecification.QOI_OP_RGB_TAG) {
                // ps. first is tag
                idx += decodeQoiOpRGB(tab, data, previousPixel[3], position, idx + 1);
            } else if (data[idx] == QOISpecification.QOI_OP_RGBA_TAG) {
                idx += decodeQoiOpRGBA(tab, data, position, idx + 1);
            } else {
                byte tag = (byte) (data[idx] & 0xC0);
                if (tag == QOISpecification.QOI_OP_DIFF_TAG) {
                    tab[position] = decodeQoiOpDiff(previousPixel, data[idx]);
                } else if (tag == QOISpecification.QOI_OP_LUMA_TAG) {
                    tab[position] = decodeQoiOpLuma(previousPixel, new byte[]{data[idx++], data[idx]});
                } else if (tag == QOISpecification.QOI_OP_RUN_TAG) {
                    int c = decodeQoiOpRun(tab, previousPixel, data[idx], position);
                    position += c;
                } else {
                    tab[position] = indexHashTable[data[idx] & 0x3F];
                }
            }
            previousPixel = tab[position++];
            indexHashTable[QOISpecification.hash(previousPixel)] = previousPixel;
        }

        return tab;
    }

    public static Image decodeQoiFile(byte[] content){
        assert content != null;
        assert ArrayUtils.equals(ArrayUtils.extract(content, content.length - QOISpecification.QOI_EOF.length, QOISpecification.QOI_EOF.length), QOISpecification.QOI_EOF);

        byte[] header = ArrayUtils.extract(content, 0, QOISpecification.HEADER_SIZE);
        int[] headers = decodeHeader(header);

        byte[][] bytes = decodeData(
                ArrayUtils.extract(content, QOISpecification.HEADER_SIZE, content.length - QOISpecification.HEADER_SIZE - QOISpecification.QOI_EOF.length),
                headers[0], headers[1]
        );

        int[][] channelsToImage = ArrayUtils.channelsToImage(bytes, headers[1], headers[0]);
        return new Image(channelsToImage, (byte) headers[2], (byte) headers[3]);
    }

}