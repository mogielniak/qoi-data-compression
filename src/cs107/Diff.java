package cs107;

import java.util.Arrays;

public final class Diff {

    // ============================================================================================
    // ======================================= DIFF API ===========================================
    // ============================================================================================
    public static void diff(byte[] b1, byte[] b2){
        assert b1 != null;
        assert b2 != null;

        if(Arrays.equals(b1, b2))
            showSameFileMessage();
        else {
            var size_to_check = b1.length != b2.length ? sizeWarning(b1.length, b2.length) : b1.length;
            compareAndShow(b1, b2, size_to_check);
        }

        showEnd();
    }

    public static void diff(String file_1, String file_2){
        assert file_1 != null;
        assert file_2 != null;

        var b1 = Helper.read(file_1);
        var b2 = Helper.read(file_2);
        showHeader(file_1, file_2, b1, b2);
        diff(b1, b2);
    }

    // ============================================================================================


    // Hide default constructor
    private Diff(){}

    private static void showHeader(String file_1, String file_2, byte[] stream_f1, byte[] stream_f2){
        System.out.println("========================================== DIFF ==========================================");
        System.out.printf("== File 1 : '%s', size = %d bytes %n", file_1, stream_f1.length);
        System.out.printf("== File 2 : '%s', size = %d bytes %n", file_2, stream_f2.length);
        System.out.println("==========================================================================================");
    }

    private static int sizeWarning(int size_1, int size_2){
        var min = Integer.min(size_1, size_2);
        System.out.printf("== WARNING : The two input have different sizes, we will only check the %d first bytes%n", min);
        return min;
    }

    private static void showSameFileMessage(){
        System.out.println("== WARNING : The two inputs have the same content");
    }

    private static void compareAndShow(byte[] b1, byte[] b2, int size_to_check){
        for (var i = 0; i < size_to_check; i++){
            if (b1[i] != b2[i]){
                System.out.printf("[%06X] ~ %02x ~ %02x%n", i, b1[i], b2[i]);
            }
        }
    }

    private static void showEnd(){
        System.out.println("========================================= END DIFF =======================================");
    }

}