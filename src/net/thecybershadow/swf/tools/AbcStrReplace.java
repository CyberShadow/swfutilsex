// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class AbcStrReplace extends AbcProcessor
{
	public static void main(String[] args) throws IOException
	{
		if (args.length != 3)
		{
			System.err.println("Usage: AbcStrReplace input.abc patterns.txt output.abc");
			return;
		}

		File fin = new File(args[0]);
		FileInputStream in = new FileInputStream(fin);
		byte[] abc = new byte[(int) fin.length()];
		in.read(abc); // FIXME

		BufferedReader fp = new BufferedReader(new FileReader(args[1]));
		String s;
		Vector<String> pats = new Vector<String>();
		while ((s = fp.readLine()) != null)
			pats.add(s);
		if (pats.size() % 2 != 0)
		{
			System.err.println("Un-even number of lines in input file");
			return;
		}
		String[] pata = new String[pats.size()];
		pats.toArray(pata);
		AbcStrReplace d = new AbcStrReplace(abc, pata);
		d.process();
		FileOutputStream out = new FileOutputStream(args[2]);
		out.write(d.toByteArray());
		out.close();
	}

	private final String[] patterns;

	public AbcStrReplace(byte[] abc, String[] patterns)
	{
		super(abc);
		this.patterns = patterns;
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
			for (int j = 0; j < patterns.length; j += 2)
				if (s.matches(patterns[j]))
				{
					s = s.replaceAll(patterns[j], patterns[j + 1]);
					break;
				}
			writeU32(s.length());
			writeUTFBytes(s);
			stringConstants[i] = s;
		}
		discardInput();
	}
}
