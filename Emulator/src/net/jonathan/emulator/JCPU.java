package net.jonathan.emulator;

/**
 * Created by Jonathan
 * Date: 8/3/2014
 */
public class JCPU
{

	/* Operation codes */
	private static final byte INSTRUCTION_LW = 0x00;
	private static final byte INSTRUCTION_SW = 0x01;
	private static final byte INSTRUCTION_LDA = 0x02;
	private static final byte INSTRUCTION_LC = 0x03;
	private static final byte INSTRUCTION_ADD = 0x04;
	private static final byte INSTRUCTION_OR = 0x05;
	private static final byte INSTRUCTION_NOR = 0x06;
	private static final byte INSTRUCTION_AND = 0x07;
	private static final byte INSTRUCTION_JNZ = 0x08;
	private static final byte INSTRUCTION_MW = 0x09;
	private static final byte INSTRUCTION_LDF = 0x0A;
	private static final byte INSTRUCTION_LWA = 0x0B;
	private static final byte INSTRUCTION_SWA = 0x0D;
	private static final byte INSTRUCTION_ADC = 0x0C;
	private static final byte INSTRUCTION_PUSH = 0x0E;
	private static final byte INSTRUCTION_POP = 0x0F;

	/* Programmer visible registers */
	public byte registerB = 0x00;
	public byte registerC = 0x00;
	public byte registerH = 0x00;
	public byte registerL = 0x00;
	public byte registerD = 0x00;
	public byte registerE = 0x00;
	public byte registerA = 0x00;
	public byte registerF = 0x00;

	/* IR, ARG0, and ARG1 */
	public byte registerInstruction = 0x00;
	public byte registerArgOne = 0x00;
	public byte registerArgTwo = 0x00;

	/* The lower 4 bits of the current instruction */
	private byte instructionLow;

	public byte[] systemROM = new byte[32768];	/* 0x0000 to 0x7EEE */
	public byte[] systemRAM = new byte[32512];	/* 0x8000 to 0xFEFF */
	public byte[] systemIO = new byte[256];  	/* 0xFF00 to 0xFFFF */

	public byte phaseCounter = 0x00;
	public byte programCounterLow = 0x00;
	public byte programCounterHigh = 0x00;

	public byte stackPointerHigh = 0x00;
	public byte stackPointerLow = 0x00;

	private boolean loadArgTwo = false;
	private byte lastOutputZero = 0x00;

	private PiInterface piInterface;

	public JCPU(PiInterface piInterface)
	{
		this.piInterface = piInterface;
	}

	public void reset()
	{
		/* Clear all memory locations */
		systemROM = new byte[32768];
		systemRAM = new byte[32512];
		systemIO = new byte[256];

		lastOutputZero = 0x00;
		loadArgTwo = false;

		phaseCounter = 0x00;
		programCounterLow = 0x00;
		programCounterHigh = 0x00;

		stackPointerHigh = 0x00;
		stackPointerLow = 0x00;

		registerB = 0x00;
		registerC = 0x00;
		registerH = 0x00;
		registerL = 0x00;
		registerD = 0x00;
		registerE = 0x00;
		registerA = 0x00;
		registerF = 0x00;

		registerInstruction = 0x00;
		registerArgOne = 0x00;
		registerArgTwo = 0x00;
	}

	public void cycle()
	{
		byte originalPhaseCounter = phaseCounter;
		switch (phaseCounter)
		{
			case 0x00:
				irload();
				break;
			case 0x01:
				argumentOneLoad();
				break;
			case 0x02:
				if (loadArgTwo)
					argumentTwoLoad();
				else
					operationOne();
				break;
			case 0x03:
				if (loadArgTwo)
					operationOne();
				else
					operationTwo();
				break;
			case 0x04:
				if (loadArgTwo)
					operationTwo();
				else
					phaseCounter = 0x00;
				break;
			case 0x05:
				if (instructionLow == INSTRUCTION_ADC || instructionLow == INSTRUCTION_ADD)
					addOperationThree();
				else
					phaseCounter = 0x00;
				break;
			case 0x06:
				phaseCounter = 0x00;
				break;
		}

		/* Increment the phase counter only if it was not just set to 0x01 */
		if (!(phaseCounter == 0x00 && originalPhaseCounter != 0x00))
			phaseCounter++;
	}

	public String getRegisterString(byte registerCode)
	{
		switch(registerCode)
		{
			case 0x00:
				return "B";
			case 0x01:
				return "C";
			case 0x02:
				return "H";
			case 0x03:
				return "L";
			case 0x04:
				return "D";
			case 0x05:
				return "E";
			case 0x06:
				return "A";
			case 0x07:
				return "F";
			default:
				return "ERR";
		}
	}

