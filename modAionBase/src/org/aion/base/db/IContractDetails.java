package org.aion.base.db;

import java.util.Collection;
import java.util.Map;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.vm.api.interfaces.Address;

public interface IContractDetails {

    /**
     * Inserts a key-value pair containing the given key and the given value.
     *
     * @param key the key to be inserted
     * @param value the value to be inserted
     */
    void put(ByteArrayWrapper key, ByteArrayWrapper value);

    /**
     * Deletes any key-value pair that matches the given key.
     *
     * @param key the key to be deleted
     */
    void delete(ByteArrayWrapper key);

    /**
     * Returns the value associated with key.
     *
     * @implNote Some implementations may handle a non-existent key-value pair differently.
     * @param key The key to query.
     * @return The associated value or some non-value indicator in the case of no such key-value
     *     pair.
     */
    ByteArrayWrapper get(ByteArrayWrapper key);

    /**
     * Returns the code of the address associated with this IContractDetails class. This is for
     * addresses that are smart contracts.
     *
     * @return the code of the associated address.
     */
    byte[] getCode();

    /**
     * Returns the code whose hash is codeHash.
     *
     * @param codeHash The hashed code.
     * @return the code.
     */
    byte[] getCode(byte[] codeHash);

    /**
     * Sets the code of the associated address to code.
     *
     * @param code The code to set.
     */
    void setCode(byte[] code);

    /**
     * Returns the storage hash.
     *
     * @return the storage hash.
     */
    byte[] getStorageHash();

    /**
     * Decodes an IContractDetails object from the RLP encoding rlpCode.
     *
     * @implNote Implementing classes may not necessarily support this method.
     * @param rlpCode The encoding to decode.
     */
    void decode(byte[] rlpCode);

    /**
     * Decodes an IContractDetails object from the RLP encoding rlpCode including the fast check
     * optional.
     *
     * @implNote Implementing classes may not necessarily support this method.
     * @param rlpCode The encoding to decode.
     * @param fastCheck fast check does the contractDetails needs syncing with external storage
     */
    void decode(byte[] rlpCode, boolean fastCheck);

    /**
     * Sets the dirty value to dirty.
     *
     * @param dirty The dirty value.
     */
    void setDirty(boolean dirty);

    /**
     * Sets the deleted value to deleted.
     *
     * @param deleted the deleted value.
     */
    void setDeleted(boolean deleted);

    /**
     * Returns true iff the IContractDetails is dirty.
     *
     * @return only if this is dirty.
     */
    boolean isDirty();

    /**
     * Returns true iff the IContractDetails is deleted.
     *
     * @return only if this is deleted.
     */
    boolean isDeleted();

    /**
     * Returns an rlp encoding of this IContractDetails object.
     *
     * @implNote Implementing classes may not necessarily support this method.
     * @return an rlp encoding of this.
     */
    byte[] getEncoded();

    /**
     * Returns a mapping of all the key-value pairs that have keys in the given collection keys.
     *
     * @param keys the keys to query for
     * @return the associated mappings
     */
    Map<ByteArrayWrapper, ByteArrayWrapper> getStorage(Collection<ByteArrayWrapper> keys);

    /**
     * Sets the storage to contain the specified key-value mappings.
     *
     * @param storage the specified mappings
     * @apiNote Used for testing.
     * @implNote A {@code null} value is interpreted as deletion.
     */
    void setStorage(Map<ByteArrayWrapper, ByteArrayWrapper> storage);

    /**
     * Get the address associated with this IContractDetails.
     *
     * @return the associated address.
     */
    Address getAddress();

    /**
     * Sets the associated address to address.
     *
     * @param address The address to set.
     */
    void setAddress(Address address);

    /**
     * Returns a string representation of this IContractDetails.
     *
     * @return a string representation.
     */
    String toString();

    /** Syncs the storage trie. */
    void syncStorage();

    /**
     * Returns an IContractDetails object pertaining to a specific point in time given by the root
     * hash hash.
     *
     * @implNote Implementing classes may not necessarily support this method.
     * @param hash The root hash to search for.
     * @return the specified IContractDetails.
     */
    IContractDetails getSnapshotTo(byte[] hash);

    /**
     * Sets the data source to dataSource.
     *
     * @implNote Implementing classes may not necessarily support this method.
     * @param dataSource The new dataSource.
     */
    void setDataSource(IByteArrayKeyValueStore dataSource);

    /**
     * Returns a sufficiently deep copy of this object. It is up to all implementations of this
     * method to declare which original object references are in fact leaked by this copy, if any,
     * and to provide justification of why, despite this, the copy is nonetheless sufficiently deep.
     *
     * @return A copy of this object.
     */
    IContractDetails copy();
}
