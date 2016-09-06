import java.util.Arrays;

public class BoggleModel{
	private char[] board;
	private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	public BoggleModel(){
		board = new char[16];
		for(int i = 0; i < board.length; i++){
			int index = (int)(Math.random()*26);
			board[i] = alphabet.charAt(index);
		}
	}
	
	public String toString(){
		StringBuilder boardString = new StringBuilder();
		for(int i = 0; i < board.length; i++){
			boardString.append(board[i]);
		}
		return boardString.toString();
	}
}