package ccfinderx.utilities;

import java.util.ArrayList;
import java.util.Arrays;

public class CcfxCommandLine
{
	/* *
	 * ccfx [MODE] [-h] OTHER_PARAMETERS...
	 * Mode
	 *  d: detect code clones
	 *  f: find files
	 *  m: calculate metrics
	 *  p: pretty-print
	 *  s: make subset of clone data
	 * */
	public static final String ccfxExecutionDirectory = System.getProperty("user.dir") + "\\plugins\\ccfx\\bin\\";
	public static final String ccfxExecutionFile = ccfxExecutionDirectory + "ccfx.exe";

	public static String preprocessScript = "";
	public static String tempFileName = "";
	public static String cloneDataFileName = "";

	public CcfxCommandLine()
	{
		preprocessScript = "java";
		tempFileName = TemporaryFileManager.createTemporaryFileName();
		cloneDataFileName = TemporaryFileManager.getFileNameOnTemporaryDirectory("a.ccfxd");
	}

	public String[] findFiles(String outPutFileName, String[] directories)
	{
		/* *
		 * Usage 1: ccfx F PREPROCESS_SCRIPT OPTIONS directories...
		 *   Finds files that matches the preprocess_script.
		 * Option
		 *   -a: prints out each file in full path (absolute path).
		 *   -e: prints out extensions that are associated with the preprocess script.
		 *   -l n: generates lines for preprocessed-file directories.
		 *   -l is: generates lines for group separators.
		 *   -o out.txt: output file.
		 *   --listoptions: prints out preprocess options.
		 *   --preprocessedextension: prints out extension of preprocessed file.
		 *   --getdefaultparameterizing: prints out default matching option for each parameter names.
		 * Usage 2: ccfx F -c [-o output]
		 *   Prints out available encodings.
		 * Usage 3: ccfx F -p [-o output]
		 *   Prints out names of the available preprocess scripts.
		 * Usage 4: ccfx F -n [-a] [-o output] directories...
		 *   Prints out preprocessed-file directories.
		 * */

		ArrayList<String> cmdarraylist = new ArrayList<String>();

		cmdarraylist.add(ccfxExecutionFile);
		cmdarraylist.add("F");
		cmdarraylist.add(preprocessScript);
		if (outPutFileName == null || outPutFileName.length() == 0)
		{
			outPutFileName = tempFileName;
		}
		cmdarraylist.addAll(Arrays.asList(new String[] { "-o", outPutFileName }));

		for (String directory : directories)
		{
			cmdarraylist.add(directory);
		}

		return cmdarraylist.toArray(new String[0]);
	}

	public String[] detectCodeClones(String encodingName, String fileListPath, int minCloneLength, 
			int minTKS, int shaperLevel, boolean usePMatch, int chunkSize, int maxWorkerThreads, 
			String[] preprocessFileDirectories, boolean usePrescreening)
	{
		/* *
		 * Usage: ccfx D PREPROCESS_SCRIPT OPTIONS inputfiles...
		 *   Detects code clones from input files.
		 *   (Use "ccfx F -p" to obtain a list of available preprocess scripts.)
		 * Option
		 *   -b number: minimum length of a clone fragment. (50)
		 *   -c encoding: encoding of input files. (-c char)
		 *   -d directory: finds input files from the directory.
		 *   -dn dir: shortcut for '-d dir -n dir'
		 *   -i listfile: list of input files.
		 *   -ig fileid,...: makes a file group.
		 *   -j-: don't use majoritarian shaper.
		 *   -k size: chunk size (60M).
		 *   -k-: on-memory detection. don't divide the input into chunks.
		 *   -mr name: don't detect clones from files with the named remark.
		 *   -n dir: the directory where preprocessed files are created.
		 *   -o out.ccfxd: outputs clone-data file name.
		 *   -p: performs only preprocessing.
		 *   -pp-: exact match. *experimental*
		 *   -r value: an option passed to preprocess script.
		 *   -s number: =0 don't use block shaper, =1 easy, =2 soft, =3 hard. (2)
		 *   -s-: don't use block shaper.
		 *   -t number: minimum size of token set of a code fragment. (12)
		 *   -u-: don't use p-match, which checks unification of parameters.
		 *   -v: verbose option.
		 *   -w params: detects within file/between files/between groups (-w w+f+g+).
		 *   --errorfiles=output: don't stop detection when syntax errors found. *experimental*
		 *   --prescreening=LEN.gt.num: don't detect clones from source files of length > num
		 *   --threads=number: max working threads (0).
		 * */

		ArrayList<String> cmdarraylist = new ArrayList<String>();
		cmdarraylist.add(ccfxExecutionFile);
		cmdarraylist.add("D");
		cmdarraylist.add(preprocessScript);
		cmdarraylist.addAll(Arrays.asList(new String[] { "-i", fileListPath })); //$NON-NLS-1$
		if (encodingName != null && encodingName.length() > 0)
		{
			cmdarraylist.addAll(Arrays.asList(new String[] { "-c", encodingName })); //$NON-NLS-1$
		}
		cmdarraylist.addAll(Arrays.asList(new String[] { "-b", String.valueOf(minCloneLength) })); //$NON-NLS-1$
		cmdarraylist.addAll(Arrays.asList(new String[] { "-t", String.valueOf(minTKS) })); //$NON-NLS-1$
		if (shaperLevel >= 0)
		{
			cmdarraylist.addAll(Arrays.asList(new String[] { "-s", String.valueOf(shaperLevel) })); //$NON-NLS-1$
		}
		cmdarraylist.addAll(Arrays.asList(new String[] { "-u", usePMatch ? "+" : "-" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (String p : preprocessFileDirectories)
		{
			cmdarraylist.addAll(Arrays.asList(new String[] { "-n", p })); //$NON-NLS-1$
		}
		cmdarraylist.addAll(Arrays.asList(new String[] { "-k",  //$NON-NLS-1$
				chunkSize != 0 ? String.valueOf(chunkSize) + "M" : "0" })); //$NON-NLS-1$ //$NON-NLS-2$
		if (maxWorkerThreads > 0) {
			cmdarraylist.add("--threads=" + String.valueOf(maxWorkerThreads)); //$NON-NLS-1$
		}
		cmdarraylist.add("-v"); //$NON-NLS-1$
		cmdarraylist.addAll(Arrays.asList(new String[] { "-o", cloneDataFileName }));
		if (usePrescreening)
		{
			cmdarraylist.addAll(Arrays.asList(new String[] { "-mr", "masked" }));   //$NON-NLS-1$ //$NON-NLS-2$
		}

		return cmdarraylist.toArray(new String[0]);
	}
}
