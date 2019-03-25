package org.aion.zero.types;

import static org.aion.util.bytes.ByteUtil.ZERO_BYTE_ARRAY;

import java.math.BigInteger;
import java.util.Arrays;
import org.aion.mcf.tx.TransactionTypes;
import org.aion.mcf.vm.types.DataWordImpl;
import org.aion.types.Address;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKey.MissingPrivateKeyException;
import org.aion.crypto.HashUtil;
import org.aion.crypto.ISignature;
import org.aion.crypto.SignatureFac;
import org.aion.mcf.types.AbstractTransaction;
import org.aion.mcf.vm.Constants;
import org.aion.rlp.RLP;
import org.aion.rlp.RLPList;
import org.aion.util.bytes.ByteUtil;
import org.aion.util.time.TimeInstant;

/** Aion transaction class. */
public class AionTransaction extends AbstractTransaction {

    public static final int RLP_TX_NONCE = 0,
            RLP_TX_TO = 1,
            RLP_TX_VALUE = 2,
            RLP_TX_DATA = 3,
            RLP_TX_TIMESTAMP = 4,
            RLP_TX_NRG = 5,
            RLP_TX_NRGPRICE = 6,
            RLP_TX_TYPE = 7,
            RLP_TX_SIG = 8;

    /* Tx in encoded form */
    protected byte[] rlpEncoded;

    private byte[] rlpRaw;

    protected Address from;

    /** These four members doesn't include into the RLP encode data */
    private long txIndexInBlock = 0;

    private long blockNumber = 0;
    private byte[] blockHash = null;
    private long nrgConsume = 0;

    /*
     * Indicates if this transaction has been parsed from the RLP-encoded data
     */
    protected boolean parsed = false;

    public AionTransaction(byte[] encodedData) {
        this.rlpEncoded = encodedData;
        parsed = false;
    }

    public AionTransaction(
            byte[] nonce, Address to, byte[] value, byte[] data, long nrg, long nrgPrice) {
        super(nonce, to, value, data, nrg, nrgPrice);
        parsed = true;
    }

    public AionTransaction(
            byte[] nonce,
            Address to,
            byte[] value,
            byte[] data,
            long nrg,
            long nrgPrice,
            byte type) {
        super(nonce, to, value, data, nrg, nrgPrice);
        this.type = type;
        parsed = true;
    }

    // constructor for create nrgEstimate transaction
    public AionTransaction(
            byte[] nonce,
            Address from,
            Address to,
            byte[] value,
            byte[] data,
            long nrg,
            long nrgPrice) {
        super(nonce, to, value, data, nrg, nrgPrice);
        this.from = from;
        parsed = true;
    }

    // constructor for explicitly setting a transaction type.
    public AionTransaction(
            byte[] nonce,
            Address from,
            Address to,
            byte[] value,
            byte[] data,
            long nrg,
            long nrgPrice,
            byte txType) {

        super(nonce, to, value, data, nrg, nrgPrice);
        this.from = from;
        this.type = txType;
        parsed = true;
    }

    // For InternalTx constructor
    public AionTransaction(byte[] nonce, Address to, byte[] value, byte[] data) {
        super(nonce, to, value, data, 0L, 0L);
        parsed = true;
    }

    /**
     * The AionTransaction constructor for creating a gc transaction
     *
     * @param to the avm contract need to be GC
     * @param blockTimeStamp the timeStamp for the new block.
     */
    public AionTransaction(Address to, long blockTimeStamp) {
        super(to, blockTimeStamp);
        parsed = true;
    }

    @Override
    public AionTransaction clone() {
        if (!parsed) {
            rlpParse();
        }

        AionTransaction tx2 = new AionTransaction(nonce, to, value, data, nrg, nrgPrice, type);

        tx2.signature = signature; // NOTE: reference copy
        tx2.timeStamp = timeStamp;

        return tx2;
    }

