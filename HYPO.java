// ************************************************************
// Name: Ian Nielsen
// ID: 300802131
// Homework Number: 1
// Date: February 10th 2016
// 
// Description of HYPO Simulator: 
// The HYPO simulator simulates a hypothetical decimal machine. 
// Instead of reading binary machine code the HYPO simulator 
// reads in values that are 0 through nine, and reads words that 
// are up to 6 digits in length. The machine code is read from 
// a file provided by the user. The machine code is then loaded 
// into the HYPO memory.The machine code is then executed. 
// Based on the opcodes and the modes of the operands, each 
// line of the machine is executed, which will change values 
// in memory as well as the registers. A memory dump is 
// performed before and after execution of the machine code. 
// The memory dump will display the values in memory up to a 
// given address, the registers, the program counter, the 
// stack point, the clock value, and program status register. 
// Before the execution all of the variables representing the 
// machine hardware are initialized to zero.
// ************************************************************

import java.io.*;
import java.util.Scanner;

public class HYPO
{ 
  //Error codes returned by methods if error occurs
  final static int OK = 0;
  final static int HaltEncounteredStatus = -1;
  final static int ErrorFileCantOpen = -2;
  final static int ErrorInvalidMemoryAddress = -3;
  final static int ErrorNoEndOfProgram = -4;
  final static int ErrorImmediateModeNotAllowed = -5;
  final static int ErrorDivisionByZero = -6;
  final static int ErrorInvalidOpcode = -7;
  final static int ErrorInvalidMode = -8;
  final static int ErrorLoadingProgram = -9;
  final static int ErrorInvalidStartAddr = -10;
  final static int ErrorInvalidSize = -11;
  final static int ErrorInvalidOp1Register = -12;
  final static int ErrorInvalidOp2Register = -13;
  final static int ErrorInvalidBranchToAddress = -14;

  //Fields representing HYPO hardware. In java int is 32 bits
  //which is needed for 6 digit words. 
  private static int[] memory;
  private static int mar;
  private static int mbr;
  private static int clock;
  private static int[] gpr;
  private static int pc;
  private static int sp;
  private static int psr;
  private static int IR;

  //String to hold output of the program which will be written
  //to a file.
  private static String outputToFile = "";

  //Address representing the address of the halt instruction.
  private static int lastAddress = 0;

  // ******************************************************
  // Function Name: main
  //
  // Task Description:  
  //   Runs absoluteLoader, initializeSystem, 
  //   CPUexecuteProgram, and dumpMemory functions. Also, 
  //   main reads in a file from the user which contains 
  //   the machine code, and checks for errors returned by 
  //   the loader and execution function.
  //   
  // Input parameters
  //   Command line arguments  - none for program.
  // Output parameters
  //   None                    - return type must be void.
  // Function return value
  //   None                    - return type must be void.
  // ******************************************************
  
  public static void main(String[] args)
  {
    try
    {
      PrintWriter output = new PrintWriter("output.txt");
      
      //Initialize system variables to zero.
      HYPO.initializeSystem();
      
      //Read in file containing machine code and load to memory.
      System.out.print("What is the program to run? :");
      HYPO.outputToFile += "What is the program to run? :\n";
      Scanner input = new Scanner(System.in);
      String file = input.next();
      int value = HYPO.absoluteLoader(file);

      //If return value of loader is error code then terminate program.
      if(value < 0)
      {
        System.exit(0);
      }
 
      //Set pc value to return value of loader.
      HYPO.pc = value;

      //Dump memory before and after execution.If execution or dump
      //status is negative display error message 
      int dumpStatus = OK;
      dumpStatus = HYPO.dumpMemory("Dump after program load\n",0,30);
      if(dumpStatus != OK)
      {
        System.exit(0);
      }
      int executionCompletionStatus = HYPO.CPUexecuteprogram();
      if(executionCompletionStatus != HaltEncounteredStatus)
      {
        System.exit(0);
      }
      dumpStatus = HYPO.dumpMemory("Dump after program execution\n",0,30);
      if(dumpStatus != OK)
      {
        System.exit(0);
      }

      //Print output to file and close file.
      output.print(HYPO.outputToFile);
      output.close();
    } 
    
    //Catch exception if output file cannot be opened.
    catch(IOException exception)
    {
      System.out.println("Unable to open the file.");
    }
  }
  
