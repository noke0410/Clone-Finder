package ccfinderx.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class ProjectDialog extends JDialog
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final int defaultWidth = 300;
	private final int defaultHeight = 225;
	
	private List<String> selectedProjects;
	private JList<String> list;
	private DefaultListModel<String> listModel;
	
	public ProjectDialog()
	{
		setSize(defaultWidth, defaultHeight);
		setLocationRelativeTo(null); // 置中顯示
		
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		GridBagLayout gbl_centerPanel = new GridBagLayout();
		gbl_centerPanel.columnWidths = new int[]{0, 0};
		gbl_centerPanel.rowHeights = new int[]{0, 0, 0};
		gbl_centerPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_centerPanel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		centerPanel.setLayout(gbl_centerPanel);
		
		JLabel lblProject = new JLabel("Project");
		lblProject.setFont(new Font("YaHei Consolas Hybrid", Font.BOLD, 18));
		GridBagConstraints gbc_lblProject = new GridBagConstraints();
		gbc_lblProject.insets = new Insets(0, 0, 5, 0);
		gbc_lblProject.gridx = 0;
		gbc_lblProject.gridy = 0;
		centerPanel.add(lblProject, gbc_lblProject);
		
		list = new JList<String>();
		GridBagConstraints gbc_list = new GridBagConstraints();
		gbc_list.fill = GridBagConstraints.BOTH;
		gbc_list.gridx = 0;
		gbc_list.gridy = 1;
		centerPanel.add(list, gbc_list);
		
		JPanel southPanel = new JPanel();
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		
		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedProjects = list.getSelectedValuesList();
				setVisible(false);
			}
		});
		southPanel.add(btnOk);
		
		setModal(true);
	}
	
	// 設定List.
	public void setProjectList(ArrayList<String> items)
	{
		listModel = new DefaultListModel<String>();

		for (String item : items)
		{
			listModel.addElement(item);
		}

		list.setModel(listModel);
		list.setSelectedIndex(0);
	}

	// 取得選擇清單
	public List<String> getSelectedProjects()
	{
		return selectedProjects;
	}

}