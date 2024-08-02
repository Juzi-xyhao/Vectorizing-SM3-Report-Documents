import cn.xxx.SM3Engine;
import cn.xxx.SM3WithVector;

import java.util.HexFormat;



public class VectorTest {
    private static final HexFormat HEX = HexFormat.of();

    public static byte[] toBytes(String hex) {
        return HEX.parseHex(hex);
    }

    private static final byte[] MESSAGE_SHORT = toBytes("616263");
    private static final byte[] DIGEST_SHORT = toBytes(
            "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");

    private static final byte[] MESSAGE_LONG = toBytes(
            "61626364616263646162636461626364616263646162636461626364616263646162636461626364616263646162636461626364616263646162636461626364");
    private static final byte[] DIGEST_LONG = toBytes(
            "debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732");



    public void testDigest() {
        byte[] digest = new byte[32];

        SM3Engine sm3Engine = new SM3Engine();
        long startTime = System.nanoTime();
        sm3Engine.update(MESSAGE_LONG);
        sm3Engine.doFinal(digest);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("原始方式耗时 " + duration + "纳秒");

        printHex(digest);
    }

    public void testDigestWithVector() {
        byte[] digest = new byte[32];

        SM3WithVector sm3WithVector = new SM3WithVector();
        long startTime = System.nanoTime();
        sm3WithVector.update(MESSAGE_LONG);
        sm3WithVector.doFinal(digest);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("\nVector API耗时 " + duration + "纳秒");

        printHex(digest);
    }

    public static void printHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02X", b);
            hexString.append(hex);
        }
        System.out.println(hexString);
    }


    public static void main(String[] args){
        VectorTest vectorTest = new VectorTest();
        vectorTest.testDigest();
        vectorTest.testDigestWithVector();

    }
}