  // ******************************************************  
  // Function: initializeSystem()
  //
  // Task Description:
  //   Initializes all variables representing HYPO hardware
  //   to zero.
  // Input parameters
  //   None
  // Output parameters
  //   None
  // Function return value
  //   None
  // ******************************************************

  public static void initializeSystem()
  {
    //Set all values in memory to zero.
    memory = new int[10000];
    for(int i = 0; i < 10000; i++)
    {
      HYPO.memory[i] = 0;
    }
   
    //Set all registers to zero.
    gpr = new int[8];
    for(int j = 0; j < 8; j++)
    {
      HYPO.gpr[j] = 0;
    }
  
    //Set all HYPO hardware to zero.
    HYPO.mar = 0;
    HYPO.mbr = 0;
    HYPO.IR = 0;
    HYPO.clock = 0;
    HYPO.pc = 0;
    HYPO.sp = 0;
    HYPO.psr = 0;
  }
  
  // ******************************************************
  // Function: absoluteLoader()
  //
  // Task Description:
  //   Loads the given machine code into memory by reading 
  //   from the file and spliting the addresses and content
  //   and ignoring the comments.
  // Input parameters
  //   String filename     - File containing machine code
  // Output parameters
  //   None.
  // Function return values
  //   int content         - pc value to signify where 
  //                         execution should begin.
  //   ErrorInvalidAddress - address is invalid error code
  //   ErrorNoEndOfProgram - machine code is missing end of
  //                         program indicator.
  //   ErrorFileCantOpen   - unable to open file.
  // ******************************************************
  
  public static int absoluteLoader(String filename) 
  {
    try
    {
      File machineCode = new File(filename);
      Scanner in = new Scanner(machineCode);
      
      while(in.hasNextLine())
      {
        //Read next line in file.
        String line = in.nextLine();
        
        //Increment i until a digit or a negative sign is encountered.
        int i = 0;
        while((Character.isDigit(line.charAt(i))) || (line.charAt(i) == '-'))
        {
          i++; 
        }
         
        //Set the address string to the line up to i.
        String addressString = line.substring(0,i);
	
        //Until the new line is reached increment j.
        int j = i+1;
        while(Character.isDigit(line.charAt(j)))
        {
          j++;
          if(j == line.length()) 
          {
            break;
          }
        }
    
        //Set the content string to the line from i to the new line.
        String contentString = line.substring(i,j);
      	
        //Convert the address and content strings to integers.
        int address = Integer.parseInt(addressString.trim());
        int content = Integer.parseInt(contentString.trim());
        
        //If the address is in the range then set that address to content.
        //Else the end of the program has been encountered or the address
        //is invalid.
        if((address >= 0) && (address <= 1499))
        {
          HYPO.memory[address] = content;
          HYPO.lastAddress = address;
        }
        else if(address < 0)
        {
          System.out.println("End of program encountered.");
          HYPO.outputToFile += "End of program encountered.\n";
          in.close();
          return(content);
        }
        else
        {
          System.out.println("Invalid memory address to load to.");
          HYPO.outputToFile += "Invalid memory address to load to.\n";
          in.close();
          return ErrorInvalidMemoryAddress;
        }
      }

      //If the program has not returned then the EOP indicator 
      //is missing. Return a proper error message.
      System.out.println("Missing end of program indicator.");
      HYPO.outputToFile += "Missing end of program indicator.\n";
      in.close();
      return ErrorNoEndOfProgram;
    }
 
    //If the file could not be opened then throw an exception and 
    //display an error message.
    catch(FileNotFoundException Exception)
    {
      System.out.println("File not found.\n");
      HYPO.outputToFile += "File not found.\n";
      return ErrorFileCantOpen;
    }
  }
    
