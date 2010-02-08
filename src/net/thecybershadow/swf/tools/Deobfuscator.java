// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import flash.swf.Tag;
import flash.swf.TagDecoder;
import flash.swf.TagEncoder;
import flash.swf.tags.DoABC;
import flash.swf.tags.SymbolClass;
import flash.util.FileUtils;

public class Deobfuscator extends TagEncoder {
	DeobfuscationParameters params = new DeobfuscationParameters();
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("Usage: Deobfuscator [-noreorder] [-dict dictionary.txt] input.swf output.swf");
			return;
		}
			
		Deobfuscator encoder = new Deobfuscator();
		String input=null, output=null;
		
		for (int i=0; i<args.length; i++)
		{
			if (args[i].equals("-noreorder"))
				encoder.params.reorderCode = false;
			else
			if (args[i].equals("-dict"))
				encoder.params.loadDictionary(args[++i]);
			else
			if (input == null)
				input = args[i];
			else
			if (output == null)
				output = args[i];
			else
				System.err.println("Ignoring extraneous argument: " + args[i]);
		}
		
		if (input==null || output==null)
		{
			System.err.println("Input/output not specified");
			return;
		}
		
		URL url = FileUtils.toURL(new File(input));
		InputStream in = url.openStream();
		new TagDecoder(in, url).parse(encoder);
		encoder.finish();
		FileOutputStream out = new FileOutputStream(output);
		out.write(encoder.toByteArray());
	}
	
	public void doABC(DoABC tag) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		AbcDeobfuscator ad = new AbcDeobfuscator(tag.abc, baos, params);
		try {
			ad.deobfuscate();
			tag.abc = baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			tag.abc = null;
		}
		super.doABC(tag);
	}

	public void symbolClass(SymbolClass tag)
	{
		HashMap<String, Tag> newMap = new HashMap<String, Tag>();
		Iterator<Map.Entry<String, Tag>> it = tag.class2tag.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, Tag> e = it.next();
			String className = e.getKey();
			newMap.put(params.deobfuscateFQN(className), e.getValue());
		}
		tag.class2tag = newMap;
		super.symbolClass(tag);
	}
}
