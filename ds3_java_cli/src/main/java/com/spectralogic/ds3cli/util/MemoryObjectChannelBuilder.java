/*
 * *****************************************************************************
 *   Copyright 2014-2016 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ***************************************************************************
 */

package com.spectralogic.ds3cli.util;

import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Random;

/**
 * THIS CLASS DISCARDS ALL INCOMING RESTORE DATA!
 * Good for testing, bad for general customer applications.
 *
 * Reading from this provides all 1's
 */
public class MemoryObjectChannelBuilder implements Ds3ClientHelpers.ObjectChannelBuilder {
    private final int bufferSize;
    private final long sizeOfFiles;

    public final static int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    public final static long DEFAULT_FILE_SIZE = 1024L;

    public MemoryObjectChannelBuilder() {
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.sizeOfFiles = DEFAULT_FILE_SIZE;
    }

    public MemoryObjectChannelBuilder(final int bufferSize, final long sizeOfFile) {
        this.bufferSize = bufferSize;
        this.sizeOfFiles = sizeOfFile;
    }

    @Override
    public SeekableByteChannel buildChannel(final String key) throws IOException {
        return new DevNullByteChannel(this.bufferSize, this.sizeOfFiles);
    }

    private class DevNullByteChannel implements SeekableByteChannel {
        final private int bufferSize;
        private byte[] backingArray;
        final private long limit;
        private int position;
        private boolean isOpen;
        private final Random random;

        public DevNullByteChannel(final int bufferSize, final long size) {
            this.bufferSize = bufferSize;
            backingArray = new byte[bufferSize];
            random = new Random(System.currentTimeMillis());
            random.nextBytes(backingArray);
            this.position = 0;
            this.limit = size * 1024L * 1024L;
            this.isOpen = true;
        }

        // throw a new random chunk in the buffer
        private void randomize() {
            final int len = bufferSize / 10;
            final byte[] newbytes =  new byte[len];
            final int pos = random.nextInt(len * 9);
            System.arraycopy(newbytes, 0, this.backingArray, pos, len);
        }

        public boolean isOpen() {
            return this.isOpen;
        }

        public void close() throws IOException {
            this.isOpen = false;
        }

        public int read(final ByteBuffer dst) throws IOException {
            final int amountToRead = Math.min(dst.remaining(), this.bufferSize);
            dst.put(this.backingArray, 0, amountToRead);
            return amountToRead;
        }

        public int write(final ByteBuffer src) throws IOException {
            this.randomize();
            final int amountToWrite = Math.min(src.remaining(), this.bufferSize);
            src.get(this.backingArray, 0, amountToWrite);
            return amountToWrite;
        }

        public long position() throws IOException {
            return (long) this.position;
        }

        public SeekableByteChannel position(final long newPosition) throws IOException {
            this.position = (int) newPosition;
            return this;
        }

        public long size() throws IOException {
            return this.limit;
        }

        public SeekableByteChannel truncate(final long size) throws IOException {
            return this;
        }
    }
}
