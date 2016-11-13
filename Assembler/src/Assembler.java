import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class Assembler
{
	/* Register codes */
	private static final int REGISTER_B = 0x00;
	private static final int REGISTER_C = 0x01;
	private static final int REGISTER_H = 0x02;
	private static final int REGISTER_L = 0x03;
	private static final int REGISTER_D = 0x04;
	private static final int REGISTER_E = 0x05;
	private static final int REGISTER_A = 0x06;
	private static final int REGISTER_F = 0x07;

	private enum LogLevel
	{
		DEFAULT,
		VERBOSE
	}

	private static LogLevel logLevel = LogLevel.DEFAULT;

	/* Each of these corresponds to one of the passes that the assembler makes. If one of them is true, then the code
	 * at that pass is written to a file chosen by the user
	 */
	private static boolean[] writePass = new boolean[4];
	private static String[] writePassFiles = new String[4];
	private static String outputFilename = null;
	private static String inputFilename = null;

	/* If this is true, we print the time the assembler took a the end. It is automatically printed when the log
	 * level is set to verbose
	 */
	private static boolean printTime = false;

	public static void main(String[] args)
	{
		/* Make sure that we have a file to assemble */
		if (args.length < 1)
			error("One argument must be the file to assemble");

		/* Grab parameters */
		try
		{

			/* TODO: Fix this so that "-ds1 -ds2 somefile.asm" is no longer valid */
			for (int i = 0; i < args.length; i++)
			{
				String arg = args[i];

				/* -ds1 = "Dump stage one" */
				if (arg.equalsIgnoreCase("-ds0"))
				{
					writePassFiles[0] = args[++i];
					writePass[0] = true;
				} else if (arg.equalsIgnoreCase("-ds1"))
				{
					writePassFiles[1] = args[++i];
					writePass[1] = true;
				} else if (arg.equalsIgnoreCase("-ds2"))
				{
					writePassFiles[2] = args[++i];
					writePass[2] = true;
				} else if (arg.equalsIgnoreCase("-ds3"))
				{
					writePassFiles[3] = args[++i];
					writePass[3] = true;
				} else if (arg.equalsIgnoreCase("-o"))
					outputFilename = args[++i];
				else if (arg.equalsIgnoreCase("-t"))
					printTime = true;
				else if (arg.equalsIgnoreCase("-v"))
				{
					logLevel = LogLevel.VERBOSE;
				}else
					inputFilename = arg;
			}
		} catch (ArrayIndexOutOfBoundsException e)
		{
			error("Oops! You left out a parameter for an argument. ");
		}

		/* Make sure that our file actually exists */
		File file = new File(inputFilename);
		if (!file.exists())
			error("File " + file.getName() + " does not exist");

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));

			/* Read the file into a list of lines */
			ArrayList<String> lines = new ArrayList<>();

			String line;
			while ((line = reader.readLine()) != null)
					lines.add(line);
			reader.close();

			/* Assemble it and write it to the output file */
			File outputFile;
			if (outputFilename == null)
				outputFile = new File(inputFilename.substring(0, inputFilename.lastIndexOf(".")) + ".bin");
			else
				outputFile = new File(outputFilename);

			/* Write the raw bytes to a file */
			FileOutputStream fos = new FileOutputStream(outputFile);
			fos.write(assemble(lines));
			fos.close();
		} catch (Exception e)
		{
			e.printStackTrace();
			error("Error reading and/or writing file");
		}
	}

	private static void error(String err)
	{
		System.err.println("ERROR: " + err);
		System.exit(1);
	}

	private static void log(LogLevel level, String message)
	{
		if (level == LogLevel.DEFAULT || (logLevel == LogLevel.VERBOSE && level == LogLevel.VERBOSE))
			System.out.println(message);
	}

	private static byte getAsciiValue(char c)
	{
		return (byte) c;
	}

	private static byte getAsciiEscapedValue(char c)
	{
		char lowercase = Character.toLowerCase(c);
		switch (lowercase)
		{
			case '0': /* Null value */
				return 0x00;
			case 'a': /* Bell (?) */
				return 0x07;
			case 'b': /* Backspace */
				return 0x08;
			case 't': /* Horizontal tab */
				return 0x09;
			case 'n': /* Newline */
				return 0x0A;
			case 'v': /* Vertical tab */
				return 0x0B;
			case 'f': /* Form feed (?) */
				return 0x0C;
			case 'r': /* Carriage return */
				return 0x0D;
			case 'e': /* Escape */
				return 0x1B;
			case '\\': /* Backslash */
				return 0x5C;
			case '\'': /* Single quotation mark */
				return 0x27;
			case '\"': /* Double quotation mark */
				return 0x22;
			default: /* Unrecognized escape code */
				error("Invalid escaped character '" + c + "'");
				return (byte) 0xFF;
		}
	}

	private static byte[] assemble(ArrayList<String> lines)
	{
		/* Basic things that every program needs to define, or have defaults set */
		long startTimeMs = System.currentTimeMillis();

		/* TThe default origin is at the start of the RAM */
		int org = 0x00008000;

		/* Data stored with db and resb (In order!) */
		ArrayList<StoredData> storedData = new ArrayList<>();

		/* Constants defined with #define */
		HashMap<String, String> constants = new HashMap<>();

		/* Procedures and their memory locations */
		HashMap<String, Integer> procedures = new HashMap<>();

		/* If this is true, that means that the next instruction we encounter is the start of a procedure. */
		boolean tieProcedure = false;
		String procedureSymbol = "null";

		/* First pass: Interpret some assembler commands  */
		for (int i = 0; i < lines.size(); i++)
		{
			/* Convert this line to be uppercase, but ignore characters in double and single quotes */
			boolean inDQuotes = false;
			boolean inSQuotes = false;

			char[] chArray = lines.get(i).trim().toCharArray();
			for (int j = 0; j < chArray.length; j++)
			{
				char c = chArray[j];
				if (c == '\'')
				{
					if (!inDQuotes && (j - 1 == -1 || chArray[j - 1] != '\\'))
						inSQuotes = !inSQuotes;
				} else if (c == '\"')
				{
					if (!inSQuotes && (j - 1 == -1 || chArray[j - 1] != '\\'))
						inDQuotes = !inDQuotes;
				} else if (!inDQuotes && !inSQuotes)
					chArray[j] = Character.toUpperCase(c);
			}

			/* Put the line back in its new uppercase-d form */
			lines.set(i, new String(chArray));

			if (lines.get(i).contains(";"))
			{
				/* Remove a (possible) comment on this line. Ignore semicolons inside of single or double quotes */
				String s = lines.get(i).trim();
				boolean inDoubleQuotes = false;
				boolean inSingleQuotes = false;
				int lastIndexOfSemicolon = -1;

				char[] charArray = s.toCharArray();
				for (int j = 0; j < charArray.length; j++)
				{
					char c = charArray[j];
					if (c == '\'')
					{
						if (!inDoubleQuotes && (j - 1 == -1 || charArray[j - 1] != '\\'))
							inSingleQuotes = !inSingleQuotes;
					} else if (c == '\"')
					{
						if (!inSingleQuotes && (j - 1 == -1 || charArray[j - 1] != '\\'))
							inDoubleQuotes = !inDoubleQuotes;
					} else if (c == ';' && !inDoubleQuotes && !inSingleQuotes)
						lastIndexOfSemicolon = j;
				}

				/* If there was a comment, remove it */
				if (lastIndexOfSemicolon != -1)
					lines.set(i, s.substring(0, lastIndexOfSemicolon));
			}

			if (lines.get(i).trim().startsWith("#"))
			{
				String str = lines.get(i);

				String assemblerCommand = str.replace("#", "").split(" ")[0];

				if (assemblerCommand.equalsIgnoreCase("db") || assemblerCommand.equalsIgnoreCase("dw"))
				{
					/* Get everything without the command */
					String withoutCommand = str.replace("#", "").trim().split(" ", 2)[1].trim();

					ArrayList<Byte> dataBytes = new ArrayList<>();
					StoredData data = new StoredData();

					/* Get the symbol used to refer to this data */
					data.dataSymbol = withoutCommand.split(" ")[0];

					/* Split the data up into individual strings, characters, bytes, etc. */

					/* Each individual piece of the data, i.e. '0x55', 'Hello!', '0x00' */
					ArrayList<String> dataPieces = new ArrayList<>();

					StringBuilder currentValue = new StringBuilder();

					/* Used to determine where we are in each data piece */
					boolean readingValue = false;
					boolean inSingleQuotes = false;
					boolean inDoubleQuotes = false;

					/* Iterate over each character to split the line up into individual data pieces */
					char[] dataChars = withoutCommand.split(" ", 2)[1].trim().toCharArray();
					for (int j = 0; j < dataChars.length; j++)
					{
						char c = dataChars[j];

						/* We can skip this if there are just spaces in between values */
						if ((c == ' ' || c == ',') && !readingValue)
							continue;
						if ((c == ' ' || c == ',') && !inSingleQuotes && !inDoubleQuotes)
						{
							/* We have just finished reading a value */
							dataPieces.add(currentValue.toString());
							currentValue.setLength(0);
							readingValue = false;
						} else
						{
							/* If we aren't reading a value and encounter a non-space character, we need to read it */
							if (!readingValue)
							{
								/* We're reading a value now. Add the first character to start with */
								currentValue.append(c);
								readingValue = true;
							} else
								currentValue.append(c);

							/* Check if we have just entered/left a string.
							 * Start out by making sure that this quote is not part of an escaped character.
							 */
							if (j - 1 == -1 || dataChars[j - 1] != '\\')
							{
								if (c == '\'')
									inSingleQuotes = !inSingleQuotes;
								else if (c == '\"')
									inDoubleQuotes = !inDoubleQuotes;
							}
						}
					}

					/* Add the last value in to the list of data pieces */
					dataPieces.add(currentValue.toString());

					/* Loop through each data piece and add it to the bytes for the stored data */
					for (String dataPiece : dataPieces)
					{
						if (dataPiece.matches("\'(.*?)\'"))
						{
							/* This is a character sequence */
							char[] charArray = dataPiece.toCharArray();
							for (int j = 0; j < charArray.length; j++)
							{
								char c = charArray[j];
								if (c == '\\')
								{
									/* This is an escape code. Start by making sure it is not at the end of the string. */
									if (j + 1 == charArray.length)
										error("Escape sequence at string end on line " + i);

									/* If the backslash is not at the end of the string, that means that a character is following us */
									char escapedChar = charArray[++j];

									/* Add the escaped char to the data bytes */
									dataBytes.add(getAsciiEscapedValue(escapedChar));
								} else
									/* This is a normal ASCII character, so just add it to the data */
									dataBytes.add(getAsciiValue(c));
							}
						} else if (dataPiece.matches("\"(.*?)\""))
						{
							/* This is a string. Process it like a character sequence, but add a null byte to the end */
							char[] charArray = dataPiece.toCharArray();
							for (int j = 0; j < charArray.length; j++)
							{
								char c = charArray[j];
								if (c == '\\')
								{
									/* This is an escape code. Start by making sure it is not at the end of the string. */
									if (j + 1 == charArray.length)
										error("Escape sequence at string end on line " + i);

									/* If the backslash is not at the end of the string, that means that a character is following us */
									char escapedChar = charArray[++j];

									/* Add the escaped char to the data bytes */
									dataBytes.add(getAsciiEscapedValue(escapedChar));
								} else
									/* This is a normal ASCII character, so just add it to the data */
									dataBytes.add(getAsciiValue(c));
							}

							/* Add the null terminating byte. */
							dataBytes.add((byte) 0x00);
						} else if (dataPiece.startsWith("0X"))
						{
							/* This is a hexadecimal byte */
							try
							{
								dataBytes.add((byte) Integer.parseInt(dataPiece.replace("0X", ""), 16));
							} catch (Exception e)
							{
								error("Invalid hexadecimal db on line " + i);
							}
						} else
						{
							/* This SHOULD be a decimal number */
							try
							{
								dataBytes.add((byte) Integer.parseInt(dataPiece));
							} catch (Exception e)
							{
								error("Invalid decimal db on line " + i);
							}
						}
					}

					/* Convert the data list to a byte array and add it to the stored data */
					byte[] storedBytes = new byte[dataBytes.size()];
					for (int k = 0; k < dataBytes.size(); k++)
						storedBytes[k] = dataBytes.get(k);
					data.data = storedBytes;

					/* Add this stored data to the ArrayList */
					storedData.add(data);

					log(LogLevel.VERBOSE, "Added stored data with name \"" + data.dataSymbol + "\" and size (bytes) "
							+ data.data.length);
				} else if (assemblerCommand.equalsIgnoreCase("resb"))
				{
					String withoutCommand = str.replace("#", "").trim().split(" ", 2)[1].trim();
					StoredData data = new StoredData();

					int numBytes = 0;
					try
					{
						numBytes = Integer.parseInt(str.replace("#", "").trim().split(" ")[2].trim());
					} catch (Exception e)
					{
						error("Invalid number of bytes to reserve on line " + i);
					}

					data.dataSymbol = withoutCommand.split(" ")[0];
					data.data = new byte[numBytes];
					storedData.add(data);
				} else if (assemblerCommand.equalsIgnoreCase("org"))
				{
					try
					{
						String newOrg = str.replace("#", "").trim().split(" ", 2)[1].trim();
						if(newOrg.contains("0X"))
							org = Integer.parseInt(newOrg.replace("0X", ""), 16);
						else
							org = Integer.parseInt(newOrg);
					} catch (Exception e)
					{
						error("Invalid origin on line " + i);
					}
				}else if (assemblerCommand.equalsIgnoreCase("include"))
				{
					/* First, verify that the file actually exists */
					String path = str.replace("#", "").trim().split(" ", 2)[1].replace("\"", "").trim();

					File includeFile = new File(path);
					if(!includeFile.exists())
						error("Included file at " + path + " does not exist! ");

					/* Read each line of the included file into the current source file */
					try
					{
						int currentLocation = i;

						BufferedReader reader = new BufferedReader(new FileReader(includeFile));
						String line;
						while ((line = reader.readLine()) != null)
							lines.add(++currentLocation, line);
						reader.close();
					} catch (IOException e)
					{
						e.printStackTrace();
						error("Error reading included file at " + path);
					}

				}else if (assemblerCommand.equalsIgnoreCase("define"))
				{
					try
					{
						constants.put(str.replace("#", "").trim().split(" ")[1].trim(),
								str.replace("#", "").trim().split(" ", 3)[2].trim());
					} catch (Exception e)
					{
						error("Invalid constant definition on line " + i);
					}
				} else if (str.endsWith(":"))
				{
					/* This is a procedure. Tie it to the next instruction. */
					tieProcedure = true;
					procedureSymbol = str.replace("#", "").replace(":", "").trim();
					if (procedureSymbol.contains(" "))
						error("Illegal character in procedure symbol on line " + i);
				} else
					error("Unrecognized assembler command \"" + assemblerCommand + "\" on line " + i);
			} else if (!lines.get(i).trim().equalsIgnoreCase(""))
			{
				/* This is an instruction. Append the ": PROC ????" to it to identify it as the start of a procedure. */
				if (tieProcedure)
				{
					lines.set(i, lines.get(i).trim() + " : PROC " + procedureSymbol);
					tieProcedure = false;
				}
			}
		}

		if (writePass[0])
			writeArrayListToFile(writePassFiles[0], lines);

		log(LogLevel.VERBOSE, "First pass completed, assembler commands interpreted.");

		/* Second pass: Count the number of bytes for all instructions,
		 * compute procedure addresses, and replace '$' with the current location in memory.
		 */
		int numBytes = 0;
		for (int i = 0; i < lines.size(); i++)
		{
			/* Convert to uppercase because startsWith() is case sensitive */
			String line = lines.get(i).trim().toUpperCase().replace(",", "");

			if (line.startsWith("#") || line.startsWith(";") || line.equalsIgnoreCase(""))
				continue;

			/* If this line has a procedure on it, figure out where it is in memory */
			if (line.contains(": PROC"))
			{
				log(LogLevel.VERBOSE, "Bound procedure \"" + line.substring(line.lastIndexOf(':') + 7) +
						"\" to instruction \"" + line.substring(0, line.lastIndexOf(':') - 1)
						+ "\" at memory location 0x" + String.format("%04x", org + numBytes).toUpperCase());
				procedures.put(line.substring(line.lastIndexOf(':') + 7), org + numBytes);

				/* Set the line back to the state it was in without the procedure */
				lines.set(i, line.substring(0, line.lastIndexOf(':') - 1));
			}

			if (line.contains("$"))
			{
				lines.set(i, line.replace("$", "0X" + String.format("%04x", org + numBytes).substring(0, 2)
						+ " 0X" + String.format("%04x", org + numBytes).substring(2, 4)));

				log(LogLevel.VERBOSE, "Replaced \'$\' on line " + i + " with address 0x"
						+ String.format("%04x", org + numBytes).toUpperCase());
			}

			if (line.startsWith("LW") || line.startsWith("SW") || line.startsWith("JNZ") || line.startsWith("PUSH") ||
					line.startsWith("POP"))
				numBytes += 2;
			else if (line.startsWith("LDA") || line.startsWith("LC") || line.startsWith("ADD") || line.startsWith("ADC") ||
					line.startsWith("NOR") || line.startsWith("MW") || line.startsWith("LDF") || line.startsWith("OR") ||
					line.startsWith("LWA") || line.startsWith("SWA") || line.startsWith("AND"))
				numBytes += 3;
			else if (line.startsWith("JMP") && line.split(" ").length == 3)
				numBytes += 8;
			else if (line.startsWith("JMP"))
				numBytes += 5;
			else if (line.startsWith("JC") && (line.contains("0X") || line.split(" ").length == 2))
				numBytes += 8;
			else if (line.startsWith("JC"))
				numBytes += 11;
			else if (line.startsWith("JNC") && (line.contains("0X") || line.split(" ").length == 2))
				numBytes += 11;
			else if (line.startsWith("JNC"))
				numBytes += 14;
			else if (line.startsWith("NOT"))
				numBytes += 3;
			else if (line.startsWith("NAND"))
				numBytes += 6;
			else if (line.startsWith("XOR"))
				numBytes += 12;
			else if (line.startsWith("XNOR"))
				numBytes += 18;
			else if (line.startsWith("STC"))
				numBytes += 3;
			else if (line.startsWith("SUB"))
				numBytes += 12;
			else if (line.startsWith("GTN"))
				numBytes += 9;
			else if (line.startsWith("LTN"))
				numBytes += 9;
			else if (line.startsWith("EQU"))
				numBytes += 9;
			else if (line.startsWith("MOV"))
				numBytes += 3;
			else if (line.startsWith("MOVMR"))
				numBytes += 8;
			else if (line.startsWith("MOVRM"))
				numBytes += 8;
			else if (line.startsWith("CALL") && (line.contains("0X") || line.split(" ").length == 2))
			{
				numBytes += 9;

				/* This line needs to be marked with the return address to push to the stack */
				lines.set(i, lines.get(i) + " : 0X" + String.format("%04x", org + numBytes));
			} else if (line.startsWith("CALL"))
			{
				numBytes += 12;

				/* Do the same as above for register calls */
				lines.set(i, lines.get(i) + " : 0X" + String.format("%04x", org + numBytes));
			} else if (line.startsWith("RET"))
				numBytes += 5;
			else if (line.startsWith("NOP"))
				numBytes += 3;
			else if (line.startsWith("INC"))
				numBytes += 3;
			else if (line.startsWith("DEC"))
				numBytes += 6;
			else
				error("Unrecognized instruction \"" + line + "\" on line " + i);
		}

		if (writePass[1])
			writeArrayListToFile(writePassFiles[1], lines);

		log(LogLevel.VERBOSE, "Second pass completed, total number of instruction bytes: " + numBytes);

		/* Now generate constant addresses for data bytes */
		int dataLocation = org + numBytes;
		for (StoredData data : storedData)
		{
			data.memoryLocation = dataLocation;
			dataLocation += data.data.length;
			numBytes += data.data.length;

			log(LogLevel.VERBOSE, "Mapped stored data \"" + data.dataSymbol + "\" to memory location 0x" +
					String.format("%04x", data.memoryLocation).toUpperCase() + " with a size of "
					+ data.data.length + " bytes.");
		}

		log(LogLevel.VERBOSE, "Total program size in bytes: " + (dataLocation - org));

		/* Convert all stored data to a HashMap for faster lookup */
		HashMap<String, StoredData> dataHashMap = new HashMap<>();
		for (StoredData s : storedData)
			dataHashMap.put(s.dataSymbol, s);

		/* Third pass: Replace procedure and constant symbols with their actual values */
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim().replace(",", "");

			/* Skip comments, assembler commands, and blank lines. */
			if (line.startsWith("#") || line.startsWith(";") || line.equalsIgnoreCase(""))
				continue;

			String[] splitLine = line.split(" ");
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < splitLine.length; j++)
			{
				String str = splitLine[j];
				if (procedures.containsKey(str))
					splitLine[j] = "0X" + String.format("%04x", procedures.get(str)).substring(0, 2) + " 0X" +
							String.format("%04x", procedures.get(str)).substring(2, 4);
				else if (str.contains(".H") && dataHashMap.containsKey(str.substring(0, str.lastIndexOf(".H"))))
				{
					/* This is the high byte of some stored data */
					splitLine[j] = "0X" + String.format("%04x",
							dataHashMap.get(str.substring(0, str.lastIndexOf(".H"))).memoryLocation).substring(0, 2);
				} else if (str.contains(".L") && dataHashMap.containsKey(str.substring(0, str.lastIndexOf(".L"))))
				{
					/* This is the low byte of some stored data */
					splitLine[j] = "0X" + String.format("%04x",
							dataHashMap.get(str.substring(0, str.lastIndexOf(".L"))).memoryLocation).substring(2, 4);
				} else if (constants.containsKey(str))
				{
					splitLine[j] = constants.get(str);
				} else if (j != 0 && (getRegisterCode(splitLine[j]) == 0xFF && !splitLine[j].contains("0X")))
					error("Invalid symbol \"" + splitLine[j] + "\" on line " + i);

				sb.append(splitLine[j]).append(" ");
			}

			/* Write the modified line to the file */
			lines.set(i, sb.toString().trim().toUpperCase());
		}

		if (writePass[2])
			writeArrayListToFile(writePassFiles[2], lines);

		log(LogLevel.VERBOSE, "Third pass completed, constants, procedures, and data addresses remapped.");

		/* Fourth pass: Replace macros with basic instructions */
		int lineNumber = 0;
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();

			/* Use this for log messages instead of i because i changes throughout this loop */
			lineNumber++;

			/* Skip comments, assembler commands, and blank lines */
			if (line.startsWith("#") || line.startsWith(";") || line.equalsIgnoreCase(""))
				continue;

			String[] tokens = line.split(" ");

			if (tokens[0].equals("JMP"))
			{
				if (line.contains("0X"))
				{
					/* This is a jump to a location in memory */
					if (tokens.length == 2)
					{
						lines.set(i, "LDA 0X" + tokens[1].replace("0X", "").substring(0, 2) + " 0X"
								+ tokens[1].replace("0X", "").substring(2, 4));
						lines.add(++i, "JNZ 0X01");
					} else if (tokens.length == 3)
					{
						lines.set(i, "LDA " + tokens[1] + " " + tokens[2]);
						lines.add(++i, "JNZ 0X01");
					} else
						error("Invalid jump on line " + lineNumber);
				} else
				{
					if (tokens.length != 2)
						error("Invalid jump on line " + lineNumber);

					lines.set(i, "MW " + tokens[1] + " H");
					lines.add(++i, "MW " + tokens[2] + " L");
					lines.add(++i, "JNZ 0X01");
				}
			} else if (tokens[0].equals("JC"))
			{
				if (line.contains("0X"))
				{
					/* This is a jump to a location in memory */
					if (tokens.length == 2)
					{
						lines.set(i, "AND F 0X01");
						lines.add(++i, "LDA 0X" + tokens[1].replace("0X", "").substring(0, 2) + " 0X"
								+ tokens[1].replace("0X", "").substring(2, 4));
						lines.add(++i, "JNZ F");
					} else if (tokens.length == 3)
					{
						lines.set(i, "AND F 0X01");
						lines.add(++i, "LDA " + tokens[1] + " " + tokens[2]);
						lines.add(++i, "JNZ F");
					} else
						error("Invalid JNC on line " + lineNumber);
				} else
				{
					if (tokens.length != 2)
						error("Invalid JNC on line " + lineNumber);

					lines.set(i, "MW " + tokens[1] + " H");
					lines.add(++i, "MW " + tokens[2] + " L");
					lines.add(++i, "AND F 0X01");
					lines.add(++i, "JNZ F");
				}
			} else if (tokens[0].equals("JNC"))
			{
				if (line.contains("0X"))
				{
					/* This is a jump to a location in memory */
					if (tokens.length == 2)
					{
						lines.set(i, "LDA 0X" + tokens[1].replace("0X", "").substring(0, 2) + " 0X"
								+ tokens[1].replace("0X", "").substring(2, 4));
						lines.add(++i, "NOR F F");
						lines.add(++i, "AND F 0X01");
						lines.add(++i, "JNZ F");
					} else if (tokens.length == 3)
					{
						lines.set(i, "LDA " + tokens[1] + " " + tokens[2]);
						lines.add(++i, "NOR F F");
						lines.add(++i, "AND F 0X01");
						lines.add(++i, "JNZ F");
					} else
						error("Invalid JNC on line " + lineNumber);
				} else
				{
					if (tokens.length != 2)
						error("Invalid JNC on line " + lineNumber);

					lines.set(i, "MW " + tokens[1] + " H");
					lines.add(++i, "MW " + tokens[2] + " L");
					lines.add(++i, "NOR F F");
					lines.add(++i, "AND F 0X01");
					lines.add(++i, "JNZ F");
				}
			} else if (tokens[0].equals("NOT"))
			{
				if (tokens.length != 2)
					error("Invalid NOT on line " + lineNumber);

				lines.set(i, "NOR " + tokens[1] + tokens[1]);
			} else if (tokens[0].equals("NAND"))
			{
				if (tokens.length != 3)
					error("Invalid NAND on line " + lineNumber);

				lines.set(i, "AND " + tokens[1] + " " + tokens[2]);
				lines.add(++i, "NOR " + tokens[1] + tokens[1]);
			} else if (tokens[0].equals("XOR"))
			{
				if (tokens.length != 3)
					error("Invalid XOR on line " + lineNumber);

				if (tokens[2].contains("0X"))
				{
					lines.set(i, "AND " + tokens[1] + " " + tokens[2]);
					lines.add(++i, "LC H " + tokens[2]);
					lines.add(++i, "NOR H " + tokens[1]);
					lines.add(++i, "NOR " + tokens[1] + " H");
				} else
				{
					lines.set(i, "MW " + tokens[2] + " H");
					lines.add(++i, "AND " + tokens[1] + " " + tokens[2]);
					lines.add(++i, "NOR H " + tokens[1]);
					lines.add(++i, "NOR " + tokens[1] + " H");
				}
			} else if (tokens[0].equals("XNOR"))
			{
				if (tokens.length != 3)
					error("Invalid XNOR on line " + lineNumber);

				if (tokens[2].contains("0X"))
				{
					lines.set(i, "LC L " + tokens[2]);
					lines.add(++i, "MW " + tokens[1] + " H");
					lines.add(++i, "NOR H L");
					lines.add(++i, "NOR " + tokens[1] + " H");
					lines.add(++i, "NOR H " + tokens[2]);
					lines.add(++i, "NOR " + tokens[1] + " H");
				} else
				{
					lines.set(i, "MW " + tokens[2] + " L");
					lines.add(++i, "MW " + tokens[1] + " H");
					lines.add(++i, "NOR H L");
					lines.add(++i, "NOR " + tokens[1] + " H");
					lines.add(++i, "NOR H " + tokens[2]);
					lines.add(++i, "NOR " + tokens[1] + " H");
				}
			} else if (tokens[0].equals("STC"))
			{
				if (tokens.length != 1)
					error("Invalid STC on line " + lineNumber);

				lines.set(i, "OR F 0X01");
			} else if (tokens[0].equals("SUB"))
			{
				if (tokens.length != 3)
					error("Invalid SUB on line " + lineNumber);

				if (tokens[2].contains("0X"))
				{
					lines.set(i, "LC H " + tokens[2]);
					lines.add(++i, "NOR H H");
					lines.add(++i, "OR F 0X01");
					lines.add(++i, "ADC " + tokens[1] + " H");
				} else
				{
					lines.set(i, "MW " + tokens[2] + " H");
					lines.add(++i, "NOR H H");
					lines.add(++i, "OR F 0X01");
					lines.add(++i, "ADC " + tokens[1] + " H");
				}
			} else if (tokens[0].equals("GTN"))
			{
				if (tokens.length != 3)
					error("Invalid GTN on line " + lineNumber);

				lines.set(i, "LDF " + tokens[1] + " " + tokens[2]);
				lines.add(++i, "AND F 0X02");
				lines.add(++i, "MW F " + tokens[1]);
			} else if (tokens[0].equals("LTN"))
			{
				if (tokens.length != 3)
					error("Invalid LTN on line " + lineNumber);

				lines.set(i, "LDF " + tokens[1] + " " + tokens[2]);
				lines.add(++i, "AND F 0X08");
				lines.add(++i, "MW F " + tokens[1]);
			} else if (tokens[0].equals("EQU"))
			{
				if (tokens.length != 3)
					error("Invalid GTN on line " + lineNumber);

				lines.set(i, "LDF " + tokens[1] + " " + tokens[2]);
				lines.add(++i, "AND F 0X04");
				lines.add(++i, "MW F " + tokens[1]);
			} else if (tokens[0].equals("MOV"))
			{
				if (tokens.length != 3 && tokens.length != 4)
					error("Invalid MOV on line " + lineNumber);

				if (tokens.length == 3)
				{
					/* MOV from register to register */
					lines.set(i, "MW " + tokens[1] + tokens[2]);
				} else if (tokens[1].contains("0X"))
				{
					/* MOV from register to memory location */
					lines.set(i, "SWA " + tokens[1] + " " + tokens[2] + " " + tokens[3]);
				} else
				{
					/* MOV from memory location to register */
					lines.set(i, "LWA " + tokens[3] + " " + tokens[1] + " " + tokens[2]);
				}
			} else if (tokens[0].equals("MOVMR"))
			{
				if (tokens.length != 4)
					error("Invalid MOVMR on line " + lineNumber);

				lines.set(i, "MW " + tokens[2] + " H");
				lines.add(++i, "MW " + tokens[3] + " L");
				lines.add(++i, "LW " + tokens[1]);
			} else if (tokens[0].equals("MOVRM"))
			{
				if (tokens.length != 4)
					error("Invalid MOVRM on line " + lineNumber);

				lines.set(i, "MW " + tokens[1] + " H");
				lines.add(++i, "MW " + tokens[2] + " L");
				lines.add(++i, "SW " + tokens[3]);
			} else if (tokens[0].equals("CALL"))
			{
				if (line.substring(0, line.lastIndexOf(":")).trim().split(" ").length != 3)
					error("Invalid CALL on line " + lineNumber);

				if (tokens[1].contains("0X") && line.substring(0, line.lastIndexOf(":")).trim().split(" ").length == 3)
				{
					String returnAddressHex = line.substring(line.lastIndexOf(":") + 1, line.length()).replace("0X", "").trim();
					lines.set(i, "PUSH 0X" + returnAddressHex.substring(0, 2));
					lines.add(++i, "PUSH 0X" + returnAddressHex.substring(2, 4));
					lines.add(++i, "LDA " + tokens[1] + " " + tokens[2]);
					lines.add(++i, "JNZ 0X01");
				} else if (tokens[1].contains("0X"))
				{
					String returnAddressHex = line.substring(line.lastIndexOf(":") + 1, line.length()).replace("0X", "").trim();
					lines.set(i, "PUSH 0X" + returnAddressHex.substring(0, 2));
					lines.add(++i, "PUSH 0X" + returnAddressHex.substring(2, 4));
					lines.add(++i, "LDA 0X" + tokens[1].replace("0X", "").substring(0, 2)
							+ " 0X" + tokens[1].replace("0X", "").substring(2, 4));
					lines.add(++i, "JNZ 0X01");
				} else
				{
					String returnAddressHex = line.substring(line.lastIndexOf(":") + 1, line.length()).replace("0X", "").trim();
					lines.set(i, "PUSH 0X" + returnAddressHex.substring(0, 2));
					lines.add(++i, "PUSH 0X" + returnAddressHex.substring(2, 4));
					lines.add(++i, "MW " + tokens[1] + " H");
					lines.add(++i, "MW " + tokens[2] + " L");
					lines.add(++i, "JNZ 0X01");
				}
			} else if (tokens[0].equals("RET"))
			{
				if (tokens.length != 1)
					error("Invalid RET on line " + lineNumber);

				lines.set(i, "POP L");
				lines.add(++i, "POP H");
				lines.add(++i, "JNZ 0X01");
			} else if (tokens[0].equals("NOP"))
			{
				if (tokens.length != 1)
					error("Invalid NOP on line " + lineNumber);

				lines.set(i, "MW F F");
			} else if (tokens[0].equals("INC"))
			{
				if (tokens.length != 2)
					error("Invalid INC on line " + lineNumber);

				lines.set(i, "ADD " + tokens[1] + "0X01");
			} else if (tokens[0].equals("DEC"))
			{
				if (tokens.length != 2)
					error("Invalid DEC on line " + lineNumber);

				lines.set(i, "OR F 0X01");
				lines.add(++i, "ADC " + tokens[1] + " 0XFE");
			}

			/* No error checking needs to be done at the end here as any invalid instructions should have already been
			 * caught when counting bytes.
			 */
		}

		if (writePass[3])
			writeArrayListToFile(writePassFiles[3], lines);

		log(LogLevel.VERBOSE, "Fourth pass completed, instructions expanded into subroutines.");

		/* Allocate the number of bytes that we counted, plus a few extra */
		ByteBuffer outputBuffer = ByteBuffer.allocate(numBytes + 4);

		/* Fifth pass: Assemble basic instructions */
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();

			/* Skip comments, assembler commands, and blank lines */
			if (line.startsWith("#") || line.startsWith(";") || line.equalsIgnoreCase(""))
				continue;

			String[] tokens = line.split(" ");

			if (tokens[0].equals("LW"))
			{
				outputBuffer.put((byte) 0x00);
				outputBuffer.put(getRegisterCode(tokens[1]));
			} else if (tokens[0].equals("SW"))
			{
				if (tokens[1].contains("0X"))
				{
					outputBuffer.put((byte) 0x81);
					outputBuffer.put((byte) Integer.parseInt(tokens[1].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x01);
					outputBuffer.put(getRegisterCode(tokens[1]));
				}
			} else if (tokens[0].equals("LDA"))
			{
				outputBuffer.put((byte) 0x02);
				outputBuffer.put((byte) Integer.parseInt(tokens[1].replace("0X", ""), 16));
				outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
			} else if (tokens[0].equals("LC"))
			{
				outputBuffer.put((byte) 0x03);
				outputBuffer.put(getRegisterCode(tokens[1]));
				outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
			} else if (tokens[0].equals("ADD"))
			{
				if (tokens[2].contains("0X"))
				{
					outputBuffer.put((byte) 0x84);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x04);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put(getRegisterCode(tokens[2]));
				}
			} else if (tokens[0].equals("OR"))
			{
				if (tokens[2].contains("0X"))
				{
					outputBuffer.put((byte) 0x85);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x05);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put(getRegisterCode(tokens[2]));
				}
			} else if (tokens[0].equals("NOR"))
			{
				if (tokens[2].contains("0X"))
				{
					outputBuffer.put((byte) 0x86);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x06);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put(getRegisterCode(tokens[2]));
				}
			} else if (tokens[0].equals("AND"))
			{
				if (tokens[2].contains("0X"))
				{
					outputBuffer.put((byte) 0x87);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x07);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put(getRegisterCode(tokens[2]));
				}
			} else if (tokens[0].equals("JNZ"))
			{
				if (tokens[1].contains("0X"))
				{
					outputBuffer.put((byte) 0x88);
					outputBuffer.put((byte) Integer.parseInt(tokens[1].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x08);
					outputBuffer.put(getRegisterCode(tokens[1]));
				}
			} else if (tokens[0].equals("MW"))
			{
				outputBuffer.put((byte) 0x09);
				outputBuffer.put(getRegisterCode(tokens[1]));
				outputBuffer.put(getRegisterCode(tokens[2]));
			} else if (tokens[0].equals("LDF"))
			{
				if (tokens[2].contains("0X"))
				{
					outputBuffer.put((byte) 0x87);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x07);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put(getRegisterCode(tokens[2]));
				}
			} else if (tokens[0].equals("LWA"))
			{
				outputBuffer.put((byte) (0x0B | (getRegisterCode(tokens[1]) << 4)));
				outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				outputBuffer.put((byte) Integer.parseInt(tokens[3].replace("0X", ""), 16));
			} else if (tokens[0].equals("SWA"))
			{
				outputBuffer.put((byte) (0x0D | (getRegisterCode(tokens[1]) << 4)));
				outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				outputBuffer.put((byte) Integer.parseInt(tokens[3].replace("0X", ""), 16));
			} else if (tokens[0].equals("ADC"))
			{
				if (tokens[2].contains("0X"))
				{
					outputBuffer.put((byte) 0x8C);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put((byte) Integer.parseInt(tokens[2].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x0C);
					outputBuffer.put(getRegisterCode(tokens[1]));
					outputBuffer.put(getRegisterCode(tokens[2]));
				}
			} else if (tokens[0].equals("PUSH"))
			{
				if (tokens[1].contains("0X"))
				{
					outputBuffer.put((byte) 0x8E);
					outputBuffer.put((byte) Integer.parseInt(tokens[1].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x0E);
					outputBuffer.put(getRegisterCode(tokens[1]));
				}
			} else if (tokens[0].equals("POP"))
			{
				if (tokens[1].contains("0X"))
				{
					outputBuffer.put((byte) 0x8F);
					outputBuffer.put((byte) Integer.parseInt(tokens[1].replace("0X", ""), 16));
				} else
				{
					outputBuffer.put((byte) 0x0F);
					outputBuffer.put(getRegisterCode(tokens[1]));
				}
			} else
			{
				log(LogLevel.DEFAULT, "Invalid instruction encountered on pass 5: \"" + line + "\"");
			}
		}

		for (StoredData data : storedData)
		{
			log(LogLevel.VERBOSE, "Allocating " + data.data.length + " bytes for data \"" + data.dataSymbol + "\"");

			for (Byte b : data.data)
				outputBuffer.put(b);
		}

		log(LogLevel.VERBOSE, "Fifth pass completed, flat binary generated.");

		if(logLevel == LogLevel.VERBOSE || printTime)
			log(LogLevel.DEFAULT, "Assembly took " + (System.currentTimeMillis() - startTimeMs) + " milliseconds. (" +
					((System.currentTimeMillis() - startTimeMs) / 1000.0f) + " seconds)");

		return outputBuffer.array();
	}

	private static byte getRegisterCode(String reg)
	{
		if (reg.equalsIgnoreCase("A"))
			return (byte) REGISTER_A;
		else if (reg.equalsIgnoreCase("B"))
			return (byte) REGISTER_B;
		else if (reg.equalsIgnoreCase("C"))
			return (byte) REGISTER_C;
		else if (reg.equalsIgnoreCase("D"))
			return (byte) REGISTER_D;
		else if (reg.equalsIgnoreCase("E"))
			return (byte) REGISTER_E;
		else if (reg.equalsIgnoreCase("F"))
			return (byte) REGISTER_F;
		else if (reg.equalsIgnoreCase("H"))
			return (byte) REGISTER_H;
		else if (reg.equalsIgnoreCase("L"))
			return (byte) REGISTER_L;

		return (byte) 0xFF;
	}

	private static void writeArrayListToFile(String filename, ArrayList<String> list)
	{
		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for (String s : list)
				writer.write(s + "\n");
			writer.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			error("Error writing ArrayList to file");
		}
	}

	/* Data stored with db, dw, and resb */
	private static class StoredData
	{
		public String dataSymbol;
		public int memoryLocation;
		public byte[] data;
	}
}