	private byte getRegister(byte registerCode)
	{
		switch (registerCode)
		{
			case 0x00:
				return registerB;
			case 0x01:
				return registerC;
			case 0x02:
				return registerH;
			case 0x03:
				return registerL;
			case 0x04:
				return registerD;
			case 0x05:
				return registerE;
			case 0x06:
				return registerA;
			case 0x07:
				return registerF;
		}

		System.out.println("Invalid register code: 0x" + Integer.toHexString(registerCode) + " on instruction " +
				"0x" + Integer.toHexString(registerInstruction & 0xFF) + " 0x" + Integer.toHexString(registerArgOne & 0xFF) +
				" 0x" + Integer.toHexString(registerArgTwo & 0xFF));
		return (byte) 0xFF;
	}

	private void setRegister(byte registerCode, byte value)
	{
		switch (registerCode)
		{
			case 0x00:
				registerB = value;
				break;
			case 0x01:
				registerC = value;
				break;
			case 0x02:
				registerH = value;
				break;
			case 0x03:
				registerL = value;
				break;
			case 0x04:
				registerD = value;
				break;
			case 0x05:
				registerE = value;
				break;
			case 0x06:
				registerA = value;
				break;
			case 0x07:
				registerF = value;
				break;
			default:
				System.out.println("Invalid register code: 0x" + Integer.toHexString(registerCode & 0xFF) + " on instruction " +
						"0x" + Integer.toHexString(registerInstruction) + " 0x" + Integer.toHexString(registerArgOne) +
						" 0x" + Integer.toHexString(registerArgTwo));
		}
	}

	public byte getSystemMemoryAt(byte addressHigh, byte addressLow)
	{
		int address = ((addressHigh & 0xFF) << 8) | (addressLow & 0xFF);
		if (address < 0x8000)
			return systemROM[address];
		else if (address >= 0xFF00)
			return systemIO[address - 0xFF00];
		else
			return systemRAM[address - 0x8000];
	}

	public void setSystemMemoryAt(byte addressHigh, byte addressLow, byte value)
	{
		int address = ((addressHigh & 0xFF) << 8) | (addressLow & 0xFF);
		if (address < 0x8000)
		{
			systemROM[address] = value;
		} else if (address >= 0xFF00)
		{
			systemIO[address - 0xFF00] = value;
			ioRespond(address - 0xFF00);
		} else
		{
			systemRAM[address - 0x8000] = value;
		}
	}

	private void ioRespond(int ioAddress)
	{
		if (ioAddress == 0x08)
			stackPointerLow = systemIO[ioAddress];
		else if (ioAddress == 0x09)
			stackPointerHigh = systemIO[ioAddress];
	}

	/* Increment the program counter */
	private void pcInc()
	{
		if ((programCounterLow & 0xFF) == 0xFF)
		{
			programCounterHigh = (byte) ((programCounterHigh & 0xFF) + 1);
			programCounterLow = 0x00;
		}else
		{
			programCounterLow = (byte) ((programCounterLow & 0xFF) + 1);
		}
	}

	private void irload()
	{
		/* Check for IO from the last instruction execution */
		if(lastOutputZero != systemIO[0])
			piInterface.handle(this);

		lastOutputZero = systemIO[0];

		registerInstruction = getSystemMemoryAt(programCounterHigh, programCounterLow);

		instructionLow = (byte) (registerInstruction & 0x0F);
		loadArgTwo = !(instructionLow == INSTRUCTION_JNZ ||
				instructionLow == INSTRUCTION_LW ||
				instructionLow == INSTRUCTION_SW ||
				instructionLow == INSTRUCTION_PUSH ||
				instructionLow == INSTRUCTION_POP);
		pcInc();
	}

	private void argumentOneLoad()
	{
		registerArgOne = getSystemMemoryAt(programCounterHigh, programCounterLow);
		pcInc();
	}

	private void argumentTwoLoad()
	{
		registerArgTwo = getSystemMemoryAt(programCounterHigh, programCounterLow);
		pcInc();
	}

