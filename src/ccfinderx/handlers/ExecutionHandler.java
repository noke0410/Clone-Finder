package ccfinderx.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import ccfinderx.constants.CcfxDefaultSettings;
import ccfinderx.ui.dialogs.ProjectDialog;
import ccfinderx.utilities.CcfxCommandLine;
import ccfinderx.utilities.TemporaryFileManager;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class ExecutionHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public ExecutionHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot wsRoot = workspace.getRoot();
		IProject[] projects = wsRoot.getProjects();
		IProject project;
		ArrayList<String> projectList;
		ArrayList<String> selectedProjectDirectories;

		projectList = new ArrayList<String>();
		for (IProject p : projects)
		{
			if (p.getName().equals("RemoteSystemsTempFiles"))
			{
				continue;
			}
			projectList.add(p.getName());
		}

		ProjectDialog dialog = new ProjectDialog();
		dialog.setProjectList(projectList);
		dialog.setVisible(true);

		selectedProjectDirectories = new ArrayList<String>();
		for (String projectName : dialog.getSelectedProjects())
		{
			project = wsRoot.getProject(projectName);
			selectedProjectDirectories.add(project.getLocation().toString());
		}

		//CCFX參數
		CcfxCommandLine ccfxCmd = new CcfxCommandLine();
		boolean usePrescreening = false;
		String tempFileName = TemporaryFileManager.createTemporaryFileName();
		String fileListName = null;

		Runtime rt = Runtime.getRuntime();
		Process proc;
		int exitVal;

		//執行CCFX
		try {
			//change directory
			//			File file = new File(ccfxCmd.ccfxExecutionDirectory);
			//			if (file.exists())
			//			{
			//				System.setProperty("user.dir", ccfxCmd.ccfxExecutionDirectory);
			//			}
			//find files
			proc = rt.exec(ccfxCmd.findFiles(tempFileName, selectedProjectDirectories));
			exitVal = proc.waitFor();
			System.out.println("ExitValue: " + exitVal);
			outputProcessStream(proc);
			//prescreening
			fileListName = tempFileName;
			//detect clone data
			String [] cmdarray = ccfxCmd.detectCodeClones(CcfxDefaultSettings.initEncoding, fileListName, 
					CcfxDefaultSettings.initMinimumCloneLength, CcfxDefaultSettings.initMinimumTKS, 
					CcfxDefaultSettings.initShaperLevel, CcfxDefaultSettings.initUsePMatch, 
					CcfxDefaultSettings.initChunkSize, CcfxDefaultSettings.initMaxWorkerThreads, 
					selectedProjectDirectories.toArray(new String[0]), usePrescreening);
			proc = rt.exec(cmdarray);
			exitVal = proc.waitFor();
			System.out.println("ExitValue: " + exitVal);
			outputProcessStream(proc);

		} catch (Exception e) {
		}

		return null;
	}

	private void outputProcessStream(Process proc) {
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		String s = null;
		
		try {
			// read the output from the command
			System.out.println("Here is the standard output of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}
			// read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
		} catch (Exception e) {
		}
	}
}
