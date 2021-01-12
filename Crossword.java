/********************************************************************
 * COMPILATION: javac Crossword.java
 * EXECUTION:   java Crossword dictionary_file.txt crossword_board.txt
 *******************************************************************/

import java.io.*;
import java.util.*;

public class Crossword
{
	static Cell[][] board;
	static TrieSTNew<String> trie;
	static int boardSize;
	static int numOfSolutions;
	
	public static void main(String [] args) throws IOException
	{
		long startTime = System.nanoTime();
		
		scanBoard(args[1]);	// scans the crossword from command line
							// and generates the board structure
		scanDict(args[0]);	// scans the dictionary from command line
							// and generates the trie data structure
							
		run();		// starts algorithm and finds all solutions for
					// the given crossword board
					
		System.out.println("Found "+numOfSolutions+" solutions");
		long endTime = System.nanoTime();
		System.out.println("Executed in "+(endTime-startTime)+" nanoseconds");
		
	}
	
	// prints the current state of the board
	public static void printBoard()
	{
		for (int i = 0; i < boardSize; i++) {
			for (int j = 0; j < boardSize; j++) {
				System.out.print(board[i][j].letter);
			}
			System.out.println("");
		}
	}
	
	// initiates the recursive algorithm
	public static void run()
	{
		trySolution(0);
	}
	
	// pruning recursive algorithm that finds all possible solutions to
	// a given crossword board with a given dictionary
	public static void trySolution(int space)
	{
		String prefix;
		boolean valid;
		
		// if the function recurses past all possible cells,
		// then it has found a solution! (base case)
		if (space >= (boardSize*boardSize)) {
			if (numOfSolutions % 10000 == 0) {
				System.out.println("Solution "+numOfSolutions);
				printBoard();
			}
			numOfSolutions++;
		}
		
		// if the board is not entirely valid, then recurse to try new
		// letters and check validity of other cells (recursive case)
		else {
			
			// calculates coordinates of the current cell
			int i = space / boardSize;
			int j = space % boardSize;
			
			// open squares ('+'): iterates through all possible letters
			// and then check validity of related prefixes
			if (board[i][j].letter == '+') {
				for(char ch = 'a'; ch <= 'z'; ch++) {
					// pick a letter
					board[i][j].letter = ch;
					
					// assess horizontal standings
					prefix = makePrefix(i, j, true);
					board[i][j].type = trie.searchPrefix(prefix);
					if (!checkValidity(i,j,true))
						continue;
					
					// assess vertical standings
					prefix = makePrefix(i, j, false);
					board[i][j].type = trie.searchPrefix(prefix);
					if (!checkValidity(i,j,false))
						continue;
					
					// if valid: go to the next cell
					trySolution(space+1);
				}
				board[i][j].letter = '+';
			}
			
			// filled-in squares ('-'): ...do nothing
			else if (board[i][j].letter == '-') {
				trySolution(space+1);
			}
			
			// letter squares ('a'..'z'): check validity of related prefixes
			else {
				// assess horizontal standings
				prefix = makePrefix(i, j, true);
				board[i][j].type = trie.searchPrefix(prefix);
				if (!checkValidity(i,j,true))
					return;
				
				// assess vertical standings
				prefix = makePrefix(i, j, false);
				board[i][j].type = trie.searchPrefix(prefix);
				if (!checkValidity(i,j,false))
					return;
				
				// if valid: go to the next cell
				trySolution(space+1);
			}
		}
	}
	
