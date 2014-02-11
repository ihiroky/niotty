package net.ihiroky.niotty.nio.local;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Provides the interface to access the native codes with JNA.
 */
public class Native {

    static {
        com.sun.jna.Native.register("c");
    }


    /*======================================================================
     * /usr/include/x86_64-linux-gnu/bits/socket.h
     *======================================================================*/

    static final int AF_UNIX = 1;
    static final int SOCK_STREAM = 1;
    static final int SOCK_DGRAM = 2;
    static final int PROTOCOL = 0;

    static final int SHUT_RD = 0;
    static final int SHUT_WR = 1;
    static final int SHUT_RDWR = 2;

    public static class SockAddrUn extends Structure {
        public final static int UNIX_PATH_MAX = 108;
        public final static byte[] ZERO_BYTE = new byte[] {0};

        public short sun_family_ = AF_UNIX;
        public byte[] sun_path_ = new byte[UNIX_PATH_MAX];

        public SockAddrUn() {
        }

        public SockAddrUn(String path) {
            setSunPath(path);
        }

        public void setSunPath(String sunPath) {
            System.arraycopy(sunPath.getBytes(), 0, sun_path_, 0, sunPath.length());
            System.arraycopy(ZERO_BYTE, 0, sun_path_, sunPath.length(), 1);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("sun_family_", "sun_path_");
        }
    }

    static native int socket(int domain, int type, int protocol);
    static native int socketpair(int domain, int type, int protocol, int[] sv);
    static native int bind(int fd, SockAddrUn sockaddr, int addrlen);
    static native int connect(int fd, SockAddrUn sockaddr, int addrlen);
    static native int listen(int fd, int backlog);
    static native int accept(int fd, SockAddrUn sockaddr, IntByReference addrlen);
    static native int shutdown(int sockfd, int how);

    static native int getsockname(int fd, SockAddrUn addr, IntByReference addrlen);
    static native int getpeername(int fd, SockAddrUn addr, IntByReference addrlen);
    static native int getsockopt(int fd, int level, int optname, ByteBuffer optval, IntByReference optlen);
    static native int setsockopt(int fd, int level, int optname, ByteBuffer optval, int optlen);
    static native int recvfrom(int fd, ByteBuffer buf, int len, int flags, SockAddrUn from, IntByReference fromlen);
    static native int sendto(int fd, ByteBuffer buf, int len, int flags, SockAddrUn to, int tolen);


    /*======================================================================
     * /usr/include/unistd.h
     *======================================================================*/
    static native int read(int fd, ByteBuffer buffer, int count);
    static native int write(int fd, ByteBuffer buffer, int count);
    static native int close(int fd);
    static native int pread(int fd, ByteBuffer buffer, int count, int offset);
    static native int pwrite(int fd, ByteBuffer buffer, int count, int offset);


    /*======================================================================
     * /usr/include/asm-generic/socket.h
     *======================================================================*/

    static final int SOL_SOCKET = 1;
    static final int SO_SNDBUF = 7;
    static final int SO_RCVBUF = 8;
    static final int SO_PASSCRED = 16;


    /*======================================================================
     * /usr/include/x86_64-linux-gnu/bits/uio.h
     * /usr/include/x86_64-linux-gnu/sys/uio.h
     *======================================================================*/

    static int IOV_MAX = 1024;

    //    44 struct iovec
//            45   {
//        46     void *iov_base;»/* Pointer to data.  */
//        47     size_t iov_len;»/* Length of data.  */
//        48   };↲

    public static class IOVec extends Structure {

        public ByteBuffer iovBase_;
        public int iovLen_;

        public IOVec() {
        }

        public IOVec(ByteBuffer iovBase, int iovLen) {
            iovBase_ = iovBase;
            iovLen_ = iovLen;
        }

        public IOVec(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("iovBase_", "iovLen_");
        }

        public static class ByReference extends IOVec implements Structure.ByReference {
            public ByReference(ByteBuffer iovBase, int iovLen) {
                super(iovBase, iovLen);
            }
        }
    }

    // extern ssize_t readv (int __fd, __const struct iovec *__iovec, int __count)
    // extern ssize_t writev (int __fd, __const struct iovec *__iovec, int __count)
    static native long readv(int fd, Pointer iovec, int count);
    static native long writev(int fd, Pointer iovec, int count);

    /*======================================================================
     * /usr/include/x86_64-linux-gnu/sys/epoll.h
     *======================================================================*/

    static final int EPOLL_CTL_ADD = 1;
    static final int EPOLL_CTL_DEL = 2;
    static final int EPOLL_CTL_MOD = 3;

    static int EPOLLIN = 0x001;
    static int EPOLLOUT = 0x004;
    static int EPOLLONESHOT = 1 << 30;
    static int EPOLLET = 1 << 31;

    public static class EPollDataT extends Union {
        public Pointer ptr_;
        public int fd_;
        public int u32_;
        public long u64_;

        public EPollDataT() {
            super();
        }

        public EPollDataT(int fd_or_u32) {
            super();
            u32_ = fd_ = fd_or_u32;
            setType(Integer.TYPE);
        }

        public EPollDataT(long u64) {
            super();
            u64_ = u64;
            setType(Long.TYPE);
        }

        public EPollDataT(Pointer ptr) {
            super();
            ptr_ = ptr;
            setType(Pointer.class);
        }

        public static class ByReference extends EPollDataT implements Structure.ByReference {
        }

        public static class ByValue extends EPollDataT implements Structure.ByValue {
        }
    }

    public static class EPollEvent extends Structure {
        public int events_;        //Epoll events
        public EPollDataT data_; //User data variable

        protected List getFieldOrder() {
            return Arrays.asList("events_", "data_");
        }

