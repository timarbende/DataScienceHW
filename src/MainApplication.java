import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.GridLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class MainApplication implements Listener{

	private MainController controller;
	private MyIO myIO;
	
	private JFrame frame;
	private JProgressBar progressBar;
	private JButton btnReadData;
	private JTextField tfShingleSize;
	private Component horizontalStrut_1;
	private JLabel lbWarning;
	private JList<String> listBags;
	private JPanel panelStats;
	private JPanel panelShinglings;
	private JLabel lbEstimateText;
	private JLabel lbEstimateValue;
	private JLabel lbExactText;
	private JLabel lbExactValue;
	private JTextArea taShinglesFirst;
	private JTextArea taShinglesSecond;
	private JPanel panelDetails;
	private JScrollPane scrollPane;
	private JMenuBar menuBar;
	private JMenu mnEstimateFeatures;
	private JMenuItem mntmMaxEstimate;
	private JMenu mnExactFeatures;
	private JMenuItem mntmMaxExact;
	private JMenuItem mntmHighestEstimates;
	private JMenuItem mntmhighestExacts;
	private JProgressBar pbEstimateCompute;
	private JTextArea taEstimates;
	private JTextField tfEstimatesCount;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainApplication window = new MainApplication();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainApplication() {
		initialize();
		
		controller = new MainController();
		controller.listener = this;
		
		myIO = new MyIO();
		myIO.listener = this;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame("LSH application using NLP Medical DataSet");
		frame.setBounds(0, 0, 1200, 700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel panelBottom = new JPanel();
		panelBottom.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frame.getContentPane().add(panelBottom, BorderLayout.SOUTH);
		
		lbWarning = new JLabel("");
		lbWarning.setForeground(Color.RED);
		panelBottom.add(lbWarning);
		
		JLabel lblNewLabel = new JLabel("Shingle size");
		panelBottom.add(lblNewLabel);
		
		tfShingleSize = new JTextField();
		panelBottom.add(tfShingleSize);
		tfShingleSize.setColumns(10);
		tfShingleSize.getDocument().addDocumentListener(new DocumentListener() {
			  public void changedUpdate(DocumentEvent e) {
				  lbWarning.setText("");
				  int newSize = Integer.parseInt(tfShingleSize.getText());
				  if(newSize == 0) {
					  lbWarning.setText("Shingle size must be at least 1!");
					  btnReadData.setEnabled(false);
					  return;
				  }
				  
				  btnReadData.setEnabled(true);
				  Shingler.SetShingleSize(newSize);
			  }
			  public void removeUpdate(DocumentEvent e) {
				  lbWarning.setText("");
				  if(tfShingleSize.getText().length() == 0) {
					  btnReadData.setEnabled(false);
					  lbWarning.setText("Shingle size must be entered!");
					  return;
				  }
				  
				  btnReadData.setEnabled(true);
				  Shingler.SetShingleSize(Integer.parseInt(tfShingleSize.getText()));
			  }
			  public void insertUpdate(DocumentEvent e) {
				  lbWarning.setText("");
				  if(tfShingleSize.getText().length() > 0) {
					  int newSize = Integer.parseInt(tfShingleSize.getText());
					  if(newSize == 0) {
						  lbWarning.setText("Shingle size must be at least 1!");
						  btnReadData.setEnabled(false);
						  return;
					  }
					  
					  btnReadData.setEnabled(true);
					  Shingler.SetShingleSize(newSize);
				  }
			  }
		});
		
		Component horizontalStrut = Box.createHorizontalStrut(100);
		panelBottom.add(horizontalStrut);
		
		btnReadData = new JButton("Read data file");
		btnReadData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnReadDataClickHandler();
			}
		});
		btnReadData.setEnabled(false);
		panelBottom.add(btnReadData);
		
		horizontalStrut_1 = Box.createHorizontalStrut(20);
		panelBottom.add(horizontalStrut_1);
		
		progressBar = new JProgressBar();
		panelBottom.add(progressBar);
		
		scrollPane = new JScrollPane();
		frame.getContentPane().add(scrollPane, BorderLayout.WEST);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
		
		listBags = new JList<String>();
		listBags.setLayoutOrientation(JList.VERTICAL);
		listBags.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		listBags.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				List<String> selectedValues = listBags.getSelectedValuesList();
				taShinglesFirst.setText("");
				taShinglesSecond.setText("");
				
				if(selectedValues.size() > 2 || selectedValues.size() == 0) {
					listBags.clearSelection();
					return;
				}

				String selectedFirst = selectedValues.get(0);
				String firstShinglesText = Shingler.Shingle(selectedFirst).toString();
				firstShinglesText = firstShinglesText.substring(1, firstShinglesText.length() - 1);
				taShinglesFirst.setText(firstShinglesText);
				if (selectedValues.size() == 2) {
					String selectedSecond = selectedValues.get(1);
					String secondShinglesText = Shingler.Shingle(selectedSecond).toString();
					secondShinglesText = secondShinglesText.substring(1, secondShinglesText.length() - 1);
					taShinglesSecond.setText(secondShinglesText);

					lbEstimateValue.setText(controller.estimateSimilarity(selectedFirst, selectedSecond).toString());
					lbExactValue.setText(controller.computeExactSimilarity(selectedFirst, selectedSecond).toString());
				}
			}
		});
		Dimension d = listBags.getPreferredSize();
		d.width = 300;
		scrollPane.setViewportView(listBags);
		scrollPane.setPreferredSize(d);
		
		panelDetails = new JPanel();
		panelDetails.setLayout(new BorderLayout(0, 0));
		
		panelShinglings = new JPanel();
		panelDetails.add(panelShinglings);
		panelShinglings.setLayout(new GridLayout(0, 2, 0, 0));
		
		taShinglesFirst = new JTextArea();
		taShinglesFirst.setWrapStyleWord(true);
		taShinglesFirst.setLineWrap(true);
		taShinglesFirst.setEditable(false);
		taShinglesFirst.setRows(10);
		JScrollPane scrollPaneShinglesFirst = new JScrollPane(taShinglesFirst);
		scrollPaneShinglesFirst.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
		panelShinglings.add(scrollPaneShinglesFirst);
		
		taShinglesSecond = new JTextArea();
		taShinglesSecond.setWrapStyleWord(true);
		taShinglesSecond.setLineWrap(true);
		taShinglesSecond.setEditable(false);
		taShinglesSecond.setRows(10);
		JScrollPane scrollPaneShinglesSecond = new JScrollPane(taShinglesSecond);
		scrollPaneShinglesSecond.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		panelShinglings.add(scrollPaneShinglesSecond);
		
		frame.getContentPane().add(panelDetails, BorderLayout.CENTER);
		
		panelStats = new JPanel();
		lbEstimateText = new JLabel("Estimated similarity based on LSH: ");
		panelStats.add(lbEstimateText);
		
		lbEstimateValue = new JLabel("  ");
		lbEstimateValue.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		panelStats.add(lbEstimateValue);
		
		lbExactText = new JLabel("Exact Jaccard similarity: ");
		panelStats.add(lbExactText);
		
		lbExactValue = new JLabel("  ");
		panelStats.add(lbExactValue);
		panelDetails.add(panelStats, BorderLayout.SOUTH);
		
		menuBar = new JMenuBar();
		menuBar.setEnabled(false);
		frame.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		mnEstimateFeatures = new JMenu("Estimate features");
		menuBar.add(mnEstimateFeatures);
		
		mntmMaxEstimate = new JMenuItem("Generate pair with max estimated similarity");
		mntmMaxEstimate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JDialog dialog = generateDialog("Computing the highest estimate", "Computing the highest estimated Jaccard similarity");
	            dialog.setVisible(true);
	            
	            controller.threadMode = 1;
	            Thread thread = new Thread(controller);
	            thread.start();
			}
		});
		mnEstimateFeatures.add(mntmMaxEstimate);
		
		mntmHighestEstimates = new JMenuItem("Generate pairs with the highest estimated similarities...");
		mntmHighestEstimates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JDialog countDialog = new JDialog(frame, "How many?");
				JPanel dPanel = new JPanel();
				countDialog.setBounds(frame.getBounds().x + frame.getBounds().width / 2, frame.getBounds().y + frame.getBounds().height / 2, 300, 200);
				
				dPanel.add(new JLabel("How many estimates should we compute?"));
				
				tfEstimatesCount = new JTextField();
				tfEstimatesCount.setPreferredSize(new Dimension(100, tfEstimatesCount.getPreferredSize().height));
				dPanel.add(tfEstimatesCount);
				
				JButton btnOk = new JButton("Enter");
				btnOk.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						controller.similaritiesCount = Integer.parseInt(tfEstimatesCount.getText());
						
						JDialog dialog = generateDialog("Computing the highest estimates", "Computing the highest " + controller.similaritiesCount + " estimated Jaccard similarity");
			            dialog.setVisible(true);
			            
			            controller.threadMode = 2;
			            Thread thread = new Thread(controller);
			            thread.start();
			            
			            countDialog.dispose();
					}
				});
				dPanel.add(btnOk);
				
				countDialog.getContentPane().add(dPanel);
				countDialog.setVisible(true);
			}
		});
		mnEstimateFeatures.add(mntmHighestEstimates);
		
		mnExactFeatures = new JMenu("Exact features");
		menuBar.add(mnExactFeatures);
		
		mntmMaxExact = new JMenuItem("Generate pair with max exact similarity");
		mntmMaxExact.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JDialog dialog = generateDialog("Computing the highest exact", "Computing the highest exact Jaccard similarity");
	            dialog.setVisible(true);
	            
	            controller.threadMode = 3;
	            Thread thread = new Thread(controller);
	            thread.start();
			}
		});
		mnExactFeatures.add(mntmMaxExact);
		
		mntmhighestExacts = new JMenuItem("Generate pairs with the highest exact similarities...");
		mntmhighestExacts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JDialog countDialog = new JDialog(frame, "How many?");
				JPanel dPanel = new JPanel();
				countDialog.setBounds(frame.getBounds().x + frame.getBounds().width / 2, frame.getBounds().y + frame.getBounds().height / 2, 300, 200);
			
				dPanel.add(new JLabel("How many exact values should we compute?"));
			
				tfEstimatesCount = new JTextField();
				tfEstimatesCount.setPreferredSize(new Dimension(100, tfEstimatesCount.getPreferredSize().height));
				dPanel.add(tfEstimatesCount);
			
				JButton btnOk = new JButton("Enter");
				btnOk.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						controller.similaritiesCount = Integer.parseInt(tfEstimatesCount.getText());
					
						JDialog dialog = generateDialog("Computing the highest exacts", "Computing the highest " + controller.similaritiesCount + " exact Jaccard similarity");
						dialog.setVisible(true);
		            
						controller.threadMode = 4;
						Thread thread = new Thread(controller);
						thread.start();
		            
						countDialog.dispose();
					}
				});
				dPanel.add(btnOk);
			
				countDialog.getContentPane().add(dPanel);
				countDialog.setVisible(true);
			}
		});
		mnExactFeatures.add(mntmhighestExacts);
		
		setMenuEnabled(false);
	}
	
	private void btnReadDataClickHandler() {
		btnReadData.setEnabled(false);
		tfShingleSize.setEnabled(false);
		
		Thread ioThread = new Thread(myIO);
		ioThread.start();
	}
	
	private JDialog generateDialog(String title, String labelMessage) {
		JDialog dialog = new JDialog(frame, title);
		JPanel dPanel = new JPanel();
		dialog.setBounds(frame.getBounds().x + frame.getBounds().width / 2, frame.getBounds().y + frame.getBounds().height / 2, 600, 400);
        
        dPanel.add(new JLabel(labelMessage));
        pbEstimateCompute = new JProgressBar();
        dPanel.add(pbEstimateCompute);
        
        taEstimates = new JTextArea();
        taEstimates.setWrapStyleWord(true);
        taEstimates.setLineWrap(true);
        taEstimates.setEditable(false);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(taEstimates);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        dPanel.add(scrollPane);
        
        dialog.getContentPane().add(dPanel);
        
        return dialog;
	}

	@Override
	public void OnProgress(int progress) {
		progressBar.setValue(progress);
	}
	
	@Override
	public void OnDataReadDone(Data data) {
		controller.setData(data);
		
		DefaultListModel<String> model = new DefaultListModel<>();
		for(String entry: data.document) {
		  model.addElement( entry );
		}
		
		listBags.setModel(model);
		listBags.invalidate();
		
		setMenuEnabled(true);
		btnReadData.setEnabled(true);
		tfShingleSize.setEnabled(true);
	}
	
	private void setMenuEnabled(boolean enabled) {
		for(int i = 0; i < menuBar.getMenuCount(); i++) {
			for(int j = 0; j < menuBar.getMenu(i).getItemCount(); j++) {
				menuBar.getMenu(i).getItem(j).setEnabled(enabled);
			}
		}
	}
	
	@Override
	public void OnCharacteristicMatrixGenerated() {
		btnReadData.setEnabled(true);
		tfShingleSize.setEnabled(true);
	}

	@Override
	public void OnCalculationProgress(int progress) {
		pbEstimateCompute.setValue(progress);
	}

	@Override
	public void OnHighestSimilaritiesComputed(ArrayList<SimilarityDataHolder> highestEstimates) {
		StringBuilder builder = new StringBuilder();
		if(highestEstimates.size() == 1)
			builder.append("Highest similarity:\n");
		else
			builder.append("Highest " + highestEstimates.size() + " similarities:\n");
		
		for(SimilarityDataHolder dataHolder : highestEstimates) {
			builder.append(dataHolder.i);
			builder.append(" and ");
			builder.append(dataHolder.j);
			builder.append(" : ");
			builder.append(dataHolder.similarity);
			builder.append("\n");
		}
		taEstimates.setText(builder.toString());
	}
}
