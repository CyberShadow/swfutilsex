// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

// Copy-edit of AbcDeobfuscator, which itself is a copy-edit of flash.swf.tools.AbcPrinter.

package net.thecybershadow.swf.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import net.thecybershadow.misc.RegexFilter;

public class AbcCutter
{

	public static void main(String[] args)
	{
	}

	private final byte[] abc;
	private final OutputStream out;
	private int offset = 0;
	private int outOffset = 0;
	private int copyOffset = 0;

	private int[] intConstants;
	private long[] uintConstants;
	// private double[] floatConstants;
	private String[] stringConstants;
	private String[] namespaceConstants;
	private String[][] namespaceSetConstants;
	private MultiName[] multiNameConstants;

	private MethodInfo[] methods;
	private String[] instanceNames;
	private final RegexFilter filter;

	public AbcCutter(byte[] abc, OutputStream out, RegexFilter filter)
	{
		this.abc = abc;
		this.out = out;
		this.filter = filter;
	}

	// Copies all read input since last copyInput/discardInput call.
	private void copyInput() throws IOException
	{
		out.write(abc, copyOffset, offset - copyOffset);
		outOffset += offset - copyOffset;
		copyOffset = offset;
	}

	// Discards all read input since last copyInput/discardInput call.
	private void discardInput()
	{
		copyOffset = offset;
	}

	public void process() throws IOException
	{
		offset += 4; // versions
		readIntConstantPool();
		readUintConstantPool();
		readDoubleConstantPool();
		readStringConstantPool();
		readNamespaceConstantPool();
		readNamespaceSetsConstantPool();
		readMultiNameConstantPool();
		readMethods();
		readMetaData();
		readClasses();
		readScripts();
		readBodies();
		copyInput();
	}

	final int TRAIT_Slot = 0x00;
	final int TRAIT_Method = 0x01;
	final int TRAIT_Getter = 0x02;
	final int TRAIT_Setter = 0x03;
	final int TRAIT_Class = 0x04;
	final int TRAIT_Function = 0x05;
	final int TRAIT_Const = 0x06;

	long readU32()
	{
		long b = abc[offset++];
		b &= 0xFF;
		long u32 = b;
		if (!((u32 & 0x00000080) == 0x00000080))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x0000007f | b << 7;
		if (!((u32 & 0x00004000) == 0x00004000))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x00003fff | b << 14;
		if (!((u32 & 0x00200000) == 0x00200000))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x001fffff | b << 21;
		if (!((u32 & 0x10000000) == 0x10000000))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x0fffffff | b << 28;
		return u32;
	}

	String readUTFBytes(long n)
	{
		StringWriter sw = new StringWriter();
		for (int i = 0; i < n; i++)
		{
			sw.write(abc[offset++]);
		}
		return sw.toString();
	}