        public EPollEvent() {
            super(Structure.ALIGN_NONE);
        }

        public EPollEvent(Pointer p) {
            super(p, Structure.ALIGN_NONE);
            setAlignType(Structure.ALIGN_NONE);
            read();
        }

        public EPollEvent(int events, EPollDataT data) {
            super(Structure.ALIGN_NONE);
            events_ = events;
            data_ = data;
        }

        public void reuse(Pointer p, int offset) {
            useMemory(p, offset);
            read();
        }

        public static class ByReference extends EPollEvent implements Structure.ByReference {
            public ByReference() {
                super();
            }

            public ByReference(int fd, int events) {
                super();
                events_ = events;
                data_.fd_ = fd;
                data_.setType(Integer.TYPE);
            }

            public ByReference(Pointer ptr, int events) {
                super();
                events_ = events;
                data_.ptr_ = ptr;
                data_.setType(Pointer.class);
            }

            public EPollEvent.ByReference oneshot() {
                events_ = events_ | EPOLLONESHOT;
                return this;
            }
        }

        public static class ByValue extends EPollEvent implements Structure.ByValue {
        }
    }

    static native int epoll_create(int size);
    static native int epoll_ctl(int epfd, int op, int fd, EPollEvent.ByReference event);
    static native int epoll_wait(int epfd, Pointer events, int maxevents, int timeout);


    /*======================================================================
     * /usr/include/x86_64-linux-gnu/sys/eventfd.h
     *======================================================================*/

    static int EFD_NONBLOCK = 04000;

    static native int eventfd(int count, int flags);
    static native int eventfd_read(int fd, LongByReference value);
    static native int eventfd_write(int fd, long value);


    /*======================================================================
     * /usr/include/asm-generic/fcntl.h
     *======================================================================*/

    static final int O_NONBLOCK = 04000;
    static final int F_GETFL = 3;
    static final int F_SETFL = 4;

    static native int fcntl(int fd, int cmd, int value);


    /*======================================================================
     * /usr/include/string.h
     *======================================================================*/
    static native String strerror(int errno);

    static String getLastError() {
        return strerror(com.sun.jna.Native.getLastError());
    }


    /*======================================================================
     * /usr/include/stdio.h
     *======================================================================*/
    static native void perror(String s);


    public static void main(String[] args) throws Exception {
        int sd = socket(AF_UNIX, SOCK_STREAM, PROTOCOL);
        if (sd < 0) {
            exit("Failed to create server socket.", 1);
        }

        File file = new File("/tmp/unix-domain-socket");
        SockAddrUn ssa = new SockAddrUn();
        ssa.clear();
        ssa.sun_family_ = AF_UNIX;
        ssa.setSunPath(file.getAbsolutePath());

        file.delete();

        if (bind(sd, ssa, ssa.size()) < 0) {
            close(sd);
            perror("bind");
            exit("Failed to bind server socket.", 1);
        }
        System.out.println("Bind.");
        if (listen(sd, 10) < 0) {
            perror("listen");
            close(sd);
            exit("Failed to listen on server socket.", 1);
        }
        System.out.println("Listen.");

        EPollEvent.ByReference ev = new EPollEvent.ByReference(sd, EPOLLIN);
        EPollEvent eventsHead = new EPollEvent();
        EPollEvent[] events = (EPollEvent[]) eventsHead.toArray(32);

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int epfd = epoll_create(events.length);
        if (epfd < 0) {
            throw new Exception("Failed to create epoll.");
        }
        System.out.printf("Create epoll: %d\n", epfd);

        epoll_ctl(epfd, EPOLL_CTL_ADD, sd, ev);
        System.out.printf("Register socket %d to epoll %d.\n", sd, epfd);

        for (;;) {
            int nfd = epoll_wait(epfd, eventsHead.getPointer(), events.length, -1);
            System.out.printf("Found %d events.\n", nfd);
            for (int i = 0; i < nfd; i++) {
                EPollEvent cev = new EPollEvent(events[i].getPointer());
                System.out.printf("fd_:%d\n", cev.data_.fd_);
                if (cev.data_.fd_ == sd) {
                    SockAddrUn caddr = new SockAddrUn();
                    IntByReference caddrLength = new IntByReference();
                    int cd = accept(sd, caddr, caddrLength);
                    System.out.printf("Accept socket %d\n", cd);
                    if (cd < 0) {
                        perror("accept");
                        System.out.println("Failed to accept.");
                        continue;
                    }
                    int flag = fcntl(cd, F_GETFL, 0);
                    fcntl(cd, F_SETFL, flag | O_NONBLOCK);
                    ev.clear();
                    ev.events_ = EPOLLIN | EPOLLET;
                    ev.data_.fd_ = cd;
                    epoll_ctl(epfd, EPOLL_CTL_ADD, cd, ev);
                    System.out.printf("Register socket %d to epoll %d\n", cd, epfd);
                } else {
                    int cd = cev.data_.fd_;
                    System.out.printf("Event for client %d\n", cd);
                    int n = read(cd, buffer, buffer.capacity());
                    if (n < 0) {
                        epoll_ctl(epfd, EPOLL_CTL_DEL, cd, ev);
                        close(cd);
                        System.out.printf("Client %d is closed with %d.\n", cd, n);
                    } else if (n == 0) {
                        epoll_ctl(epfd, EPOLL_CTL_DEL, cd, ev);
                        close(cd);
                        System.out.printf("Client %d is closed.\n", cd);
                    } else {
                        // position adn limit is not updated by jna.
                        System.out.println(n + ", " + buffer);
                        buffer.limit(n);
                        write(cd, buffer, n);
                    }
                    buffer.clear();
                }
            }
        }
    }

    private static void exit(String msg, int code) {
        System.out.println(msg);
        System.exit(code);
    }
}
