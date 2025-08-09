package com.example.paaltjesvoetbal.model;
import com.example.paaltjesvoetbal.model.Player;

public class Team {
    private int score;
    private Player player1;
    private Player player2;

    public Team(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }


}