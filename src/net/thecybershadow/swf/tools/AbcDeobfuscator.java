// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

// readBodies does the actual work. It also prints some debug information to stdout.

package net.thecybershadow.swf.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

import flash.util.FileUtils;

public class AbcDeobfuscator extends AbcProcessor
{
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2)
		{
			System.err.println("Usage: AbcDeobfuscator [-noreorder] [-dict dictionary.txt] input.abc output.abc");
			return;
		}

		String input = null, output = null;
		DeobfuscationParameters params = new DeobfuscationParameters();

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-noreorder"))
				params.reorderCode = false;
			else if (args[i].equals("-dict"))
				params.loadDictionary(args[++i]);
			else if (input == null)
				input = args[i];
			else if (output == null)
				output = args[i];
			else
				System.err.println("Ignoring extraneous argument: " + args[i]);
		}

		if (input == null || output == null)
		{
			System.err.println("Input/output not specified");
			return;
		}

		File fin = new File(input);
		URL url = FileUtils.toURL(fin);
		InputStream in = url.openStream();
		byte[] abc = new byte[(int) fin.length()];
		in.read(abc);
		AbcDeobfuscator d = new AbcDeobfuscator(abc, params);
		d.process();
		FileOutputStream out = new FileOutputStream(output);
		out.write(d.toByteArray());
		out.close();
	}

	private final DeobfuscationParameters params;

	public AbcDeobfuscator(byte[] abc, DeobfuscationParameters params)
	{
		super(abc);
		this.params = params;
	}

	@Override
	protected void readStringConstantPool()
	{
		// TODO: only deobfuscate strings which are used as code identifiers.
		// This requires deserialization/serialization.
		long n = readU32();
		stringConstants = new String[(n > 0) ? (int) n : 1];
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

	@Override
	protected void readBodies()
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
			copyInput();
			int outputDelta = outOffset - offset;
			int codeLengthOffset = offset;
			int codeLength = (int) readU32();
			// if (showByteCode)
			// {
			// for (int x = start; x < offset; x++)
			// {
			// out.print(hex(abc[(int)x]) + " ");
			// }
			// for (int x = offset - start; x < 7; x++)
			// {
			// out.print("   ");
			// }
			// }
			MethodInfo mi = methods[methodIndex];
			System.out.println("===== " + mi + " =====");
			// out.print(traitKinds[mi.kind] + " ");
			// out.print(mi.className + "::" + mi.name + "(");
			// for (int x = 0; x < mi.paramCount - 1; x++)
			// {
			// out.print(multiNameConstants[mi.params[x]].toString() + ", ");
			// }
			// if (mi.paramCount > 0)
			// out.print(multiNameConstants[mi.params[mi.paramCount -
			// 1]].toString());
			// out.print("):");
			// out.println(multiNameConstants[mi.returnType].toString());
			// printOffset();
			// out.print("maxStack:" + maxStack + " localCount:" + localCount +
			// " ");
			// out.println("initScopeDepth:" + initScopeDepth +
			// " maxScopeDepth:" + maxScopeDepth);

			int start = offset;
			int stopAt = codeLength + offset;

			TreeSet<Integer> boundaries = new TreeSet<Integer>();
			boundaries.add(offset);
			Vector<Fixup> fixups = new Vector<Fixup>();
			HashMap<Integer, Integer> jumps = new HashMap<Integer, Integer>();

			while (offset < stopAt)
			{
				// start = offset;
				// printOffset();
				int opcode = abc[offset++] & 0xFF;

				/*
				 * if (opcode == OP_label || labels.hasLabelAt(offset - 1)) { s
				 * = labels.getLabelAt(offset - 1) + ":"; while (s.length() < 4)
				 * s += " "; } else s = "    ";
				 */

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
						int maxindex = (int) readU32();
						// s += "default:" + labels.getLabelAt(target); //
						// target + "("+(target-pos)+")"
						// s += " maxcase:" + Integer.toString(maxindex);
						for (int m = 0; m <= maxindex; m++)
						{
							fixups.add(new Fixup(offset, pos));
							target = pos + readS24();
							boundaries.add(target);
							// s += " " + labels.getLabelAt(target); // target +
							// "("+(target-pos)+")"
						}
						break;
					case OP_iftrue:
					case OP_iffalse:
					case OP_ifeq:
					case OP_ifne:
					case OP_ifge:
					case OP_ifnge:
					case OP_ifgt:
					case OP_ifngt:
					case OP_ifle:
					case OP_ifnle:
					case OP_iflt:
					case OP_ifnlt:
					case OP_ifstricteq:
					case OP_ifstrictne:
						fixups.add(new Fixup(offset, offset + 3));
						delta = readS24();
						targ = offset + delta;
						boundaries.add(targ);
						// s += target + " ("+offset+")"
						// s += labels.getLabelAt(targ);
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
						// abc.methods[method_id].anon = true (do later?)
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
						/*
						 * if (opNames[opcode] ==
						 * ("0x"+opcode.toString(16).toUpperCase())) s +=
						 * " UNKNOWN OPCODE"
						 */
						// throw new DecompileException();
						break;
				}
				// if (showByteCode)
				// {
				// for (int x = start; x < offset; x++)
				// {
				// out.print(hex(abc[(int)x]) + " ");
				// }
				// for (int x = offset - start; x < 7; x++)
				// {
				// out.print("   ");
				// }
				// }
				// out.println(s);
			}
			boundaries.add(offset);

			if (params.reorderCode)
				try
				{
					Integer[] boundArr = new Integer[boundaries.size()];
					boundaries.toArray(boundArr);

					// if (mi.toString() ==
					// "function com.popcap.flash.games.bej3.blitz:_-B6::private:_-Fu(:XML)::void")
					// if (mi.name.endsWith("_-Fu"))
					// boundArr = null;

					// Split blocks
					System.out.println("=== Boundaries ===");
					for (int j = 0; j < boundArr.length; j++)
						System.out.println(boundArr[j]);
					System.out.println("=== /Boundaries ===");

					CodeBlocks blocks = new CodeBlocks();
					for (int j = 1; j < boundArr.length; j++)
					{
						CodeBlock block = blocks.add(boundArr[j - 1]);
						block.length = boundArr[j] - boundArr[j - 1];
						if (jumps.containsKey(boundArr[j]))
						{
							int target = jumps.get(boundArr[j]);
							if (target == 0) // return from function
								block.nextAddr = 0;
							else
							// unconditional jump
							{
								block.length -= 4;
								block.nextAddr = jumps.get(boundArr[j]);
							}
						}
						else
						{
							if (j == boundArr.length - 1) // last block
								block.nextAddr = 0;
							else
								block.nextAddr = boundArr[j];
						}
					}

					System.out.println("=== Blocks ===");
					for (CodeBlock block : blocks.blocks)
						System.out.println(block.origStart + ".." + (block.origStart + block.length) + " -> "
								+ block.nextAddr);
					System.out.println("=== /Blocks ===");

					/*
					 * System.out.println("=== Fixups ==="); for (Fixup fixup :
					 * fixups) { int targ = fixup.getTargetAddress();
					 * System.out.print(fixup.address + ": " + fixup.base +
					 * " -> " + targ); if (blocks.findBlock(targ,
					 * RangeBound.IncludeStart)==null) System.out.print(" !!!");
					 * System.out.println(); }
					 * System.out.println("=== /Fixups ===");
					 */

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
						System.out.println((outputDelta + block.newStart) + ".."
								+ (outputDelta + block.newStart + block.length) + " <- " + block.origStart + ".."
								+ (block.origStart + block.length));
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
							System.out.println((outputDelta + blocks.translateAddress(fixup.address,
									RangeBound.IncludeStart))
									+ ": "
									+ (outputDelta + blocks.translateAddress(fixup.base, RangeBound.IncludeEnd))
									+ " -> " + (outputDelta + newAddress));
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
							// block = null;
							System.out.println("Block starts at " + outOffset + ", should be at "
									+ (block.newStart + outputDelta) + " !");
						out.write(abc, block.origStart, block.length);
						outOffset += block.length;
						if (block.jumpTo != null)
						{
							out.write(OP_jump);
							outOffset++;
							writeS24(block.jumpTo.jumpTarget() - (block.newStart + block.length + 4));
						}
					}

				}
				catch (Exception e)
				{
					System.out.println("Error with parsing method " + mi + " :");
					e.printStackTrace();
				}
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

			if (params.reorderCode)
			{
				discardInput();
				writeU32(0); // write 0 extras
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

	class Fixup
	{
		int address; // where is the fixup S24
		int base; // base of the fixup relative address

		public Fixup(int address, int base)
		{
			this.address = address;
			this.base = base;
		}

		// Returns the "original" address of the fixup target. Only works before
		// the fixups are applied.
		public int getTargetAddress()
		{
			return base + readS24(abc, address);
		}

		public int apply(CodeBlocks blocks)
		{
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

		public int translateAddress(int address)
		{
			return newStart + (address - origStart);
		}

		public int jumpTarget()
		{
			return newStart - (writeLabel ? 1 : 0);
		}
	}

	enum RangeBound
	{
		IncludeStart, IncludeEnd, IncludeBoth
	}

	class CodeBlocks
	{
		public Vector<CodeBlock> blocks = new Vector<CodeBlock>();

		public CodeBlock add(int offset)
		{
			CodeBlock block = new CodeBlock();
			block.origStart = offset;
			blocks.add(block);
			return block;
		}

		public CodeBlock findBlock(int addr, RangeBound bounds)
		{
			// TODO: binary search?
			if (bounds != RangeBound.IncludeEnd)
				for (CodeBlock block : blocks)
					if (block.length > 0)
					{
						if (addr >= block.origStart && addr < block.origStart + block.length)
							return block;
					}
					else if (addr == block.origStart)
						return block;

			if (bounds != RangeBound.IncludeStart)
				for (CodeBlock block : blocks)
					if (block.length > 0)
					{
						if (addr > block.origStart && addr <= block.origStart + block.length)
							return block;
					}
					else if (addr == block.origStart)
						return block;

			return null;
		}

		public CodeBlock findNewReferencedBlock(Vector<Fixup> fixups)
		{
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

		public int translateAddress(int address, RangeBound bounds)
		{
			CodeBlock block = findBlock(address, bounds);
			if (!block.written)
				block = null;
			return block.translateAddress(address);
		}
	}
}