  // ******************************************************
  // Function: CPUexecuteprogram()
  //
  // Task Description:
  //   Executes the machine code stored in memory. The 
  //   method will operate on the hardware based on opcodes
  //   and the modes of the operands.
  // Input parameters
  //   None.
  // Output parameters
  //   None.
  // Function return values
  //   OK                           - no errors
  //   ErrorImmediateModeNotAllowed - opcode of 6 not 
  //                                  allowed.
  //   ErrorDivisionByZero          - division by zero.
  //   ErrorInvalidOpcode           - invalid opcode.
  // ******************************************************

  
  public static int CPUexecuteprogram() 
  {
    int status = OK;

    //Continue to execute while there is no error and 
    //halt command has not been reached.
    while(status == OK)
    {
      
      //If pc is in memory range set mar to pc, increment pc,
      //and set mbr to the content of the adress pc point to.
      if((HYPO.pc >= 0) && (HYPO.pc <= 1499))
      {
        HYPO.mar = HYPO.pc++;
        HYPO.mbr = HYPO.memory[HYPO.mar];
      }

      //Else the address is invalid and return error code.
      else
      {
        System.out.println("Runtime error: invalid address.");
        HYPO.outputToFile += "Runtime error: invalid address.\n";
        return ErrorInvalidAddress;
      }
  
      //Set the instruction register to the mbr content and seperate
      //instruction into the opcode and the mode and gpr of each
      //operand.
      HYPO.IR = HYPO.mbr;
      int opCode = HYPO.IR / 10000;
      int remainder = HYPO.IR % 10000;
      int op1Mode = remainder / 1000;
      remainder = remainder % 1000;
      int op1Gpr = remainder / 100;
      remainder = remainder % 100;
      int op2Mode = remainder / 10;
      int op2Gpr = remainder % 10;

      //Check for invalid register number for operand 1 and operand 2
      if((op1Gpr < 0) || (op1Gpr > 7))
      {
        HYPO.outputToFile += "Invalid register number for operand one.\n";
        System.out.println("Invalid register number for operand one.);
        return ErrorInvalidOp1Register;
      }
      if((op2Gpr < 0) || (op2Gpr > 7))
      {
        HYPO.outputToFile += "Invalid register number for operand two.\n";
        System.out.println("Invalid register number for operand two.);
        return ErrorInvalidOp2Register;
      }

      int op1Address, op1Value, op2Address, op2Value, result;
      int[] operands = new int[3];
   
      switch(opCode)
      {
        //Halt opcode
        case 0:
          System.out.println("Halt instruction has been encountered.");
          HYPO.outputToFile += "Halt instruction has been encountered.\n";      
          HYPO.clock = HYPO.clock + 12;
          return HaltEncounteredStatus;
        
        //Add opcode
        case 1:
          
          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
   
          //Fetch operand two's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op2Mode, op2Gpr);
          op2Address = operands[0];
          op2Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
     
          //Perform the operation.
          result = op1Value + op2Value;
   
          //Place the result in the appropriate memory type based on the mode 
          //of the first operand.
          if(op1Mode == 1)
          {
            HYPO.gpr[op1Gpr] = result;
          }
          else if(op1Mode == 6)
          {
            System.out.println("Destination operand mode cannot be immediate mode.");
            outputToFile += "Destination operand mode cannot be immediate mode.\n";
            return ErrorImmediateModeNotAllowed;
          }
          else
          {
            HYPO.memory[op1Address] = result;  
          }
   
          //Add 3 to the execution time. 
          HYPO.clock = HYPO.clock + 3;
          break;

        //Subtract opcode
        case 2:
    
          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
  
          //Fetch operand two's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op2Mode, op2Gpr);
          op2Address = operands[0];
          op2Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }

          //Perform the operation.
          result = op1Value - op2Value;

          //Place the result in the appropriate memory type based on the mode 
          //of the first operand.
          if(op1Mode == 1)
          {
            HYPO.gpr[op1Gpr] = result;
          }
          else if(op1Mode == 6)
          {
            System.out.println("Destination operand mode cannot be immediate mode.");
            outputToFile += "Destination operand mode cannot be immediate mode.\n";
            return ErrorImmediateModeNotAllowed;
          }
          else
          {
            HYPO.memory[op1Address] = result;  
          }
         
          //Add 3 to the execution time.
          HYPO.clock = HYPO.clock + 3;
          break;

        //Multiply opcode
        case 3:
    
          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
  
          //Fetch operand two's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op2Mode, op2Gpr);
          op2Address = operands[0];
          op2Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }

          //Perform the operation
          result = op1Value * op2Value;

          
          //Place the result in the appropriate memory type based on the mode 
          //of the first operand.
          if(op1Mode == 1)
          {
            HYPO.gpr[op1Gpr] = result;
          }
          else if(op1Mode == 6)
          {
            System.out.println("Destination operand mode cannot be immediate mode.");
            HYPO.outputToFile += "Destination operand mode cannot be immediate mode.\n";
            return ErrorImmediateModeNotAllowed;
          }
          else
          {
            HYPO.memory[op1Address] = result;
          }
         
          //Add 6 to the execution time.
          HYPO.clock = HYPO.clock + 6;
          break;

        //Divide opcode
        case 4:

          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op2Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          } 
  
          //Fetch operand two's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op2Mode, op2Gpr);
          op2Address = operands[0];
          op2Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
    
          //Return error if division by zero.
          if(op2Value == 0)
          {
            System.out.println("Error: division by zero.");
            return ErrorDivisionByZero;
          }

          //Perform the operation.
          result = op1Value / op2Value;

          
          //Place the result in the appropriate memory type based on the mode 
          //of the first operand.
          if(op1Mode == 1)
          {
            HYPO.gpr[op1Gpr] = result;
          }
          else if(op1Mode == 6)
          {
            System.out.println("Destination operand mode cannot be immediate mode.");
            HYPO.outputToFile += "Destination operand mode cannot be immediate mode.\n";
            return ErrorImmediateModeNotAllowed;
          }
          else
          {
            HYPO.memory[op1Address] = result;
          }
         
          //Add 6 to the execution time.
          HYPO.clock = HYPO.clock + 6;
          break;
      
        //Move opcode
        case 5:

          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
 
          //Fetch operand two's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op2Mode, op2Gpr);
          op2Address = operands[0];
          op2Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }
 
          //Perform the operation.
          op1Value = op2Value;

          
          //Place the result in the appropriate memory type based on the mode 
          //of the first operand.
          if(op1Mode == 1)
          {
            HYPO.gpr[op1Gpr] = op1Value;
          }
          else if(op1Mode == 6)
          {
            System.out.println("Destination operand mode cannot be immediate mode.");
            HYPO.outputToFile += "Destination operand mode cannot be immediate mode.\n";
            return ErrorImmediateModeNotAllowed;
          }
          else
          {
            HYPO.memory[op1Address] = op1Value;
          }
         
          //Add 2 to the execution time.
          HYPO.clock = HYPO.clock + 2;
          break;

        //Branch opcode
        case 6:
          
          //Check if address to jump to is valid. If not display an error message.
          if((HYPO.memory[pc] < 0) || (HYPO.memory[pc] > 5500))
          {
            System.out.println("The address to branch to is invalid.");
            HYPO.outputToFile += "The address to branch to is invalid.";
            return ErrorInvalidBranchToAddress;
          }

          //Set the pc to the next word in the instruction
          HYPO.pc = HYPO.memory[pc];
 
          //Add 2 to the execution time
          HYPO.clock = HYPO.clock + 2;
          break;

        //BranchOnMinus opcode
        case 7:
        
          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }

          //If operand one is less than zero than set the pc to the next
          //word in the instruction.
          if(op1Value < 0)
          {
       
            //Check if address to jump to is valid. If not display an error message.
            if((HYPO.memory[pc] < 0) || (HYPO.memory[pc] > 5500))
            {
              System.out.println("The address to branch to is invalid.");
              HYPO.outputToFile += "The address to branch to is invalid.";
              return ErrorInvalidBranchToAddress;
            }
            HYPO.pc = HYPO.memory[HYPO.pc];
          }
          else
          {
            HYPO.pc++;
          }
         
          //Add 4 to the execution time.
          HYPO.clock = HYPO.clock + 4;
          break;

        //BranchOnPlus opcode
        case 8:

          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          } 

          //If the value of the first operand is greater than zero 
          //then set the pc to the next word in the instruction.
          if(op1Value > 0)
          { 
            
            //Check if address to jump to is valid. If not display an error message.
            if((HYPO.memory[pc] < 0) || (HYPO.memory[pc] > 5500))
            {
              System.out.println("The address to branch to is invalid.");
              HYPO.outputToFile += "The address to branch to is invalid.";
              return ErrorInvalidBranchToAddress;
            }
            HYPO.pc = HYPO.memory[HYPO.pc];
          }
          else
          {
            HYPO.pc++;
          }
         
          //Add 4 to the execution time.
          HYPO.clock = HYPO.clock + 4;
          break;
  
        //BranchOnZero
        case 9:
   
          //Fetch operand one's address and value based on mode and gpr.
          operands = HYPO.fetchOperand(op1Mode, op1Gpr);
          op1Address = operands[0];
          op1Value = operands[1];
          status = operands[2];
          if(status < 0)
          {
            return status;
          }

          //If operand one is zero than set the pc to the next word in 
          //the instruction.
          if(op1Value == 0)
          {
            //Check if address to jump to is valid. If not display an error message.
            if((HYPO.memory[pc] < 0) || (HYPO.memory[pc] > 5500))
            {
              System.out.println("The address to branch to is invalid.");
              HYPO.outputToFile += "The address to branch to is invalid.";
              return ErrorInvalidBranchToAddress;
            }
            HYPO.pc = HYPO.memory[HYPO.pc];
          }
          else
          {
            HYPO.pc++;
          }
         
          //Add 4 to the execution time.
          HYPO.clock = HYPO.clock + 4;
          break;
        default:
          System.out.println("Invalid opcode.");
          HYPO.outputToFile += "Invalid opcode.";
          status = ErrorInvalidOpcode;
          break;
      }
    }
    return status;
  }

  // ******************************************************
  // Function: fetchOperand()
  //
  // Task Description:
  //   Fetches the value of the operand based on the opMode
  //   and the register if applicable. If the value does 
  //   not reside in a register than the address where the 
  //   value is stored is returned
  // Input parameters
  //   int opMode                   - mode of the operand
  //   int opReg		    - register where 
  //                                  operand is if applicable
  // Output parameters
  //   None.
  // Function return values
  //   Array is returned with three values:
  //   status
  //     OK                           - no errors
  //     ErrorInvalidAddress          - invalid address
  //     ErrorInvalidMode             - invalid mode
  //   int opAddress                  - address where the 
  //                                    operand is
  //   int opValue                    - value of operand
  // ******************************************************

  public static int[] fetchOperand(int opMode, int opReg) 
  {
    int opAddress = 0, opValue = 0, status = OK;
    int[] operands = new int[3];
  
    //Determine operand value and address based on opMode.
    switch(opMode)
    {
      
      //Register mode
      case 1:
  
        //Address value is not applicable
        opAddress = -1;

        //Operand value is in the specified register.
        opValue = HYPO.gpr[opReg];
        break;

      //Register deferred mode
      case 2:

        //The register contains the address of the operand.
        opAddress = HYPO.gpr[opReg];
        
        //If the address is in the correct range then set the
        //operand value to the address content.Else the 
        //address is invalid and display an error message.
        if((opAddress >= 0) && (opAddress <= 5499))
        {
          opValue = HYPO.memory[opAddress];
        }
        else
        {
          System.out.println("Invalid address when fetching value of operand.");
          HYPO.outputToFile += "Invalid address when fetching value of operand.";
          status = ErrorInvalidMemoryAddress;
        }
        break;

      //Autoincrement mode
      case 3:
 
        //The register contains the address of the operand.
        opAddress = HYPO.gpr[opReg];

        //If the address is in the correct range then set the 
        //value of the operand to the address content. Else
        //the address is invalid and display an error.
        if((opAddress >= 0) && (opAddress <= 5499))
        {
          opValue = HYPO.memory[opAddress];
        }
        else
        {
          System.out.println("Invalid address when fetching value of operand.");
          HYPO.outputToFile += "Invalid address when fetching value of operand.";
          status = ErrorInvalidMemoryAddress;
        }
        
        //Increment the register content.
        HYPO.gpr[opReg]++;
        break;

      //Autodecrement mode
      case 4:

        //Register content is decremented by one.
        --HYPO.gpr[opReg];

        //The decremented value is the address of the operand.
        opAddress = HYPO.gpr[opReg];

        //If the address is in the correct range then set the
        //value of the operand to the address content. 
        //Else the address is invalid and display an error.
        if((opAddress >= 0) && (opAddress <= 5499))
        {
          opValue = HYPO.memory[opAddress];
        }
        else
        {
          System.out.println("Invalid address when fetching value of operand.");
          HYPO.outputToFile += "Invalid address when fetching value of operand.";
          status = ErrorInvalidMemoryAddress;
        }
        break;

      //Direct mode
      case 5:
 
        //The next word contains the address of the operand.
        opAddress = HYPO.memory[HYPO.pc++];

        //If the address is in the correct range then set the
        //value of the operand to the address content.
        //Else the address is invalid and display an error.
        if((opAddress >= 0) && (opAddress <= 5499))
        {
          opValue = HYPO.memory[opAddress];
        }
        else
        {
          System.out.println("Invalid address when fetching value of operand.");
          HYPO.outputToFile += "Invalid address when fetching value of operand.";
          status = ErrorInvalidMemoryAddress;
        }
        break;
      
      //Immediate mode
      case 6:
        
        //Address value is not applicable.
        opAddress = -1;

        //Next word in instruction contains the value of the operand.
        opValue = HYPO.memory[HYPO.pc++];
        break;
 
      //Invalid mode
      default:
        System.out.println("Invalid mode.");
        HYPO.outputToFile += "Invalid mode.";
        status = ErrorInvalidMode;
        break;
    }
    operands[0] = opAddress;
    operands[1] = opValue;
    operands[2] = status;
    return operands;
  }

  
  // ******************************************************
  // Function: dumpMemory()
  //
  // Task Description:
  //  Displays the contents of the registers, the program
  //  counter, the stack pointer, the values in memory up
  //  to a given address, the value of the clock, and the
  //  program status register. The method also displays a
  //  a given string before the content is displayed.
  // Input parameters
  //   String string                  - string to be displayed
  //   int startAddress		      - first address to be 
  //                                    displayed
  //   int size                       - number of addresses 
  //                                    to be displayed
  // Output parameters
  //   None.
  // Function return values
  //   OK			      - no errors;
  //   ErrorInvalidStartAddr          - invalid start address
  //   ErrorInvalidSize               - invalid range size
  // ******************************************************

  public static int dumpMemory(String string, int startAddress, int size)
  {

    //Display given string
    HYPO.outputToFile += (string);
    System.out.print(string);
  
    //Check if start address value is valid. If not display
    //an error message.
    if((startAddress < 0) || (startAddress > 1499))
    {
      System.out.println("Invalid start address.");
      HYPO.outputToFile += "Invalid start address.\n";
      return ErrorInvalidStartAddr;
    }

    //Create an end address. Check is the end address is
    //in the valid range. If not display an error message.
    int endAddress = (startAddress + size) + 1;
    if((endAddress < 0) || (endAddress > 1499))
    {
      System.out.println("Invalid size of address range.");
      HYPO.outputToFile += "Invalid size of address range.\n";
      return ErrorInvalidSize;
    } 
    
    //Output proper formatting, and gpr values.
    HYPO.outputToFile += ("GPRs:" + "\t\t");
    System.out.print("GPRs:" + "\t\t");
    for(int i = 0; i < 8; i++)
    {
      System.out.print(HYPO.gpr[i] + "\t");
      HYPO.outputToFile += (HYPO.gpr[i] + "\t");
    }
    System.out.println(HYPO.sp + "\t" + HYPO.pc);
    HYPO.outputToFile += HYPO.sp + "\t" + HYPO.pc + "\n";
    System.out.println("Address\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9");
    HYPO.outputToFile += "Address\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9\n";
  
    //Loop through the addresses given and display their contents.
    //Display the address where each line begins.
    int address = startAddress;
    while(address < endAddress)
    {
      HYPO.outputToFile += (address + "\t");
      System.out.print(address + "\t");
      for(int i = 0; i < 10; i++)
      {
        if(address < endAddress)
        {
          System.out.print("\t" + HYPO.memory[address]);
          HYPO.outputToFile += ("\t" + HYPO.memory[address]);
          address++;
        }
        else
        {
          break;
        }
          
      }  
      System.out.println();
      HYPO.outputToFile += "\n";
    }

    //Display the clock and psr values.
    HYPO.outputToFile += ("Clock: " + HYPO.clock + "\n");
    HYPO.outputToFile += ("PSR: " + HYPO.psr + "\n");
    return OK;
  }
}