    public void rlpParse() {

        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList tx = (RLPList) decodedTxList.get(0);

        this.nonce = tx.get(RLP_TX_NONCE).getRLPData();
        this.value = tx.get(RLP_TX_VALUE).getRLPData();
        this.data = tx.get(RLP_TX_DATA).getRLPData();

        byte[] rlpTo = tx.get(RLP_TX_TO).getRLPData();
        if (rlpTo == null || rlpTo.length == 0) {
            this.to = null;
        } else {
            this.to = Address.wrap(tx.get(RLP_TX_TO).getRLPData());
        }

        this.timeStamp = tx.get(RLP_TX_TIMESTAMP).getRLPData();
        this.nrg = new BigInteger(1, tx.get(RLP_TX_NRG).getRLPData()).longValue();
        this.nrgPrice = new BigInteger(1, tx.get(RLP_TX_NRGPRICE).getRLPData()).longValue();
        this.type = new BigInteger(1, tx.get(RLP_TX_TYPE).getRLPData()).byteValue();

        byte[] sigs = tx.get(RLP_TX_SIG).getRLPData();
        if (sigs != null) {
            // Singature Factory will decode the signature based on the algo
            // presetted in main() entry.
            ISignature is = SignatureFac.fromBytes(sigs);
            if (is != null) {
                this.signature = is;
            } else {
                // still ok if signature is null, but log it out.
                this.signature = null;
                LOG.error("tx -> unable to decode signature");
            }
        } else {
            if (!isGCTransaction()) {
                LOG.error("tx -> no signature found");
            } else {
                this.signature = null;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("GC transaction! {}", this);
                }
            }

            this.parsed = true;
            this.hash = this.getTransactionHash();
        }
    }

    public boolean isParsed() {
        return parsed;
    }

    @Override
    public byte[] getTransactionHash() {
        if (hash != null) {
            return hash;
        }

        if (!parsed) {
            rlpParse();
        }
        byte[] plainMsg = this.getEncoded();
        // cache it.
        hash = HashUtil.h256(plainMsg);
        return hash;
    }

    public byte[] getRawHash() {
        if (!parsed) {
            rlpParse();
        }
        byte[] plainMsg = this.getEncodedRaw();
        return HashUtil.h256(plainMsg);
    }

    @Override
    public byte[] getNonce() {
        if (!parsed) {
            rlpParse();
        }
        return nonce == null ? ZERO_BYTE_ARRAY : nonce;
    }

    public BigInteger getNonceBI() {
        return new BigInteger(1, getNonce());
    }

    @Override
    public byte[] getTimestamp() {
        return getTimeStamp();
    }

    @Override
    public byte[] getTimeStamp() {
        if (!parsed) {
            rlpParse();
        }
        return this.timeStamp == null ? ZERO_BYTE_ARRAY : this.timeStamp;
    }

    public BigInteger getTimeStampBI() {
        return new BigInteger(1, getTimestamp());
    }

    @Override
    public long getEnergyLimit() {
        if (!parsed) {
            rlpParse();
        }
        return this.nrg;
    }

    @Override
    public long getEnergyPrice() {
        if (!parsed) {
            rlpParse();
        }
        return this.nrgPrice;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = BigInteger.valueOf(timeStamp).toByteArray();
        this.parsed = true;
    }

    @Override
    public byte[] getValue() {
        if (!parsed) {
            rlpParse();
        }
        return value == null ? ZERO_BYTE_ARRAY : value;
    }

    @Override
    public Address getDestinationAddress() {
        if (!parsed) {
            rlpParse();
        }
        return to;
    }

    @Override
    public byte[] getData() {
        if (!parsed) {
            rlpParse();
        }
        return data;
    }

    @Override
    public byte getTargetVM() {
        if (!parsed) {
            rlpParse();
        }
        return this.type;
    }

    public ISignature getSignature() {
        if (!parsed) {
            rlpParse();
        }
        return signature;
    }

    public Address getContractAddress() {
        if (!this.isContractCreationTransaction()) {
            return null;
        }

        Address from = this.getSenderAddress();

        if (from == null) {
            return null;
        }

        try {
            return Address.wrap(HashUtil.calcNewAddr(from.toBytes(), this.getNonce()));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean isContractCreationTransaction() {
        if (!parsed) {
            rlpParse();
        }

        return this.to == null;
    }

    @Override
    public synchronized Address getSenderAddress() {
        if (from != null) {
            return this.from;
        }

        if (!parsed) {
            rlpParse();
        }

        if (this.signature == null) {
            LOG.error("no signature!");
            return null;
        }

        try {
            from = Address.wrap(this.signature.getAddress());
            return from;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public void sign(ECKey key) throws MissingPrivateKeyException {
        this.timeStamp = ByteUtil.longToBytes(TimeInstant.now().toEpochMicro());
        this.signature = key.sign(this.getRawHash());
        this.rlpEncoded = null;
    }

    @Override
    public String toString() {
        return toString(Integer.MAX_VALUE);
    }

    public String toString(int maxDataSize) {
        if (!parsed) {

            rlpParse();
        }
        String dataS;
        if (data == null) {
            dataS = "";
        } else if (data.length < maxDataSize) {
            dataS = ByteUtil.toHexString(data);
        } else {
            dataS =
                    ByteUtil.toHexString(Arrays.copyOfRange(data, 0, maxDataSize))
                            + "... ("
                            + data.length
                            + " bytes)";
        }
        return "TransactionData ["
                + "hash="
                + ByteUtil.toHexString(hash)
                + ", nonce="
                + (nonce == null ? "null" : new BigInteger(1, nonce))
                + ", receiveAddress="
                + (to == null ? "null" : to.toString())
                + ", value="
                + (value == null ? "null" : new BigInteger(1, value))
                + ", data="
                + dataS
                + ", timeStamp="
                + ByteUtil.byteArrayToLong(timeStamp)
                + ", Nrg="
                + this.nrg
                + ", NrgPrice="
                + this.nrgPrice
                + ", txType="
                + this.type
                + ", sig="
                + ((signature == null) ? "null" : signature.toString())
                + "]";
    }

    /** For signatures you have to keep also RLP of the transaction without any signature data */
    public byte[] getEncodedRaw() {

        if (!parsed) {
            rlpParse();
        }
        if (rlpRaw != null) {
            return rlpRaw;
        }

        byte[] nonce = RLP.encodeElement(this.nonce);

        byte[] to;
        if (this.to == null) {
            to = RLP.encodeElement(null);
        } else {
            to = RLP.encodeElement(this.to.toBytes());
        }

        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);
        byte[] timeStamp = RLP.encodeElement(this.timeStamp);
        byte[] nrg = RLP.encodeLong(this.nrg);
        byte[] nrgPrice = RLP.encodeLong(this.nrgPrice);
        byte[] type = RLP.encodeByte(this.type);

        rlpRaw = RLP.encodeList(nonce, to, value, data, timeStamp, nrg, nrgPrice, type);
        return rlpRaw;
    }

    public byte[] getEncoded() {

        if (rlpEncoded != null) {
            return rlpEncoded;
        }

        byte[] nonce = RLP.encodeElement(this.nonce);

        byte[] to;
        if (this.to == null) {
            to = RLP.encodeElement(null);
        } else {
            to = RLP.encodeElement(this.to.toBytes());
        }

        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);
        byte[] timeStamp = RLP.encodeElement(this.timeStamp);
        byte[] nrg = RLP.encodeLong(this.nrg);
        byte[] nrgPrice = RLP.encodeLong(this.nrgPrice);
        byte[] type = RLP.encodeByte(this.type);

        byte[] sigs;

        if (signature == null) {
            if (!isGCTransaction()) {
                LOG.error("Encoded transaction has no signature!");
                return null;
            } else {
                sigs = RLP.encodeElement(null);
            }
        } else {
            sigs = RLP.encodeElement(signature.toBytes());
        }

        this.rlpEncoded =
                RLP.encodeList(nonce, to, value, data, timeStamp, nrg, nrgPrice, type, sigs);
        this.hash = this.getTransactionHash();

        return rlpEncoded;
    }

    /**
     * @implNote the GC transaction only has `to` and `timestamp` data field
     * @return the boolean value when the transaction is a GC transaction.
     */
    public boolean isGCTransaction() {
        return getNonce() == ZERO_BYTE_ARRAY
                && getValue() == ZERO_BYTE_ARRAY
                && getEnergyLimit() == 0
                && getEnergyPrice() == 0
                && getTargetVM() == TransactionTypes.AVM_CREATE_CODE
                && from == null
                && data == null
                && to != null
                && timeStamp != null;
    }

    @Override
    public int hashCode() {

        byte[] hash = this.getTransactionHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof AionTransaction)) {
            return false;
        }
        AionTransaction tx = (AionTransaction) obj;

        return tx.hashCode() == this.hashCode();
    }

    public static AionTransaction createDefault(
            String to, BigInteger amount, BigInteger nonce, long nrg, long nrgPrice)
            throws Exception {
        return create(to, amount, nonce, nrg, nrgPrice);
    }

    public static AionTransaction create(
            String to, BigInteger amount, BigInteger nonce, long nrg, long nrgPrice)
            throws Exception {
        return new AionTransaction(
                nonce.toByteArray(), Address.wrap(to), amount.toByteArray(), null, nrg, nrgPrice);
    }

    @Override
    public void setEncoded(byte[] _encodedData) {
        this.rlpEncoded = _encodedData;
        parsed = false;
    }

    public DataWordImpl nrgPrice() {
        return new DataWordImpl(this.nrgPrice);
    }

    public long nrgLimit() {
        return new DataWordImpl(this.nrg).longValue();
    }

    @Override
    public long getTransactionCost() {
        return transactionCost(0);
    }

    public long transactionCost(long blockNumber) {
        long nonZeroes = nonZeroBytesInData();
        long zeroes = zeroBytesInData();

        return (this.isContractCreationTransaction() ? Constants.NRG_CREATE_CONTRACT_MIN : 0)
                + Constants.NRG_TRANSACTION_MIN
                + zeroes * Constants.NRG_TX_DATA_ZERO
                + nonZeroes * Constants.NRG_TX_DATA_NONZERO;
    }

    public long nonZeroBytesInData() {
        int total = (data == null) ? 0 : data.length;

        return total - zeroBytesInData();
    }

    public long zeroBytesInData() {
        if (data == null) {
            return 0;
        }

        int c = 0;
        for (byte b : data) {
            c += (b == 0) ? 1 : 0;
        }
        return c;
    }

    public void setTxIndexInBlock(long idx) {
        this.txIndexInBlock = idx;
    }

    public void setBlockNumber(long blkNr) {
        this.blockNumber = blkNr;
    }

    public void setBlockHash(byte[] hash) {
        this.blockHash = hash;
    }

    public long getTxIndexInBlock() {
        return this.txIndexInBlock;
    }

    public long getBlockNumber() {
        return this.blockNumber;
    }

    public byte[] getBlockHash() {
        return this.blockHash;
    }

    public long getNrgConsume() {
        return this.nrgConsume;
    }

    @Override
    public void setNrgConsume(long consume) {
        this.nrgConsume = consume;
    }
}
