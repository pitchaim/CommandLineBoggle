/*
	Austin Marcus - aim792@pitt.edu
	CS401 - Spring 2016
	Project 5: Boggle
*/

import java.util.Scanner;

public class Boggle {
	private BoggleController controller;
	private BoggleModel model;
	private String board;
	private Scanner input;
	private boolean gameEnded;
	
	public Boggle(){
		model = new BoggleModel();
		controller = new BoggleController(model);
		board = model.toString();
		input = new Scanner(System.in);
	}
	
	public static void main(String[] args){
		Boggle game = new Boggle();
		for(int i = 0; i < game.board.length(); i++){
			if(i > 0 && i%4 == 0){
				System.out.println();
			}
			System.out.print(game.board.charAt(i) + " ");
		}
		System.out.println();
		System.out.println();
		while(!game.gameEnded){
			game.runTurn();
		}
		game.highScoreCheck();
	}
	
	public void runTurn(){
		String guess = input.next();
		if(guess.equals("Q") || guess.equals("q")){
			gameEnded = true;
			return;
		}
		int guessResult = controller.checkGuess(guess);
		if(guessResult == -1){
			System.out.println("Sorry, not a valid guess! Please try again.");
		}
		else if(guessResult == -2){
			System.out.println("Sorry, you've already guessed that word!");
		}
		else{
			System.out.println(guess.toUpperCase() + " scored " + guessResult + " points. Your total score is " + controller.getTotal() + ".");
		}
	}
	
	public void highScoreCheck(){ 
		int totalScore = controller.getTotal();
		int scoreIndex = controller.isHighScore(totalScore);
		if(scoreIndex >= 0){
			System.out.print(totalScore + " is a new high score! Enter your initials: ");
			String initials = input.next();
			controller.writeHighScore(totalScore, initials);
		}
		else{
			System.out.println(totalScore + " is not a high score. Better luck next time!");
		}
	}
}