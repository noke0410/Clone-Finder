package ccfinderx.handlers;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import ccfinderx.ui.dialogs.ProjectDialog;
import ccfinderx.utilities.CcfxCommandLine;

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
		ArrayList<String> selectedProjectsLocation;
		
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
		
		selectedProjectsLocation = new ArrayList<String>();
		for (String projectName : dialog.getSelectedProjects())
		{
			project = wsRoot.getProject(projectName);
			selectedProjectsLocation.add(project.getLocation().toString());
		}
		
		//CCFX參數
		CcfxCommandLine ccfxCmd = new CcfxCommandLine();
		
		Runtime rt = Runtime.getRuntime();
		Process proc;
		
		//執行CCFX
		try {
			proc = rt.exec(ccfxCmd.findFile(selectedProjectsLocation));
            int exitVal = proc.waitFor();
            System.out.println ("ExitValue: " + exitVal);
		} catch (Exception e) {
		}
		
		return null;
	}
}
