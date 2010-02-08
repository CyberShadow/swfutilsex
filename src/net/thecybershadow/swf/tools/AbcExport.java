// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import flash.swf.TagDecoder;
import flash.swf.TagHandler;
import flash.swf.tags.DoABC;
import flash.util.FileUtils;

public class AbcExport extends TagHandler {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Exports DoABC tags from SWFs to individual .abc files.");
			System.err.println("Usage: AbcExport input.swf");
			return;
		}
			
		File file = new File(args[0]);
		URL url = FileUtils.toURL(file);
		AbcExport abcdump = new AbcExport(args[0].substring(0, args[0].lastIndexOf(".")));
		InputStream in = url.openStream();
		new TagDecoder(in, url).parse(abcdump);
		abcdump.finish();
	}
	
	int counter = 0;
	String prefix;
	
	public AbcExport(String prefix) {
		this.prefix = prefix;
	}

	public void doABC(DoABC tag) {
		try {
			FileOutputStream fos = new FileOutputStream(prefix + counter++ + ".abc");
			fos.write(tag.abc);
			fos.close();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	static String changeExtension(String originalName, String newExtension) {
		int lastDot = originalName.lastIndexOf(".");
		if (lastDot != -1) {
			return originalName.substring(0, lastDot) + newExtension;
		} else {
			return originalName + newExtension;
		}
	}
}
