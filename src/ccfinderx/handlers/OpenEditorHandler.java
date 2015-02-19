package ccfinderx.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
	
	private void setCodeFragment(CodeFragment selectedCodeFragment, long cloneSetID, CloneSetView src)
	{
		assert selectedCodeFragment != null;
		
		long[] selectedIDs = new long[] { cloneSetID };
		
	}

}