	void readIntConstantPool()
	{
		long n = readU32();
		intConstants = new int[(n > 0) ? (int) n : 1];
		intConstants[0] = 0;
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			intConstants[i] = (int) val;
		}
	}

	void readUintConstantPool()
	{
		long n = readU32();
		uintConstants = new long[(n > 0) ? (int) n : 1];
		uintConstants[0] = 0;
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			uintConstants[i] = (int) val;
		}
	}

	void readDoubleConstantPool()
	{
		long n = readU32();
		if (n > 0)
			offset += (n - 1) * 8;
	}

	void readStringConstantPool() throws IOException
	{
		long n = readU32();
		stringConstants = new String[(n > 0) ? (int) n : 1];
		stringConstants[0] = "";
		for (int i = 1; i < n; i++)
			stringConstants[i] = readUTFBytes(readU32());
	}

	void readNamespaceConstantPool()
	{
		long n = readU32();
		namespaceConstants = new String[(n > 0) ? (int) n : 1];
		namespaceConstants[0] = "public";
		for (int i = 1; i < n; i++)
		{
			byte b = abc[offset++];
			String s;
			if (b == 5)
			{
				readU32();
				s = "private";
			}
			else
			{
				s = stringConstants[(int) readU32()];
			}
			namespaceConstants[i] = s;
		}
	}

	void readNamespaceSetsConstantPool()
	{
		long n = readU32();
		namespaceSetConstants = new String[(n > 0) ? (int) n : 1][];
		namespaceSetConstants[0] = new String[0];
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			String[] nsset = new String[(int) val];
			namespaceSetConstants[i] = nsset;
			for (int j = 0; j < val; j++)
			{
				nsset[j] = namespaceConstants[(int) readU32()];
			}
		}
	}

	void readMultiNameConstantPool()
	{
		long n = readU32();
		multiNameConstants = new MultiName[(n > 0) ? (int) n : 1];
		multiNameConstants[0] = new MultiName();
		for (int i = 1; i < n; i++)
		{
			byte b = abc[offset++];
			multiNameConstants[i] = new MultiName();
			multiNameConstants[i].kind = b;
			switch (b)
			{
				case 0x07: // QName
				case 0x0D:
					multiNameConstants[i].long1 = (int) readU32();
					multiNameConstants[i].long2 = (int) readU32();
					break;
				case 0x0F: // RTQName
				case 0x10:
					multiNameConstants[i].long1 = (int) readU32();
					break;
				case 0x11: // RTQNameL
				case 0x12:
					break;
				case 0x13: // NameL
				case 0x14:
					break;
				case 0x09:
				case 0x0E:
					multiNameConstants[i].long1 = (int) readU32();
					multiNameConstants[i].long2 = (int) readU32();
					break;
				case 0x1B:
				case 0x1C:
					multiNameConstants[i].long1 = (int) readU32();
					break;
				case 0x1D:
					int nameIndex = (int) readU32();
					MultiName mn = multiNameConstants[nameIndex];
					int count = (int) readU32();
					MultiName types[] = new MultiName[count];
					for (int t = 0; t < count; t++)
					{
						int typeIndex = (int) readU32();
						types[t] = multiNameConstants[typeIndex];
					}
					multiNameConstants[i].typeName = mn;
					multiNameConstants[i].types = types;
			}
		}
	}

	void readMethods()
	{
		long n = readU32();
		methods = new MethodInfo[(int) n];
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			MethodInfo m = methods[i] = new MethodInfo();
			m.paramCount = (int) readU32();
			m.returnType = (int) readU32();
			m.params = new int[m.paramCount];
			for (int j = 0; j < m.paramCount; j++)
			{
				m.params[j] = (int) readU32();
			}
			int nameIndex = (int) readU32();
			if (nameIndex > 0)
				m.name = stringConstants[nameIndex];
			else
				m.name = "no name";

			m.flags = abc[offset++];
			if ((m.flags & 0x8) == 0x8)
			{
				// read in optional parameter info
				m.optionCount = (int) readU32();
				m.optionIndex = new int[m.optionCount];
				m.optionKinds = new int[m.optionCount];
				for (int k = 0; k < m.optionCount; k++)
				{
					m.optionIndex[k] = (int) readU32();
					m.optionKinds[k] = abc[offset++];
				}
			}
			if ((m.flags & 0x80) == 0x80)
			{
				// read in parameter names info
				m.paramNames = new int[m.paramCount];
				for (int k = 0; k < m.paramCount; k++)
				{
					m.paramNames[k] = (int) readU32();
				}
			}
		}

	}

	void readMetaData()
	{
		long n = readU32();
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String s = stringConstants[(int) readU32()];
			long val = readU32();
			for (int j = 0; j < val; j++)
			{
				s += " " + stringConstants[(int) readU32()];
			}
			for (int j = 0; j < val; j++)
			{
				s += " " + stringConstants[(int) readU32()];
			}
		}
	}

	void readClasses()
	{
		long n = readU32();
		instanceNames = new String[(int) n];
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String name = multiNameConstants[(int) readU32()].toString();
			instanceNames[i] = name;
			String base = multiNameConstants[(int) readU32()].toString();
			int b = abc[offset++];
			if ((b & 0x8) == 0x8)
				readU32(); // eat protected namespace
			long val = readU32();
			String s = "";
			for (int j = 0; j < val; j++)
			{
				s += " " + multiNameConstants[(int) readU32()].toString();
			}
			int init = (int) readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name;
			mi.className = name;
			mi.kind = TRAIT_Method;

			int numTraits = (int) readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				s = multiNameConstants[(int) readU32()].toString(); // eat trait
																	// name;
				b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						mi = methods[(int) readU32()]; // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					val = readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
			}
		}
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			MethodInfo mi = methods[(int) readU32()];
			String name = instanceNames[i];
			mi.name = name + "$cinit";
			mi.className = name;
			mi.kind = TRAIT_Method;
			String base = "Class";

			int numTraits = (int) readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
																			// trait
																			// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						mi = methods[(int) readU32()]; // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
			}
		}
	}

	void readScripts()
	{
		long n = readU32();
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String name = "script" + Integer.toString(i);
			int init = (int) readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name + "$init";
			mi.className = name;
			mi.kind = TRAIT_Method;

			int numTraits = (int) readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
																			// trait
																			// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						mi = methods[(int) readU32()]; // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
			}
		}

	}

	void readBodies() throws IOException
	{
		long n = readU32();
		// printOffset();
		// out.println("===== " + n + " Method Bodies" + " =====");
		for (int i = 0; i < n; i++)
		{
			copyInput();
			int functionStartOffset = offset;
			int functionStartOutOffset = outOffset;
			// printOffset();
			// int start = offset;
			int methodIndex = (int) readU32();
			int maxStack = (int) readU32();
			int localCount = (int) readU32();
			int initScopeDepth = (int) readU32();
			int maxScopeDepth = (int) readU32();
			int codeLength = (int) readU32();
			offset += codeLength;

			MethodInfo mi = methods[methodIndex];

			copyInput();

			int exCount = (int) readU32();
			// printOffset();
			// out.println(exCount + " Extras");
			for (int j = 0; j < exCount; j++)
			{
				// start = offset;
				// printOffset();
				int from = (int) readU32();
				int to = (int) readU32();
				int target = (int) readU32();
				int typeIndex = (int) readU32();
				int nameIndex = (int) readU32();
				// if (showByteCode)
				// {
				// for (int x = start; x < offset; x++)
				// {
				// out.print(hex(abc[(int)x]) + " ");
				// }
				// }
				// out.print(multiNameConstants[nameIndex] + " ");
				// out.print("type:" + multiNameConstants[typeIndex] + " from:"
				// + from + " ");
				// out.println("to:" + to + " target:" + target);
			}

			int numTraits = (int) readU32(); // number of traits
			// printOffset();
			// out.println(numTraits + " Traits Entries");
			for (int j = 0; j < numTraits; j++)
			{
				// printOffset();
				// start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
																			// trait
																			// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						readU32(); // method
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
				// if (showByteCode)
				// {
				// for (int x = start; x < offset; x++)
				// {
				// out.print(hex(abc[(int)x]) + " ");
				// }
				// }
				// out.println(s);
			}
			// out.println("");
			System.out.println("======================================================================");
		}
	}

	class MultiName
	{
		public MultiName()
		{
		}

		public int kind;
		public int long1;
		public int long2;
		public MultiName typeName;
		public MultiName types[];

		@Override
		public String toString()
		{
			String s = "";

			String[] nsSet;
			int len;
			int j;

			switch (kind)
			{
				case 0x07: // QName
				case 0x0D:
					s = namespaceConstants[long1] + ":";
					s += stringConstants[long2];
					break;
				case 0x0F: // RTQName
				case 0x10:
					s = stringConstants[long1];
					break;
				case 0x11: // RTQNameL
				case 0x12:
					s = "RTQNameL";
					break;
				case 0x13: // NameL
				case 0x14:
					s = "NameL";
					break;
				case 0x09:
				case 0x0E:
					nsSet = namespaceSetConstants[long2];
					len = nsSet.length;
					for (j = 0; j < len - 1; j++)
					{
						s += nsSet[j] + ",";
					}
					if (len > 0)
						s += nsSet[len - 1] + ":";
					s += stringConstants[long1];
					break;
				case 0x1B:
				case 0x1C:
					nsSet = namespaceSetConstants[long1];
					len = nsSet.length;
					for (j = 0; j < len - 1; j++)
					{
						s += nsSet[j] + ",";
					}
					if (len > 0)
						s += nsSet[len - 1] + ":";
					s += "null";
					break;
				case 0x1D:
					s += typeName.toString();
					for (int t = 0; t < types.length; t++)
						s += types[t].toString();
			}
			return s;
		}
	}

	class MethodInfo
	{
		int paramCount;
		int returnType;
		int[] params;
		String name;
		int kind;
		int flags;
		int optionCount;
		int[] optionKinds;
		int[] optionIndex;
		int[] paramNames;
		String className;
	}
}
