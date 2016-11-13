package net.jonathan.emulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Jonathan
 * Date: 7/23/2014
 * Time: 1:03 PM
 */
public class Emulator
{
	private JFrame frame = new JFrame();
	private GraphicsPanel screen = new GraphicsPanel();
	private PiKeyboardListener keyboard = new PiKeyboardListener();

	private PiInterface piInterface;
	private StorageController storageController;
	private JCPU cpu;

	/* All files currently loaded into memory (Used for reset button) */
	private HashMap<Integer, File> loadedFiles = new HashMap<>();

	/* GUI components */
	private JLabel[] registerLabels = new JLabel[16];
	private JLabel currentInstructionLabel = new JLabel();
	private JLabel cpsLabel = new JLabel();
	private JMenuBar menuBar = new JMenuBar();
	private JMenu file = new JMenu("File");
	private JMenuItem loadFile = new JMenuItem("Load file into memory...");

	private JButton stepButton = new JButton("Step");
	private JButton resetButton = new JButton("Reset");
	private JLabel clockEnableLabel = new JLabel("Enable clock");
	private JLabel clockSpeedLabel = new JLabel("Clock speed (hz)");
	private JCheckBox clockEnabled = new JCheckBox();
	private JTextField clockSpeedField = new JTextField();

	private int clockSpeedHz = 100;

	private long lastSecond = System.currentTimeMillis();
	private int cps = 0;

	public static void main(String[] args)
	{
		new Emulator().init();
	}

