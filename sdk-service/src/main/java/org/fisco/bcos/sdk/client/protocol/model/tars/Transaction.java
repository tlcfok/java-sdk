// **********************************************************************
// This file was generated by a TARS parser!
// TARS version 1.7.2.
// **********************************************************************

package org.fisco.bcos.sdk.client.protocol.model.tars;

import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.annotation.TarsStruct;
import com.qq.tars.protocol.tars.annotation.TarsStructProperty;
import com.qq.tars.protocol.util.TarsUtil;

@TarsStruct
public class Transaction {

    @TarsStructProperty(order = 1, isRequire = false)
    public TransactionData data = null;

    @TarsStructProperty(order = 2, isRequire = false)
    public byte[] dataHash = null;

    @TarsStructProperty(order = 3, isRequire = false)
    public byte[] signature = null;

    @TarsStructProperty(order = 4, isRequire = false)
    public long importTime = 0L;

    public TransactionData getData() {
        return this.data;
    }

    public void setData(TransactionData data) {
        this.data = data;
    }

    public byte[] getDataHash() {
        return this.dataHash;
    }

    public void setDataHash(byte[] dataHash) {
        this.dataHash = dataHash;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public long getImportTime() {
        return this.importTime;
    }

    public void setImportTime(long importTime) {
        this.importTime = importTime;
    }

    public Transaction() {}

    public Transaction(TransactionData data, byte[] dataHash, byte[] signature, long importTime) {
        this.data = data;
        this.dataHash = dataHash;
        this.signature = signature;
        this.importTime = importTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + TarsUtil.hashCode(this.data);
        result = prime * result + TarsUtil.hashCode(this.dataHash);
        result = prime * result + TarsUtil.hashCode(this.signature);
        result = prime * result + TarsUtil.hashCode(this.importTime);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction) obj;
        return (TarsUtil.equals(this.data, other.data)
                && TarsUtil.equals(this.dataHash, other.dataHash)
                && TarsUtil.equals(this.signature, other.signature)
                && TarsUtil.equals(this.importTime, other.importTime));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Transaction(");
        sb.append("data:");
        if (this.data == null) {
            sb.append("null");
        } else {
            sb.append(this.data);
        }
        sb.append(", ");
        sb.append("dataHash:");
        if (this.dataHash == null) {
            sb.append("null");
        } else {
            sb.append(this.dataHash);
        }
        sb.append(", ");
        sb.append("signature:");
        if (this.signature == null) {
            sb.append("null");
        } else {
            sb.append(this.signature);
        }
        sb.append(", ");
        sb.append("importTime:");
        sb.append(this.importTime);
        sb.append(")");
        return sb.toString();
    }

    public void writeTo(TarsOutputStream _os) {
        if (null != this.data) {
            _os.write(this.data, 1);
        }
        if (null != this.dataHash) {
            _os.write(this.dataHash, 2);
        }
        if (null != this.signature) {
            _os.write(this.signature, 3);
        }
        _os.write(this.importTime, 4);
    }

    static TransactionData cache_data;

    static {
        cache_data = new TransactionData();
    }

    static byte[] cache_dataHash;

    static {
        cache_dataHash = new byte[1];
        byte var_12 = (byte) 0;
        cache_dataHash[0] = var_12;
    }

    static byte[] cache_signature;

    static {
        cache_signature = new byte[1];
        byte var_13 = (byte) 0;
        cache_signature[0] = var_13;
    }

    public void readFrom(TarsInputStream _is) {
        this.data = (TransactionData) _is.read(cache_data, 1, false);
        this.dataHash = (byte[]) _is.read(cache_dataHash, 2, false);
        this.signature = (byte[]) _is.read(cache_signature, 3, false);
        this.importTime = _is.read(this.importTime, 4, false);
    }
}