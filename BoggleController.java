/*
*	
*	Back-end gameplay manager for text-based Boggle.
*
*	Copyright (c) 2016 Austin Marcus.
*	All rights reserved.
*
*	This file is part of CommandLineBoggle,
*	and is made available under the terms of
*	the GNU Public License (see LICENSE).
*/

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class BoggleController{
	private BoggleModel model;
	private String board;
	private ArrayList<String> dictionary;
	private ArrayList<String> usedGuesses;
	private ArrayList<String> highScores;
	private String[] highScoresArr;
	private String[] dictionaryArr;
	private int totalScore;
	//the least offensively-long way to hard-code letter point values; still moderately horrible
	private static final Object[][][] pointValues = {
														{{'A', 'E', 'I', 'L', 'N', 'O', 'R', 'S', 'T', 'U'}, {1}},
														{{'D', 'G'}, {2}}, 
														{{'B', 'C', 'M', 'P'}, {3}}, 
														{{'F', 'H', 'V', 'W', 'Y'}, {4}}, 
														{{'K'}, {5}}, 
														{{'J', 'X'}, {8}}, 
														{{'Q', 'Z'}, {10}}
													};
													
	public BoggleController(BoggleModel model){
		this.model = model;
		board = model.toString();
		dictionary = new ArrayList<>();
		usedGuesses = new ArrayList<>();
		highScores = new ArrayList<>();
		//load in dictionary of legal words
		try{
			Scanner dictionaryIn = new Scanner(new File("dict.txt"));
			while(dictionaryIn.hasNextLine()){
				dictionary.add(dictionaryIn.nextLine());
			}
			dictionaryIn.close();
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		dictionaryArr = new String[dictionary.size()];
		dictionary.toArray(dictionaryArr);
		//load in high scores, or create high score file if it doesn't exist yet
		try{
			File hiScoresFile = new File("highscores.txt");
			if(!hiScoresFile.exists()){
				PrintWriter hiScoresWriter = new PrintWriter("highscores.txt");
				hiScoresWriter.close();
			}
			else{
				Scanner hiScoresIn = new Scanner(new File("highscores.txt"));
				while(hiScoresIn.hasNextLine()){
					highScores.add(hiScoresIn.nextLine());
				}
				hiScoresIn.close();
			}
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		highScoresArr = new String[highScores.size()];
		highScores.toArray(highScoresArr);
	}
	
	//run through all guess vetting methods
	public int checkGuess(String guess){
		if(isLegalWord(guess) && boardContainsChars(guess) && isValidGuess(guess) && (arrayContains(usedGuesses, guess) == false)){
			usedGuesses.add(guess);
			return getScore(guess);
		}
		else if(arrayContains(usedGuesses, guess)){
			return -2;
		}
		else{
			return -1;
		}
	}
	
	//simple, O(n)-time array searcher that requires no sorting 
	private boolean arrayContains(ArrayList<String> arrayList, String key){
		key.toUpperCase();
		for(String str : arrayList){
			str.toUpperCase();
			if(str.equals(key)){
				return true;
			}
		}
		return false;
	}
	
	//check if guess is contained in the dictionary of acceptable words
	private boolean isLegalWord(String guess){
		if(guess.length() < 3){
			return false;
		}
		else{
			if(Arrays.binarySearch(dictionaryArr, guess, String.CASE_INSENSITIVE_ORDER) >= 0){
				return true;
			}
			else{
				return false;
			}
		}
	}
	
	//helps check for board neighbors based on layout constraints
	private int modIndexDiff(int first, int second){
		return Math.abs(first%4 - second%4);
	}
	
	//check if board contains all characters in guess
	private boolean boardContainsChars(String guess){
		int checkSum = 0;
		for(int i = 0; i < guess.length(); i++){
			for(int j = 0; j < board.length(); j++){
				if(Character.toUpperCase(guess.charAt(i)) == board.charAt(j)){
					checkSum++;
					break;
				}
			}
		}
		if(checkSum == guess.length()){
			return true;
		}
		else{
			return false;
		}
	}
	
	//Find indices of every instance (on board) of each character in guess;
	//find all board-valid pairs of consecutive characters from guess within those indices;
	//determine whether guess can be fully constructed on board by chaining valid pairs,
	//i.e. first character in next pair has same index as second character of last pair, for each pair.
	//Please excuse the fact that time costs associated with this method are horrendous!
	private boolean isValidGuess(String guess){
		int[][] allIndices = getAllIndices(guess);
		int[][] validIndices = new int[guess.length()][];
		ArrayList<Object[]> validPairs = new ArrayList<Object[]>(); 
		ArrayList<Object[]> chainedPairs = new ArrayList<Object[]>();
		
		//will hold count of valid options for each character pair, 
		//to ensure all possible configurations are accounted for.
		int[] validPairCount = new int[guess.length() - 1];
		Arrays.fill(validPairCount, 0);
		
		//find all valid instances on board of two sequential characters in guess
		for(int i = 0; i < allIndices.length - 1; i++){
			for(int j = 0; j < allIndices[i].length; j++){
				for(int k = 0; k < allIndices[i + 1].length; k++){
					if(areBoardNeighbors(allIndices[i][j], allIndices[i + 1][k])){
						validPairCount[i]++;
						validPairs.add(new Object[]{i, guess.charAt(i), guess.charAt(i + 1), allIndices[i][j], allIndices[i + 1][k]});
					}
				}
			}
		}
		
		//quick check to rule out easy cases where one pair of characters is
		//not a valid pair on the board
		for(int i = 0; i < validPairCount.length; i++){
			if(validPairCount[i] == 0){
				return false;
			}
		}
		
		//find & collect all chained pairs 
		for(int i = 0; i < validPairs.size(); i++){ 
			//used to ensure that each pair to be added to chainedPairs
			//can be chained to a both a previous and subsequent character pair
			int checkSum = 0;
			for(int j = 0; j < validPairs.size(); j++){
				if(j == i){
					continue;
				}
				else if(j > i){
					if((int)validPairs.get(j)[0] == (int)validPairs.get(i)[0] + 1 && 
						(char)validPairs.get(j)[1] == (char)validPairs.get(i)[2] && 
						(int)validPairs.get(j)[3] == (int)validPairs.get(i)[4]){
						checkSum++;
					}
				}
				else{
					if((int)validPairs.get(j)[0] == (int)validPairs.get(i)[0] - 1 &&
						(char)validPairs.get(j)[2] == (char)validPairs.get(i)[1] &&
						(int)validPairs.get(j)[4] == (int)validPairs.get(i)[3]){
						checkSum++;
					}
				}
			}
			if(((i > 0 && i != validPairs.size() - 1) && checkSum >= 2) || 
				(i == validPairs.size() - 1 && checkSum > 0) || (i == 0 && checkSum > 0)){
				chainedPairs.add(validPairs.get(i));
			}
		}
		
		//rule out cases where not enough chained pairs exist to
		//construct guess on board
		if(chainedPairs.size() < guess.length() - 1){
			return false;
		}
		
		//finally, check that each pair of characters in the guess is represented in the
		//collection of chained pairs (using a checksum for simplicity)
		int finalCheckSum = 0;
		for(int i = 1; i <= guess.length() - 1; i++){
			for(Object[] pair : chainedPairs){
				if((int)pair[0] == i - 1){
					finalCheckSum++;
				}
			}
		}
		
		if(finalCheckSum >= guess.length() - 1){
			return true;
		}
		else{
			return false;
		}
	}
	
	private boolean areBoardNeighbors(int first, int second){
		if((modIndexDiff(first, second) <= 1) && Math.abs(first - second) <= 5){
			return true;
		}
		else{
			return false;
		}
	}
	
	private int[] getIndicesOf(char charToCount){
		ArrayList<Integer> indices = new ArrayList<>();
		for(int i = 0; i < board.length(); i++){
			if(board.charAt(i) == Character.toUpperCase(charToCount)){
				indices.add(i);
			}
		}
		int[] toReturn = new int[indices.size()];
		for(int i = 0; i < toReturn.length; i++){
			toReturn[i] = indices.get(i).intValue();
		}
		return toReturn;
	}
	
	private int[][] getAllIndices(String guess){
		int[][] indices = new int[guess.length()][];
		for(int i = 0; i < guess.length(); i++){
			indices[i] = getIndicesOf(guess.charAt(i));
		}
		return indices;
	}
	
	//get current guess score
	private int getScore(String guess){
		int guessScore = 0;
		for(int i = 0; i < guess.length(); i++){
			POINTGROUP: for(int j = 0; j < pointValues.length; j++){
				for(int k = 0; k < pointValues[j][0].length; k++){
					if(Character.toUpperCase(guess.charAt(i)) == (char)pointValues[j][0][k]){
						guessScore += (int)pointValues[j][1][0];
						break POINTGROUP;
					}
				}
			}
		}
		totalScore += guessScore;
		return guessScore;
	} 
	
	public int getTotal(){
		return totalScore;
	}
	
	public int isHighScore(int score){
		if(highScoresArr.length == 0){
			return 0;
		}
		else{
			for(int i = 0; i < highScoresArr.length; i++){
				if(score >= getScoreFromLine(highScoresArr[i])){
					return i;
				}
			}
		}
		return -1;
	}
	
	//find appropriate place in high scores file for new high score,
	//insert formatted line with new score
	public void writeHighScore(int score, String initials){
		if(isHighScore(score) < 0){
			return;
		}
		String scoreLine = initials + ": " + score;
		try{
			if(highScores.size() == 0){
				PrintWriter mainFileWriter = new PrintWriter("highscores.txt");
				scoreLine = "1. " + scoreLine;
				mainFileWriter.println(scoreLine);
				mainFileWriter.close();
			}
			else{
				PrintWriter tempFileWriter = new PrintWriter("temp.txt");
				int writeIndex = isHighScore(score);
				scoreLine = (writeIndex + 1) + ". " + scoreLine;
				//merge old and new high scores in new array
				String[] newHighScoresArr = new String[highScoresArr.length + 1];
				for(int i = 0; i < newHighScoresArr.length; i++){
					if(i == writeIndex){
						newHighScoresArr[i] = scoreLine;
					}
					else{
						if(i > writeIndex){
							newHighScoresArr[i] = 
								(i + 1) + highScoresArr[i - 1].substring(highScoresArr[i - 1].indexOf('.'));
						}
						if(writeIndex > 0 && i < writeIndex){
							newHighScoresArr[i] = highScoresArr[i];
						}
					}
				}
				//write new high score array, line-by-line, to temporary file,
				//only keeping 10 highest scores
				for(int i = 0; i < 10; i++){
					if(i >= newHighScoresArr.length){
						break;
					}
					else{
						tempFileWriter.println(newHighScoresArr[i]);
					}
				}
				tempFileWriter.close();
				//re-christen temporary file as new high score file
				File mainFile = new File("highscores.txt");
				mainFile.delete();
				Path tempName = new File("temp.txt").toPath();
				Path newName = new File("highscores.txt").toPath();
				Files.move(tempName, newName, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	//get the score value from a line in the high scores file
	private int getScoreFromLine(String scoreLine){
		return Integer.parseInt(scoreLine.substring(scoreLine.lastIndexOf(" ") + 1));
	}
}
