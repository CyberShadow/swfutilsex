// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

// This file is a copy-edit of flash.swf.tools.AbcPrinter.
// Most methods have been stripped of their output code, so they just read (and in some cases store) data.
// readBodies does the actual work. It also prints some debug information to stdout.

package net.thecybershadow.swf.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

public class AbcDeobfuscator
{
	private byte[] abc;
	private OutputStream out;
	private int offset = 0;
	private int outOffset = 0;
	private int copyOffset = 0;
	
	private int[] intConstants;
	private long[] uintConstants;
//	private double[] floatConstants;
	private String[] stringConstants;
	private String[] namespaceConstants;
	private String[][] namespaceSetConstants;
	private MultiName[] multiNameConstants;
	
	private MethodInfo[] methods;
	private String[] instanceNames;
	private DeobfuscationParameters params;
	
	public AbcDeobfuscator(byte[] abc, OutputStream out, DeobfuscationParameters params)
	{
		this.abc = abc;
		this.out = out;
		this.params = params;
	}
	
	// Copies all read input since last copyInput/discardInput call.
	private void copyInput() throws IOException {
		out.write(abc, copyOffset, offset-copyOffset);
		outOffset += offset-copyOffset;
		copyOffset = offset;
	}
	
	// Discards all read input since last copyInput/discardInput call.
	private void discardInput() {
		copyOffset = offset;
	}

	public void deobfuscate() throws IOException
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
	
	final String[] traitKinds = {
		"var", 
		"function", 
		"function get", 
		"function set", 
		"class", 
		"function", 
		"const"
	};
	
