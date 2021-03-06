package org.aion.base.type;

import org.aion.vm.api.interfaces.Address;

/** @author jay */
public interface IBlockHeader {

    // Getter
    byte[] getParentHash();

    byte[] getStateRoot();

    byte[] getTxTrieRoot();

    byte[] getReceiptsRoot();

    byte[] getLogsBloom();

    byte[] getExtraData();

    byte[] getNonce();

    byte[] getHash();

    byte[] getEncoded();

    Address getCoinbase();

    long getTimestamp();

    long getNumber();

    // Setter
    void setCoinbase(Address _cb);

    void setStateRoot(byte[] _strt);

    void setReceiptsRoot(byte[] _rcrt);

    void setTransactionsRoot(byte[] _txrt);

    void setTimestamp(long _ts);

    void setNumber(long _nb);

    void setNonce(byte[] _nc);

    void setLogsBloom(byte[] _lb);

    void setExtraData(byte[] _ed);

    boolean isGenesis();
}
