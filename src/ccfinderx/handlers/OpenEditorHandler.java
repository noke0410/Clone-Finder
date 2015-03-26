package ccfinderx.handlers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ccfinderx.model.ClonePair;
import ccfinderx.model.CodeFragment;
import ccfinderx.model.Model;
import ccfinderx.resources.TextColors;
import ccfinderx.ui.editors.MultipleTextPaneEditor;
import ccfinderx.ui.editors.MultipleTextPaneEditorInput;
import ccfinderx.ui.views.CloneSetView;

public class OpenEditorHandler extends AbstractHandler
{

	public OpenEditorHandler()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		IWorkbenchWindow window;
		IWorkbenchPage page;
		IViewPart view;
		CloneSetView cloneSetView;
		MultipleTextPaneEditor multipleTextPaneEditor;
		long id;
		Model currentScope;
		
		String defaultColorConfigString = "{}";
		String colorConfigString = readColorConfigFile();
		pathjson.Converter jsonConv = new pathjson.Converter();
		LinkedHashMap<String, Object> settings = jsonConv.stringToMap(colorConfigString == null ? defaultColorConfigString : colorConfigString);
		
		TextColors.initialize(null, settings);
		
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		page = window.getActivePage();
		
		view = page.findView(CloneSetView.ID);
		
		assert view != null;
		
		cloneSetView = (CloneSetView)view;
		
		currentScope = cloneSetView.getModel();
		
		id = cloneSetView.selectedCloneSetIDs[0];
		long[] cloneSetIDs = new long[] { id };
		
		assert cloneSetIDs != null;
		
		long[] availableIDs = currentScope.getAvailableCloneSetID(cloneSetIDs);
		
		MultipleTextPaneEditorInput input = new MultipleTextPaneEditorInput(id);
		try {
			for (IEditorReference editorReference : page.getEditorReferences())
			{
				assert cloneSetIDs != null;
				if (editorReference.getPartName().equals("Clone Code"))
				{
					page.closeEditor(page.findEditor(editorReference.getEditorInput()), true);
				}
			}
			multipleTextPaneEditor = (MultipleTextPaneEditor)page.openEditor(input, MultipleTextPaneEditor.ID);
			multipleTextPaneEditor.updateModel(cloneSetView.getModel(), false);
			ClonePair[] pairs = currentScope.getClonePairsOfCloneSets(cloneSetIDs);
			if (pairs.length > 0) {
				int ClonePairIndex = (pairs.length + 1) % pairs.length;
				int fileIndex1 = pairs[ClonePairIndex].leftFile;
				int fileIndex2 = pairs[ClonePairIndex].rightFile;
				int[] files = new int[] { fileIndex1, fileIndex2 };
				multipleTextPaneEditor.setSelection(files, false);
				multipleTextPaneEditor.clearInitalTopPosition();
				multipleTextPaneEditor.setClonePairSelection(pairs[ClonePairIndex]);
			}
			//multipleTextPaneEditor.updateModel(cloneSetView.getModel(), false);
			//multipleTextPaneEditor.setCloneSelection(cloneSetIDs);
		} catch (PartInitException e) {
		}
		/*
		ClonePair[] pairs = currentScope.getClonePairsOfCloneSets(cloneSetIDs);
		
		MultipleTextPaneEditorInput input = new MultipleTextPaneEditorInput(id);
		try {
			multipleTextPaneEditor = (MultipleTextPaneEditor)page.openEditor(input, MultipleTextPaneEditor.ID);
			if (pairs.length > 0) {
				CodeFragment f = pairs[0].getLeftCodeFragment();
				int[] files = new int[] { f.file };
				multipleTextPaneEditor.updateModel(cloneSetView.getModel(), false);
				//multipleTextPaneEditor.setCloneSelection(cloneSetIDs);
				multipleTextPaneEditor.setSelection(files, false);
				multipleTextPaneEditor.setCodeFragmentSelection(f, id);
				setCodeFragment(f, id, cloneSetView);
			}
		}
		catch (PartInitException e) {
		}
		 */
		
		return null;
	}
	
	private static String readColorConfigFile()
	{
		final String directory = System.getProperty("user.dir") + "\\plugins\\ccfx\\bin\\";
		final String fileName = directory + "colors.json"; //$NON-NLS-1$
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8")); //$NON-NLS-1$
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				builder.append(line);
			}
			in.close();
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			return null;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
}
	

