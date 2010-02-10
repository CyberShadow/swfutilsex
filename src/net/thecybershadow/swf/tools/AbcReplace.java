// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import flash.swf.TagDecoder;
import flash.swf.TagEncoder;
import flash.swf.tags.DoABC;
import flash.util.FileUtils;

public class AbcReplace extends TagEncoder
{
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2)
		{
			System.err.println("Replaces contents of DoABC tags in a SWF file.");
			System.err.println("Usage: AbcReplace input.swf abc1.abc ... abcN.abc output.swf");
			return;
		}

		URL inurl = FileUtils.toURL(new File(args[0]));
		InputStream in = inurl.openStream();
		byte[][] abc = new byte[args.length - 2][];
		for (int i = 0; i < args.length - 2; i++)
		{
			File file = new File(args[i + 1]);
			InputStream abcin = FileUtils.toURL(file).openStream();
			abc[i] = new byte[(int) file.length()];
			abcin.read(abc[i]);
		}
		AbcReplace abcreplace = new AbcReplace(abc);
		new TagDecoder(in, inurl).parse(abcreplace);
		abcreplace.finish();
		FileOutputStream out = new FileOutputStream(args[args.length - 1]);
		out.write(abcreplace.toByteArray());
	}

	int counter = 0;
	String prefix;
	byte[][] abc;

	public AbcReplace(byte[][] abc)
	{
		this.abc = abc;
	}

	@Override
	public void doABC(DoABC tag)
	{
		if (counter < abc.length)
		{
			tag.abc = this.abc[counter];
			super.doABC(tag);
		}
		counter++;
	}

	@Override
	public void finish()
	{
		if (counter > abc.length)
			System.err.println("Warning: more ABC tags in SWF file than specified in command line."
					+ "Extra DoABC tags deleted.");
		if (counter < abc.length)
			System.err.println("Warning: less ABC tags in SWF file than specified in command line."
					+ "Extra .abc files omitted.");
	}
}
