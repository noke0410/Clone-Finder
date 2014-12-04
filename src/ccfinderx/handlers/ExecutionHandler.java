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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ccfinderx.constants.CcfxDefaultSettings;
import ccfinderx.model.CloneSet;
import ccfinderx.model.Model;
import ccfinderx.ui.dialogs.ProjectDialog;
import ccfinderx.ui.views.CloneSetView;
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

		//generate project list.
		projectList = new ArrayList<String>();
		for (IProject p : projects)
		{
			if (p.getName().equals("RemoteSystemsTempFiles"))
			{
				continue;
			}
			projectList.add(p.getName());
		}

		//select project
		ProjectDialog dialog = new ProjectDialog();
		dialog.setProjectList(projectList);
		dialog.setVisible(true);

		//generate full path list
		selectedProjectDirectories = new ArrayList<String>();
		for (String projectName : dialog.getSelectedProjects())
		{
			project = wsRoot.getProject(projectName);
			selectedProjectDirectories.add(project.getLocation().toString());
		}

		execCCFX(selectedProjectDirectories.toArray(new String[0]));

		return null;
	}
	
	private void execCCFX(String[] preprocessFileDirectories) {
		CcfxCommandLine ccfxCmd = new CcfxCommandLine();
		boolean usePrescreening = false;
		String tempFileName = TemporaryFileManager.createTemporaryFileName();
		String fileListName = null;
		Model rootModel = new Model();

		Runtime rt = Runtime.getRuntime();
		Process proc;

		try {
			//find files
			proc = rt.exec(ccfxCmd.findFiles(tempFileName, preprocessFileDirectories));
			outputProcessStream(proc);
			//prescreening
			fileListName = tempFileName;
			//detect clone data
			String [] cmdarray = ccfxCmd.detectCodeClones(CcfxDefaultSettings.initEncoding, fileListName, 
					CcfxDefaultSettings.initMinimumCloneLength, CcfxDefaultSettings.initMinimumTKS, 
					CcfxDefaultSettings.initShaperLevel, CcfxDefaultSettings.initUsePMatch, 
					CcfxDefaultSettings.initChunkSize, CcfxDefaultSettings.initMaxWorkerThreads, 
					preprocessFileDirectories, usePrescreening);
			proc = rt.exec(cmdarray);
			outputProcessStream(proc);
			
			rootModel.readCloneDataFile(ccfxCmd.cloneDataFileName);
			
			update_model(rootModel);
			
		} catch (Exception e) {
		}
	}
	
	private void update_model(Model newModel) {
		IViewPart iViewPart;
		CloneSetView cloneSetView;
		
		iViewPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(CloneSetView.ID);
		if (iViewPart == null)
		{
			try {
				iViewPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CloneSetView.ID);
			}
			catch (PartInitException e) {
			}
		}
		
		assert iViewPart != null;
		
		cloneSetView = (CloneSetView)iViewPart;
		cloneSetView.updateModel(newModel);
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
