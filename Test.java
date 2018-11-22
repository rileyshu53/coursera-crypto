import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import sun.security.provider.DSAPublicKeyImpl;
import sun.security.x509.X509Key;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class Test {
    public static void main(String[] args){

        //Basic setup starts here

        ArrayList<KeyPair> keyList = new ArrayList<>();
        for (int i = 0; i<3; i++) {
            keyList.add(generateKeyPair());
        }

        Transaction existingTx = new Transaction();
        existingTx.addOutput(50, keyList.get(0).getPublic());
        existingTx.addOutput(20, keyList.get(1).getPublic());
        existingTx.addOutput(10, keyList.get(2).getPublic());
        byte[] txHash = "existingTx".getBytes();
        existingTx.setHash(txHash);

        UTXOPool utxoPool = new UTXOPool();
        utxoPool.addUTXO(new UTXO(txHash, 0), existingTx.getOutput(0));
        utxoPool.addUTXO(new UTXO(txHash, 1), existingTx.getOutput(1));
        utxoPool.addUTXO(new UTXO(txHash, 2), existingTx.getOutput(2));

        //Basic setup is done

        Transaction tx1 = new Transaction();
        tx1.setHash("tx1".getBytes());
        tx1.addInput(existingTx.getHash(), 0);
        tx1.addOutput(50, keyList.get(2).getPublic());
        byte[] sig0 = generateSig(keyList.get(0).getPrivate(), tx1.getRawDataToSign(0));
        tx1.addSignature(sig0, 0);

        Transaction tx2 = new Transaction();
        tx2.setHash("tx1".getBytes());
        tx2.addInput(existingTx.getHash(), 1);
        byte[] sig2_0 = generateSig(keyList.get(1).getPrivate(), tx2.getRawDataToSign(0));
        tx2.addSignature(sig2_0, 0);
        tx2.addOutput(20, keyList.get(2).getPublic());

        Transaction tx3 = new Transaction();
        tx3.setHash("tx1".getBytes());
        tx3.addInput(existingTx.getHash(), 0);
        byte[] sig3_0 = generateSig(keyList.get(0).getPrivate(), tx3.getRawDataToSign(0));
        tx3.addSignature(sig3_0, 0);

        tx3.addInput(existingTx.getHash(), 2);
        byte[] sig3_2 = generateSig(keyList.get(2).getPrivate(), tx3.getRawDataToSign(1));
        tx3.addSignature(sig3_2, 0);
        tx3.addOutput(20, keyList.get(2).getPublic());


        Transaction tx1Invalid = new Transaction();
        tx1.addInput(existingTx.getHash(), 0);
        byte[] sig0invalid = generateSig(keyList.get(2).getPrivate(), tx1.getRawDataToSign(0));
        tx1.addSignature(sig0invalid, 0);

        //isValid Test
         boolean isValidResult = true;
        TxHandler isValidTest = new TxHandler(utxoPool);
        if (isValidTest.isValidTx(tx1)) {
            isValidResult = false;
        }
        if (!isValidTest.isValidTx(existingTx)) {
            isValidResult = false;
        }
        if (isValidResult) {
            System.out.println("Congratulaitons for passing the isValid test!");
        } else {
            System.out.println("Oh no you failed the isValid test!");
        }

        //healthy test
        boolean healthyResult = true;
        TxHandler healthTest = new TxHandler(utxoPool);
//        if (healthTest.isHealthy(tx1Invalid)) {
//            healthyResult = false;
//        }
        if (!healthTest.isHealthy(tx1)) {
            healthyResult = false;
        }
        if (healthyResult) {
            System.out.println("Congratulaitons for passing the healthy test!");
        } else {
            System.out.println("Oh no you failed the healthy test!");
        }
        //Simple test: should include one valid transaction
        TxHandler test1 = new TxHandler(utxoPool);
        Boolean test1Result = true;
        if (test1.isValidTx(tx1) == true) {
            test1Result = false;
        }

        Transaction[] test1Input = {tx1, tx1Invalid};
        test1.handleTxs(test1Input);

        if (test1.isValidTx(tx1) == false) {
            test1Result = false;
        }
        if (test1Result) {
            System.out.println("Congratulaitons for passing the simple test!");
        } else {
            System.out.println("Oh no you failed the simple test!");
        }

        //advanced test
        TxHandler test2 = new TxHandler(utxoPool);
        Boolean test2Result = true;
        if (test2.isValidTx(tx1) == true) {
            test1Result = false;
        }
        if (test2.isValidTx(tx2) == true) {
            test1Result = false;
        }
        if (test2.isValidTx(tx3) == true) {
            test1Result = false;
        }

        Transaction[] test2Input = {tx3, tx1, tx2};
        test2.handleTxs(test2Input);

        if (test1.isValidTx(tx1) == false) {
            test2Result = false;
        }
        if (test1.isValidTx(tx2) == false) {
            test2Result = false;
        }
        if (test1.isValidTx(tx3) == true) {
            test2Result = false;
        }
        if (test2Result) {
            System.out.println("Congratulaitons for passing the advanced test!");
        } else {
            System.out.println("Oh no you failed the advanced test!");
        }

    }

    public static KeyPair generateKeyPair(){
        try{
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            return kp;
        }

        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] generateSig(PrivateKey privateKey, byte[] rawDataToSign){
        try{
            Signature dsa = Signature.getInstance("SHA256withRSA");
            dsa.initSign(privateKey);
            dsa.update(rawDataToSign);
            return dsa.sign();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
