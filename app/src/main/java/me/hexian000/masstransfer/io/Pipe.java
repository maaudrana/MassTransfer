package me.hexian000.masstransfer.io;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

public class Pipe implements Reader, Writer {
	private int limit;
	private Semaphore capacity;
	private BlockingQueue<byte[]> q;
	private byte[] current = null;
	private int offset = 0;
	private boolean closed = false;

	public Pipe(int capacity) {
		q = new LinkedBlockingDeque<>();
		limit = capacity;
		this.capacity = new Semaphore(capacity);
	}

	public int getSize() {
		return limit - capacity.availablePermits();
	}

	@Override
	public synchronized int read(byte[] buffer) throws InterruptedException {
		if (closed) {
			return -1;
		}
		int read = 0;
		while (read < buffer.length) {
			if (current == null) {
				current = q.take();
				if (current.length == 0) {
					return -1;
				}
			} else {
				int count = Math.min(current.length - offset, buffer.length - read);
				System.arraycopy(current, offset, buffer, read, count);
				offset += count;
				read += count;
				if (offset == current.length) {
					current = null;
					offset = 0;
				}
			}
		}
		capacity.release(read);
		return read;
	}

	@Override
	public void write(byte[] buffer) throws InterruptedException {
		if (buffer.length > 0) {
			capacity.acquire(buffer.length);
			q.put(buffer);
		}
	}

	@Override
	public void close() {
		closed = true;
		try {
			q.put(new byte[0]);
		} catch (InterruptedException ignored) {
		}
	}
}