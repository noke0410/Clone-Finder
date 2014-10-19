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
		ArrayList<String> selectedProjects;
		
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
		
		selectedProjects = new ArrayList<String>();
		for (String projectName : dialog.getSelectedProjects())
		{
			project = wsRoot.getProject(projectName);
			selectedProjects.add(project.getLocation().toString());
		}
		
		return null;
	}
}
