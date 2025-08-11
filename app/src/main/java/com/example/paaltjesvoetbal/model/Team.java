package com.example.paaltjesvoetbal.model;
import com.example.paaltjesvoetbal.model.Player;

public class Team {
    private int score;
    private Player player1;
    private Player player2;

    private int[] scorePosition = new int[] { 0, 0 };

    public Team(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public void setScorePosition(int x, int y) {
        this.scorePosition[0] = x;
        this.scorePosition[1] = y;
    }

}