	char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	final int OP_bkpt = 0x01;
	final int OP_nop = 0x02;
	final int OP_throw = 0x03;
	final int OP_getsuper = 0x04;
	final int OP_setsuper = 0x05;
	final int OP_dxns = 0x06;
	final int OP_dxnslate = 0x07;
	final int OP_kill = 0x08;
	final int OP_label = 0x09;
	final int OP_ifnlt = 0x0C;
	final int OP_ifnle = 0x0D;
	final int OP_ifngt = 0x0E;
	final int OP_ifnge = 0x0F;
	final int OP_jump = 0x10;
	final int OP_iftrue = 0x11;
	final int OP_iffalse = 0x12;
	final int OP_ifeq = 0x13;
	final int OP_ifne = 0x14;
	final int OP_iflt = 0x15;
	final int OP_ifle = 0x16;
	final int OP_ifgt = 0x17;
	final int OP_ifge = 0x18;
	final int OP_ifstricteq = 0x19;
	final int OP_ifstrictne = 0x1A;
	final int OP_lookupswitch = 0x1B;
	final int OP_pushwith = 0x1C;
	final int OP_popscope = 0x1D;
	final int OP_nextname = 0x1E;
	final int OP_hasnext = 0x1F;
	final int OP_pushnull = 0x20;
	final int OP_pushundefined = 0x21;
	final int OP_pushintant = 0x22;
	final int OP_nextvalue = 0x23;
	final int OP_pushbyte = 0x24;
	final int OP_pushshort = 0x25;
	final int OP_pushtrue = 0x26;
	final int OP_pushfalse = 0x27;
	final int OP_pushnan = 0x28;
	final int OP_pop = 0x29;
	final int OP_dup = 0x2A;
	final int OP_swap = 0x2B;
	final int OP_pushstring = 0x2C;
	final int OP_pushint = 0x2D;
	final int OP_pushuint = 0x2E;
	final int OP_pushdouble = 0x2F;
	final int OP_pushscope = 0x30;
	final int OP_pushnamespace = 0x31;
	final int OP_hasnext2 = 0x32;
	final int OP_newfunction = 0x40;
	final int OP_call = 0x41;
	final int OP_construct = 0x42;
	final int OP_callmethod = 0x43;
	final int OP_callstatic = 0x44;
	final int OP_callsuper = 0x45;
	final int OP_callproperty = 0x46;
	final int OP_returnvoid = 0x47;
	final int OP_returnvalue = 0x48;
	final int OP_constructsuper = 0x49;
	final int OP_constructprop = 0x4A;
	final int OP_callsuperid = 0x4B;
	final int OP_callproplex = 0x4C;
	final int OP_callinterface = 0x4D;
	final int OP_callsupervoid = 0x4E;
	final int OP_callpropvoid = 0x4F;
	final int OP_newobject = 0x55;
	final int OP_newarray = 0x56;
	final int OP_newactivation = 0x57;
	final int OP_newclass = 0x58;
	final int OP_getdescendants = 0x59;
	final int OP_newcatch = 0x5A;
	final int OP_findpropstrict = 0x5D;
	final int OP_findproperty = 0x5E;
	final int OP_finddef = 0x5F;
	final int OP_getlex = 0x60;
	final int OP_setproperty = 0x61;
	final int OP_getlocal = 0x62;
	final int OP_setlocal = 0x63;
	final int OP_getglobalscope = 0x64;
	final int OP_getscopeobject = 0x65;
	final int OP_getproperty = 0x66;
	final int OP_getpropertylate = 0x67;
	final int OP_initproperty = 0x68;
	final int OP_setpropertylate = 0x69;
	final int OP_deleteproperty = 0x6A;
	final int OP_deletepropertylate = 0x6B;
	final int OP_getslot = 0x6C;
	final int OP_setslot = 0x6D;
	final int OP_getglobalslot = 0x6E;
	final int OP_setglobalslot = 0x6F;
	final int OP_convert_s = 0x70;
	final int OP_esc_xelem = 0x71;
	final int OP_esc_xattr = 0x72;
	final int OP_convert_i = 0x73;
	final int OP_convert_u = 0x74;
	final int OP_convert_d = 0x75;
	final int OP_convert_b = 0x76;
	final int OP_convert_o = 0x77;
	final int OP_coerce = 0x80;
	final int OP_coerce_b = 0x81;
	final int OP_coerce_a = 0x82;
	final int OP_coerce_i = 0x83;
	final int OP_coerce_d = 0x84;
	final int OP_coerce_s = 0x85;
	final int OP_astype = 0x86;
	final int OP_astypelate = 0x87;
	final int OP_coerce_u = 0x88;
	final int OP_coerce_o = 0x89;
	final int OP_negate = 0x90;
	final int OP_increment = 0x91;
	final int OP_inclocal = 0x92;
	final int OP_decrement = 0x93;
	final int OP_declocal = 0x94;
	final int OP_typeof = 0x95;
	final int OP_not = 0x96;
	final int OP_bitnot = 0x97;
	final int OP_concat = 0x9A;
	final int OP_add_d = 0x9B;
	final int OP_add = 0xA0;
	final int OP_subtract = 0xA1;
	final int OP_multiply = 0xA2;
	final int OP_divide = 0xA3;
	final int OP_modulo = 0xA4;
	final int OP_lshift = 0xA5;
	final int OP_rshift = 0xA6;
	final int OP_urshift = 0xA7;
	final int OP_bitand = 0xA8;
	final int OP_bitor = 0xA9;
	final int OP_bitxor = 0xAA;
	final int OP_equals = 0xAB;
	final int OP_strictequals = 0xAC;
	final int OP_lessthan = 0xAD;
	final int OP_lessequals = 0xAE;
	final int OP_greaterthan = 0xAF;
	final int OP_greaterequals = 0xB0;
	final int OP_instanceof = 0xB1;
	final int OP_istype = 0xB2;
	final int OP_istypelate = 0xB3;
	final int OP_in = 0xB4;
	final int OP_increment_i = 0xC0;
	final int OP_decrement_i = 0xC1;
	final int OP_inclocal_i = 0xC2;
	final int OP_declocal_i = 0xC3;
	final int OP_negate_i = 0xC4;
	final int OP_add_i = 0xC5;
	final int OP_subtract_i = 0xC6;
	final int OP_multiply_i = 0xC7;
	final int OP_getlocal0 = 0xD0;
	final int OP_getlocal1 = 0xD1;
	final int OP_getlocal2 = 0xD2;
	final int OP_getlocal3 = 0xD3;
	final int OP_setlocal0 = 0xD4;
	final int OP_setlocal1 = 0xD5;
	final int OP_setlocal2 = 0xD6;
	final int OP_setlocal3 = 0xD7;
	final int OP_debug = 0xEF;
	final int OP_debugline = 0xF0;
	final int OP_debugfile = 0xF1;
	final int OP_bkptline = 0xF2;
	
	String hex(byte b)
	{
		return new StringBuilder().append(hexChars[(b >> 4) & 0xF]).append(hexChars[b & 0xF]).toString();
	}
	
	int readS24()
	{
		int b = abc[offset++];
		b &= 0xFF;
		b |= abc[offset++] << 8;
		b &= 0xFFFF;
		b |= abc[offset++] << 16;
		return b;
	}
	
	static int readS24(byte[] abc, int offset)
	{
		int b = abc[offset++];
		b &= 0xFF;
		b |= abc[offset++] << 8;
		b &= 0xFFFF;
		b |= abc[offset++] << 16;
		return b;
	}
	