	public void init()
	{
		frame = new JFrame("JCPU Emulator");
		frame.setFocusable(true);
		frame.addKeyListener(keyboard);
		frame.setSize(640, 400);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setJMenuBar(menuBar);

		frame.setLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2) - 320,
				(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2) - 240);

		/* Get storage set up */
		JOptionPane.showMessageDialog(frame, "Please select a file to be used as storage", "Storage selection", JOptionPane.INFORMATION_MESSAGE);
		JFileChooser jFileChooser = new JFileChooser();
		jFileChooser.showOpenDialog(frame);

		/* Apparently the user doesn't want us open :( */
		if(jFileChooser.getSelectedFile() == null)
			System.exit(0);

		storageController = new StorageController(jFileChooser.getSelectedFile());
		piInterface = new PiInterface(screen, keyboard, storageController);
		cpu = new JCPU(piInterface);

		/* No layout manager. This allows for absolute positions */
		frame.setLayout(null);

		screen.getGraphicsPanel().setLocation(10, 10);
		frame.add(screen.getGraphicsPanel());

		/* Position the register labels */
		for (int i = 0; i < registerLabels.length; i++)
		{
			registerLabels[i] = new JLabel();
			registerLabels[i].setBounds(340, 10 + (i * 20), 100, 20);
			registerLabels[i].setVisible(true);
			registerLabels[i].setEnabled(true);
		}

		currentInstructionLabel.setBounds(450, 10, 200, 20);
		currentInstructionLabel.setVisible(true);
		currentInstructionLabel.setEnabled(true);
		frame.add(currentInstructionLabel);

		cpsLabel.setBounds(450, 40, 200, 20);
		cpsLabel.setVisible(true);
		cpsLabel.setEnabled(true);
		frame.add(cpsLabel);

		updateRegisterLabels();

		for (JLabel registerLabel : registerLabels)
			frame.add(registerLabel);

		/* Configure the menu bar and its items */
		file.setMnemonic('F');
		loadFile.setMnemonic('L');
		menuBar.add(file);
		file.add(loadFile);

		loadFile.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.showOpenDialog(frame);
				try
				{
					byte[] fileBytes = Files.readAllBytes(jFileChooser.getSelectedFile().toPath());
					int memoryLocation = Integer.parseInt(
							JOptionPane.showInputDialog("Where should this file be loaded in memory? ('0x' prefixed hex)").replace("0x", ""), 16);

					if (memoryLocation + fileBytes.length > 0xFFFF)
					{
						JOptionPane.showMessageDialog(frame, "File exceeds address 0xFFFF in memory", "Error!", JOptionPane.ERROR_MESSAGE);
						return;
					}

					loadedFiles.put(memoryLocation, jFileChooser.getSelectedFile());

					for (byte fileByte : fileBytes)
					{
						cpu.setSystemMemoryAt((byte) ((memoryLocation >>> 8) & 0xFF), (byte) (memoryLocation & 0xFF), fileByte);
						memoryLocation++;
					}

				} catch (IOException exc)
				{
					exc.printStackTrace();
					JOptionPane.showMessageDialog(frame, "Error loading file", "Error!", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		stepButton.setBounds(10, 260, 100, 20);
		stepButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				cpu.cycle();
				updateRegisterLabels();
			}
		});
		frame.add(stepButton);

		resetButton.setBounds(120, 260, 100, 20);
		resetButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				/* If the clock is running, this would be a bad idea */
				if(!clockEnabled.isSelected())
				{
					cpu.reset();
					piInterface.reset();
					updateRegisterLabels();
					screen.clear();

					if (loadedFiles.size() != 0)
					{
						int yesNo = JOptionPane.showConfirmDialog(null, "Load all previously loaded files into same memory locations?",
								"Choose an option or face the consequences", JOptionPane.YES_NO_OPTION);

						if (yesNo == JOptionPane.YES_OPTION)
						{
							for (Map.Entry<Integer, File> entry : loadedFiles.entrySet())
							{
								int memoryLocation = entry.getKey();
								try
								{
									byte[] fileBytes = Files.readAllBytes(entry.getValue().toPath());
									for (byte fileByte : fileBytes)
									{
										cpu.setSystemMemoryAt((byte) ((memoryLocation >>> 8) & 0xFF), (byte) (memoryLocation & 0xFF), fileByte);
										memoryLocation++;
									}
								} catch (IOException e1)
								{
									JOptionPane.showMessageDialog(null, "Error loading file: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
									e1.printStackTrace();
								}
							}
						} else
						{
							loadedFiles.clear();
						}
					}
				}

			}
		});
		frame.add(resetButton);

		clockEnableLabel.setBounds(10, 290, 80, 20);
		frame.add(clockEnableLabel);

		clockEnabled.setBounds(110, 290, 20, 20);
		clockEnabled.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				stepButton.setEnabled(!clockEnabled.isSelected());
				resetButton.setEnabled(!clockEnabled.isSelected());
			}
		});
		frame.add(clockEnabled);

		clockSpeedLabel.setBounds(10, 320, 100, 20);
		frame.add(clockSpeedLabel);

		clockSpeedField.setText(String.valueOf(clockSpeedHz));
		clockSpeedField.setBounds(110, 320, 100, 20);
		clockSpeedField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					clockSpeedHz = Integer.parseInt(clockSpeedField.getText());
				}catch(NumberFormatException ex)
				{
					JOptionPane.showMessageDialog(frame, "Please enter a valid integer", "u is dum", JOptionPane.ERROR_MESSAGE);
					clockSpeedField.setText(String.valueOf(clockSpeedHz));
				}
			}
		});
		frame.add(clockSpeedField);

		frame.revalidate();
		frame.setVisible(true);

		int registerLabelUpdateCounter = clockSpeedHz / 5;
		while(frame.isVisible())
			if(clockEnabled.isSelected())
			{
				/* Update register labels 5 times per second while the clock is running */
				if(registerLabelUpdateCounter == 0)
				{
					updateRegisterLabels();
					registerLabelUpdateCounter = clockSpeedHz / 5;
				}
				registerLabelUpdateCounter--;

				cpu.cycle();
				CycleSync.sync(clockSpeedHz);

				/* Calculate instructions per second */
				cps++;
				if(lastSecond + 1000 <= System.currentTimeMillis())
				{
					cpsLabel.setText("CPS: " + cps);
					cps = 0;
					lastSecond = System.currentTimeMillis();
				}
			}else
				try
				{
					Thread.sleep(20);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
	}

	private void updateRegisterLabels()
	{
		registerLabels[0].setText("B: 0x" + String.format("%02X", cpu.registerB));
		registerLabels[1].setText("C: 0x" + String.format("%02X", cpu.registerC));
		registerLabels[2].setText("H: 0x" + String.format("%02X", cpu.registerH));
		registerLabels[3].setText("L: 0x" + String.format("%02X", cpu.registerL));
		registerLabels[4].setText("D: 0x" + String.format("%02X", cpu.registerD));
		registerLabels[5].setText("E: 0x" + String.format("%02X", cpu.registerE));
		registerLabels[6].setText("A: 0x" + String.format("%02X", cpu.registerA));
		registerLabels[7].setText("F: 0x" + String.format("%02X", cpu.registerF));
		registerLabels[8].setText("IR: 0x" + String.format("%02X", cpu.registerInstruction));
		registerLabels[9].setText("ARG0: 0x" + String.format("%02X", cpu.registerArgOne));
		registerLabels[10].setText("ARG1: 0x" + String.format("%02X", cpu.registerArgTwo));
		registerLabels[11].setText("SPH: 0x" + String.format("%02X", cpu.stackPointerHigh));
		registerLabels[12].setText("SPL: 0x" + String.format("%02X", cpu.stackPointerLow));
		registerLabels[13].setText("PCH: 0x" + String.format("%02X", cpu.programCounterHigh));
		registerLabels[14].setText("PCL: 0x" + String.format("%02X", cpu.programCounterLow));
		registerLabels[15].setText("PHASE: 0x" + String.format("%02X", cpu.phaseCounter));

		String currentInstructionText = "INVALID";

		/* Decode the current instruction to put it in plain text (Switch on the lower half) */
		switch(cpu.registerInstruction & 0x0F)
		{
			case 0x00:
				currentInstructionText = "LW " + String.format("0x%02x", cpu.registerArgOne) + " "
						+ String.format("0x%02x", cpu.registerArgTwo);
				break;
			case 0x01:
				/* Is this an immediate value or a register code? */
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "SW " + String.format("0x%02x", cpu.registerArgOne);
				else
					currentInstructionText = "SW " + cpu.getRegisterString(cpu.registerArgOne);
				break;
			case 0x02:
				currentInstructionText = "LDA " + String.format("0x%02x", cpu.registerArgOne) + " "
						+ String.format("0x%02x", cpu.registerArgTwo);
				break;
			case 0x03:
				currentInstructionText = "LC " + cpu.getRegisterString(cpu.registerArgOne) + " "
						+ String.format("0x%02x", cpu.registerArgTwo);
				break;
			case 0x04:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "ADD " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ String.format("0x%02x", cpu.registerArgTwo);
				else
					currentInstructionText = "ADD " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x05:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "OR " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ String.format("0x%02x", cpu.registerArgTwo);
				else
					currentInstructionText = "OR " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x06:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "NOR " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ String.format("0x%02x", cpu.registerArgTwo);
				else
					currentInstructionText = "NOR " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x07:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "AND " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ String.format("0x%02x", cpu.registerArgTwo);
				else
					currentInstructionText = "AND " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x08:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "JNZ " + String.format("0x%02x", cpu.registerArgOne);
				else
					currentInstructionText = "JNZ " + cpu.getRegisterString(cpu.registerArgOne);
				break;
			case 0x09:
				currentInstructionText = "MW " + cpu.getRegisterString(cpu.registerArgOne) + " "
						+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x0A:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "LDF " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ String.format("0x%02x", cpu.registerArgTwo);
				else
					currentInstructionText = "LDF " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x0B:
				currentInstructionText = "LWA " + cpu.getRegisterString((byte) (((cpu.registerInstruction & 0x70) >> 4) & 0xFF))
							+ String.format("0x%02x", cpu.registerArgOne) + " " + String.format("0x%02x", cpu.registerArgTwo);
				break;
			case 0x0D:
				currentInstructionText = "SWA " + cpu.getRegisterString((byte) (((cpu.registerInstruction & 0x70) >> 4) & 0xFF))
						+ String.format("0x%02x", cpu.registerArgOne) + " " + String.format("0x%02x", cpu.registerArgTwo);
				break;
			case 0x0C:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "ADC " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ String.format("0x%02x", cpu.registerArgTwo);
				else
					currentInstructionText = "ADC " + cpu.getRegisterString(cpu.registerArgOne) + " "
							+ cpu.getRegisterString(cpu.registerArgTwo);
				break;
			case 0x0E:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "PUSH " + String.format("0x%02x", cpu.registerArgOne);
				else
					currentInstructionText = "PUSH " + cpu.getRegisterString(cpu.registerArgOne);
				break;
			case 0x0F:
				if((cpu.registerInstruction & 0x80) == 0x80)
					currentInstructionText = "POP " + String.format("0x%02x", cpu.registerArgOne);
				else
					currentInstructionText = "POP " + cpu.getRegisterString(cpu.registerArgOne);
				break;
		}

		currentInstructionLabel.setText("Instruction: " + currentInstructionText);
	}

}
