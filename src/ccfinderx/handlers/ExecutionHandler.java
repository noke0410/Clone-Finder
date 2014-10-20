package ccfinderx.handlers;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Shell;

import ccfinderx.CCFinderX;
import ccfinderx.ui.dialogs.ProjectDialog;
import ccfinderx.utilities.ExecutionModuleDirectory;
import ccfinderx.utilities.TemporaryFileManager;
import ccfinderx.utilities.TemporaryMouseCursorChanger;

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
		Shell shell;
		//Display display;
		
		//display = new Display();
		shell = new Shell();
		
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
		//private final String tempFileName1 = TemporaryFileManager.createTemporaryFileName();
		String tempFileName = TemporaryFileManager.createTemporaryFileName();
		ArrayList<String> args = new ArrayList<String>();
		args.add("F");
		args.add("java");
		args.addAll(Arrays.asList(new String[] { "-o", tempFileName }));
		
		for (String directory : selectedProjectsLocation)
		{
			args.add(directory);
		}
		
		TemporaryMouseCursorChanger tmcc = new TemporaryMouseCursorChanger(shell);
		try {
			ExecutionModuleDirectory.setDebugMode(true);
			CCFinderX.theInstance.setModuleDirectory(ExecutionModuleDirectory.get());
			int r = CCFinderX.theInstance.invokeCCFinderX(args.toArray(new String[0]));
			if (r != 0) {
				//showErrorMessage("error in invocation of ccfx."); //$NON-NLS-1$
				return null;
			}
		} finally {
			tmcc.dispose();
		}
		
		return null;
	}
}