	// returns true if the cell contains a valid prefix given
	// their type and surrounding cells
		/* 0 = is not word, is not prefix
		 * 1 = is not word, is prefix
		 * 2 = is word, is not prefix
		 * 3 = is word, is prefix */
	public static boolean checkValidity(int i, int j, boolean horizontal)
	{
		// type 0 is never valid, type 3 is always valid
		if (board[i][j].type == 0)
			return false;
		else if (board[i][j].type == 3)
			return true;
		
		// horizontal cases
		else if (horizontal) {
			// if it's not a word and next cell is blank '-': return
			if (board[i][j].type < 2 &&
					(j == (boardSize - 1) || board[i][j+1].letter == '-'))
				return false;
			// if it's not a prefix and not next to a '-': return
			else if ((board[i][j].type % 2 == 0) &&
					(j < (boardSize - 1) && board[i][j+1].letter != '-'))
				return false;
		}
		
		// vertical cases
		else {
			// if it's not a word and upper cell is blank '-': return
			if (board[i][j].type < 2 &&
					(i == (boardSize - 1) || board[i+1][j].letter == '-'))
				return false;
			// if it's not a prefix and not above to a '-': return
			else if ((board[i][j].type % 2 == 0) &&
					(i < (boardSize - 1) && board[i+1][j].letter != '-'))
				return false;
		}
		
		// is valid if passes all prior tests
		return true;
	}
	
	// this builds the prefix in reverse order; stopping
	// when the start of the horizontal word has been reached
	public static String makePrefix(int i, int j, boolean horizontal)
	{
		String prefix = "";
		
		// horizontal case
		if (horizontal) {
			for (int x = j; x >= 0; x--) {
				if (board[i][x].letter == '-')
					break;
				prefix = ""+board[i][x].letter+prefix;
				if (board[i][x].headOfHWord == board[i][j])
					break;
			}
		}
		
		// vertical case
		else {
			for (int y = i; y >= 0; y--) {
				if (board[y][j].letter == '-')
					break;
				prefix = ""+board[y][j].letter+prefix;
				if (board[y][j].headOfVWord == board[i][j])
					break;
			}
		}
		
		return prefix;
	}
	
	// scans the crossword board given at command line and generates the
	// board with Cell data structure
	public static void scanBoard(String arg) throws IOException
	{
		Scanner boardScan = new Scanner(new FileInputStream(arg));
		boardSize = boardScan.nextInt();	// first char of file is board size
		
		String boardRow;
		char cellData;
		board = new Cell[boardSize][boardSize];
		
		// reads a single row from the board text file, then parses
		// the individual characters, creating each their own board cell
		for (int i = 0; i < boardSize; i++) {
			boardRow = boardScan.next();
			for (int j = 0; j < boardSize; j++) {
				cellData = boardRow.charAt(j);
				board[i][j] = new Cell();
				board[i][j].letter = cellData;
			}
		}
		
		// references for each cell the start of its horizontal word and
		// the start of its vertical word
		for (int space = 0; space < boardSize*boardSize; space++)
		{
			// calculates coordinates of the current cell
			int i = space / boardSize;
			int j = space % boardSize;
			
			// if is leftmost cell or cell to left is filled-in,
			// it is the start of its horizontal word
			if (j == 0 || board[i][j-1].letter == '-')
				board[i][j].headOfHWord = board[i][j];
			else
				board[i][j].headOfHWord = board[i][j-1].headOfHWord;
			
			// if is upmost cell or cell above is filled-in,
			// it is the start of its vetical word
			if (i == 0 || board[i-1][j].letter == '-')
				board[i][j].headOfVWord = board[i][j];
			else
				board[i][j].headOfVWord = board[i-1][j].headOfVWord;
		}
	}
	
	// scans the dictionary given at command line and generates the
	// trie data structure
	public static void scanDict(String arg) throws IOException
	{
		Scanner dictScan = new Scanner(new FileInputStream(arg));
		
		trie = new TrieSTNew<String>();
		String st;
		
		// inserts each word of the dictionary into the trie data structure
		while (dictScan.hasNext()) {
			st = dictScan.nextLine();
			
			/************************************************************
			 * EXTRA CREDIT
			 * my main program will prune the words in the dictionary
			 * if they are too large for the size of the board; my
			 * less-optimized version will forgo this step. I suspect
			 * the lack of this next step will increase the runtime
			 * as the program will iterate through more possible prefixes
			 * in the trie that will not lead to solutions.
			 ************************************************************/
			
			// prunes words too large for board
			if (st.length() <= boardSize)
				trie.put(st, st);
		}
	}
	
	// data structure for each "square" on the crossword board
	private static class Cell
	{
		public int type;
		public char letter;
		public Cell headOfHWord;	// cell that starts word horizontally
		public Cell headOfVWord;	// cell that starts word vertically
	}
}



