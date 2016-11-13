package net.jonathan.emulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class StorageController
{

	private File storageFile;
	private short storageSizeMiB;

	private FileChannel channel;
	private long currentAddress;

	public StorageController(File storageFile)
	{
		this.storageFile = storageFile;

		storageSizeMiB = (short) (storageFile.length() / (1024 * 1024));

		try
		{
			channel = new FileInputStream(storageFile).getChannel();
		} catch (FileNotFoundException e)
		{
			System.err.println("Error opening storage file");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void setAddress(long address)
	{
		currentAddress = address;
	}

	public short getStorageSizeMiB()
	{
		return storageSizeMiB;
	}

	public short read()
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);

		try
		{
			channel.read(buffer, currentAddress);
		} catch (IOException e)
		{
			System.err.println("Error reading storage file");
			e.printStackTrace();
			System.exit(1);
		}

		return buffer.getShort();
	}

	public void write(short data)
	{
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(data);

		try
		{
			channel.write(buffer, currentAddress);
		} catch (IOException e)
		{
			System.err.println("Error writing storage file");
			e.printStackTrace();
			System.exit(1);
		}
	}

}