	static private void writeS24(byte[] abc, int offset, int v) {
		abc[offset++] = (byte) v;
		abc[offset++] = (byte) (v >> 8);
		abc[offset++] = (byte) (v >> 16);
	}

	private void writeS24(int v) throws IOException {
		out.write((byte) v);
		out.write((byte) (v >> 8));
		out.write((byte) (v >> 16));
		outOffset += 3;
}

	private void writeU32(long v) throws IOException {
        if ( v < 128 && v > -1 )
        {
            out.write((byte) v);
    		outOffset += 1;
        }
        else if ( v < 16384 && v > -1)
        {
            out.write((byte) ((v & 0x7F) | 0x80));
            out.write((byte) ((v >> 7) & 0x7F));
    		outOffset += 2;
        }
        else if ( v < 2097152 && v > -1)
        {
            out.write((byte) ((v & 0x7F) | 0x80));
            out.write((byte) ((v >> 7) | 0x80));
            out.write((byte) ((v >> 14) & 0x7F));
    		outOffset += 3;
        }
        else if (  v < 268435456 && v > -1)
        {
            out.write((byte) ((v & 0x7F) | 0x80));
            out.write((byte) (v >> 7 | 0x80));
            out.write((byte) (v >> 14 | 0x80));
            out.write((byte) ((v >> 21) & 0x7F));
    		outOffset += 4;
        }
        else
        {
            out.write((byte) ((v & 0x7F) | 0x80));
            out.write((byte) (v >> 7 | 0x80));
            out.write((byte) (v >> 14 | 0x80));
            out.write((byte) (v >> 21 | 0x80));
            out.write((byte) ((v >> 28) & 0x0F ));
    		outOffset += 5;
        }
	}

