// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AbcStrReplace extends AbcProcessor
{
	public static void main(String[] args) throws IOException
	{
		if (args.length != 4)
		{
			System.err.println("Usage: AbcStrReplace input.abc \"from\" \"to\" output.abc");
			return;
		}

		File fin = new File(args[0]);
		FileInputStream in = new FileInputStream(fin);
		byte[] abc = new byte[(int) fin.length()];
		in.read(abc); // FIXME
		AbcStrReplace d = new AbcStrReplace(abc, args[1], args[2]);
		d.process();
		FileOutputStream out = new FileOutputStream(args[3]);
		out.write(d.toByteArray());
		out.close();
	}

	private final String from, to;

	public AbcStrReplace(byte[] abc, String from, String to)
	{
		super(abc);
		this.from = from;
		this.to = to;
	}

	@Override
	protected void readStringConstantPool()
	{
		long n = readU32();
		stringConstants = new String[(n > 0) ? (int) n : 1];
		stringConstants[0] = "";
		copyInput();
		for (int i = 1; i < n; i++)
		{
			String s = readUTFBytes(readU32());
			if (s == from)
				s = to;
			writeU32(s.length());
			writeUTFBytes(s);
			stringConstants[i] = s;
		}
		discardInput();
	}
}