	private void operationOne()
	{
		if (instructionLow == INSTRUCTION_LW)
		{
			setRegister(registerArgOne, getSystemMemoryAt(getRegister((byte) 0x02), getRegister((byte) 0x03)));
		} else if (instructionLow == INSTRUCTION_SW)
		{
			setSystemMemoryAt(getRegister((byte) 0x02), getRegister((byte) 0x03),
					(registerInstruction & 0x80) == 0x80 ? registerArgOne : getRegister(registerArgOne));
		} else if (instructionLow == INSTRUCTION_LDA)
		{
			setRegister((byte) 0x02, registerArgOne);
		} else if (instructionLow == INSTRUCTION_LC)
		{
			setRegister(registerArgOne, registerArgTwo);
		} else if (((registerInstruction & 0x04) == 0x04 && (registerInstruction & 0x08) != 0x08) ||
				instructionLow == INSTRUCTION_LDF || instructionLow == INSTRUCTION_ADC)
		{
			 /* The real CPU sets the ALU X register here, but that is not necessary for the emulator */
		} else if (instructionLow == INSTRUCTION_JNZ)
		{
			if (((registerInstruction & 0x80) == 0x80 ? registerArgOne : getRegister(registerArgOne)) != 0)
			{
				programCounterHigh = getRegister((byte) 0x02);
				programCounterLow = getRegister((byte) 0x03);
			}
		} else if (instructionLow == INSTRUCTION_MW)
		{
			setRegister(registerArgTwo, getRegister(registerArgOne));
		} else if (instructionLow == INSTRUCTION_LWA)
		{
			setRegister((byte) ((registerInstruction & 0x70) >> 4), getSystemMemoryAt(registerArgOne, registerArgTwo));
		} else if (instructionLow == INSTRUCTION_SWA)
		{
			setSystemMemoryAt(registerArgOne, registerArgTwo, getRegister((byte) ((registerInstruction & 0x70) >> 4)));
		} else if (instructionLow == INSTRUCTION_PUSH)
		{
			setSystemMemoryAt(stackPointerHigh, stackPointerLow,
					(registerInstruction & 0x80) == 0x80 ? registerArgOne : getRegister(registerArgOne));
		} else if (instructionLow == INSTRUCTION_POP)
		{
			/* The stack can only be 256 bytes, so don't bother doing anything with the high byte */
			stackPointerLow = (byte) ((stackPointerLow & 0xFF) + 1);
		}
	}

	private void operationTwo()
	{
		if (instructionLow == INSTRUCTION_LW)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_SW)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_LDA)
		{
			setRegister((byte) 0x03, registerArgTwo);
		} else if (instructionLow == INSTRUCTION_LC)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_ADD)
		{
			int regValueOne = getRegister(registerArgOne) & 0xFF;
			int regValueTwo = ((registerInstruction & 0x80) == 0x80 ? (registerArgTwo & 0xFF) : (getRegister(registerArgTwo) & 0xFF));

			if (regValueOne + regValueTwo > 255)
				registerF |= 0x01;

			setRegister(registerArgOne, (byte) (regValueOne + regValueTwo));
		}else if (instructionLow == INSTRUCTION_ADC)
		{
			int carry = (registerF & 0x01) == 0x01 ? 1 : 0;
			int regValueOne = getRegister(registerArgOne) & 0xFF;
			int regValueTwo = ((registerInstruction & 0x80) == 0x80 ? (registerArgTwo & 0xFF) : (getRegister(registerArgTwo) & 0xFF));

			if (regValueOne + regValueTwo + carry > 255)
				registerF |= 0x01;

			setRegister(registerArgOne, (byte) (regValueOne + regValueTwo + carry));
		}else if (instructionLow == INSTRUCTION_OR)
		{
			setRegister(registerArgOne, (byte) (getRegister(registerArgOne) |
					((registerInstruction & 0x80) == 0x80 ? registerArgTwo : getRegister(registerArgTwo))));
		} else if (instructionLow == INSTRUCTION_NOR)
		{
			setRegister(registerArgOne, (byte) ~(getRegister(registerArgOne) |
					((registerInstruction & 0x80) == 0x80 ? registerArgTwo : getRegister(registerArgTwo))));
		} else if (instructionLow == INSTRUCTION_AND)
		{
			setRegister(registerArgOne, (byte) (getRegister(registerArgOne) &
					((registerInstruction & 0x80) == 0x80 ? registerArgTwo : getRegister(registerArgTwo))));
		} else if (instructionLow == INSTRUCTION_JNZ)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_MW)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_LDF)
		{
			registerF = 0x00;

			int registerOne = getRegister(registerArgOne) & 0xFF;
			int registerTwo = ((registerInstruction & 0x80) == 0x80 ? registerArgTwo : getRegister(registerArgTwo)) & 0xFF;
			if (registerOne > registerTwo)
				registerF |= 0x02;
			if (registerOne == registerTwo)
				registerF |= 0x04;
			if (registerOne < registerTwo)
				registerF |= 0x08;
		} else if (instructionLow == INSTRUCTION_LWA)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_SWA)
		{
			phaseCounter = 0x00;
		} else if (instructionLow == INSTRUCTION_PUSH)
		{
			stackPointerLow = (byte) ((stackPointerLow & 0xFF) - 1);
		} else if (instructionLow == INSTRUCTION_POP)
		{
			setRegister(registerArgOne, getSystemMemoryAt(stackPointerHigh, stackPointerLow));
		}
	}

	private void addOperationThree()
	{
		/* Load flags */
		int registerOne = getRegister(registerArgOne) & 0xFF;
		int registerTwo = ((registerInstruction & 0x80) == 0x80 ? registerArgTwo : getRegister(registerArgTwo)) & 0xFF;
		if (registerOne > registerTwo)
			registerF |= 0x02;
		if (registerOne == registerTwo)
			registerF |= 0x04;
		if (registerOne < registerTwo)
			registerF |= 0x08;
	}

}
