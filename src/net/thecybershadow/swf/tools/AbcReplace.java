// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.thecybershadow.swf.tools.doabclayout.Layout;
import net.thecybershadow.swf.tools.doabclayout.Slot;
import net.thecybershadow.swf.tools.doabclayout.Tag;
import flash.swf.TagDecoder;
import flash.swf.TagEncoder;
import flash.swf.tags.DoABC;
import flash.util.FileUtils;

public class AbcReplace extends TagEncoder
{
	public static void main(String[] args) throws IOException, JAXBException
	{
		if (args.length != 2)
		{
			System.err.println("Replaces contents of DoABC tags in a SWF file.");
			System.err.println("Usage: AbcReplace input.swf output.swf < abclayout.xml");
			return;
		}

		URL inurl = FileUtils.toURL(new File(args[0]));
		InputStream in = inurl.openStream();

		JAXBContext jc = JAXBContext.newInstance("net.thecybershadow.swf.tools.doabclayout");
		Unmarshaller u = jc.createUnmarshaller();
		Layout layout = (Layout) u.unmarshal(System.in);

		AbcReplace abcreplace = new AbcReplace(layout);
		new TagDecoder(in, inurl).parse(abcreplace);
		abcreplace.finish();
		FileOutputStream out = new FileOutputStream(args[args.length - 1]);
		out.write(abcreplace.toByteArray());
	}

	int counter = 0;
	String prefix;
	List<Slot> slots;

	public AbcReplace(Layout layout)
	{
		this.slots = layout.getSlot();
	}

	@Override
	public void doABC(DoABC oldTag)
	{
		if (counter < slots.size())
		{
			List<Tag> tags = slots.get(counter).getTag();
			for (Tag tag : tags)
			{
				try
				{
					DoABC newTag = new DoABC(tag.getName(), tag.getFlag());
					File f = new File(tag.getFilename());
					FileInputStream fis = new FileInputStream(f);
					newTag.abc = FileUtils.toByteArray(fis, (int) f.length());
					super.doABC(newTag);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		counter++;
	}

	@Override
	public void finish()
	{
		if (counter > slots.size())
			System.err.println("Warning: more DoABC tags in SWF file than slots in layout XML."
					+ "Extra DoABC tags deleted.");
		if (counter < slots.size())
			System.err.println("Warning: less DoABC tags in SWF file than slots in layout XML."
					+ "Extra XML slots ignored.");
		super.finish();
	}
}