	private static int getU32length(long v) {
        if ( v < 128 && v > -1 )
    		return 1;
        else if ( v < 16384 && v > -1)
            return 2;
        else if ( v < 2097152 && v > -1)
            return 3;
        else if (  v < 268435456 && v > -1)
            return 4;
        else
            return 5;
	}

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
		intConstants = new int[(n > 0) ? (int)n : 1];
		intConstants[0] = 0;
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			intConstants[i] = (int)val;
		}
	}
	
	void readUintConstantPool()
	{
		long n = readU32();
		uintConstants = new long[(n > 0) ? (int)n : 1];
		uintConstants[0] = 0;
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			uintConstants[i] = (int)val;
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
		// TODO: only deobfuscate strings which are used as code identifiers. This requires deserialization/serialization.
		long n = readU32();
		stringConstants = new String[(n > 0) ? (int)n : 1];
		stringConstants[0] = "";
		copyInput();
		for (int i = 1; i < n; i++)
		{
			String s = readUTFBytes(readU32());
			s = params.deobfuscateString(s);
			writeU32(s.length());
			writeUTFBytes(s);
			stringConstants[i] = s;
		}
		discardInput();
	}
	
	private void writeUTFBytes(String s) throws IOException {
		StringReader sr = new StringReader(s);
	     int byte_;
	     while ((byte_ = sr.read()) != -1)
	    	 out.write (byte_);
	}

	void readNamespaceConstantPool()
	{
		long n = readU32();
		namespaceConstants = new String[(n > 0) ? (int)n : 1];
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
				s = stringConstants[(int)readU32()];
			}
			namespaceConstants[i] = s;
		}
	}
	
	void readNamespaceSetsConstantPool()
	{
		long n = readU32();
		namespaceSetConstants = new String[(n > 0) ? (int)n : 1][];
		namespaceSetConstants[0] = new String[0];
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			String[] nsset = new String[(int)val];
			namespaceSetConstants[i] = nsset;
			for (int j = 0; j < val; j++)
			{
				nsset[j] = namespaceConstants[(int)readU32()];
			}
		}
	}
	
	void readMultiNameConstantPool()
	{
		long n = readU32();
		multiNameConstants = new MultiName[(n > 0) ? (int)n : 1];
		multiNameConstants[0] = new MultiName();
		for (int i = 1; i < n; i++)
		{
			byte b = abc[offset++];
			multiNameConstants[i] = new MultiName();
			multiNameConstants[i].kind = b;
			switch (b)
			{
				case 0x07:	// QName
				case 0x0D:
					multiNameConstants[i].long1 = (int)readU32();
					multiNameConstants[i].long2 = (int)readU32();
					break;
				case 0x0F:	// RTQName
				case 0x10:
					multiNameConstants[i].long1 = (int)readU32();
					break;
				case 0x11:	// RTQNameL
				case 0x12:
					break;
				case 0x13:	// NameL
				case 0x14:
					break;
				case 0x09:
				case 0x0E:
					multiNameConstants[i].long1 = (int)readU32();
					multiNameConstants[i].long2 = (int)readU32();
					break;
				case 0x1B:
				case 0x1C:
					multiNameConstants[i].long1 = (int)readU32();
					break;
				case 0x1D:
					int nameIndex = (int)readU32();
					MultiName mn = multiNameConstants[nameIndex];
					int count = (int)readU32();
					MultiName types[] = new MultiName[count];
					for (int t = 0; t < count; t++)
					{
						int typeIndex = (int)readU32();
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
		methods = new MethodInfo[(int)n];
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			MethodInfo m = methods[i] = new MethodInfo();
			m.paramCount = (int)readU32();
			m.returnType = (int)readU32();
			m.params = new int[m.paramCount];
			for (int j = 0; j < m.paramCount; j++)
			{
				m.params[j] = (int)readU32();
			}
			int nameIndex = (int)readU32();
			if (nameIndex > 0)
				m.name = stringConstants[nameIndex];
			else
				m.name = "no name";
			
			m.flags = abc[offset++];
			if ((m.flags & 0x8) == 0x8)
			{
				// read in optional parameter info
				m.optionCount = (int)readU32();
				m.optionIndex = new int[m.optionCount];
				m.optionKinds = new int[m.optionCount];
				for (int k = 0; k < m.optionCount; k++)
				{
					m.optionIndex[k] = (int)readU32();
					m.optionKinds[k] = abc[offset++];
				}
			}
			if ((m.flags & 0x80) == 0x80)
			{
				// read in parameter names info
				m.paramNames = new int[m.paramCount];
				for (int k = 0; k < m.paramCount; k++)
				{
					m.paramNames[k] = (int)readU32();
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
			String s = stringConstants[(int)readU32()];
			long val = readU32();
			for (int j = 0; j < val; j++)
			{
				s += " " + stringConstants[(int)readU32()];
			}
			for (int j = 0; j < val; j++)
			{
				s += " " + stringConstants[(int)readU32()];
			}
		}
	}
	
	void readClasses()
	{
		long n = readU32();
		instanceNames = new String[(int)n];
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String name = multiNameConstants[(int)readU32()].toString();
			instanceNames[i] = name;
			String base = multiNameConstants[(int)readU32()].toString();
			int b = abc[offset++];
			if ((b & 0x8) == 0x8)
				readU32();	// eat protected namespace
			long val = readU32();
			String s = "";
			for (int j = 0; j < val; j++)
			{
				s += " " + multiNameConstants[(int)readU32()].toString();
			}
			int init = (int)readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name;
			mi.className = name;
			mi.kind = TRAIT_Method;
			
			int numTraits = (int)readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				s = multiNameConstants[(int)readU32()].toString(); // eat trait name;
				b =  abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00:	// slot
					case 0x06:	// const
						readU32();	// id
						readU32();	// type
						int index = (int)readU32();	// index;
						if (index != 0)
							offset++;	// kind
						break;
					case 0x04:	// class
						readU32();	// id
						readU32();	// value;
						break;
					default:
						readU32();	// id
						mi = methods[(int)readU32()];  // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					val = readU32();	// metadata count
					for (int k = 0; k < val; k++)
					{
						readU32();	// metadata
					}
				}
			}
		}
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			MethodInfo mi = methods[(int)readU32()];
			String name = instanceNames[i];
			mi.name = name + "$cinit";
			mi.className = name;
			mi.kind = TRAIT_Method;
			String base = "Class";
			
			int numTraits = (int)readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				String s = multiNameConstants[(int)readU32()].toString(); // eat trait name;
				int b =  abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00:	// slot
					case 0x06:	// const
						readU32();	// id
						readU32();	// type
						int index = (int)readU32();	// index;
						if (index != 0)
							offset++;	// kind
						break;
					case 0x04:	// class
						readU32();	// id
						readU32();	// value;
						break;
					default:
						readU32();	// id
						mi = methods[(int)readU32()];  // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int)readU32();	// metadata count
					for (int k = 0; k < val; k++)
					{
						readU32();	// metadata
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
			int init = (int)readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name + "$init";
			mi.className = name;
			mi.kind = TRAIT_Method;
			
			int numTraits = (int)readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				String s = multiNameConstants[(int)readU32()].toString(); // eat trait name;
				int b =  abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00:	// slot
					case 0x06:	// const
						readU32();	// id
						readU32();	// type
						int index = (int)readU32();	// index;
						if (index != 0)
							offset++;	// kind
						break;
					case 0x04:	// class
						readU32();	// id
						readU32();	// value;
						break;
					default:
						readU32();	// id
						mi = methods[(int)readU32()];  // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int)readU32();	// metadata count
					for (int k = 0; k < val; k++)
					{
						readU32();	// metadata
					}
				}
			}
		}
		
		
	}
	
	void readBodies() throws IOException
	{
		long n = readU32();
//		printOffset();
//		out.println("===== " + n + " Method Bodies" + " =====");
		for (int i = 0; i < n; i++)
		{
			copyInput();
			int functionStartOffset = offset;
			int functionStartOutOffset = outOffset;
//			printOffset();
//			int start = offset;
			int methodIndex = (int)readU32();
			int maxStack = (int)readU32();
			int localCount = (int)readU32();
			int initScopeDepth = (int)readU32();
			int maxScopeDepth = (int)readU32();
			copyInput();
			int outputDelta = outOffset-offset;
			int codeLengthOffset = offset;
			int codeLength = (int)readU32();
//			if (showByteCode)
//			{
//				for (int x = start; x < offset; x++)
//				{
//					out.print(hex(abc[(int)x]) + " ");
//				}
//				for (int x = offset - start; x < 7; x++)
//				{
//					out.print("   ");
//				}
//			}
			MethodInfo mi = methods[methodIndex];
			System.out.println("===== " + mi + " =====");
//			out.print(traitKinds[mi.kind] + " ");
//			out.print(mi.className + "::" + mi.name + "(");
//			for (int x = 0; x < mi.paramCount - 1; x++)
//			{
//				out.print(multiNameConstants[mi.params[x]].toString() + ", ");
//			}
//			if (mi.paramCount > 0)
//				out.print(multiNameConstants[mi.params[mi.paramCount - 1]].toString());
//			out.print("):");
//			out.println(multiNameConstants[mi.returnType].toString());
//			printOffset();
//			out.print("maxStack:" + maxStack + " localCount:" + localCount + " ");
//			out.println("initScopeDepth:" + initScopeDepth + " maxScopeDepth:" + maxScopeDepth);
			
			int start = offset;
			int stopAt = codeLength + offset;

			TreeSet<Integer> boundaries = new TreeSet<Integer>();
			boundaries.add(offset);
			Vector<Fixup> fixups = new Vector<Fixup>();
			HashMap<Integer, Integer> jumps = new HashMap<Integer, Integer>();
			
			while (offset < stopAt)
			{
//				start = offset;
//				printOffset();
				int opcode = abc[offset++] & 0xFF;
				
				/*if (opcode == OP_label || labels.hasLabelAt(offset - 1)) 
				{
					s = labels.getLabelAt(offset - 1) + ":";
					while (s.length() < 4)
						s += " ";
				}
				else
					s = "    ";*/
				
				
				switch (opcode)
				{
				case OP_jump:
					int delta = readS24();
					int targ = offset + delta;
					boundaries.add(offset);
					boundaries.add(targ);
					jumps.put(offset, targ);
					break;
				case OP_returnvalue:
				case OP_returnvoid:
					boundaries.add(offset);
					jumps.put(offset, 0);
					break;
				case OP_lookupswitch:
					int pos = offset - 1;
					fixups.add(new Fixup(offset, pos));
					int target = pos + readS24();
					boundaries.add(target);
					int maxindex = (int)readU32();
//					s += "default:" + labels.getLabelAt(target); // target + "("+(target-pos)+")"
//					s += " maxcase:" + Integer.toString(maxindex);
					for (int m = 0; m <= maxindex; m++) 
					{
						fixups.add(new Fixup(offset, pos));
						target = pos + readS24();
						boundaries.add(target);
//						s += " " + labels.getLabelAt(target); // target + "("+(target-pos)+")"
					}
					break;
				case OP_iftrue:		case OP_iffalse:
				case OP_ifeq:		case OP_ifne:
				case OP_ifge:		case OP_ifnge:
				case OP_ifgt:		case OP_ifngt:
				case OP_ifle:		case OP_ifnle:
				case OP_iflt:		case OP_ifnlt:
				case OP_ifstricteq:	case OP_ifstrictne:
					fixups.add(new Fixup(offset, offset+3));
					delta = readS24();
					targ = offset + delta;
					boundaries.add(targ);
					//s += target + " ("+offset+")"
//					s += labels.getLabelAt(targ);
					break;
					
					
				case OP_debugfile:
				case OP_pushstring:
					readU32();
					break;
				case OP_pushnamespace:
					readU32();
					break;
				case OP_pushint:
					readU32();
					break;
				case OP_pushuint:
					readU32();
					break;
				case OP_pushdouble:
					readU32();
					break;
				case OP_getsuper: 
				case OP_setsuper: 
				case OP_getproperty: 
				case OP_initproperty: 
				case OP_setproperty: 
				case OP_getlex: 
				case OP_findpropstrict: 
				case OP_findproperty:
				case OP_finddef:
				case OP_deleteproperty: 
				case OP_istype: 
				case OP_coerce: 
				case OP_astype: 
				case OP_getdescendants:
					readU32();
					break;
				case OP_constructprop:
				case OP_callproperty:
				case OP_callproplex:
				case OP_callsuper:
				case OP_callsupervoid:
				case OP_callpropvoid:
					readU32();
					readU32();
					break;
				case OP_newfunction:
					readU32();
					// abc.methods[method_id].anon = true  (do later?)
					break;
				case OP_callstatic:
					readU32();
					readU32();
					break;
				case OP_newclass: 
					readU32();
					break;
				case OP_inclocal:
				case OP_declocal:
				case OP_inclocal_i:
				case OP_declocal_i:
				case OP_getlocal:
				case OP_kill:
				case OP_setlocal:
				case OP_debugline:
				case OP_getglobalslot:
				case OP_getslot:
				case OP_setglobalslot:
				case OP_setslot:
				case OP_pushshort:
				case OP_newcatch:
					readU32();
					break;
				case OP_debug:
					offset++; 
					readU32();
					offset++;
					readU32();
					break;
				case OP_newobject:
					readU32();
					break;
				case OP_newarray:
					readU32();
					break;
				case OP_call:
				case OP_construct:
				case OP_constructsuper:
					readU32();
					break;
				case OP_pushbyte:
				case OP_getscopeobject:
					offset++;
					break;
				case OP_hasnext2:
					readU32();
					readU32();
				default:
					/*if (opNames[opcode] == ("0x"+opcode.toString(16).toUpperCase()))
					 s += " UNKNOWN OPCODE"*/
//						throw new DecompileException();
					break;
				}
//				if (showByteCode)
//				{
//					for (int x = start; x < offset; x++)
//					{
//						out.print(hex(abc[(int)x]) + " ");
//					}
//					for (int x = offset - start; x < 7; x++)
//					{
//						out.print("   ");
//					}
//				}
//				out.println(s);
			}
			boundaries.add(offset);
			
			if (params.reorderCode)
				try {
					Integer[] boundArr = new Integer[boundaries.size()];
					boundaries.toArray(boundArr);
	
					//if (mi.toString() == "function com.popcap.flash.games.bej3.blitz:_-B6::private:_-Fu(:XML)::void")
					//if (mi.name.endsWith("_-Fu"))
						//boundArr = null;
					
					// Split blocks
					System.out.println("=== Boundaries ===");
					for (int j=0; j<boundArr.length; j++)
						System.out.println(boundArr[j]);
					System.out.println("=== /Boundaries ===");
					
					CodeBlocks blocks = new CodeBlocks();
					for (int j=1; j<boundArr.length; j++)
					{
						CodeBlock block = blocks.add(boundArr[j-1]);
						block.length = boundArr[j] - boundArr[j-1];
						if (jumps.containsKey(boundArr[j]))
						{
							int target = jumps.get(boundArr[j]);
							if (target == 0) // return from function
								block.nextAddr = 0;
							else // unconditional jump
							{
								block.length -= 4;
								block.nextAddr = jumps.get(boundArr[j]);
							}
						}
						else
						{
							if (j == boundArr.length-1) // last block
								block.nextAddr = 0;
							else
								block.nextAddr = boundArr[j];
						}
					}
					
					System.out.println("=== Blocks ===");
					for (CodeBlock block : blocks.blocks)
						System.out.println(block.origStart + ".." + (block.origStart + block.length) + " -> " + block.nextAddr);
					System.out.println("=== /Blocks ===");
	
					/*System.out.println("=== Fixups ===");
					for (Fixup fixup : fixups)
					{
						int targ = fixup.getTargetAddress();
						System.out.print(fixup.address + ": " + fixup.base + " -> " + targ);
						if (blocks.findBlock(targ, RangeBound.IncludeStart)==null)
							System.out.print(" !!!");
						System.out.println();
					}
					System.out.println("=== /Fixups ===");*/
					
					// Reorder blocks
					int blockOffset = start;
					CodeBlock currentBlock = blocks.blocks.get(0);
					Vector<CodeBlock> writtenBlocks = new Vector<CodeBlock>();
					while (currentBlock != null)
					{
						writtenBlocks.add(currentBlock);
						currentBlock.written = true;
						if (abc[currentBlock.origStart] != OP_label)
						{
							currentBlock.writeLabel = true;
							blockOffset++;
						}
						currentBlock.newStart = blockOffset;
						blockOffset += currentBlock.length;
						CodeBlock nextBlock = null;
						if (currentBlock.nextAddr != 0)
						{
							nextBlock = blocks.findBlock(currentBlock.nextAddr, RangeBound.IncludeStart);
							if (nextBlock.written)
								nextBlock = null;
						}
						if (nextBlock == null)
							nextBlock = blocks.findNewReferencedBlock(fixups);
	
						if (currentBlock.nextAddr != 0)
							if (nextBlock == null || nextBlock.origStart != currentBlock.nextAddr)
							{
								currentBlock.jumpTo = blocks.findBlock(currentBlock.nextAddr, RangeBound.IncludeStart);
								blockOffset += 4;
							}
						currentBlock = nextBlock;
					}
					int newCodeLength = blockOffset - start;
					outputDelta += getU32length(newCodeLength) - getU32length(codeLength); 
	
					System.out.println("Function offset: " + functionStartOffset + " -> " + functionStartOutOffset);
					System.out.println("Length offset: " + codeLengthOffset + " -> " + outOffset);
					System.out.println("Code offset: " + start + " -> " + (outOffset + getU32length(newCodeLength)));
					System.out.println("Code length: " + codeLength + " -> " + newCodeLength);
					
					System.out.println("=== ReorderedBlocks ===");
					for (CodeBlock block : writtenBlocks)
						System.out.println((outputDelta + block.newStart) + ".." + (outputDelta + block.newStart+block.length) + " <- " + block.origStart + ".." + (block.origStart+block.length));
					System.out.println("=== /ReorderedBlocks ===");
					
					System.out.println("=== AppliedFixups ===");
					for (Fixup fixup : fixups)
					{
						int oldAddress = fixup.getTargetAddress();
						System.out.print(fixup.address + ": " + fixup.base + " -> " + oldAddress + " ==>> ");
						int newAddress = fixup.apply(blocks);
						if (newAddress == 0)
							System.out.println("x");
						else
							System.out.println((outputDelta + blocks.translateAddress(fixup.address, RangeBound.IncludeStart)) + ": " + (outputDelta + blocks.translateAddress(fixup.base, RangeBound.IncludeEnd)) + " -> " + (outputDelta + newAddress));
					}		
					System.out.println("=== /AppliedFixups ===");
					
					// Write blocks
					System.out.println("outOffset before writing newCodeLength: " + outOffset);
					discardInput();
					writeU32(newCodeLength);
					System.out.println("outOffset after writing newCodeLength: " + outOffset);
					for (CodeBlock block : writtenBlocks)
					{
						if (block.writeLabel)
						{
							out.write(OP_label);
							outOffset++;
						}
						if (outOffset != block.newStart + outputDelta)
							//block = null;
							System.out.println("Block starts at " + outOffset + ", should be at " + (block.newStart + outputDelta) + " !");
						out.write(abc, block.origStart, block.length);
			    		outOffset += block.length;
						if (block.jumpTo != null)
						{
							out.write(OP_jump);
				    		outOffset++;
							writeS24(block.jumpTo.jumpTarget() - (block.newStart + block.length + 4));
						}
					}
					
				} catch (Exception e) {
					System.out.println("Error with parsing method " + mi + " :");
					e.printStackTrace();
				}
			copyInput();
			
			int exCount = (int)readU32();
//			printOffset();
//			out.println(exCount + " Extras");
			for (int j = 0; j < exCount; j++)
			{
//				start = offset;
//				printOffset();
				int from = (int)readU32();
				int to = (int)readU32();
				int target = (int)readU32();
				int typeIndex = (int)readU32();
				int nameIndex = (int)readU32();
//				if (showByteCode)
//				{
//					for (int x = start; x < offset; x++)
//					{
//						out.print(hex(abc[(int)x]) + " ");
//					}
//				}
//				out.print(multiNameConstants[nameIndex] + " ");
//				out.print("type:" + multiNameConstants[typeIndex] + " from:" + from + " ");
//				out.println("to:" + to + " target:" + target);
			}
			
			if (params.reorderCode)
			{
				discardInput();
				writeU32(0); // write 0 extras
			}
			
			int numTraits = (int)readU32(); // number of traits
//			printOffset();
//			out.println(numTraits + " Traits Entries");
			for (int j = 0; j < numTraits; j++)
			{
//				printOffset();
//				start = offset;
				String s = multiNameConstants[(int)readU32()].toString(); // eat trait name;
				int b =  abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00:	// slot
					case 0x06:	// const
						readU32();	// id
						readU32();	// type
						int index = (int)readU32();	// index;
						if (index != 0)
							offset++;	// kind
						break;
					case 0x04:	// class
						readU32();	// id
						readU32();	// value;
						break;
					default:
						readU32();	// id
						readU32();  // method
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int)readU32();	// metadata count
					for (int k = 0; k < val; k++)
					{
						readU32();	// metadata
					}
				}
//				if (showByteCode)
//				{
//					for (int x = start; x < offset; x++)
//					{
//						out.print(hex(abc[(int)x]) + " ");
//					}
//				}
//				out.println(s);
			}
