// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class AbcStrReplace extends AbcProcessor
{
	public static void main(String[] args) throws IOException
	{
		if (args.length < 4 || args.length % 2 != 0)
		{
			System.err.println("Usage: AbcStrReplace input.abc \"from1\" \"to1\" ... \"fromN\" \"toN\"output.abc");
			return;
		}

		File fin = new File(args[0]);
		FileInputStream in = new FileInputStream(fin);
		byte[] abc = new byte[(int) fin.length()];
		in.read(abc); // FIXME
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 1; i < args.length - 1; i += 2)
			map.put(args[i], args[i + 1]);
		AbcStrReplace d = new AbcStrReplace(abc, map);
		d.process();
		FileOutputStream out = new FileOutputStream(args[args.length - 1]);
		out.write(d.toByteArray());
		out.close();
	}

	private final HashMap<String, String> map;

	public AbcStrReplace(byte[] abc, HashMap<String, String> map)
	{
		super(abc);
		this.map = map;
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
			if (map.containsKey(s))
				s = map.get(s);
			writeU32(s.length());
			writeUTFBytes(s);
			stringConstants[i] = s;
		}
		discardInput();
	}
}
