package ccfinderx.utilities;

import java.util.ArrayList;
import java.util.Arrays;

public class CcfxCommandLine
{
	public static String ccfxExecutionFile = "";
	public static String scriptName = "";
	public static String tempFileName = "";
	public static String cloneDataFileName = "";

	public CcfxCommandLine()
	{
		ccfxExecutionFile = "\\ccfx\\ccfx.exe";
		scriptName = "java";
		tempFileName = TemporaryFileManager.createTemporaryFileName();
		cloneDataFileName = TemporaryFileManager.getFileNameOnTemporaryDirectory("a.ccfxd");
	}

	public void setScriptName(String scriptname)
	{
		scriptName = scriptname;
	}

	public String[] findFile(ArrayList<String> directories)
	{
		ArrayList<String> cmdarraylist = new ArrayList<String>();
		
		cmdarraylist.add(ccfxExecutionFile);
		cmdarraylist.add("F");
		cmdarraylist.add(scriptName);
		cmdarraylist.addAll(Arrays.asList(new String[] { "-o", tempFileName }));
		
		for (String directory : directories)
		{
			cmdarraylist.add(directory);
		}
		
		return cmdarraylist.toArray(new String[0]);
	}
}