//			out.println("");
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
		
		public String toString()
		{
			String s = "";
			
			String[] nsSet;
			int len;
			int j;
			
			switch (kind)
			{
				case 0x07:	// QName
				case 0x0D:
					s = namespaceConstants[long1] + ":";
					s += stringConstants[long2];
					break;
				case 0x0F:	// RTQName
				case 0x10:
					s = stringConstants[long1];
					break;
				case 0x11:	// RTQNameL
				case 0x12:
					s = "RTQNameL";
					break;
				case 0x13:	// NameL
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
		
		public String toString() {
			String result = "";
			result += traitKinds[kind] + " ";
			result += className + "::" + name + "(";
			for (int x = 0; x < paramCount - 1; x++)
			{
				result += multiNameConstants[params[x]].toString() + ", ";
			}
			if (paramCount > 0)
				result += multiNameConstants[params[paramCount - 1]].toString();
			result += "):";
			result += multiNameConstants[returnType].toString();
			return result;
		}
		
	}
	
	class Fixup
	{
		int address; // where is the fixup S24
		int base;    // base of the fixup relative address
		
		public Fixup(int address, int base) {
			this.address = address;
			this.base = base;
		}

		/// Returns the "original" address of the fixup target. Only works before the fixups are applied.
		public int getTargetAddress() {
			return base + readS24(abc, address);
		}
		
		public int apply(CodeBlocks blocks) {
			CodeBlock block = blocks.findBlock(base, RangeBound.IncludeEnd);
			if (!block.written)
				return 0;
			int targetAddress = getTargetAddress();
			CodeBlock child = blocks.findBlock(targetAddress, RangeBound.IncludeBoth);
			if (!child.written)
				block = null;
			int newAddress = child.translateAddress(targetAddress);
			if (newAddress == child.newStart && child.writeLabel)
				newAddress--;

			writeS24(abc, address, newAddress - block.translateAddress(base));
			return newAddress;
		}
	}

	class CodeBlock
	{
		int origStart, newStart;
		int length;
		boolean written = false;
		CodeBlock jumpTo = null;
		boolean writeLabel = false;
		int nextAddr = 0;
		
		public int translateAddress(int address) {
			return newStart + (address - origStart);
		}
		public int jumpTarget() {
			return newStart - (writeLabel ? 1 : 0);
		}
	}
	
	enum RangeBound
	{
		IncludeStart,
		IncludeEnd,
		IncludeBoth
	}
	
	class CodeBlocks
	{
		public Vector<CodeBlock> blocks = new Vector<CodeBlock>();
		
		public CodeBlock add(int offset) {
			CodeBlock block = new CodeBlock();
			block.origStart = offset;
			blocks.add(block);
			return block;
		}

		public CodeBlock findBlock(int addr, RangeBound bounds) {
			// TODO: binary search?
			if (bounds != RangeBound.IncludeEnd)
				for (CodeBlock block : blocks)
					if (block.length > 0)
					{
						if (addr >= block.origStart && addr < block.origStart + block.length)
							return block;
					}
					else
						if (addr == block.origStart)
							return block;
			
			if (bounds != RangeBound.IncludeStart)
				for (CodeBlock block : blocks)
					if (block.length > 0)
					{
						if (addr > block.origStart && addr <= block.origStart + block.length)
							return block;
					}
					else
						if (addr == block.origStart)
							return block;
			
			return null;
		}
		
		public CodeBlock findNewReferencedBlock(Vector<Fixup> fixups) {
			// TODO: currently O(m*n), optimize?
			for (Fixup fixup : fixups)
			{
				CodeBlock parent = findBlock(fixup.address, RangeBound.IncludeStart);
				if (parent.written)
				{
					CodeBlock child = findBlock(fixup.getTargetAddress(), RangeBound.IncludeBoth);
					if (!child.written)
						return child;
				}
			}
			return null;
		}

		public int translateAddress(int address, RangeBound bounds) {
			CodeBlock block = findBlock(address, bounds);
			if (!block.written)
				block = null;
			return block.translateAddress(address);
		}
	}
}