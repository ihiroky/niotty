package net.ihiroky.niotty.nio.unix;

import com.sun.jna.ptr.IntByReference;

/**
 *
 */
class AddressBuffer {

    private final Native.SockAddrUn address_;
    private final IntByReference size_;

    private AddressBuffer() {
        address_ = new Native.SockAddrUn();
        size_ = new IntByReference();
    }

    private static final ThreadLocal<AddressBuffer> INSTANCE = new ThreadLocal<AddressBuffer>() {
        @Override
        protected AddressBuffer initialValue() {
            return new AddressBuffer();
        }
    };

    public static AddressBuffer getInstance() {
        return INSTANCE.get();
    }

    public Native.SockAddrUn getAddress() {
        return address_;
    }

    public IntByReference getSize() {
        return size_;
    }
}
