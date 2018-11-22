import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool state = new UTXOPool();
    private Map<Double, Transaction> healthTx = new TreeMap<>();

    public TxHandler(UTXOPool utxoPool) {
        state = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<UTXO> allUTXOs = state.getAllUTXO();
        boolean foundOutput = false;
        for (int i = 0; i < tx.numInputs(); i++) {
            UTXO copyCat = new UTXO(tx.getHash(), i);
            for (UTXO unspent: allUTXOs) {
                if (unspent.equals(copyCat)) {
                    foundOutput =  true;
                }
            }
            if (!foundOutput) {
                return false;
            } else {
                foundOutput = false;
            }
        }
        return true;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> result = new ArrayList<>();
        UTXOPool copyOfState = new UTXOPool(state);
        for (Transaction tx: possibleTxs) {
            isHealthy(tx);
        }
        //the healthTx map should be populated now, ascending
        for (Transaction tx: healthTx.values()) {
            int totalInput = tx.numInputs();
            int processed = 0;
         for (int j = 0; j< tx.numInputs(); j++) {
            Transaction.Input i = tx.getInput(j);
            UTXO supposedU = new UTXO(i.prevTxHash, i.outputIndex);
            for (UTXO u: copyOfState.getAllUTXO()) {
                if (u.equals(supposedU)) {
                    copyOfState.removeUTXO(u);
                     processed +=1;
                    }
                }
            }
            if (processed == totalInput) {
                //add valid tx output
                for (int i = 0; i< tx.numOutputs(); i++) {
                    UTXO newU = new UTXO(tx.getHash(), i);
                    copyOfState.addUTXO(newU, tx.getOutput(i));
                }
                state = copyOfState;
                result.add(tx);
            } else {
                break;
            }
        }

        Transaction[] resultArray = new Transaction[result.size()];
        resultArray = result.toArray(resultArray);
        return resultArray;
    }
    /**
     * @return true if:
     * (1) all inputs  claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isHealthy(Transaction tx) {

        UTXOPool copyOfState = new UTXOPool(state);
        ArrayList<UTXO> allUTXOs = state.getAllUTXO();
        //check statement1
        Double sumOfInput = 0.0;
        Double sumOfOutput = 0.0;
        boolean foundInState = false;
        for (int j = 0; j< tx.numInputs(); j++) {
            Transaction.Input i = tx.getInput(j);
            UTXO supposedU = new UTXO(i.prevTxHash, i.outputIndex);
            for (UTXO u: allUTXOs) {
                if (u.equals(supposedU)) {
                    foundInState = true;
                    Transaction.Output targetOutput = state.getTxOutput(u);
                    //checking requirement 2
                    if(!Crypto.verifySignature(targetOutput.address, tx.getRawDataToSign(j), i.signature)) {
                        return false;
                    }
                    //checking requirement 3
                    if (!copyOfState.contains(u)) {
                        return false;
                    }
                    copyOfState.removeUTXO(u);
                    sumOfInput += targetOutput.value;
                }
            }
        }
        //checking requirement 1
        if (!foundInState) {
            return false;
        }

        for (int j = 0; j< tx.numOutputs(); j++) {
           Transaction.Output output  = tx.getOutput(j);
           if (output.value < 0) {
               return false;
           }
           sumOfOutput += output.value;
        }

        if (sumOfInput< sumOfOutput) {
            return false;
        }
        healthTx.put(sumOfInput, tx);
        return true;
    }


}
