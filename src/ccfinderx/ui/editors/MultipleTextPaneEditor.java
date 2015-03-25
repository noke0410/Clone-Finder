package ccfinderx.ui.editors;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import ccfinderx.model.ClonePair;
import ccfinderx.model.CodeFragment;
import ccfinderx.model.Model;
import ccfinderx.ui.editors.TextPane.BeginEnd;
import ccfinderx.utilities.PrepToken;

public class MultipleTextPaneEditor extends EditorPart
{
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "ccfinderx.ui.editors.MultipleTextPaneEditor";
	
	private MultipleTextPaneEditorInput input;
	private ArrayList<TextPane> textPanes;
	private ITextRuler ruler;
	private Composite sc;
	private SashForm sash;
	private Model viewedModel;
	private boolean independentMode;
	private int focusedTextPaneIndex = -1;

	private ArrayList<Listener> addedListeners = new ArrayList<Listener>();
	
	public MultipleTextPaneEditor()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		this.input = (MultipleTextPaneEditorInput) input;
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent)
	{
		// TODO Auto-generated method stub
		int ntipleText = 2;
		
		this.textPanes = new ArrayList<TextPane>();
		
		sc = new Composite(parent, SWT.NONE);
		{
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			sc.setLayout(layout);
		}
		SashForm sashRulerAndPanes = new SashForm(sc, SWT.HORIZONTAL);
		{
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			sashRulerAndPanes.setLayoutData(gridData);
		}
		{
			ruler = new TextRuler(sc);
			ruler.setTextPane(this);
			
			sash = new SashForm(sashRulerAndPanes, SWT.HORIZONTAL);
			{
				GridData gridData = new GridData();
				gridData.horizontalAlignment = GridData.FILL;
				gridData.verticalAlignment = GridData.FILL;
				gridData.grabExcessHorizontalSpace = true;
				gridData.grabExcessVerticalSpace = true;
				sash.setLayoutData(gridData);
			}
			
			for (int i = 0; i < ntipleText; ++i) {
				TextPane pane = new TextPane(sash);
				pane.addScrollListener(ruler);
				for (int j = 0; j < addedListeners.size(); ++j) {
					final Listener listener = addedListeners.get(j);
					pane.addListener(SWT.FocusIn, listener);
				}
				pane.addListener(SWT.FocusIn, new Listener() {
					public void handleEvent(Event event) {
						for (int i = 0; i < textPanes.size(); ++i) {
							if (event.widget == textPanes.get(i).getControl()) {
								MultipleTextPaneEditor.this.focusedTextPaneIndexChanged(i);
							}
						}
					}
				});
				textPanes.add(pane);
			}
			
			{
				int[] weights = new int[ntipleText];
				weights[0] = 1;
				for (int i = 1; i < ntipleText; ++i) {
					weights[i] = 0;
				}
				sash.setWeights(weights);
			}
		}
		//{
		//	int[] weights = new int[] { 3, 36 };
		//	sashRulerAndPanes.setWeights(weights);
		//}
	}

	private void focusedTextPaneIndexChanged(int index) {
		MultipleTextPaneEditor.this.focusedTextPaneIndex = index;
		//this.ruler.changeFocusedTextPane(index);
	}
	
	@Override
	public void setFocus()
	{
		// TODO Auto-generated method stub

	}

	public void clearInitalTopPosition() {
		for (int i = 0; i < textPanes.size(); ++i) {
			TextPane pane = textPanes.get(i);
			pane.clearInitalTopPosition();
		}
	}
	
	public void setVisibleTokenCenterIndexOfPane(int paneIndex, int tokenIndex) {
		if (0 <= paneIndex && paneIndex < textPanes.size()) {
			textPanes.get(paneIndex).setVisibleTokenCenterIndex(tokenIndex);
		}
	}

	private void resizePanes(int shownPanes) {
		final int panes = textPanes.size();
		final int ntiple = shownPanes > 0 ? shownPanes : 1;
		int[] weights = new int[panes];
		int i = 0;
		for (; i < ntiple; ++i) {
			weights[i] = 1;
		}
		for (; i < panes; ++i) {
			weights[i] = 0;
		}
		sash.setWeights(weights);
	}
	
	public void changeIndependentMode(boolean value) {
		independentMode = value;
	}
	
	public void setSelection(int[] fileIndices, boolean requireAllFileViewingMode)
	{
		int ntiple = textPanes.size();
		if (fileIndices.length < ntiple) {
			ntiple = fileIndices.length;
		}
		final int panes = textPanes.size();
		assert panes >= ntiple;
		try {
			{
				int i;
				for (i = 0; i < ntiple; ++i) {
					int[] f = new int[] { fileIndices[i] };
					textPanes.get(i).setSelection(f);
				}
				for (; i < panes; ++i) {
					textPanes.get(i).setSelection(null);
				}
			}
			resizePanes(ntiple);
		} finally {
		}

	}
	
	public int[] getViewedFiles() {
		final int panes = textPanes.size();
		int count = 0;
		for (int i = 0; i < panes; ++i) {
			count += textPanes.get(i).getViewedFiles().length;
		}
		int[] files = new int[count];
		int j = 0;
		for (int i = 0; i < panes; ++i) {
			int[] filesI = textPanes.get(i).getViewedFiles();
			for (int k = 0; k < filesI.length; ++k) {
				files[j] = filesI[k];
				++j;
			}
		}
		return files;
	}
	
	public void setClonePairSelection(ClonePair selectedPair) {
		changeIndependentMode(false);
		
		try {
			final int panes = textPanes.size();
			textPanes.get(0).setCodeFragmentSelection(selectedPair.getLeftCodeFragment(), selectedPair.classID);
			textPanes.get(1).setCodeFragmentSelection(selectedPair.getRightCodeFragment(), selectedPair.classID);
			for (int i = 2; i < panes; ++i) {
				textPanes.get(i).setSelection(null);
			}
			int[] weights = new int[panes];
			weights[0] = 1;
			weights[1] = 1;
			for (int i = 2; i < panes; ++i) {
				weights[i] = 0;
			}
			sash.setWeights(weights);
		} finally {
		}
	}
	
	public void setCodeFragmentSelection(CodeFragment selectedCodeFragment, long cloneSetID)
	{
		final int panes = textPanes.size();
		if (independentMode) {
			for (int i = 0; i < panes; ++i) {
				textPanes.get(i).setCodeFragmentSelection(selectedCodeFragment, cloneSetID);
			}
			return;
		}
		
		try {
			textPanes.get(0).setCodeFragmentSelection(selectedCodeFragment, cloneSetID);
			for (int i = 1; i < panes; ++i) {
				textPanes.get(i).setSelection(null);
			}
			int[] weights = new int[panes];
			weights[0] = 1;
			for (int i = 1; i < panes; ++i) {
				weights[i] = 0;
			}
			sash.setWeights(weights);
		} finally {
		}
	}

	public boolean setEncoding(String encodingName) {
		boolean rulerVisible = ruler.isVisible();
		if (rulerVisible) {
			ruler.setVisible(false);
		}
		boolean result = true;
		try {
			final int panes = textPanes.size();
			for (int i = 0; i < panes; ++i) {
				result = textPanes.get(i).setEncoding(encodingName);
				if (! result) {
					break; // for
				}
			}
			this.ruler.update();
		} finally {
			ruler.setVisible(rulerVisible);
		}
		return result;
	}
	
	public PrepToken[] getTokens(int fileIndex) {
		final int panes = textPanes.size();
		for (int i = 0; i < panes; ++i) {
			PrepToken[] tokens = textPanes.get(i).getTokens(fileIndex);
			if (tokens != null) {
				return tokens;
			}
		}
		return null;
	}
	
	public ClonePair[] getClonePairs(int fileIndex) {
		final int panes = textPanes.size();
		for (int i = 0; i < panes; ++i) {
			ClonePair[] clonePairs = textPanes.get(i).getClonePairs(fileIndex);
			if (clonePairs != null) {
				return clonePairs;
			}
		}
		return null;
	}

	public BeginEnd getVisibleTokenRangeOfPane(int paneIndex) {
		final int panes = textPanes.size();
		assert 0 <= paneIndex && paneIndex < panes;
		return textPanes.get(paneIndex).getVisibleTokenRange();
	}
	
	public void setCloneSelection(long[] cloneSetIDs)
	{
		try {
			final int panes = textPanes.size();
			for (int i = 0; i < panes; ++i) {
				textPanes.get(i).setCloneSelection(cloneSetIDs);
			}
		} finally {
		}
	}
	
	public void updateModel(Model data, boolean isAllFileViewedModeEnabled) {
		this.viewedModel = data;
		
		changeIndependentMode(false);
		
		try {
			final int panes = textPanes.size();
			for (int i = 0; i < panes; ++i) {
				textPanes.get(i).updateModel(data);
			}
			if (data.getFileCount() <= panes) {
				int files = data.getFileCount();
				int[] indices = new int[files];
				for (int i = 0; i < files; ++i) {
					indices[i] = i;
				}
				this.setSelection(indices, isAllFileViewedModeEnabled);
			}
		} finally {
		}
	}

}
