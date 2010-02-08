// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

package net.thecybershadow.swf.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class DeobfuscationParameters
{
	HashMap<String, String> dictionary = new HashMap<String, String>();
	public boolean reorderCode = true;
	static HashSet<String> reservedWords = new HashSet<String>();
	static final String[] reservedWordsArr = new String[] { "as", "break", "case", "catch", "class", "const", "continue", "default", "delete", "do", "else", "extends", "false", "finally", "for", "function", "get", "if", "implements", "import", "in", "include", "instanceof", "interface", "is", "namespace", "new", "null", "package", "private", "protected", "public", "return", "set", "super", "switch", "this", "throw", "true", "try", "typeof", "use", "var", /*"void", */"while", "with" };
	
	{
		for (String word : reservedWordsArr)
			reservedWords.add(word);
	}
	
	public String deobfuscateString(String s)
	{
		if (s.startsWith("_-"))
			s = "__" + s.substring(2);
		else
		if (reservedWords.contains(s))
			s += "_";
		
		if (dictionary.containsKey(s))
			s = dictionary.get(s);
		return s;
	}

	public String deobfuscateFQN(String className)
	{
		String[] segments = className.split("\\.");
		for (int i=0; i<segments.length; i++)
			segments[i] = deobfuscateString(segments[i]);
		String result = segments[0];
		for (int i=1; i<segments.length; i++)
			result += "." + segments[i];
		return result;
	}

	public void loadDictionary(String fileName) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(fileName)); 
		String line;
		while ((line = r.readLine()) != null)
		{
			int tabPos = line.indexOf("\t");
			if (tabPos < 0)
				continue;
			dictionary.put(line.substring(0, tabPos), line.substring(tabPos+1));
		}
		r.close();
	}
}
