import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    UTXOPool pool = null;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        pool = utxoPool;
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
        // IMPLEMENT THIS


        if (!checkAllOutputsClaimed(tx)) return false;
        if (!checkValidInputSignatures(tx)) return false;
        if (isDoubleSpending(tx)) return false;
        if (checkNegativeOutput(tx)) return false;
        if (!checkValidInputOutputSum(tx)) return false;



        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        ArrayList<Transaction> res = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs)
        {
            if(!isValidTx(tx)) continue;

            for( int inp_idx = 0; inp_idx < tx.getInputs().size(); ++inp_idx )
            {
                Transaction.Input inp = tx.getInput(inp_idx);
                UTXO utxo = getUtxo(inp.prevTxHash, inp.outputIndex);
                pool.removeUTXO(utxo);
            }

            for( int out_idx = 0; out_idx < tx.getOutputs().size(); ++out_idx )
            {
                UTXO utxo = getUtxo(tx.getHash(), out_idx);
                pool.addUTXO(utxo, tx.getOutput(out_idx));
            }

            res.add(tx);
        }

        return res.toArray(new Transaction[0]);

    }

    boolean checkValidInputOutputSum(Transaction tx)
    {
        double inpSum = 0, outSum=0;
        for( int inp_idx = 0; inp_idx < tx.getInputs().size(); ++inp_idx )
        {
            Transaction.Input inp = tx.getInput(inp_idx);
            Transaction.Output prevOut = pool.getTxOutput(new UTXO(inp.prevTxHash, inp.outputIndex));
            inpSum += prevOut.value;
        }

        for( Transaction.Output out : tx.getOutputs() )
        {
            outSum += out.value;
        }

        return inpSum >= outSum;
    }


        boolean checkNegativeOutput(Transaction tx)
    {
        for( Transaction.Output out : tx.getOutputs() )
        {
            if(out.value < 0) return true;
        }
        return false;
    }

    boolean checkAllOutputsClaimed(Transaction tx)
    {
        for( Transaction.Input inp : tx.getInputs() )
        {
            if( !pool.contains( new UTXO(inp.prevTxHash, inp.outputIndex) ) )
                return false;
        }
        return true;
    }

    boolean isDoubleSpending(Transaction tx)
    {

        HashSet<UTXO> s = new HashSet<UTXO>();

        for( Transaction.Input inp : tx.getInputs() )
        {
            UTXO uxto = new UTXO(inp.prevTxHash, inp.outputIndex);
            if( pool.contains( uxto ) && !s.add( uxto ) )
                return true;
        }
        return false;
    }


    Transaction.Output getPrevTxOutputFromPool( byte[] prevTxHash, int outputIndex )
    {
        return getPrevTxOutputFromPool( getUtxo(prevTxHash, outputIndex) );
    }

    UTXO getUtxo(byte[] prevTxHash, int outputIndex)
    {
        return new UTXO(prevTxHash, outputIndex);
    }

    Transaction.Output getPrevTxOutputFromPool( UTXO utxo )
    {
        return pool.getTxOutput(utxo);
    }



    boolean checkValidInputSignatures(Transaction tx)
    {
        for( int inp_idx = 0; inp_idx < tx.getInputs().size(); ++inp_idx )
        {
            Transaction.Input inp = tx.getInput(inp_idx);

            Transaction.Output prevOut = getPrevTxOutputFromPool(inp.prevTxHash, inp.outputIndex);

            Signature sig = null;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(prevOut.address);
                sig.update(tx.getRawDataToSign(inp_idx));

                if (!sig.verify(inp.signature))
                    return false;
            }
            catch (SignatureException e1)
            {
                return false;
            }
            catch (NoSuchAlgorithmException | InvalidKeyException e)
            {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

}
