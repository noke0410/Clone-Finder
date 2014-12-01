package ccfinderx.ui.views;


import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ccfinderx.model.CloneSet;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class CloneSetView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "ccfinderx.ui.views.CloneSetView";

	private TableViewer viewer;

	/**
	 * The constructor.
	 */
	public CloneSetView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "CCFINDERX.viewer");

		createColumns(parent, viewer);

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new ArrayContentProvider());
		// get the content for the viewer, setInput will call getElements in the
		// contentProvider
		viewer.setInput(null);
		// make the selection available to other views
		getSite().setSelectionProvider(viewer);

		// define layout for the viewer
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);
	}

	// create the columns for the table
	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = { "Clone-Set ID", "LEN"};
		int[] bounds = { 100, 100};

		for (int index = 0; index < titles.length; index++) {
			createTableViewerColumn(titles[index], bounds[index]);
		}
		setTableViewerColumn();
	}

	private void createTableViewerColumn(String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();

		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
	}

	private void setTableViewerColumn() {
		TableColumn[] tableColumns = viewer.getTable().getColumns();

		for (TableColumn tableColumn:tableColumns) {
			TableViewerColumn viewerColumn = (TableViewerColumn) tableColumn.getData(Policy.JFACE + ".columnViewer"); //$NON-NLS-1$
			switch (tableColumn.getText()) {
				case "Clone-Set ID":
					viewerColumn.setLabelProvider(new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							CloneSet cloneSet = (CloneSet) element;
							return String.valueOf(cloneSet.id);
						}
					});
					break;
				case "LEN":
					viewerColumn.setLabelProvider(new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							CloneSet cloneSet = (CloneSet) element;
							return String.valueOf(cloneSet.length);
						}
					});
					break;
			}
		}
	}

	public void setInput(Object input) {
		viewer.setInput(input);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}