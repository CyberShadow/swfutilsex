// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

// Copyedit of flex2.tools.PostLink.merge

package net.thecybershadow.swf.tools;

import java.io.FileOutputStream;
import java.io.IOException;

import macromedia.abc.BytecodeBuffer;
import macromedia.abc.ConstantPool;
import macromedia.abc.Decoder;
import macromedia.abc.DecoderException;
import macromedia.abc.Encoder;

public class AbcMerge
{
	public static void main(String[] args) throws IOException, DecoderException
	{
		if (args.length < 2)
		{
			System.err.println("Usage: AbcMerge input1.abc ... inputN.abc output.abc");
			return;
		}

		int abcSize = args.length - 1;

		Decoder[] decoders = new Decoder[abcSize];
		ConstantPool[] pools = new ConstantPool[abcSize];

		Encoder encoder;
		Decoder decoder;

		boolean skipFrame = false;
		int majorVersion = 0, minorVersion = 0;

		// create decoders...
		for (int j = 0; j < abcSize; j++)
		{
			BytecodeBuffer in = new BytecodeBuffer(args[j]);

			// ThreadLocalToolkit.logInfo(tag.name);
			decoders[j] = new Decoder(in);
			majorVersion = decoders[j].majorVersion;
			minorVersion = decoders[j].minorVersion;
			pools[j] = decoders[j].constantPool;
		}

		encoder = new Encoder(majorVersion, minorVersion);
		// all the constant pools are merged here...
		encoder.addConstantPools(pools);
		// if (!keepDebugOpcodes)
		// encoder.disableDebugging();

		// always remove metadata...
		// encoder.removeMetadata();

		// keep the following metadata
		// for (int m = 0; as3metadata != null && m < as3metadata.length; m++)
		// encoder.addMetadataToKeep(as3metadata[m]);

		// always enable peephole optimization...
		// if (runPeephole)
		// encoder.enablePeepHole();

		encoder.configure(decoders);

		// decode methodInfo...
		for (int j = 0; j < abcSize; j++)
		{
			decoder = decoders[j];
			encoder.useConstantPool(j);

			Decoder.MethodInfo methodInfo = decoder.methodInfo;

			for (int k = 0, infoSize = methodInfo.size(); k < infoSize; k++)
				methodInfo.decode(k, encoder);
		}

		// decode metadataInfo...
		for (int j = 0; j < abcSize; j++)
		{
			decoder = decoders[j];
			encoder.useConstantPool(j);

			Decoder.MetaDataInfo metadataInfo = decoder.metadataInfo;

			for (int k = 0, infoSize = metadataInfo.size(); k < infoSize; k++)
				metadataInfo.decode(k, encoder);
		}

		// decode classInfo...
		for (int j = 0; j < abcSize; j++)
		{
			decoder = decoders[j];
			encoder.useConstantPool(j);

			Decoder.ClassInfo classInfo = decoder.classInfo;

			for (int k = 0, infoSize = classInfo.size(); k < infoSize; k++)
				classInfo.decodeInstance(k, encoder);
		}

		for (int j = 0; j < abcSize; j++)
		{
			decoder = decoders[j];
			encoder.useConstantPool(j);

			Decoder.ClassInfo classInfo = decoder.classInfo;

			for (int k = 0, infoSize = classInfo.size(); k < infoSize; k++)
				classInfo.decodeClass(k, 0, encoder);
		}

		// decode scripts...
		for (int j = 0; j < abcSize; j++)
		{
			decoder = decoders[j];
			encoder.useConstantPool(j);

			Decoder.ScriptInfo scriptInfo = decoder.scriptInfo;

			for (int k = 0, scriptSize = scriptInfo.size(); k < scriptSize; k++)
				scriptInfo.decode(k, encoder);
		}

		if (skipFrame)
			return;

		// decode method bodies...
		for (int j = 0; j < abcSize; j++)
		{
			decoder = decoders[j];
			encoder.useConstantPool(j);

			Decoder.MethodBodies methodBodies = decoder.methodBodies;

			for (int k = 0, bodySize = methodBodies.size(); k < bodySize; k++)
				methodBodies.decode(k, 2, encoder);
		}

		FileOutputStream fos = new FileOutputStream(args[abcSize]);
		fos.write(encoder.toABC());
		fos.close();
	}

}
